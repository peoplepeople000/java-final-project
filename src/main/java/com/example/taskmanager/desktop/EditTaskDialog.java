package com.example.taskmanager.desktop;

import com.example.taskmanager.desktop.DesktopApiClient.MemberDto;
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
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerDateModel;
import javax.swing.SwingWorker;
import javax.swing.border.TitledBorder;

public class EditTaskDialog extends JDialog {

    private final DesktopApiClient apiClient;
    private final Long projectId;
    private final TaskDto task;
    private final Runnable onUpdatedOrDeleted;

    private final JTextField titleField = new JTextField();
    private final JTextArea descArea = new JTextArea(4, 30);
    private final JComboBox<String> statusCombo = new JComboBox<>(new String[] { "TODO", "DOING", "DONE" });
    private final JComboBox<String> priorityCombo = new JComboBox<>(new String[] { "LOW", "MEDIUM", "HIGH" });
    private final JComboBox<UserDto> assigneeCombo = new JComboBox<>();
    private final JCheckBox dueDateCheck = new JCheckBox("Set due date");
    private final SpinnerDateModel dateModel = new SpinnerDateModel();
    private final JSpinner dueDateSpinner = new JSpinner(dateModel);
    private final JButton saveButton = new JButton("Save");
    private final JButton deleteButton = new JButton("Delete Task");
    private final JLabel statusLabel = new JLabel(" ");

    public EditTaskDialog(Window owner, DesktopApiClient apiClient, Long projectId, TaskDto task,
            Runnable onUpdatedOrDeleted) {
        super(owner, "Edit Task - " + task.getTitle(), ModalityType.APPLICATION_MODAL);
        this.apiClient = apiClient;
        this.projectId = projectId;
        this.task = task;
        this.onUpdatedOrDeleted = onUpdatedOrDeleted;
        setLayout(new BorderLayout(8, 8));
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setPreferredSize(new Dimension(520, 520));
        titleField.setColumns(30);
        descArea.setColumns(30);
        buildUi();
        pack();
        setLocationRelativeTo(owner);
        loadMembers();
    }

