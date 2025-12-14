package com.example.taskmanager.desktop;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Container;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class TaskManagerDesktopApp extends JFrame implements LoginSuccessListener {

    private static final String CARD_AUTH = "AUTH";
    private static final String CARD_BOARD = "BOARD";

    static {
        UIManager.put("OptionPane.yesButtonText", "Yes");
        UIManager.put("OptionPane.noButtonText", "No");
        UIManager.put("OptionPane.cancelButtonText", "Cancel");
    }

    private final DesktopApiClient apiClient;
    private final RealtimeUpdateClient realtimeClient;
    private final CardLayout cardLayout = new CardLayout();
    private final Container cardContainer;
    private final AuthPanel authPanel;
    private final BoardPanel boardPanel;

    public TaskManagerDesktopApp() {
        String baseUrl = "http://localhost:8081";
        this.apiClient = new DesktopApiClient(baseUrl);
        this.realtimeClient = new RealtimeUpdateClient(baseUrl);
        setTitle("Task Manager Desktop Client");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1400, 700);
        setLocationRelativeTo(null);

        this.cardContainer = getContentPane();
        cardContainer.setLayout(cardLayout);

        this.boardPanel = new BoardPanel(apiClient, realtimeClient, this::handleLogout);
        this.authPanel = new AuthPanel(apiClient, this);

        cardContainer.add(authPanel, CARD_AUTH);
        cardContainer.add(boardPanel, CARD_BOARD);

        realtimeClient.connect();
        showAuth();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new TaskManagerDesktopApp().setVisible(true));
    }

    private void showAuth() {
        cardLayout.show(cardContainer, CARD_AUTH);
    }

    private void showBoard() {
        cardLayout.show(cardContainer, CARD_BOARD);
        boardPanel.onShow();
    }

    @Override
    public void onLoginSuccess(DesktopApiClient.AuthResponse user) {
        showBoard();
    }

    private void handleLogout() {
        int choice = JOptionPane.showConfirmDialog(this, "Log out?", "Confirm Logout", JOptionPane.YES_NO_OPTION);
        if (choice == JOptionPane.YES_OPTION) {
            apiClient.setCurrentUser(null);
            showAuth();
        }
    }
}
