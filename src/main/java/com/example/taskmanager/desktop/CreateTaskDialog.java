package com.example.taskmanager.desktop;

import com.example.taskmanager.desktop.DesktopApiClient.TaskDto;
import com.example.taskmanager.desktop.DesktopApiClient.UserDto;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
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
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerDateModel;
import javax.swing.SwingWorker;

public class CreateTaskDialog extends JDialog {

    private final DesktopApiClient apiClient;
    private final Long projectId;
    private final String projectName;
    private final JTextField titleField = new JTextField();
    private final JTextArea descriptionField = new JTextArea(4, 30);
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
        setLayout(new BorderLayout(8, 8));
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setPreferredSize(new Dimension(520, 520));
        titleField.setColumns(30);
        descriptionField.setColumns(30);
        buildUi();
        pack();
        setLocationRelativeTo(owner);
        loadMembers();
    }

    private void buildUi() {
        JPanel main = new JPanel();
        main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));
        main.add(buildDetailsPanel());
        main.add(buildActionsPanel());

        statusLabel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        add(main, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);
    }

    private JPanel buildDetailsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Task Details"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Title"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(titleField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.weightx = 0;
        panel.add(new JLabel("Description"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        descriptionField.setLineWrap(true);
        descriptionField.setWrapStyleWord(true);
        panel.add(new JScrollPane(descriptionField), gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Status"), gbc);
        gbc.gridx = 1;
        panel.add(statusField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Priority"), gbc);
        gbc.gridx = 1;
        panel.add(priorityField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Assignee"), gbc);
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
        assigneeField.setEnabled(false);
        panel.add(assigneeField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(dueDateEnabled, gbc);
        gbc.gridx = 1;
        dueDateSpinner.setEditor(new JSpinner.DateEditor(dueDateSpinner, "yyyy-MM-dd HH:mm"));
        dueDateSpinner.setEnabled(false);
        dueDateEnabled.addActionListener(e -> dueDateSpinner.setEnabled(dueDateEnabled.isSelected()));
        panel.add(dueDateSpinner, gbc);

        return panel;
    }

    private JPanel buildActionsPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton createBtn = new JButton("Create Task");
        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> dispose());
        createBtn.addActionListener(e -> submit(createBtn));
        panel.add(createBtn);
        panel.add(cancelBtn);
        return panel;
    }

    private void loadMembers() {
        assigneeField.removeAllItems();
        assigneeField.setEnabled(false);
        new SwingWorker<List<DesktopApiClient.MemberDto>, Void>() {
            @Override
            protected List<DesktopApiClient.MemberDto> doInBackground() {
                return apiClient.listProjectMembers(projectId);
            }

            @Override
            protected void done() {
                try {
                    List<DesktopApiClient.MemberDto> members = get();
                    assigneeField.addItem(null); // unassigned
                    for (DesktopApiClient.MemberDto m : members) {
                        UserDto user = new UserDto();
                        user.setId(m.getUserId());
                        user.setUsername(m.getUsername());
                        assigneeField.addItem(user);
                    }
                    assigneeField.setEnabled(true);
                    statusLabel.setText("Members loaded for " + projectName);
                } catch (Exception ex) {
                    statusLabel.setText("Load members failed: " + describeError(ex));
                    assigneeField.setEnabled(false);
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
