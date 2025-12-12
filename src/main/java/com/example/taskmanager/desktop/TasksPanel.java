package com.example.taskmanager.desktop;

import com.example.taskmanager.desktop.DesktopApiClient.ProjectDto;
import com.example.taskmanager.desktop.DesktopApiClient.TaskDto;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingWorker;

public class TasksPanel extends JPanel {

    private final DesktopApiClient apiClient;
    private final DefaultListModel<TaskDto> tasksModel = new DefaultListModel<>();
    private final JList<TaskDto> tasksList = new JList<>(tasksModel);
    private final JLabel projectLabel = new JLabel("No project selected");
    private final JLabel statusLabel = new JLabel(" ");

    private final JTextField titleField = new JTextField();
    private final JTextField descriptionField = new JTextField();
    private final JComboBox<String> statusField = new JComboBox<>(new String[] { "TODO", "DOING", "DONE" });
    private final JComboBox<String> priorityField = new JComboBox<>(new String[] { "LOW", "MEDIUM", "HIGH" });
    private final JTextField assigneeIdField = new JTextField();
    private final JTextField dueDateField = new JTextField();
    private final JComboBox<String> updateStatusField = new JComboBox<>(new String[] { "TODO", "DOING", "DONE" });

    private ProjectDto currentProject;

    public TasksPanel(DesktopApiClient apiClient) {
        this.apiClient = apiClient;
        setLayout(new BorderLayout(8, 8));

        tasksList.setCellRenderer((list, value, index, isSelected, cellHasFocus) -> {
            JLabel label = new JLabel();
            if (value != null) {
                String title = value.getTitle() != null ? value.getTitle() : "(untitled)";
                String status = value.getStatus() != null ? value.getStatus() : "";
                String priority = value.getPriority() != null ? value.getPriority() : "";
                String assignee = value.getAssigneeUsername() != null ? value.getAssigneeUsername() : "Unassigned";
                label.setText("<html><b>" + title + "</b> [" + status + "] (" + priority + ")<br/>Assignee: "
                        + assignee + "</html>");
            }
            if (isSelected) {
                label.setOpaque(true);
                label.setBackground(list.getSelectionBackground());
                label.setForeground(list.getSelectionForeground());
            }
            label.setBorder(javax.swing.BorderFactory.createEmptyBorder(4, 4, 4, 4));
            return label;
        });

        add(buildHeader(), BorderLayout.NORTH);
        add(new JScrollPane(tasksList), BorderLayout.CENTER);
        add(buildForm(), BorderLayout.SOUTH);
    }

