package com.example.taskmanager.desktop;

import com.example.taskmanager.desktop.DesktopApiClient.MemberDto;
import com.example.taskmanager.desktop.DesktopApiClient.ProjectDto;
import com.example.taskmanager.desktop.DesktopApiClient.UserDto;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.border.TitledBorder;

public class EditProjectDialog extends JDialog {

    private final DesktopApiClient apiClient;
    private final ProjectDto project;
    private final Runnable onProjectDeletedOrUpdated;

    private final JTextField nameField = new JTextField();
    private final JTextArea descArea = new JTextArea(4, 30);
    private final JButton saveButton = new JButton("Save Details");
    private final JButton addMemberButton = new JButton("Add");
    private final JButton removeMemberButton = new JButton("Remove Selected");
    private final JButton deleteButton = new JButton("Delete Project");
    private final DefaultListModel<MemberDto> membersModel = new DefaultListModel<>();
    private final JList<MemberDto> membersList = new JList<>(membersModel);
    private final javax.swing.JComboBox<UserDto> allUsersCombo = new javax.swing.JComboBox<>();
    private final JLabel statusLabel = new JLabel(" ");

    public EditProjectDialog(Window owner, DesktopApiClient apiClient, ProjectDto project,
            Runnable onProjectDeletedOrUpdated) {
        super(owner, "Edit Project - " + project.getName(), ModalityType.APPLICATION_MODAL);
        this.apiClient = apiClient;
        this.project = project;
        this.onProjectDeletedOrUpdated = onProjectDeletedOrUpdated;
        setLayout(new BorderLayout(8, 8));
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        buildUi();
        setMinimumSize(new Dimension(620, 520));
        pack();
        setLocationRelativeTo(owner);
        loadMembers();
        loadAllUsers();
    }

