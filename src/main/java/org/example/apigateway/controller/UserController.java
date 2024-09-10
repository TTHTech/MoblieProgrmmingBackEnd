package org.example.apigateway.controller;
import org.example.apigateway.util.JwtUtil;
import org.example.apigateway.model.User;
import org.example.apigateway.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil; // Inject JwtUtil vào controller

    // Đăng nhập bằng username và password
    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody User user) {
        boolean isValidUser = userService.validateUser(user.getUsername(), user.getPassword());
        if (isValidUser) {
            // Tạo JWT cho người dùng
            String token = jwtUtil.generateToken(user.getUsername()); // Sử dụng jwtUtil
            return ResponseEntity.ok("Bearer " + token); // Trả về JWT cho client
        } else {
            return ResponseEntity.status(401).body("Invalid username or password");
        }
    }


    // Đăng ký tài khoản mới
    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody User user) {
        // Kiểm tra nếu username hoặc email đã tồn tại
        if (userService.findByUsername(user.getUsername()).isPresent()) {
            return ResponseEntity.status(409).body("Username already exists");
        }
        if (userService.findByEmail(user.getEmail()).isPresent()) {
            return ResponseEntity.status(409).body("Email already exists");
        }
        // Lưu người dùng mới
        userService.saveUser(new User(user.getUsername(), user.getPassword(), user.getEmail()));
        return ResponseEntity.ok("User registered successfully");
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
            return ResponseEntity.ok("Login successful");
        }
        return ResponseEntity.status(401).body("Invalid OTP or Email");
    }

    // Quên mật khẩu - Đặt lại mật khẩu bằng OTP
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

    @PutMapping("/profile/{userId}")
    public ResponseEntity<String> updateProfile(@PathVariable Long userId, @RequestBody User updatedUser) {
        try {
            userService.updateUserProfile(userId, updatedUser);
            return ResponseEntity.ok("Profile updated successfully");
        } catch (UsernameNotFoundException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }
}
