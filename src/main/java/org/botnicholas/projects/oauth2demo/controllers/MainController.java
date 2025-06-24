package org.botnicholas.projects.oauth2demo.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class MainController {
    @GetMapping("/all")
    public String getAll() {
        return "Hello All!";
    }
    @GetMapping("/home")
    public String getHome() {
        return "Hello Home!";
    }
}