    private JPanel buildHeader() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(projectLabel, BorderLayout.WEST);
        statusLabel.setBorder(javax.swing.BorderFactory.createEmptyBorder(4, 4, 4, 4));
        panel.add(statusLabel, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildForm() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;
        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel("Title"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(titleField, gbc);

        gbc.gridx = 0;
        gbc.gridy = ++row;
        gbc.weightx = 0;
        panel.add(new JLabel("Description"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(descriptionField, gbc);

        gbc.gridx = 0;
        gbc.gridy = ++row;
        gbc.weightx = 0;
        panel.add(new JLabel("Status"), gbc);
        gbc.gridx = 1;
        panel.add(statusField, gbc);

        gbc.gridx = 0;
        gbc.gridy = ++row;
        panel.add(new JLabel("Priority"), gbc);
        gbc.gridx = 1;
        panel.add(priorityField, gbc);

        gbc.gridx = 0;
        gbc.gridy = ++row;
        panel.add(new JLabel("Assignee ID"), gbc);
        gbc.gridx = 1;
        panel.add(assigneeIdField, gbc);

        gbc.gridx = 0;
        gbc.gridy = ++row;
        panel.add(new JLabel("Due Date"), gbc);
        gbc.gridx = 1;
        panel.add(dueDateField, gbc);

        JButton createButton = new JButton("Create Task");
        JButton refreshButton = new JButton("Refresh");
        JButton updateButton = new JButton("Update Status");
        JButton deleteButton = new JButton("Delete Task");

        gbc.gridx = 0;
        gbc.gridy = ++row;
        panel.add(new JLabel("Set status for selected"), gbc);
        gbc.gridx = 1;
        panel.add(updateStatusField, gbc);

        gbc.gridx = 1;
        gbc.gridy = ++row;
        gbc.anchor = GridBagConstraints.EAST;
        JPanel actions = new JPanel();
        actions.add(refreshButton);
        actions.add(updateButton);
        actions.add(deleteButton);
        actions.add(createButton);
        panel.add(actions, gbc);

        createButton.addActionListener(e -> createTask(createButton));
        refreshButton.addActionListener(e -> reloadTasks());
        updateButton.addActionListener(e -> updateSelectedTaskStatus());
        deleteButton.addActionListener(e -> deleteSelectedTask());

        return panel;
    }

    public void setCurrentProject(ProjectDto project) {
        this.currentProject = project;
        if (project == null) {
            projectLabel.setText("No project selected");
            tasksModel.clear();
            return;
        }
        projectLabel.setText("Project: " + project.getName());
        reloadTasks();
    }

    private void reloadTasks() {
        if (currentProject == null) {
            statusLabel.setText("Select a project first");
            return;
        }
        new SwingWorker<List<TaskDto>, Void>() {
            @Override
            protected List<TaskDto> doInBackground() {
                return apiClient.listTasks(currentProject.getId());
            }

            @Override
            protected void done() {
                try {
                    List<TaskDto> tasks = get();
                    tasksModel.clear();
                    for (TaskDto t : tasks) {
                        tasksModel.addElement(t);
                    }
                    statusLabel.setText("Loaded " + tasks.size() + " tasks");
                } catch (Exception ex) {
                    statusLabel.setText("Load failed: " + describeError(ex));
                }
            }
        }.execute();
    }

    private void createTask(JButton createButton) {
        if (currentProject == null) {
            statusLabel.setText("Select a project first");
            return;
        }
        createButton.setEnabled(false);
        new SwingWorker<TaskDto, Void>() {
            @Override
            protected TaskDto doInBackground() {
                Long assigneeId = null;
                if (!assigneeIdField.getText().trim().isEmpty()) {
                    try {
                        assigneeId = Long.parseLong(assigneeIdField.getText().trim());
                    } catch (NumberFormatException ignored) {
                        throw new IllegalArgumentException("Assignee ID must be a number");
                    }
                }
                return apiClient.createTask(
                        currentProject.getId(),
                        titleField.getText().trim(),
                        descriptionField.getText().trim(),
                        (String) statusField.getSelectedItem(),
                        (String) priorityField.getSelectedItem(),
                        assigneeId,
                        dueDateField.getText().trim().isEmpty() ? null : dueDateField.getText().trim()
                );
            }

            @Override
            protected void done() {
                createButton.setEnabled(true);
                try {
                    get();
                    titleField.setText("");
                    descriptionField.setText("");
                    assigneeIdField.setText("");
                    dueDateField.setText("");
                    statusLabel.setText("Task created");
                    reloadTasks();
                } catch (Exception ex) {
                    statusLabel.setText("Create failed: " + describeError(ex));
                }
            }
        }.execute();
    }

    private void updateSelectedTaskStatus() {
        TaskDto selected = tasksList.getSelectedValue();
        if (selected == null) {
            statusLabel.setText("Select a task to update");
            return;
        }
        String newStatus = (String) updateStatusField.getSelectedItem();
        new SwingWorker<TaskDto, Void>() {
            @Override
            protected TaskDto doInBackground() {
                return apiClient.updateTaskStatus(selected.getId(), newStatus);
            }

            @Override
            protected void done() {
                try {
                    get();
                    statusLabel.setText("Status updated");
                    reloadTasks();
                } catch (Exception ex) {
                    statusLabel.setText("Update failed: " + describeError(ex));
                }
            }
        }.execute();
    }

    private void deleteSelectedTask() {
        TaskDto selected = tasksList.getSelectedValue();
        if (selected == null) {
            statusLabel.setText("Select a task to delete");
            return;
        }
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                apiClient.deleteTask(selected.getId());
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    statusLabel.setText("Task deleted");
                    reloadTasks();
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
