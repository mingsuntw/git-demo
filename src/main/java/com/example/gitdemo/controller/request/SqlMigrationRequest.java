package com.example.gitdemo.controller.request;

import lombok.Getter;

@Getter
public class SqlMigrationRequest {
    private String description;
    private String sqlScript;
}
