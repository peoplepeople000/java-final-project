package com.example.taskmanager.desktop;

import com.example.taskmanager.desktop.DesktopApiClient.ProjectDto;
import com.example.taskmanager.desktop.DesktopApiClient.TaskDto;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.ListSelectionModel;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TasksListPanel extends JPanel {

    private final DesktopApiClient apiClient;
    private final DefaultListModel<TaskDto> todoModel = new DefaultListModel<>();
    private final DefaultListModel<TaskDto> doingModel = new DefaultListModel<>();
    private final DefaultListModel<TaskDto> doneModel = new DefaultListModel<>();
    private final JList<TaskDto> todoList = new JList<>(todoModel);
    private final JList<TaskDto> doingList = new JList<>(doingModel);
    private final JList<TaskDto> doneList = new JList<>(doneModel);
    private final JLabel statusLabel = new JLabel(" ");
    private final JLabel projectLabel = new JLabel("No project selected");
    private ProjectDto currentProject;
    private final Timer refreshTimer;
    private final AtomicBoolean refreshInProgress = new AtomicBoolean(false);
    private final JPopupMenu taskMenu = new JPopupMenu();
    private final JLabel todoHeader = new JLabel("TODO (0)");
    private final JLabel doingHeader = new JLabel("DOING (0)");
    private final JLabel doneHeader = new JLabel("DONE (0)");

    public TasksListPanel(DesktopApiClient apiClient) {
        this.apiClient = apiClient;
        setLayout(new BorderLayout(6, 6));
        setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        configureList(todoList);
        configureList(doingList);
        configureList(doneList);

        JPanel header = new JPanel(new BorderLayout());
        JButton addBtn = new JButton("+ Task");
        addBtn.addActionListener(e -> openCreateDialog());
        header.add(projectLabel, BorderLayout.WEST);
        header.add(addBtn, BorderLayout.EAST);

        JPanel columns = new JPanel(new GridLayout(1, 3, 8, 0));
        columns.add(buildColumn(todoHeader, todoList));
        columns.add(buildColumn(doingHeader, doingList));
        columns.add(buildColumn(doneHeader, doneList));

        statusLabel.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));

        add(header, BorderLayout.NORTH);
        add(columns, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);

        refreshTimer = new Timer(5000, e -> {
            // disabled periodic refresh; left for compatibility
        });
        refreshTimer.setRepeats(true);
    }

    public void setCurrentProject(ProjectDto project) {
        this.currentProject = project;
        if (project == null) {
            projectLabel.setText("No project selected");
            clearLists();
            statusLabel.setText(" ");
            stopAutoRefresh();
        } else {
            projectLabel.setText("Project: " + project.getName());
            refreshTasks(true);
        }
    }

    public void clearCurrentProject() {
        setCurrentProject(null);
    }

    public Long getCurrentProjectId() {
        return currentProject != null ? currentProject.getId() : null;
    }

    public Long getSelectedTaskId() {
        TaskDto t = todoList.getSelectedValue();
        if (t != null) {
            return t.getId();
        }
        t = doingList.getSelectedValue();
        if (t != null) {
            return t.getId();
        }
        t = doneList.getSelectedValue();
        return t == null ? null : t.getId();
    }

    public void upsertTask(TaskDto task) {
        if (task == null) {
            return;
        }
        Long selectedId = getSelectedTaskId();
        removeTaskById(task.getId());
        if ("DOING".equalsIgnoreCase(task.getStatus())) {
            doingModel.addElement(task);
            doingHeader.setText("DOING (" + doingModel.getSize() + ")");
        } else if ("DONE".equalsIgnoreCase(task.getStatus())) {
            doneModel.addElement(task);
            doneHeader.setText("DONE (" + doneModel.getSize() + ")");
        } else {
            todoModel.addElement(task);
            todoHeader.setText("TODO (" + todoModel.getSize() + ")");
        }
        if (selectedId != null && selectedId.equals(task.getId())) {
            if ("DOING".equalsIgnoreCase(task.getStatus())) {
                doingList.setSelectedIndex(doingModel.size() - 1);
            } else if ("DONE".equalsIgnoreCase(task.getStatus())) {
                doneList.setSelectedIndex(doneModel.size() - 1);
            } else {
                todoList.setSelectedIndex(todoModel.size() - 1);
            }
        }
    }

    public void removeTaskById(Long taskId) {
        if (taskId == null) {
            return;
        }
        removeFromModel(taskId, todoModel);
        removeFromModel(taskId, doingModel);
        removeFromModel(taskId, doneModel);
        todoHeader.setText("TODO (" + todoModel.getSize() + ")");
        doingHeader.setText("DOING (" + doingModel.getSize() + ")");
        doneHeader.setText("DONE (" + doneModel.getSize() + ")");
    }

    public void replaceAllTasks(List<TaskDto> tasks, Long preserveTaskId) {
        clearLists();
        if (tasks != null) {
            renderTasks(tasks, preserveTaskId);
        }
    }

    private void refreshTasks(boolean showErrors) {
        if (currentProject == null) {
            if (showErrors) {
                statusLabel.setText("Select a project to view tasks");
            }
            return;
        }
        // prevent overlapping refresh calls
        if (!refreshInProgress.compareAndSet(false, true)) {
            return;
        }
        Long selectedId = getSelectedTaskId();
        final Long preserveId = selectedId;
        new SwingWorker<List<TaskDto>, Void>() {
            @Override
            protected List<TaskDto> doInBackground() {
                return apiClient.listTasks(currentProject.getId());
            }

            @Override
            protected void done() {
                refreshInProgress.set(false);
                try {
                    List<TaskDto> tasks = get();
                    renderTasks(tasks, preserveId);
                    if (showErrors) {
                        statusLabel.setText("Loaded " + tasks.size() + " tasks");
                    }
                } catch (Exception ex) {
                    if (showErrors) {
                        statusLabel.setText("Auto refresh failed: " + describeError(ex));
                    }
                }
            }
        }.execute();
    }

    public void startAutoRefresh() {
        // no-op: periodic refresh disabled
    }

    public void stopAutoRefresh() {
        if (refreshTimer.isRunning()) {
            refreshTimer.stop();
        }
    }

    private void openCreateDialog() {
        stopAutoRefresh();
        if (currentProject == null) {
            JOptionPane.showMessageDialog(this, "Select a project first");
            startAutoRefresh();
            return;
        }
        java.awt.Window owner = SwingUtilities.getWindowAncestor(this);
        CreateTaskDialog dialog = new CreateTaskDialog(apiClient, owner,
                currentProject.getId(), currentProject.getName());
        dialog.setOnSuccess(() -> {
            refreshTasks(true);
            startAutoRefresh();
        });
        dialog.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                startAutoRefresh();
            }
        });
        dialog.setVisible(true);
    }

    private void openEditDialog(TaskDto task) {
        if (task == null) {
            JOptionPane.showMessageDialog(this, "Select a task first");
            return;
        }
        stopAutoRefresh();
        java.awt.Window owner = SwingUtilities.getWindowAncestor(this);
        EditTaskDialog dialog = new EditTaskDialog(owner, apiClient, currentProject.getId(), task, () -> {
            refreshTasks(true);
            startAutoRefresh();
        });
        dialog.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                startAutoRefresh();
            }
        });
        dialog.setVisible(true);
    }

    private void deleteSelected(TaskDto task) {
        if (task == null) {
            JOptionPane.showMessageDialog(this, "Select a task first");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this, "Delete selected task?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        stopAutoRefresh();
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                apiClient.deleteTask(task.getId());
                return null;
            }

            @Override
            protected void done() {
                startAutoRefresh();
                try {
                    get();
                    statusLabel.setText("Task deleted");
                    refreshTasks(true);
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

    private JPanel buildColumn(JLabel headerLabel, JList<TaskDto> list) {
        JPanel panel = new JPanel(new BorderLayout());
        headerLabel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        panel.add(headerLabel, BorderLayout.NORTH);
        panel.add(new JScrollPane(list), BorderLayout.CENTER);
        return panel;
    }

    private void configureList(JList<TaskDto> list) {
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer((l, value, index, isSelected, cellHasFocus) -> {
            JLabel label = new JLabel();
            if (value != null) {
                String title = value.getTitle() != null ? value.getTitle() : "(untitled)";
                String priority = value.getPriority() != null ? value.getPriority() : "";
                String assignee = value.getAssigneeUsername() != null ? value.getAssigneeUsername() : "Unassigned";
                String due = value.getDueDate() != null ? formatDue(value.getDueDate()) : "";
                label.setText("<html><b>" + title + "</b><br/>" + priority
                        + " | " + assignee + (due.isEmpty() ? "" : "</b><br/>Due: " + due) + "</html>");
            }
            if (isSelected) {
                label.setOpaque(true);
                label.setBackground(l.getSelectionBackground());
                label.setForeground(l.getSelectionForeground());
            }
            label.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
            return label;
        });
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    TaskDto task = getTaskAtEvent(list, e);
                    if (task != null) {
                        openEditDialog(task);
                    }
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                maybeShowPopup(list, e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                maybeShowPopup(list, e);
            }
        });
    }

    private void maybeShowPopup(JList<TaskDto> list, MouseEvent e) {
        if (e.isPopupTrigger()) {
            selectTaskAtEvent(list, e);
            TaskDto task = list.getSelectedValue();
            if (task != null) {
                prepareTaskMenu(task);
                taskMenu.show(list, e.getX(), e.getY());
            }
        }
    }

    private void prepareTaskMenu(TaskDto task) {
        taskMenu.removeAll();
        JMenuItem editItem = new JMenuItem("Edit Task");
        editItem.addActionListener(e -> openEditDialog(task));

        JMenuItem todoItem = new JMenuItem("Move to TODO");
        todoItem.addActionListener(e -> moveStatus(task, "TODO"));
        JMenuItem doingItem = new JMenuItem("Move to DOING");
        doingItem.addActionListener(e -> moveStatus(task, "DOING"));
        JMenuItem doneItem = new JMenuItem("Move to DONE");
        doneItem.addActionListener(e -> moveStatus(task, "DONE"));

        todoItem.setEnabled(!"TODO".equalsIgnoreCase(task.getStatus()));
        doingItem.setEnabled(!"DOING".equalsIgnoreCase(task.getStatus()));
        doneItem.setEnabled(!"DONE".equalsIgnoreCase(task.getStatus()));

        JMenuItem deleteItem = new JMenuItem("Delete Task");
        deleteItem.addActionListener(e -> deleteSelected(task));

        taskMenu.add(editItem);
        taskMenu.addSeparator();
        taskMenu.add(todoItem);
        taskMenu.add(doingItem);
        taskMenu.add(doneItem);
        taskMenu.addSeparator();
        taskMenu.add(deleteItem);
    }

    private void moveStatus(TaskDto task, String status) {
        if (task == null || status == null) {
            return;
        }
        new SwingWorker<TaskDto, Void>() {
            @Override
            protected TaskDto doInBackground() {
                return apiClient.updateTaskStatus(task.getId(), status);
            }

            @Override
            protected void done() {
                try {
                    get();
                    statusLabel.setText("Status updated");
                    refreshTasks(true);
                } catch (Exception ex) {
                    statusLabel.setText("Update failed: " + describeError(ex));
                }
            }
        }.execute();
    }

    private TaskDto getTaskAtEvent(JList<TaskDto> list, MouseEvent e) {
        int idx = list.locationToIndex(e.getPoint());
        if (idx >= 0) {
            list.setSelectedIndex(idx);
            if (list != todoList) {
                todoList.clearSelection();
            }
            if (list != doingList) {
                doingList.clearSelection();
            }
            if (list != doneList) {
                doneList.clearSelection();
            }
            return list.getModel().getElementAt(idx);
        }
        return null;
    }

    private void selectTaskAtEvent(JList<TaskDto> list, MouseEvent e) {
        int idx = list.locationToIndex(e.getPoint());
        if (idx >= 0) {
            list.setSelectedIndex(idx);
            // clear selection from other lists to keep single active selection
            if (list != todoList) {
                todoList.clearSelection();
            }
            if (list != doingList) {
                doingList.clearSelection();
            }
            if (list != doneList) {
                doneList.clearSelection();
            }
        }
    }

    private void renderTasks(List<TaskDto> tasks, Long preserveId) {
        todoModel.clear();
        doingModel.clear();
        doneModel.clear();
        for (TaskDto t : tasks) {
            if (t.getStatus() == null || "TODO".equalsIgnoreCase(t.getStatus())) {
                todoModel.addElement(t);
            } else if ("DOING".equalsIgnoreCase(t.getStatus())) {
                doingModel.addElement(t);
            } else {
                doneModel.addElement(t);
            }
        }
        todoHeader.setText("TODO (" + todoModel.size() + ")");
        doingHeader.setText("DOING (" + doingModel.size() + ")");
        doneHeader.setText("DONE (" + doneModel.size() + ")");
        if (preserveId != null) {
            reselectIfPresent(todoList, todoModel, preserveId);
            reselectIfPresent(doingList, doingModel, preserveId);
            reselectIfPresent(doneList, doneModel, preserveId);
        }
    }

    private void reselectIfPresent(JList<TaskDto> list, DefaultListModel<TaskDto> model, Long preserveId) {
        for (int i = 0; i < model.size(); i++) {
            if (preserveId.equals(model.get(i).getId())) {
                list.setSelectedIndex(i);
                if (list != todoList) {
                    todoList.clearSelection();
                }
                if (list != doingList) {
                    doingList.clearSelection();
                }
                if (list != doneList) {
                    doneList.clearSelection();
                }
                return;
            }
        }
    }

    private void clearLists() {
        todoModel.clear();
        doingModel.clear();
        doneModel.clear();
        todoHeader.setText("TODO (0)");
        doingHeader.setText("DOING (0)");
        doneHeader.setText("DONE (0)");
    }

    private String formatDue(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return "";
        }
        try {
            LocalDateTime ldt = LocalDateTime.parse(raw);
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            return ldt.format(fmt);
        } catch (Exception ex) {
            return raw;
        }
    }

    private void removeFromModel(Long taskId, DefaultListModel<TaskDto> model) {
        for (int i = 0; i < model.size(); i++) {
            if (taskId.equals(model.get(i).getId())) {
                model.remove(i);
                return;
            }
        }
    }
}
