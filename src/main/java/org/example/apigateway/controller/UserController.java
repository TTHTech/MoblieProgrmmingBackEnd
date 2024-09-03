package org.example.apigateway.controller;

import org.example.apigateway.model.User;
import org.example.apigateway.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody User user) {
        boolean isValidUser = userService.validateUser(user.getUsername(), user.getPassword());
        if (isValidUser) {
            return ResponseEntity.ok("Login successful");
        } else {
            return ResponseEntity.status(401).body("Invalid username or password");
        }
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody User user) {
        if (userService.findByUsername(user.getUsername()).isPresent()) {
            return ResponseEntity.status(409).body("Username already exists");
        }
        if (userService.findByEmail(user.getEmail()).isPresent()) {
            return ResponseEntity.status(409).body("Email already exists");
        }
        userService.saveUser(new User(user.getUsername(), user.getPassword(), user.getEmail()));
        return ResponseEntity.ok("User registered successfully");
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@RequestParam String email) {
        if (userService.findByEmail(email).isPresent()) {
            userService.generateAndSaveOtp(email);
            return ResponseEntity.ok("OTP sent to email");
        }
        return ResponseEntity.status(404).body("Email not found");
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<String> verifyOtp(@RequestParam String email, @RequestParam String otp, @RequestParam String newPassword) {
        if (userService.verifyOtp(email, otp)) {
            userService.resetPassword(email, newPassword);
            return ResponseEntity.ok("Password reset successfully");
        }
        return ResponseEntity.status(401).body("Invalid OTP");
    }
}
