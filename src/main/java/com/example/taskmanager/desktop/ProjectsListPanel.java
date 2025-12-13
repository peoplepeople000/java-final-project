package com.example.taskmanager.desktop;

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
import javax.swing.SwingWorker;
import javax.swing.ListSelectionModel;

public class ProjectsListPanel extends JPanel {

    private final DesktopApiClient apiClient;
    private final DefaultListModel<ProjectDto> model = new DefaultListModel<>();
    private final JList<ProjectDto> list = new JList<>(model);
    private final JLabel statusLabel = new JLabel(" ");
    private ProjectSelectionListener selectionListener;

    public ProjectsListPanel(DesktopApiClient apiClient) {
        this.apiClient = apiClient;
        setLayout(new BorderLayout(6, 6));
        setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer((l, value, index, isSelected, cellHasFocus) -> {
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
        list.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && selectionListener != null) {
                selectionListener.onProjectSelected(list.getSelectedValue());
            }
        });

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton refreshBtn = new JButton("Refresh");
        JButton addBtn = new JButton("+ Project");
        JButton membersBtn = new JButton("Members");
        membersBtn.setEnabled(false);
        toolbar.add(refreshBtn);
        toolbar.add(addBtn);
        toolbar.add(membersBtn);

        refreshBtn.addActionListener(e -> reloadProjects());
        addBtn.addActionListener(e -> openCreateDialog());
        membersBtn.addActionListener(e -> openMembersDialog());

        list.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                ProjectDto selected = list.getSelectedValue();
                DesktopApiClient.AuthResponse user = apiClient.getCurrentUser();
                boolean owner = selected != null && user != null && selected.getOwnerId() != null
                        && selected.getOwnerId().equals(user.getId());
                membersBtn.setEnabled(selected != null && owner);
            }
        });

        statusLabel.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));

        add(toolbar, BorderLayout.NORTH);
        add(new JScrollPane(list), BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);
    }

    public void setProjectSelectionListener(ProjectSelectionListener selectionListener) {
        this.selectionListener = selectionListener;
    }

    public void reloadProjects() {
        new SwingWorker<List<ProjectDto>, Void>() {
            @Override
            protected List<ProjectDto> doInBackground() {
                return apiClient.listProjects();
            }

            @Override
            protected void done() {
                try {
                    List<ProjectDto> projects = get();
                    model.clear();
                    for (ProjectDto p : projects) {
                        model.addElement(p);
                    }
                    statusLabel.setText("Loaded " + projects.size() + " projects");
                    if (!projects.isEmpty()) {
                        list.setSelectedIndex(0);
                    }
                } catch (Exception ex) {
                    statusLabel.setText("Load failed: " + describeError(ex));
                }
            }
        }.execute();
    }

    private void openCreateDialog() {
        java.awt.Window owner = javax.swing.SwingUtilities.getWindowAncestor(this);
        CreateProjectDialog dialog = new CreateProjectDialog(apiClient, owner);
        dialog.setOnSuccess(this::reloadProjects);
        dialog.setVisible(true);
    }

    private void openMembersDialog() {
        ProjectDto selected = list.getSelectedValue();
        DesktopApiClient.AuthResponse user = apiClient.getCurrentUser();
        if (selected == null || user == null || !selected.getOwnerId().equals(user.getId())) {
            statusLabel.setText("Only the project owner can manage members");
            return;
        }
        java.awt.Window owner = javax.swing.SwingUtilities.getWindowAncestor(this);
        ManageMembersDialog dialog = new ManageMembersDialog(apiClient, owner, selected.getId(), selected.getOwnerId(),
                selected.getName());
        dialog.setVisible(true);
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
