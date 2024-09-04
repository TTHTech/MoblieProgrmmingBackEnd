package org.example.apigateway.service;

import org.example.apigateway.model.User;
import org.example.apigateway.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JavaMailSender mailSender;

    // Tìm người dùng theo username
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    // Tìm người dùng theo email
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    // Lưu người dùng
    public User saveUser(User user) {
        return userRepository.save(user);
    }

    // Phương thức xác thực đăng nhập bằng username và password
    public boolean validateUser(String username, String password) {
        Optional<User> userOpt = findByUsername(username);
        if (userOpt.isPresent()) {
            return userOpt.get().getPassword().equals(password);
        }
        return false;
    }

    // Tạo mã OTP ngẫu nhiên
    public String generateOtp() {
        Random random = new Random();
        return String.valueOf(100000 + random.nextInt(900000)); // Tạo OTP gồm 6 chữ số
    }

    // Tạo và lưu OTP cho người dùng
    public void generateAndSaveOtp(String email) {
        Optional<User> userOpt = findByEmail(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            String otp = generateOtp();
            user.setOtp(otp);
            user.setOtpExpirationTime(LocalDateTime.now().plusMinutes(10)); // OTP có hiệu lực trong 10 phút
            saveUser(user); // Cập nhật thông tin người dùng
            sendOtpEmail(user.getEmail(), otp); // Gửi OTP qua email
        }
    }

    // Gửi OTP qua email
    private void sendOtpEmail(String to, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Your OTP Code");
        message.setText("Your OTP code is: " + otp);
        mailSender.send(message);
    }

    // Xác minh OTP và kiểm tra thời gian hết hạn
    public boolean verifyOtp(String email, String otp) {
        Optional<User> userOpt = findByEmail(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            return user.getOtp() != null && user.getOtp().equals(otp) && user.getOtpExpirationTime().isAfter(LocalDateTime.now());
        }
        return false;
    }

    // Đặt lại mật khẩu mới cho người dùng
    public void resetPassword(String email, String newPassword) {
        Optional<User> userOpt = findByEmail(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setPassword(newPassword); // Lưu mật khẩu mới
            saveUser(user); // Cập nhật thông tin người dùng
        }
    }
}
