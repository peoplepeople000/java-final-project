package com.example.taskmanager.desktop;

import com.example.taskmanager.desktop.DesktopApiClient.ProjectDto;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

public class CreateProjectDialog extends JDialog {

    private final DesktopApiClient apiClient;
    private final JTextField nameField = new JTextField();
    private final JTextArea descriptionField = new JTextArea(3, 20);
    private final JLabel statusLabel = new JLabel(" ");
    private Consumer<ProjectDto> onSuccess;

    public CreateProjectDialog(DesktopApiClient apiClient, Window owner) {
        super(owner, "Create Project", ModalityType.APPLICATION_MODAL);
        this.apiClient = apiClient;
        setLayout(new BorderLayout(6, 6));
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        buildUi();
        pack();
        setLocationRelativeTo(owner);
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

        JPanel actions = new JPanel();
        JButton cancelBtn = new JButton("Cancel");
        JButton createBtn = new JButton("Create");
        actions.add(cancelBtn);
        actions.add(createBtn);

        cancelBtn.addActionListener(e -> dispose());
        createBtn.addActionListener(e -> submit(createBtn));

        statusLabel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        add(form, BorderLayout.CENTER);
        add(actions, BorderLayout.SOUTH);
        add(statusLabel, BorderLayout.NORTH);
    }

    private void submit(JButton createBtn) {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            statusLabel.setText("Name is required");
            return;
        }
        createBtn.setEnabled(false);
        new SwingWorker<ProjectDto, Void>() {
            @Override
            protected ProjectDto doInBackground() {
                return apiClient.createProject(name, descriptionField.getText().trim());
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

    private String describeError(Exception ex) {
        if (ex instanceof DesktopApiClient.ApiException) {
            DesktopApiClient.ApiException apiEx = (DesktopApiClient.ApiException) ex;
            return "HTTP " + apiEx.getStatus() + " - " + apiEx.getMessage();
        }
        String msg = ex.getMessage();
        return (msg == null || msg.trim().isEmpty()) ? ex.toString() : msg;
    }
}
