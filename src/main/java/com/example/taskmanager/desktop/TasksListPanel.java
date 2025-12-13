package com.example.taskmanager.desktop;

import com.example.taskmanager.desktop.DesktopApiClient.ProjectDto;
import com.example.taskmanager.desktop.DesktopApiClient.TaskDto;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingWorker;
import javax.swing.ListSelectionModel;

public class TasksListPanel extends JPanel {

    private final DesktopApiClient apiClient;
    private final DefaultListModel<TaskDto> model = new DefaultListModel<>();
    private final JList<TaskDto> list = new JList<>(model);
    private final JLabel statusLabel = new JLabel(" ");
    private final JLabel projectLabel = new JLabel("No project selected");
    private final JComboBox<String> statusCombo = new JComboBox<>(new String[] { "TODO", "DOING", "DONE" });
    private ProjectDto currentProject;

    public TasksListPanel(DesktopApiClient apiClient) {
        this.apiClient = apiClient;
        setLayout(new BorderLayout(6, 6));
        setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer((l, value, index, isSelected, cellHasFocus) -> {
            JLabel label = new JLabel();
            if (value != null) {
                String title = value.getTitle() != null ? value.getTitle() : "(untitled)";
                String status = value.getStatus() != null ? value.getStatus() : "";
                String priority = value.getPriority() != null ? value.getPriority() : "";
                String assignee = value.getAssigneeUsername() != null ? value.getAssigneeUsername() : "Unassigned";
                String due = value.getDueDate() != null ? value.getDueDate() : "";
                label.setText("<html><b>" + title + "</b> [" + status + "] (" + priority + ")<br/>Assignee: "
                        + assignee + (due.isEmpty() ? "" : " | Due: " + due) + "</html>");
            }
            if (isSelected) {
                label.setOpaque(true);
                label.setBackground(l.getSelectionBackground());
                label.setForeground(l.getSelectionForeground());
            }
            label.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
            return label;
        });

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton refreshBtn = new JButton("Refresh");
        JButton addBtn = new JButton("+ Task");
        JButton updateBtn = new JButton("Update Status");
        JButton deleteBtn = new JButton("Delete Task");
        toolbar.add(refreshBtn);
        toolbar.add(addBtn);
        toolbar.add(statusCombo);
        toolbar.add(updateBtn);
        toolbar.add(deleteBtn);

        refreshBtn.addActionListener(e -> reloadTasks());
        addBtn.addActionListener(e -> openCreateDialog());
        updateBtn.addActionListener(e -> updateSelected());
        deleteBtn.addActionListener(e -> deleteSelected());

        statusLabel.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(toolbar, BorderLayout.CENTER);
        bottom.add(statusLabel, BorderLayout.SOUTH);

        add(projectLabel, BorderLayout.NORTH);
        add(new JScrollPane(list), BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
    }

    public void setCurrentProject(ProjectDto project) {
        this.currentProject = project;
        if (project == null) {
            projectLabel.setText("No project selected");
            model.clear();
            statusLabel.setText(" ");
        } else {
            projectLabel.setText("Project: " + project.getName());
            reloadTasks();
        }
    }

    private void reloadTasks() {
        if (currentProject == null) {
            statusLabel.setText("Select a project to view tasks");
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
                    model.clear();
                    for (TaskDto t : tasks) {
                        model.addElement(t);
                    }
                    statusLabel.setText("Loaded " + tasks.size() + " tasks");
                } catch (Exception ex) {
                    statusLabel.setText("Load failed: " + describeError(ex));
                }
            }
        }.execute();
    }

    private void openCreateDialog() {
        if (currentProject == null) {
            JOptionPane.showMessageDialog(this, "Select a project first");
            return;
        }
        java.awt.Window owner = javax.swing.SwingUtilities.getWindowAncestor(this);
        CreateTaskDialog dialog = new CreateTaskDialog(apiClient, owner,
                currentProject.getId(), currentProject.getName());
        dialog.setOnSuccess(this::reloadTasks);
        dialog.setVisible(true);
    }

    private void updateSelected() {
        TaskDto selected = list.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Select a task first");
            return;
        }
        String newStatus = (String) statusCombo.getSelectedItem();
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

    private void deleteSelected() {
        TaskDto selected = list.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Select a task first");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this, "Delete selected task?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
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
