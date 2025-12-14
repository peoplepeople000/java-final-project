package com.example.taskmanager.desktop;

import com.example.taskmanager.desktop.DesktopApiClient.MemberDto;
import com.example.taskmanager.desktop.DesktopApiClient.ProjectDto;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import javax.swing.Timer;

public class ProjectsListPanel extends JPanel {

    private final DesktopApiClient apiClient;
    private final DefaultListModel<ProjectDto> projectModel = new DefaultListModel<>();
    private final JList<ProjectDto> projectList = new JList<>(projectModel);
    private final DefaultListModel<MemberDto> membersModel = new DefaultListModel<>();
    private final JList<MemberDto> membersList = new JList<>(membersModel);
    private final JLabel statusLabel = new JLabel(" ");
    private ProjectSelectionListener selectionListener;
    private Runnable projectClearedListener;
    private final JPopupMenu projectMenu = new JPopupMenu();
    private final Timer autoRefreshTimer;
    private final AtomicBoolean refreshInProgress = new AtomicBoolean(false);

    public ProjectsListPanel(DesktopApiClient apiClient) {
        this.apiClient = apiClient;
        setLayout(new BorderLayout(6, 6));
        setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        projectList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        projectList.setCellRenderer((l, value, index, isSelected, cellHasFocus) -> {
            JLabel label = new JLabel();
            if (value != null) {
                String desc = value.getDescription() != null ? value.getDescription() : "";
                label.setText("<html><b>" + value.getName() + "</b><br/>" + desc + "</html>");
            }
            if (isSelected) {
                label.setOpaque(true);
                label.setBackground(l.getSelectionBackground());
                label.setForeground(l.getSelectionForeground());
            }
            label.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
            return label;
        });

        membersList.setCellRenderer((l, value, index, isSelected, cellHasFocus) -> {
            JLabel label = new JLabel();
            if (value != null) {
                label.setText(value.getUsername());
            }
            if (isSelected) {
                label.setOpaque(true);
                label.setBackground(l.getSelectionBackground());
                label.setForeground(l.getSelectionForeground());
            }
            label.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
            return label;
        });

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addBtn = new JButton("+ Project");
        JButton editProjectBtn = new JButton("Edit Project");
        editProjectBtn.setEnabled(false);
        toolbar.add(addBtn);
        toolbar.add(editProjectBtn);

        addBtn.addActionListener(e -> openCreateDialog());
        editProjectBtn.addActionListener(e -> openEditDialog());

        projectList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                ProjectDto selected = projectList.getSelectedValue();
                DesktopApiClient.AuthResponse user = apiClient.getCurrentUser();
                boolean owner = selected != null && user != null && selected.getOwnerId() != null
                        && selected.getOwnerId().equals(user.getId());
                editProjectBtn.setEnabled(selected != null && owner);
                loadMembersForSelected();
                if (selectionListener != null) {
                    selectionListener.onProjectSelected(selected);
                }
            }
        });

        statusLabel.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        initProjectMenu();
        projectList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && projectList.getSelectedValue() != null) {
                    openEditDialog();
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                maybeShowPopup(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                maybeShowPopup(e);
            }

            private void maybeShowPopup(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    int idx = projectList.locationToIndex(e.getPoint());
                    if (idx >= 0) {
                        projectList.setSelectedIndex(idx);
                        ProjectDto p = projectList.getSelectedValue();
                        if (p != null && isOwner(p)) {
                            projectMenu.show(projectList, e.getX(), e.getY());
                        }
                    }
                }
            }
        });

        JPanel listsPanel = new JPanel(new BorderLayout(6, 6));
        listsPanel.add(new JScrollPane(projectList), BorderLayout.CENTER);
        JPanel membersPanel = new JPanel(new BorderLayout());
        membersPanel.add(new JLabel("Members"), BorderLayout.NORTH);
        membersPanel.add(new JScrollPane(membersList), BorderLayout.CENTER);
        listsPanel.add(membersPanel, BorderLayout.SOUTH);

        add(toolbar, BorderLayout.NORTH);
        add(listsPanel, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);

        autoRefreshTimer = new Timer(10000, e -> refreshProjects(false, null));
        autoRefreshTimer.setRepeats(true);
    }

    public void setProjectSelectionListener(ProjectSelectionListener selectionListener) {
        this.selectionListener = selectionListener;
    }

    public void setProjectClearedListener(Runnable projectClearedListener) {
        this.projectClearedListener = projectClearedListener;
    }

    public void reloadProjects() {
        refreshProjects(true, null);
    }

    public void reloadProjects(Long selectProjectId) {
        refreshProjects(true, selectProjectId);
    }

    private void openCreateDialog() {
        stopAutoRefresh();
        java.awt.Window owner = javax.swing.SwingUtilities.getWindowAncestor(this);
        CreateProjectDialog dialog = new CreateProjectDialog(apiClient, owner);
        dialog.setOnSuccess((Runnable) () -> {
            refreshProjects(true, null);
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

    private void deleteSelectedProject() {
        ProjectDto selected = projectList.getSelectedValue();
        if (selected == null) {
            return;
        }
        int confirm = javax.swing.JOptionPane.showConfirmDialog(this,
                "Delete project '" + selected.getName() + "'? This will remove tasks and members.",
                "Confirm", javax.swing.JOptionPane.YES_NO_OPTION);
        if (confirm != javax.swing.JOptionPane.YES_OPTION) {
            return;
        }
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                apiClient.deleteProject(selected.getId());
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    statusLabel.setText("Deleted project");
                    reloadProjects();
                    membersModel.clear();
                    if (selectionListener != null) {
                        selectionListener.onProjectSelected(null);
                    }
                    if (projectClearedListener != null) {
                        projectClearedListener.run();
                    }
                } catch (Exception ex) {
                    statusLabel.setText("Delete failed: " + describeError(ex));
                }
            }
        }.execute();
    }

    private void openEditDialog() {
        ProjectDto selected = projectList.getSelectedValue();
        if (selected == null || !isOwner(selected)) {
            statusLabel.setText("Only the project owner can manage members");
            return;
        }
        stopAutoRefresh();
        java.awt.Window owner = javax.swing.SwingUtilities.getWindowAncestor(this);
        EditProjectDialog dialog = new EditProjectDialog(owner, apiClient, selected, () -> {
            refreshProjects(true, selected.getId());
            loadMembersForSelected();
        });
        dialog.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                startAutoRefresh();
            }
        });
        dialog.setVisible(true);
    }

    private void loadMembersForSelected() {
        ProjectDto selected = projectList.getSelectedValue();
        if (selected == null) {
            membersModel.clear();
            return;
        }
        new SwingWorker<List<MemberDto>, Void>() {
            @Override
            protected List<MemberDto> doInBackground() {
                return apiClient.listProjectMembers(selected.getId());
            }

            @Override
            protected void done() {
                try {
                    List<MemberDto> members = get();
                    membersModel.clear();
                    for (MemberDto m : members) {
                        membersModel.addElement(m);
                    }
                } catch (Exception ex) {
                    statusLabel.setText("Members load failed: " + describeError(ex));
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

    private boolean isOwner(ProjectDto project) {
        DesktopApiClient.AuthResponse user = apiClient.getCurrentUser();
        return project != null && user != null && project.getOwnerId() != null
                && project.getOwnerId().equals(user.getId());
    }

    private void initProjectMenu() {
        JMenuItem editItem = new JMenuItem("Edit Project");
        editItem.addActionListener(e -> openEditDialog());
        JMenuItem deleteItem = new JMenuItem("Delete Project");
        deleteItem.addActionListener(e -> deleteSelectedProject());
        projectMenu.add(editItem);
        projectMenu.add(deleteItem);
    }

    private void refreshProjects(boolean showErrors, Long selectProjectId) {
        if (!refreshInProgress.compareAndSet(false, true)) {
            return;
        }
        Long preserveId = selectProjectId;
        if (preserveId == null) {
            ProjectDto sel = projectList.getSelectedValue();
            preserveId = sel != null ? sel.getId() : null;
        }
        final Long targetId = preserveId;
        new SwingWorker<List<ProjectDto>, Void>() {
            @Override
            protected List<ProjectDto> doInBackground() {
                if (apiClient.getCurrentUser() == null) {
                    return java.util.Collections.emptyList();
                }
                return apiClient.listProjects();
            }

            @Override
            protected void done() {
                refreshInProgress.set(false);
                try {
                    List<ProjectDto> projects = get();
                    projectModel.clear();
                    for (ProjectDto p : projects) {
                        projectModel.addElement(p);
                    }
                    if (projects.isEmpty()) {
                        membersModel.clear();
                        if (selectionListener != null) {
                            selectionListener.onProjectSelected(null);
                        }
                        if (projectClearedListener != null) {
                            projectClearedListener.run();
                        }
                        if (showErrors) {
                            statusLabel.setText("Loaded 0 projects");
                        }
                        return;
                    }
                    if (targetId != null) {
                        int idx = -1;
                        for (int i = 0; i < projectModel.size(); i++) {
                            if (targetId.equals(projectModel.get(i).getId())) {
                                idx = i;
                                break;
                            }
                        }
                        if (idx >= 0) {
                            projectList.setSelectedIndex(idx);
                            return;
                        } else {
                            projectList.clearSelection();
                            membersModel.clear();
                            if (selectionListener != null) {
                                selectionListener.onProjectSelected(null);
                            }
                            if (projectClearedListener != null) {
                                projectClearedListener.run();
                            }
                            return;
                        }
                    }
                    projectList.setSelectedIndex(0);
                    if (showErrors) {
                        statusLabel.setText("Loaded " + projects.size() + " projects");
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
        if (!autoRefreshTimer.isRunning()) {
            autoRefreshTimer.start();
        }
    }

    public void stopAutoRefresh() {
        if (autoRefreshTimer.isRunning()) {
            autoRefreshTimer.stop();
        }
    }
}
