package com.example.taskmanager.desktop;

import com.example.taskmanager.desktop.DesktopApiClient.ProjectDto;

@FunctionalInterface
public interface ProjectSelectionListener {
    void onProjectSelected(ProjectDto project);
}
