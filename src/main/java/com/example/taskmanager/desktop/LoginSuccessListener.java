package com.example.taskmanager.desktop;

@FunctionalInterface
public interface LoginSuccessListener {
    void onLoginSuccess(DesktopApiClient.AuthResponse user);
}