    private void buildUi() {
        JPanel main = new JPanel();
        main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));
        main.add(buildDetailsPanel());
        main.add(buildDangerPanel());

        statusLabel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        add(main, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);
    }

    private JPanel buildDetailsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new TitledBorder("Task Details"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Title"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        titleField.setText(task.getTitle());
        panel.add(titleField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.weightx = 0;
        panel.add(new JLabel("Description"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        descArea.setLineWrap(true);
        descArea.setWrapStyleWord(true);
        descArea.setText(task.getDescription());
        panel.add(new JScrollPane(descArea), gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Status"), gbc);
        gbc.gridx = 1;
        statusCombo.setSelectedItem(task.getStatus() != null ? task.getStatus().toUpperCase() : "TODO");
        panel.add(statusCombo, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Priority"), gbc);
        gbc.gridx = 1;
        priorityCombo.setSelectedItem(task.getPriority() != null ? task.getPriority().toUpperCase() : "MEDIUM");
        panel.add(priorityCombo, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Assignee"), gbc);
        gbc.gridx = 1;
        assigneeCombo.setRenderer((list, value, index, isSelected, cellHasFocus) -> {
            JLabel label = new JLabel();
            label.setText(value != null ? value.getUsername() : "(Unassigned)");
            if (isSelected) {
                label.setOpaque(true);
                label.setBackground(list.getSelectionBackground());
                label.setForeground(list.getSelectionForeground());
            }
            label.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
            return label;
        });
        assigneeCombo.setEnabled(false);
        panel.add(assigneeCombo, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(dueDateCheck, gbc);
        gbc.gridx = 1;
        dueDateSpinner.setEditor(new JSpinner.DateEditor(dueDateSpinner, "yyyy-MM-dd HH:mm"));
        dueDateSpinner.setEnabled(false);
        dueDateCheck.addActionListener(e -> dueDateSpinner.setEnabled(dueDateCheck.isSelected()));
        if (task.getDueDate() != null) {
            try {
                LocalDateTime ldt = LocalDateTime.parse(task.getDueDate());
                dueDateSpinner.setValue(Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant()));
                dueDateCheck.setSelected(true);
                dueDateSpinner.setEnabled(true);
            } catch (Exception ignored) {
                // leave unchecked
            }
        }
        panel.add(dueDateSpinner, gbc);

        gbc.gridx = 1;
        gbc.gridy++;
        gbc.anchor = GridBagConstraints.EAST;
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actions.add(saveButton);
        saveButton.addActionListener(e -> saveTask());
        panel.add(actions, gbc);

        return panel;
    }

    private JPanel buildDangerPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBorder(new TitledBorder("Danger Zone"));
        deleteButton.addActionListener(e -> deleteTask());
        panel.add(deleteButton);
        return panel;
    }

    private void loadMembers() {
        assigneeCombo.setEnabled(false);
        new SwingWorker<List<MemberDto>, Void>() {
            @Override
            protected List<MemberDto> doInBackground() {
                return apiClient.listProjectMembers(projectId);
            }

            @Override
            protected void done() {
                try {
                    List<MemberDto> members = get();
                    DefaultComboBoxModel<UserDto> model = new DefaultComboBoxModel<>();
                    model.addElement(null);
                    Long currentAssigneeId = task.getAssigneeId();
                    for (MemberDto m : members) {
                        UserDto u = new UserDto();
                        u.setId(m.getUserId());
                        u.setUsername(m.getUsername());
                        model.addElement(u);
                    }
                    assigneeCombo.setModel(model);
                    // preselect assignee
                    if (currentAssigneeId != null) {
                        for (int i = 0; i < model.getSize(); i++) {
                            UserDto u = model.getElementAt(i);
                            if (u != null && currentAssigneeId.equals(u.getId())) {
                                assigneeCombo.setSelectedIndex(i);
                                break;
                            }
                        }
                    } else {
                        assigneeCombo.setSelectedIndex(0);
                    }
                    assigneeCombo.setEnabled(true);
                    statusLabel.setText("Members loaded");
                } catch (Exception ex) {
                    statusLabel.setText("Load members failed: " + describeError(ex));
                    assigneeCombo.setEnabled(false);
                }
            }
        }.execute();
    }

    private void saveTask() {
        String title = titleField.getText().trim();
        if (title.isEmpty()) {
            statusLabel.setText("Title is required");
            return;
        }
        final String description = descArea.getText();
        final String status = (String) statusCombo.getSelectedItem();
        final String priority = (String) priorityCombo.getSelectedItem();
        UserDto assignee = (UserDto) assigneeCombo.getSelectedItem();
        final Long assigneeId = assignee != null ? assignee.getId() : null;
        String dueDateString = null;
        if (dueDateCheck.isSelected()) {
            Date date = (Date) dueDateSpinner.getValue();
            if (date != null) {
                LocalDateTime ldt = LocalDateTime.ofInstant(Instant.ofEpochMilli(date.getTime()),
                        ZoneId.systemDefault());
                dueDateString = ldt.toString();
            }
        }
        final String dueDateFinal = dueDateString;
        final String titleFinal = title;
        saveButton.setEnabled(false);
        new SwingWorker<TaskDto, Void>() {
            @Override
            protected TaskDto doInBackground() {
                return apiClient.updateTask(task.getId(), titleFinal, description, status, priority, assigneeId,
                        dueDateFinal);
            }

            @Override
            protected void done() {
                saveButton.setEnabled(true);
                try {
                    get();
                    statusLabel.setText("Saved");
                    if (onUpdatedOrDeleted != null) {
                        onUpdatedOrDeleted.run();
                    }
                } catch (Exception ex) {
                    statusLabel.setText("Save failed: " + describeError(ex));
                }
            }
        }.execute();
    }

    private void deleteTask() {
        int confirm = JOptionPane.showConfirmDialog(this, "Delete this task?", "Confirm",
                JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        deleteButton.setEnabled(false);
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                apiClient.deleteTask(task.getId());
                return null;
            }

            @Override
            protected void done() {
                deleteButton.setEnabled(true);
                try {
                    get();
                    if (onUpdatedOrDeleted != null) {
                        onUpdatedOrDeleted.run();
                    }
                    dispose();
                } catch (Exception ex) {
                    statusLabel.setText("Delete failed: " + describeError(ex));
                }
            }
        }.execute();
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
