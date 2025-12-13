package com.example.taskmanager.desktop;

import com.example.taskmanager.desktop.DesktopApiClient.UserDto;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

public class ManageMembersDialog extends JDialog {

    private final DesktopApiClient apiClient;
    private final Long projectId;
    private final JLabel statusLabel = new JLabel(" ");
    private final JComboBox<UserDto> addCombo = new JComboBox<>();
    private final JList<UserDto> membersList = new JList<>();

    public ManageMembersDialog(DesktopApiClient apiClient, Window owner, Long projectId, Long ownerId, String projectName) {
        super(owner, "Members - " + projectName, ModalityType.APPLICATION_MODAL);
        this.apiClient = apiClient;
        this.projectId = projectId;
        setLayout(new BorderLayout(6, 6));
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        buildUi();
        pack();
        setLocationRelativeTo(owner);
        loadMembersAndUsers();
    }

    private void buildUi() {
        JPanel top = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        top.add(new JLabel("Add member"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        top.add(addCombo, gbc);
        JButton addBtn = new JButton("Add");
        gbc.gridx = 2;
        gbc.weightx = 0;
        top.add(addBtn, gbc);

        addBtn.addActionListener(e -> addMember(addBtn));

        statusLabel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        ListCellRenderer<Object> renderer = (list, value, index, isSelected, cellHasFocus) -> {
            JLabel label = new JLabel();
            if (value instanceof UserDto) {
                UserDto u = (UserDto) value;
                label.setText(u.getUsername());
            } else {
                label.setText(String.valueOf(value));
            }
            if (isSelected) {
                label.setOpaque(true);
                label.setBackground(list.getSelectionBackground());
                label.setForeground(list.getSelectionForeground());
            }
            label.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
            return label;
        };
        addCombo.setRenderer(renderer);
        membersList.setCellRenderer(renderer);

        add(top, BorderLayout.NORTH);
        add(new JScrollPane(membersList), BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);
    }

    private void loadMembersAndUsers() {
        new SwingWorker<MembersData, Void>() {
            @Override
            protected MembersData doInBackground() throws Exception {
                List<UserDto> members = apiClient.listProjectMembers(projectId);
                List<UserDto> all = apiClient.listAllUsers();
                return new MembersData(members, all);
            }

            @Override
            protected void done() {
                try {
                    MembersData data = get();
                    membersList.setListData(data.members.toArray(new UserDto[0]));
                    addCombo.removeAllItems();
                    Set<Long> memberIds = new HashSet<>();
                    for (UserDto u : data.members) {
                        memberIds.add(u.getId());
                    }
                    for (UserDto u : data.allUsers) {
                        if (!memberIds.contains(u.getId())) {
                            addCombo.addItem(u);
                        }
                    }
                    statusLabel.setText("Loaded members");
                } catch (Exception ex) {
                    statusLabel.setText("Load failed: " + describeError(ex));
                }
            }
        }.execute();
    }

    private void addMember(JButton addBtn) {
        UserDto selected = (UserDto) addCombo.getSelectedItem();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Select a user to add");
            return;
        }
        addBtn.setEnabled(false);
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                apiClient.addProjectMember(projectId, selected.getId());
                return null;
            }

            @Override
            protected void done() {
                addBtn.setEnabled(true);
                try {
                    get();
                    statusLabel.setText("Member added");
                    loadMembersAndUsers();
                } catch (Exception ex) {
                    statusLabel.setText("Add failed: " + describeError(ex));
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

    private static class MembersData {
        final List<UserDto> members;
        final List<UserDto> allUsers;

        MembersData(List<UserDto> members, List<UserDto> allUsers) {
            this.members = members;
            this.allUsers = allUsers;
        }
    }
}
