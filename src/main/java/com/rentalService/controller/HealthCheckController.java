package com.rentalService.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthCheckController {

    @GetMapping("/__test/controllers/alive")
    public String alive() {
        return "ok";
    }
}
