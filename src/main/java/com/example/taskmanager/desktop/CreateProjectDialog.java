package com.example.taskmanager.desktop;

import com.example.taskmanager.desktop.DesktopApiClient.MemberDto;
import com.example.taskmanager.desktop.DesktopApiClient.ProjectDto;
import com.example.taskmanager.desktop.DesktopApiClient.UserDto;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.Window;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JComboBox;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingWorker;

public class CreateProjectDialog extends JDialog {

    private final DesktopApiClient apiClient;
    private final JTextField nameField = new JTextField();
    private final JTextArea descriptionField = new JTextArea(3, 20);
    private final DefaultListModel<MemberDto> membersModel = new DefaultListModel<>();
    private final JList<MemberDto> membersList = new JList<>(membersModel);
    private final JComboBox<UserDto> addCombo = new JComboBox<>();
    private final JButton addBtn = new JButton("Add");
    private final JButton removeBtn = new JButton("Remove");
    private final JLabel statusLabel = new JLabel(" ");
    private Consumer<ProjectDto> onSuccess;

    public CreateProjectDialog(DesktopApiClient apiClient, Window owner) {
        super(owner, "Create Project", ModalityType.APPLICATION_MODAL);
        this.apiClient = apiClient;
        setLayout(new BorderLayout(6, 6));
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        buildUi();
        setMinimumSize(new Dimension(520, 520));
        pack();
        setLocationRelativeTo(owner);
        loadUsers();
    }

    private void buildUi() {
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        form.add(new JLabel("Name"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        form.add(nameField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.weightx = 0;
        form.add(new JLabel("Description"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        descriptionField.setLineWrap(true);
        descriptionField.setWrapStyleWord(true);
        form.add(descriptionField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        form.add(new JLabel("Members"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;

        addCombo.setRenderer((list, value, index, isSelected, cellHasFocus) -> {
            JLabel label = new JLabel();
            if (value != null) {
                label.setText(value.getUsername() + (value.getEmail() != null ? " (" + value.getEmail() + ")" : ""));
            }
            if (isSelected) {
                label.setOpaque(true);
                label.setBackground(list.getSelectionBackground());
                label.setForeground(list.getSelectionForeground());
            }
            label.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
            return label;
        });

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
        membersList.setSelectionMode(javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        JPanel memberControls = new JPanel(new BorderLayout(4, 4));
        JPanel addRow = new JPanel(new BorderLayout(4, 4));
        addRow.add(addCombo, BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        buttons.add(addBtn);
        buttons.add(removeBtn);
        addRow.add(buttons, BorderLayout.EAST);
        memberControls.add(addRow, BorderLayout.NORTH);
        memberControls.add(new JScrollPane(membersList), BorderLayout.CENTER);
        form.add(memberControls, gbc);

        JPanel actions = new JPanel();
        JButton cancelBtn = new JButton("Cancel");
        JButton createBtn = new JButton("Create");
        actions.add(cancelBtn);
        actions.add(createBtn);

        cancelBtn.addActionListener(e -> dispose());
        createBtn.addActionListener(e -> submit(createBtn));
        addBtn.addActionListener(e -> addSelectedUser());
        removeBtn.addActionListener(e -> removeSelectedMembers());
        removeBtn.setEnabled(false);
        membersList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                removeBtn.setEnabled(membersList.getSelectedValuesList().size() > 0);
            }
        });

        statusLabel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        add(form, BorderLayout.CENTER);
        add(actions, BorderLayout.SOUTH);
        add(statusLabel, BorderLayout.NORTH);
    }

    private void loadUsers() {
        new SwingWorker<List<UserDto>, Void>() {
            @Override
            protected List<UserDto> doInBackground() {
                return apiClient.listAllUsers();
            }

            @Override
            protected void done() {
                try {
                    List<UserDto> users = get();
                    addCombo.removeAllItems();
                    for (UserDto u : users) {
                        addCombo.addItem(u);
                    }
                     // Preselect owner in members list
                    DesktopApiClient.AuthResponse current = apiClient.getCurrentUser();
                    if (current != null) {
                        boolean exists = false;
                        for (int i = 0; i < membersModel.size(); i++) {
                            if (current.getId() != null && current.getId().equals(membersModel.get(i).getUserId())) {
                                exists = true;
                                break;
                            }
                        }
                        if (!exists) {
                            MemberDto ownerMember = new MemberDto();
                            ownerMember.setUserId(current.getId());
                            ownerMember.setUsername(current.getUsername());
                            membersModel.addElement(ownerMember);
                        }
                    }
                    statusLabel.setText("Users loaded");
                } catch (Exception ex) {
                    statusLabel.setText("Failed to load users: " + describeError(ex));
                }
            }
        }.execute();
    }

    private void submit(JButton createBtn) {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            statusLabel.setText("Name is required");
            return;
        }
        List<MemberDto> selectedMembers = membersList.getSelectedValuesList();
        DesktopApiClient.AuthResponse current = apiClient.getCurrentUser();
        Long ownerId = current != null ? current.getId() : null;
        Set<Long> toAdd = new HashSet<>();
        for (int i = 0; i < membersModel.size(); i++) {
            MemberDto m = membersModel.get(i);
            if (m.getUserId() != null && !m.getUserId().equals(ownerId)) {
                toAdd.add(m.getUserId());
            }
        }

        createBtn.setEnabled(false);
        new SwingWorker<ProjectDto, Void>() {
            @Override
            protected ProjectDto doInBackground() {
                ProjectDto project = apiClient.createProject(name, descriptionField.getText().trim());
                for (Long uid : toAdd) {
                    apiClient.addProjectMember(project.getId(), uid);
                }
                return project;
            }

            @Override
            protected void done() {
                createBtn.setEnabled(true);
                try {
                    ProjectDto project = get();
                    if (onSuccess != null) {
                        onSuccess.accept(project);
                    }
                    dispose();
                } catch (Exception ex) {
                    statusLabel.setText(describeError(ex));
                }
            }
        }.execute();
    }

    public void setOnSuccess(Runnable runnable) {
        this.onSuccess = project -> runnable.run();
    }

    public void setOnSuccess(Consumer<ProjectDto> consumer) {
        this.onSuccess = consumer;
    }

    private void addSelectedUser() {
        UserDto selected = (UserDto) addCombo.getSelectedItem();
        if (selected == null) {
            return;
        }
        // avoid duplicates
        for (int i = 0; i < membersModel.size(); i++) {
            if (selected.getId() != null && selected.getId().equals(membersModel.get(i).getUserId())) {
                return;
            }
        }
        MemberDto member = new MemberDto();
        member.setUserId(selected.getId());
        member.setUsername(selected.getUsername());
        membersModel.addElement(member);
    }

    private void removeSelectedMembers() {
        List<MemberDto> selected = membersList.getSelectedValuesList();
        if (selected == null || selected.isEmpty()) {
            return;
        }
        for (MemberDto m : selected) {
            membersModel.removeElement(m);
        }
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
