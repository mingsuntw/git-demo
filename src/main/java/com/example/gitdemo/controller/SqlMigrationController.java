package com.example.gitdemo.controller;

import com.example.gitdemo.controller.request.SqlMigrationRequest;
import com.example.gitdemo.service.SqlMigrationService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class SqlMigrationController {

    private SqlMigrationService sqlMigrationService;

    @PostMapping("/submitSQLScript")
    public ResponseEntity<String> submitSQLScript(@RequestParam String jiraCard,
                                                  @RequestParam String username,
                                                  @RequestBody SqlMigrationRequest sqlMigrationRequest) {

        String prLink = sqlMigrationService.processSqlScript(
            jiraCard, username, sqlMigrationRequest.getDescription(), sqlMigrationRequest.getSqlScript());

        return ResponseEntity.status(HttpStatus.CREATED).body(prLink);
    }

}
