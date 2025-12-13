package com.example.taskmanager.desktop;

import com.example.taskmanager.desktop.DesktopApiClient.TaskDto;
import com.example.taskmanager.desktop.DesktopApiClient.UserDto;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerDateModel;
import javax.swing.SwingWorker;

public class CreateTaskDialog extends JDialog {

    private final DesktopApiClient apiClient;
    private final Long projectId;
    private final String projectName;
    private final JTextField titleField = new JTextField();
    private final JTextField descriptionField = new JTextField();
    private final JComboBox<String> statusField = new JComboBox<>(new String[] { "TODO", "DOING", "DONE" });
    private final JComboBox<String> priorityField = new JComboBox<>(new String[] { "LOW", "MEDIUM", "HIGH" });
    private final JComboBox<UserDto> assigneeField = new JComboBox<>();
    private final SpinnerDateModel dateModel = new SpinnerDateModel();
    private final JSpinner dueDateSpinner = new JSpinner(dateModel);
    private final JCheckBox dueDateEnabled = new JCheckBox("Set due date");
    private final JLabel statusLabel = new JLabel(" ");
    private Consumer<TaskDto> onSuccess;

    public CreateTaskDialog(DesktopApiClient apiClient, Window owner, Long projectId, String projectName) {
        super(owner, "Create Task for " + projectName, ModalityType.APPLICATION_MODAL);
        this.apiClient = apiClient;
        this.projectId = projectId;
        this.projectName = projectName;
        setLayout(new BorderLayout(6, 6));
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        buildUi();
        pack();
        setLocationRelativeTo(owner);
        loadMembers();
    }

    private void buildUi() {
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        form.add(new JLabel("Title"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        form.add(titleField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.weightx = 0;
        form.add(new JLabel("Description"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        form.add(descriptionField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        form.add(new JLabel("Status"), gbc);
        gbc.gridx = 1;
        form.add(statusField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        form.add(new JLabel("Priority"), gbc);
        gbc.gridx = 1;
        form.add(priorityField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        form.add(new JLabel("Assignee"), gbc);
        gbc.gridx = 1;
        assigneeField.setRenderer((list, value, index, isSelected, cellHasFocus) -> {
            JLabel label = new JLabel();
            label.setText(value != null ? value.getUsername() : "Unassigned");
            if (isSelected) {
                label.setOpaque(true);
                label.setBackground(list.getSelectionBackground());
                label.setForeground(list.getSelectionForeground());
            }
            label.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
            return label;
        });
        form.add(assigneeField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        form.add(dueDateEnabled, gbc);
        gbc.gridx = 1;
        dueDateSpinner.setEditor(new JSpinner.DateEditor(dueDateSpinner, "yyyy-MM-dd HH:mm"));
        dueDateSpinner.setEnabled(false);
        dueDateEnabled.addActionListener(e -> dueDateSpinner.setEnabled(dueDateEnabled.isSelected()));
        form.add(dueDateSpinner, gbc);

        JPanel actions = new JPanel();
        JButton cancelBtn = new JButton("Cancel");
        JButton createBtn = new JButton("Create");
        actions.add(cancelBtn);
        actions.add(createBtn);

        cancelBtn.addActionListener(e -> dispose());
        createBtn.addActionListener(e -> submit(createBtn));

        statusLabel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        add(form, BorderLayout.CENTER);
        add(actions, BorderLayout.SOUTH);
        add(statusLabel, BorderLayout.NORTH);
    }

    private void loadMembers() {
        assigneeField.removeAllItems();
        new SwingWorker<List<UserDto>, Void>() {
            @Override
            protected List<UserDto> doInBackground() {
                return apiClient.listProjectMembers(projectId);
            }

            @Override
            protected void done() {
                try {
                    List<UserDto> members = get();
                    assigneeField.addItem(null); // unassigned
                    for (UserDto u : members) {
                        assigneeField.addItem(u);
                    }
                    statusLabel.setText("Members loaded for " + projectName);
                } catch (Exception ex) {
                    statusLabel.setText("Load members failed: " + describeError(ex));
                }
            }
        }.execute();
    }

    private void submit(JButton createBtn) {
        String title = titleField.getText().trim();
        if (title.isEmpty()) {
            statusLabel.setText("Title is required");
            return;
        }
        UserDto assignee = (UserDto) assigneeField.getSelectedItem();
        Long assigneeId = assignee != null ? assignee.getId() : null;

        String dueDateString = null;
        if (dueDateEnabled.isSelected()) {
            Date date = (Date) dueDateSpinner.getValue();
            LocalDateTime dueDate = date != null ? LocalDateTime.ofInstant(Instant.ofEpochMilli(date.getTime()),
                    ZoneId.systemDefault()) : null;
            dueDateString = dueDate != null ? dueDate.toString() : null;
        }

        createBtn.setEnabled(false);
        final String finalDue = dueDateString;
        new SwingWorker<TaskDto, Void>() {
            @Override
            protected TaskDto doInBackground() {
                return apiClient.createTask(projectId, title, descriptionField.getText().trim(),
                        (String) statusField.getSelectedItem(), (String) priorityField.getSelectedItem(),
                        assigneeId, finalDue);
            }

            @Override
            protected void done() {
                createBtn.setEnabled(true);
                try {
                    TaskDto task = get();
                    if (onSuccess != null) {
                        onSuccess.accept(task);
                    }
                    dispose();
                } catch (Exception ex) {
                    statusLabel.setText(describeError(ex));
                }
            }
        }.execute();
    }

    public void setOnSuccess(Runnable runnable) {
        this.onSuccess = task -> runnable.run();
    }

    public void setOnSuccess(Consumer<TaskDto> consumer) {
        this.onSuccess = consumer;
    }

    private String describeError(Exception ex) {
        if (ex instanceof DesktopApiClient.ApiException) {
            DesktopApiClient.ApiException apiEx = (DesktopApiClient.ApiException) ex;
            return "HTTP " + apiEx.getStatus() + " - " + apiEx.getMessage();
        }
        String msg = ex.getMessage();
        return (msg == null || msg.trim().isEmpty()) ? ex.toString() : msg;
    }
}