    private void buildUi() {
        JPanel main = new JPanel();
        main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));
        main.add(buildDetailsPanel());
        main.add(buildMembersPanel());
        main.add(buildDangerPanel());

        statusLabel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        add(main, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);
    }

    private JPanel buildDetailsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new TitledBorder("Project Details"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Name"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        nameField.setText(project.getName());
        panel.add(nameField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.weightx = 0;
        panel.add(new JLabel("Description"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        descArea.setLineWrap(true);
        descArea.setWrapStyleWord(true);
        descArea.setText(project.getDescription());
        panel.add(new JScrollPane(descArea), gbc);

        gbc.gridx = 1;
        gbc.gridy++;
        gbc.anchor = GridBagConstraints.EAST;
        saveButton.addActionListener(e -> saveDetails());
        panel.add(saveButton, gbc);
        return panel;
    }

    private JPanel buildMembersPanel() {
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBorder(new TitledBorder("Members"));

        membersList.setSelectionMode(javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        membersList.setCellRenderer((list, value, index, isSelected, cellHasFocus) -> {
            JLabel label = new JLabel();
            if (value != null) {
                label.setText(value.getUsername());
            }
            if (isSelected) {
                label.setOpaque(true);
                label.setBackground(list.getSelectionBackground());
                label.setForeground(list.getSelectionForeground());
            }
            label.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
            return label;
        });
        membersList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                removeMemberButton.setEnabled(!membersList.isSelectionEmpty());
            }
        });

        JPanel addRow = new JPanel(new BorderLayout(4, 4));
        allUsersCombo.setRenderer((list, value, index, isSelected, cellHasFocus) -> {
            JLabel label = new JLabel();
            if (value != null) {
                String text = value.getUsername();
                if (value.getEmail() != null) {
                    text += " (" + value.getEmail() + ")";
                }
                label.setText(text);
            }
            if (isSelected) {
                label.setOpaque(true);
                label.setBackground(list.getSelectionBackground());
                label.setForeground(list.getSelectionForeground());
            }
            label.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
            return label;
        });
        addRow.add(allUsersCombo, BorderLayout.CENTER);
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        btns.add(addMemberButton);
        btns.add(removeMemberButton);
        addRow.add(btns, BorderLayout.EAST);

        addMemberButton.addActionListener(e -> addMember());
        removeMemberButton.addActionListener(e -> removeSelectedMembers());
        removeMemberButton.setEnabled(false);

        panel.add(addRow, BorderLayout.NORTH);
        panel.add(new JScrollPane(membersList), BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildDangerPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBorder(new TitledBorder("Danger Zone"));
        deleteButton.addActionListener(e -> deleteProject());
        panel.add(deleteButton);
        return panel;
    }

    private void saveDetails() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            statusLabel.setText("Name is required");
            return;
        }
        saveButton.setEnabled(false);
        new SwingWorker<ProjectDto, Void>() {
            @Override
            protected ProjectDto doInBackground() {
                return apiClient.updateProject(project.getId(), name, descArea.getText());
            }

            @Override
            protected void done() {
                saveButton.setEnabled(true);
                try {
                    ProjectDto updated = get();
                    project.setName(updated.getName());
                    project.setDescription(updated.getDescription());
                    statusLabel.setText("Saved");
                    if (onProjectDeletedOrUpdated != null) {
                        onProjectDeletedOrUpdated.run();
                    }
                } catch (Exception ex) {
                    statusLabel.setText("Save failed: " + describeError(ex));
                }
            }
        }.execute();
    }

    private void addMember() {
        UserDto selected = (UserDto) allUsersCombo.getSelectedItem();
        if (selected == null) {
            statusLabel.setText("Select a user to add");
            return;
        }
        // check existing
        for (int i = 0; i < membersModel.size(); i++) {
            MemberDto m = membersModel.get(i);
            if (selected.getId() != null && selected.getId().equals(m.getUserId())) {
                statusLabel.setText("Already a member");
                return;
            }
        }
        toggleMemberButtons(false);
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                apiClient.addProjectMember(project.getId(), selected.getId());
                return null;
            }

            @Override
            protected void done() {
                toggleMemberButtons(true);
                try {
                    get();
                    statusLabel.setText("Member added");
                    loadMembers();
                    if (onProjectDeletedOrUpdated != null) {
                        onProjectDeletedOrUpdated.run();
                    }
                } catch (Exception ex) {
                    statusLabel.setText("Add failed: " + describeError(ex));
                }
            }
        }.execute();
    }

    private void removeSelectedMembers() {
        List<MemberDto> selected = membersList.getSelectedValuesList();
        if (selected == null || selected.isEmpty()) {
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this, "Remove selected member(s)?", "Confirm",
                JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        toggleMemberButtons(false);
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                Long ownerId = project.getOwnerId();
                for (MemberDto m : selected) {
                    if (ownerId != null && ownerId.equals(m.getUserId())) {
                        throw new DesktopApiClient.ApiException(400, "Cannot remove project owner", null);
                    }
                    apiClient.removeProjectMember(project.getId(), m.getUserId());
                }
                return null;
            }

            @Override
            protected void done() {
                toggleMemberButtons(true);
                try {
                    get();
                    statusLabel.setText("Member(s) removed");
                    loadMembers();
                    if (onProjectDeletedOrUpdated != null) {
                        onProjectDeletedOrUpdated.run();
                    }
                } catch (Exception ex) {
                    statusLabel.setText("Remove failed: " + describeError(ex));
                }
            }
        }.execute();
    }

    private void deleteProject() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Delete project '" + project.getName() + "'? This will remove tasks and members.",
                "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        deleteButton.setEnabled(false);
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                apiClient.deleteProject(project.getId());
                return null;
            }

            @Override
            protected void done() {
                deleteButton.setEnabled(true);
                try {
                    get();
                    statusLabel.setText("Project deleted");
                    if (onProjectDeletedOrUpdated != null) {
                        onProjectDeletedOrUpdated.run();
                    }
                    dispose();
                } catch (Exception ex) {
                    statusLabel.setText("Delete failed: " + describeError(ex));
                }
            }
        }.execute();
    }

    private void loadMembers() {
        toggleMemberButtons(false);
        new SwingWorker<List<MemberDto>, Void>() {
            @Override
            protected List<MemberDto> doInBackground() {
                return apiClient.listProjectMembers(project.getId());
            }

            @Override
            protected void done() {
                toggleMemberButtons(true);
                try {
                    List<MemberDto> members = get();
                    membersModel.clear();
                    for (MemberDto m : members) {
                        membersModel.addElement(m);
                    }
                    statusLabel.setText("Members loaded");
                } catch (Exception ex) {
                    statusLabel.setText("Load members failed: " + describeError(ex));
                }
            }
        }.execute();
    }

    private void loadAllUsers() {
        allUsersCombo.setEnabled(false);
        new SwingWorker<List<UserDto>, Void>() {
            @Override
            protected List<UserDto> doInBackground() {
                return apiClient.listAllUsers();
            }

            @Override
            protected void done() {
                allUsersCombo.setEnabled(true);
                try {
                    List<UserDto> users = get();
                    allUsersCombo.removeAllItems();
                    for (UserDto u : users) {
                        allUsersCombo.addItem(u);
                    }
                } catch (Exception ex) {
                    statusLabel.setText("Load users failed: " + describeError(ex));
                }
            }
        }.execute();
    }

    private void toggleMemberButtons(boolean enabled) {
        addMemberButton.setEnabled(enabled);
        removeMemberButton.setEnabled(enabled && !membersList.isSelectionEmpty());
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
