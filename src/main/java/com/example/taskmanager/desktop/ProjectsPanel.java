package com.example.taskmanager.desktop;

import com.example.taskmanager.desktop.DesktopApiClient.ProjectDto;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class ProjectsPanel extends JPanel {

    private final DesktopApiClient apiClient;
    private final DefaultListModel<ProjectDto> listModel = new DefaultListModel<>();
    private final JList<ProjectDto> projectsList = new JList<>(listModel);
    private final JTextField nameField = new JTextField();
    private final JTextArea descriptionField = new JTextArea(3, 20);
    private final JLabel statusLabel = new JLabel(" ");
    private ProjectSelectionListener selectionListener;

    public ProjectsPanel(DesktopApiClient apiClient) {
        this.apiClient = apiClient;
        setLayout(new BorderLayout(8, 8));

        projectsList.setCellRenderer((list, value, index, isSelected, cellHasFocus) -> {
            JLabel label = new JLabel();
            String title = value != null ? value.getName() : "(unknown)";
            String desc = value != null && value.getDescription() != null ? value.getDescription() : "";
            label.setText("<html><b>" + title + "</b><br/>" + desc + "</html>");
            if (isSelected) {
                label.setOpaque(true);
                label.setBackground(list.getSelectionBackground());
                label.setForeground(list.getSelectionForeground());
            }
            label.setBorder(javax.swing.BorderFactory.createEmptyBorder(4, 4, 4, 4));
            return label;
        });

        projectsList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting() && selectionListener != null) {
                    selectionListener.onProjectSelected(projectsList.getSelectedValue());
                }
            }
        });

        JPanel form = buildForm();

        add(new JScrollPane(projectsList), BorderLayout.CENTER);
        add(form, BorderLayout.SOUTH);
        statusLabel.setBorder(javax.swing.BorderFactory.createEmptyBorder(4, 4, 4, 4));
        add(statusLabel, BorderLayout.NORTH);
    }

    private JPanel buildForm() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Name"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(nameField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.weightx = 0;
        panel.add(new JLabel("Description"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        descriptionField.setLineWrap(true);
        descriptionField.setWrapStyleWord(true);
        panel.add(new JScrollPane(descriptionField), gbc);

        JButton createButton = new JButton("Create Project");
        JButton refreshButton = new JButton("Refresh");
        gbc.gridx = 1;
        gbc.gridy++;
        gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.EAST;
        JPanel actions = new JPanel();
        actions.add(refreshButton);
        actions.add(createButton);
        panel.add(actions, gbc);

        createButton.addActionListener(e -> createProject(createButton));
        refreshButton.addActionListener(e -> reloadProjects());
        return panel;
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
                    listModel.clear();
                    for (ProjectDto p : projects) {
                        listModel.addElement(p);
                    }
                    statusLabel.setText("Loaded " + projects.size() + " projects");
                } catch (Exception ex) {
                    statusLabel.setText("Load failed: " + describeError(ex));
                }
            }
        }.execute();
    }

    private void createProject(JButton createButton) {
        createButton.setEnabled(false);
        new SwingWorker<ProjectDto, Void>() {
            @Override
            protected ProjectDto doInBackground() {
                return apiClient.createProject(nameField.getText().trim(), descriptionField.getText().trim());
            }

            @Override
            protected void done() {
                createButton.setEnabled(true);
                try {
                    get();
                    statusLabel.setText("Project created");
                    nameField.setText("");
                    descriptionField.setText("");
                    reloadProjects();
                } catch (Exception ex) {
                    statusLabel.setText("Create failed: " + describeError(ex));
                }
            }
        }.execute();
    }

    public void setProjectSelectionListener(ProjectSelectionListener selectionListener) {
        this.selectionListener = selectionListener;
    }

    private String describeError(Exception ex) {
        if (ex instanceof DesktopApiClient.ApiException) {
            DesktopApiClient.ApiException apiEx = (DesktopApiClient.ApiException) ex;
            return "HTTP " + apiEx.getStatus() + " - " + apiEx.getMessage();
        }
        String msg = ex.getMessage();
        return (msg == null || msg.trim().isEmpty()) ? ex.toString() : msg;
    }

    @FunctionalInterface
    public interface ProjectSelectionListener {
        void onProjectSelected(ProjectDto project);
    }
}
