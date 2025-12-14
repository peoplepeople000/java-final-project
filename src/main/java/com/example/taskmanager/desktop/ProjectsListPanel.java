package com.example.taskmanager.desktop;

import com.example.taskmanager.desktop.DesktopApiClient.MemberDto;
import com.example.taskmanager.desktop.DesktopApiClient.ProjectDto;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;

public class ProjectsListPanel extends JPanel {

    private final DesktopApiClient apiClient;
    private final DefaultListModel<ProjectDto> projectModel = new DefaultListModel<>();
    private final JList<ProjectDto> projectList = new JList<>(projectModel);
    private final DefaultListModel<MemberDto> membersModel = new DefaultListModel<>();
    private final JList<MemberDto> membersList = new JList<>(membersModel);
    private final JLabel statusLabel = new JLabel(" ");
    private ProjectSelectionListener selectionListener;
    private Runnable projectClearedListener;

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
        JButton refreshBtn = new JButton("Refresh");
        JButton addBtn = new JButton("+ Project");
        JButton editProjectBtn = new JButton("Edit Project");
        editProjectBtn.setEnabled(false);
        toolbar.add(refreshBtn);
        toolbar.add(addBtn);
        toolbar.add(editProjectBtn);

        refreshBtn.addActionListener(e -> reloadProjects());
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

        JPanel listsPanel = new JPanel(new BorderLayout(6, 6));
        listsPanel.add(new JScrollPane(projectList), BorderLayout.CENTER);
        JPanel membersPanel = new JPanel(new BorderLayout());
        membersPanel.add(new JLabel("Members"), BorderLayout.NORTH);
        membersPanel.add(new JScrollPane(membersList), BorderLayout.CENTER);
        listsPanel.add(membersPanel, BorderLayout.SOUTH);

        add(toolbar, BorderLayout.NORTH);
        add(listsPanel, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);
    }

    public void setProjectSelectionListener(ProjectSelectionListener selectionListener) {
        this.selectionListener = selectionListener;
    }

    public void setProjectClearedListener(Runnable projectClearedListener) {
        this.projectClearedListener = projectClearedListener;
    }

    public void reloadProjects() {
        reloadProjects(null);
    }

    public void reloadProjects(Long selectProjectId) {
        new SwingWorker<List<ProjectDto>, Void>() {
            @Override
            protected List<ProjectDto> doInBackground() {
                return apiClient.listProjects();
            }

            @Override
            protected void done() {
                try {
                    List<ProjectDto> projects = get();
                    projectModel.clear();
                    for (ProjectDto p : projects) {
                        projectModel.addElement(p);
                    }
                    statusLabel.setText("Loaded " + projects.size() + " projects");
                    if (projects.isEmpty()) {
                        membersModel.clear();
                        if (selectionListener != null) {
                            selectionListener.onProjectSelected(null);
                        }
                        if (projectClearedListener != null) {
                            projectClearedListener.run();
                        }
                        return;
                    }
                    if (selectProjectId != null) {
                        int idx = -1;
                        for (int i = 0; i < projectModel.size(); i++) {
                            if (selectProjectId.equals(projectModel.get(i).getId())) {
                                idx = i;
                                break;
                            }
                        }
                        if (idx >= 0) {
                            projectList.setSelectedIndex(idx);
                            return;
                        }
                    }
                    projectList.setSelectedIndex(0);
                } catch (Exception ex) {
                    statusLabel.setText("Load failed: " + describeError(ex));
                }
            }
        }.execute();
    }

    private void openCreateDialog() {
        java.awt.Window owner = javax.swing.SwingUtilities.getWindowAncestor(this);
        CreateProjectDialog dialog = new CreateProjectDialog(apiClient, owner);
        dialog.setOnSuccess((Runnable) this::reloadProjects);
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
        DesktopApiClient.AuthResponse user = apiClient.getCurrentUser();
        if (selected == null || user == null || !selected.getOwnerId().equals(user.getId())) {
            statusLabel.setText("Only the project owner can manage members");
            return;
        }
        java.awt.Window owner = javax.swing.SwingUtilities.getWindowAncestor(this);
        EditProjectDialog dialog = new EditProjectDialog(owner, apiClient, selected, () -> {
            reloadProjects(selected.getId());
            loadMembersForSelected();
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
}
