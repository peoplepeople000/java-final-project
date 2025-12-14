package com.example.taskmanager.service;

public final class ChangeEventTypes {

    private ChangeEventTypes() {
    }

    public static final String PROJECT_CREATED = "PROJECT_CREATED";
    public static final String PROJECT_UPDATED = "PROJECT_UPDATED";
    public static final String PROJECT_DELETED = "PROJECT_DELETED";
    public static final String PROJECT_MEMBERS_UPDATED = "PROJECT_MEMBERS_UPDATED";

    public static final String TASK_CREATED = "TASK_CREATED";
    public static final String TASK_UPDATED = "TASK_UPDATED";
    public static final String TASK_DELETED = "TASK_DELETED";
}
