package org.example.apigateway.controller;

import org.example.apigateway.model.User;
import org.example.apigateway.service.UserService;
import org.example.apigateway.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    // Đăng ký tài khoản mới
    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody User user) {
        if (userService.findByUsername(user.getUsername()).isPresent()) {
            return ResponseEntity.status(409).body("Username already exists");
        }
        if (userService.findByEmail(user.getEmail()).isPresent()) {
            return ResponseEntity.status(409).body("Email already exists");
        }

        User newUser = new User(user.getUsername(), user.getPassword(), user.getEmail());
        newUser.setActive(false);
        userService.saveUser(newUser);

        userService.generateAndSaveOtp(user.getEmail());

        return ResponseEntity.ok("User registered successfully. Please verify OTP sent to your email.");
    }

    // Xác nhận OTP và kích hoạt tài khoản
    @PostMapping("/verify-otp-register")
    public ResponseEntity<String> verifyOtpForRegistration(@RequestParam String email, @RequestParam String otp) {
        if (userService.verifyOtp(email, otp)) {
            Optional<User> userOpt = userService.findByEmail(email);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                user.setActive(true);
                userService.saveUser(user);
                return ResponseEntity.ok("Account activated successfully");
            }
        }
        return ResponseEntity.status(401).body("Invalid OTP or email");
    }

    // Đăng nhập
    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody User user) {
        Optional<User> userOpt = userService.findByUsername(user.getUsername());
        if (userOpt.isPresent() && userOpt.get().isActive()) {
            boolean isValidUser = userService.validateUser(user.getUsername(), user.getPassword());
            if (isValidUser) {
                String token = jwtUtil.generateToken(user.getUsername());
                return ResponseEntity.ok("Bearer " + token);
            }
        } else if (userOpt.isPresent() && !userOpt.get().isActive()) {
            return ResponseEntity.status(403).body("Account not activated. Please verify your OTP.");
        }
        return ResponseEntity.status(401).body("Invalid username or password");
    }


    // Gửi OTP qua email để đăng nhập
    @PostMapping("/request-otp-email")
    public ResponseEntity<String> requestOtpByEmail(@RequestBody Map<String, String> requestBody) {
        String email = requestBody.get("email");
        if (userService.findByEmail(email).isPresent()) {
            userService.generateAndSaveOtp(email);
            return ResponseEntity.ok("OTP sent to email");
        }
        return ResponseEntity.status(404).body("Email not found");
    }

    // Đăng nhập bằng email và OTP
    @PostMapping("/login-otp")
    public ResponseEntity<String> loginWithOtp(@RequestBody Map<String, String> requestBody) {
        String email = requestBody.get("email");
        String otp = requestBody.get("otp");

        if (userService.verifyOtp(email, otp)) {
            Optional<User> userOpt = userService.findByEmail(email);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                String token = jwtUtil.generateToken(user.getUsername());
                return ResponseEntity.ok("Bearer " + token);
            }
        }
        return ResponseEntity.status(401).body("Invalid OTP or Email");
    }

    // Quên mật khẩu - Gửi OTP để đặt lại mật khẩu
    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@RequestParam String email) {
        if (userService.findByEmail(email).isPresent()) {
            userService.generateAndSaveOtp(email);
            return ResponseEntity.ok("OTP sent to email");
        }
        return ResponseEntity.status(404).body("Email not found");
    }

    // Xác nhận OTP và đặt lại mật khẩu
    @PostMapping("/verify-otp")
    public ResponseEntity<String> verifyOtp(@RequestParam String email, @RequestParam String otp, @RequestParam String newPassword) {
        if (userService.verifyOtp(email, otp)) {
            userService.resetPassword(email, newPassword);
            return ResponseEntity.ok("Password reset successfully");
        }
        return ResponseEntity.status(401).body("Invalid OTP");
    }

    // Gửi OTP để cập nhật hồ sơ
    @PostMapping("/profile/request-otp/{userId}")
    public ResponseEntity<String> requestOtpForProfileUpdate(@PathVariable Long userId) {
        Optional<User> userOpt = userService.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            userService.generateAndSaveOtp(user.getEmail());
            return ResponseEntity.ok("OTP sent to email for profile update.");
        }
        return ResponseEntity.status(404).body("User not found");
    }

    // Cập nhật hồ sơ người dùng sau khi xác nhận OTP
    @PutMapping("/profile/{userId}")
    public ResponseEntity<String> updateProfile(@PathVariable Long userId, @RequestBody User updatedUser) {
        // Tìm người dùng hiện tại trong cơ sở dữ liệu
        Optional<User> existingUserOpt = userService.findById(userId);
        if (existingUserOpt.isEmpty()) {
            return ResponseEntity.status(404).body("User not found");
        }

        User existingUser = existingUserOpt.get();

        // OTP từ body
        String otp = updatedUser.getOtp();

        // Xác minh OTP dựa trên email của existingUser
        if (!userService.verifyOtp(existingUser.getEmail(), otp)) {
            return ResponseEntity.status(400).body("Invalid OTP");
        }

        try {
            // Cập nhật hồ sơ người dùng
            userService.updateUserProfile(userId, updatedUser, otp);
            return ResponseEntity.ok("Profile updated successfully");
        } catch (UsernameNotFoundException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }


}
