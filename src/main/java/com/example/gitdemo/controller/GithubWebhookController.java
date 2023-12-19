package com.example.gitdemo.controller;

import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class GithubWebhookController {

    @PostMapping("/pull-request")
    public ResponseEntity<String> pullRequestWebhook(@RequestBody String requestBody) {

        return ResponseEntity.accepted().build();
    }
}
