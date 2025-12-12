package com.example.taskmanager.desktop;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import com.example.taskmanager.desktop.DesktopApiClient.AuthResponse;

public class TaskManagerDesktopApp extends JFrame {

    private final DesktopApiClient apiClient;
    private final JTabbedPane tabs;
    private final ProjectsPanel projectsPanel;
    private final TasksPanel tasksPanel;

    public TaskManagerDesktopApp() {
        this.apiClient = new DesktopApiClient("http://localhost:8081");
        setTitle("Task Manager Desktop Client");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);

        this.tabs = new JTabbedPane();
        this.projectsPanel = new ProjectsPanel(apiClient);
        this.tasksPanel = new TasksPanel(apiClient);
        this.projectsPanel.setProjectSelectionListener(tasksPanel::setCurrentProject);

        tabs.addTab("Register", new RegisterPanel(apiClient));
        tabs.addTab("Login", new LoginPanel(apiClient, this::onLoginSuccess));
        tabs.addTab("Projects", projectsPanel);
        tabs.addTab("Tasks", tasksPanel);

        // disable project/task tabs until login
        tabs.setEnabledAt(2, false);
        tabs.setEnabledAt(3, false);

        add(tabs, BorderLayout.CENTER);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new TaskManagerDesktopApp().setVisible(true));
    }

    private void onLoginSuccess(AuthResponse user) {
        tabs.setEnabledAt(2, true);
        tabs.setEnabledAt(3, true);
        tabs.setSelectedIndex(2);
        projectsPanel.reloadProjects();
    }

    private abstract static class BaseAuthPanel extends JPanel {
        protected final DesktopApiClient apiClient;
        protected final JLabel statusLabel = new JLabel(" ");

        protected BaseAuthPanel(DesktopApiClient apiClient) {
            this.apiClient = apiClient;
            setLayout(new BorderLayout());
            statusLabel.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 8, 8, 8));
        }

        protected void updateStatus(String message) {
            statusLabel.setText(message);
        }

        protected String describeError(Exception ex) {
            if (ex instanceof DesktopApiClient.ApiException) {
                DesktopApiClient.ApiException apiEx = (DesktopApiClient.ApiException) ex;
                return "HTTP " + apiEx.getStatus() + " - " + apiEx.getMessage();
            }
            String msg = ex.getMessage();
            return (msg == null || msg.trim().isEmpty()) ? ex.toString() : msg;
        }
    }

    private static class RegisterPanel extends BaseAuthPanel {

        RegisterPanel(DesktopApiClient apiClient) {
            super(apiClient);
            JTextField usernameField = new JTextField();
            JTextField emailField = new JTextField();
            JPasswordField passwordField = new JPasswordField();
            JButton registerButton = new JButton("Register");

            JPanel form = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(5, 5, 5, 5);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.gridx = 0;
            gbc.gridy = 0;
            form.add(new JLabel("Username"), gbc);
            gbc.gridx = 1;
            form.add(usernameField, gbc);

            gbc.gridx = 0;
            gbc.gridy++;
            form.add(new JLabel("Email"), gbc);
            gbc.gridx = 1;
            form.add(emailField, gbc);

            gbc.gridx = 0;
            gbc.gridy++;
            form.add(new JLabel("Password"), gbc);
            gbc.gridx = 1;
            form.add(passwordField, gbc);

            gbc.gridx = 1;
            gbc.gridy++;
            form.add(registerButton, gbc);

            registerButton.addActionListener(event -> {
                registerButton.setEnabled(false);
                updateStatus("Sending registration...");
                new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() {
                        try {
                            AuthResponse response = apiClient.register(usernameField.getText().trim(),
                                    emailField.getText().trim(), new String(passwordField.getPassword()));
                            updateStatus("Registered: " + response.getUsername());
                        } catch (Exception ex) {
                            updateStatus("Registration failed: " + describeError(ex));
                        }
                        return null;
                    }

                    @Override
                    protected void done() {
                        registerButton.setEnabled(true);
                    }
                }.execute();
            });

            add(form, BorderLayout.CENTER);
            add(statusLabel, BorderLayout.SOUTH);
        }
    }

    private static class LoginPanel extends BaseAuthPanel {

        private final LoginSuccessListener loginSuccessListener;

        LoginPanel(DesktopApiClient apiClient, LoginSuccessListener loginSuccessListener) {
            super(apiClient);
            this.loginSuccessListener = loginSuccessListener;
            JTextField usernameOrEmailField = new JTextField();
            JPasswordField passwordField = new JPasswordField();
            JButton loginButton = new JButton("Login");

            JPanel form = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(5, 5, 5, 5);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.gridx = 0;
            gbc.gridy = 0;
            form.add(new JLabel("Username / Email"), gbc);
            gbc.gridx = 1;
            form.add(usernameOrEmailField, gbc);

            gbc.gridx = 0;
            gbc.gridy++;
            form.add(new JLabel("Password"), gbc);
            gbc.gridx = 1;
            form.add(passwordField, gbc);

            gbc.gridx = 1;
            gbc.gridy++;
            form.add(loginButton, gbc);

            loginButton.addActionListener(event -> {
                loginButton.setEnabled(false);
                updateStatus("Signing in...");
                new SwingWorker<AuthResponse, Void>() {
                    @Override
                    protected AuthResponse doInBackground() {
                        try {
                            AuthResponse response = apiClient.login(usernameOrEmailField.getText().trim(),
                                    new String(passwordField.getPassword()));
                            updateStatus("Welcome " + response.getUsername() + " (" + response.getEmail() + ")");
                            return response;
                        } catch (Exception ex) {
                            updateStatus("Login failed: " + describeError(ex));
                            return null;
                        }
                    }

                    @Override
                    protected void done() {
                        loginButton.setEnabled(true);
                        try {
                            AuthResponse response = get();
                            if (response != null && loginSuccessListener != null) {
                                loginSuccessListener.onLoginSuccess(response);
                            }
                        } catch (Exception ignored) {
                            // ignore
                        }
                    }
                }.execute();
            });

            add(form, BorderLayout.CENTER);
            add(statusLabel, BorderLayout.SOUTH);
        }
    }

    @FunctionalInterface
    private interface LoginSuccessListener {
        void onLoginSuccess(AuthResponse user);
    }
}
