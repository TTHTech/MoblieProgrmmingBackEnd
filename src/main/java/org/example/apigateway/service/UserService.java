package org.example.apigateway.service;

import org.example.apigateway.model.User;
import org.example.apigateway.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
public class UserService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    @Lazy
    private PasswordEncoder passwordEncoder;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            return new org.springframework.security.core.userdetails.User(user.getUsername(), user.getPassword(), new ArrayList<>());
        } else {
            throw new UsernameNotFoundException("User not found with username: " + username);
        }
    }

    public String encodePassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<User> findById(Long userId) {
        return userRepository.findById(userId);
    }

    public User saveUser(User user) {
        // Chỉ mã hóa mật khẩu nếu chưa được mã hóa
        if (!user.getPassword().startsWith("$2a$")) { // kiểm tra xem mật khẩu đã được mã hóa chưa
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        return userRepository.save(user);
    }



    public boolean validateUser(String username, String password) {
        Optional<User> userOpt = findByUsername(username);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            // So sánh mật khẩu đã mã hóa với mật khẩu gốc
            boolean passwordMatches = passwordEncoder.matches(password, user.getPassword());
            System.out.println("Password matches: " + passwordMatches); // Kiểm tra
            return passwordMatches;
        }
        return false;
    }




    public String generateOtp() {
        Random random = new Random();
        return String.valueOf(100000 + random.nextInt(900000));
    }

    public void generateAndSaveOtp(String email) {
        Optional<User> userOpt = findByEmail(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            String otp = generateOtp();
            user.setOtp(otp);
            user.setOtpExpirationTime(LocalDateTime.now().plusMinutes(1));
            saveUser(user);
            sendOtpEmail(user.getEmail(), otp);
        }
    }

    private void sendOtpEmail(String to, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Your OTP Code");
        message.setText("Your OTP code is: " + otp);
        mailSender.send(message);
    }

    public boolean verifyOtp(String email, String otp) {
        Optional<User> userOpt = findByEmail(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            return user.getOtp() != null && user.getOtp().equals(otp) && user.getOtpExpirationTime().isAfter(LocalDateTime.now());
        }
        return false;
    }

    public void resetPassword(String email, String newPassword) {
        Optional<User> userOpt = findByEmail(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setPassword(passwordEncoder.encode(newPassword));
            saveUser(user);
        }
    }

    public User updateUserProfile(Long userId, User updatedUser, String otp) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User existingUser = userOpt.get();

            // Kiểm tra OTP trước khi cập nhật hồ sơ
            if (verifyOtp(existingUser.getEmail(), otp)) {
                existingUser.setFirstName(updatedUser.getFirstName());
                existingUser.setLastName(updatedUser.getLastName());
                existingUser.setPhoneNumber(updatedUser.getPhoneNumber());
                existingUser.setAddress(updatedUser.getAddress());
                existingUser.setDateOfBirth(updatedUser.getDateOfBirth());

                return userRepository.save(existingUser);
            } else {
                throw new IllegalArgumentException("Invalid OTP");
            }
        } else {
            throw new UsernameNotFoundException("User not found with id: " + userId);
        }
    }
    @Scheduled(fixedRate = 60000) // 60000 ms = 1 phút
    public void deleteExpiredUsers() {
        LocalDateTime now = LocalDateTime.now();
        List<User> expiredUsers = userRepository.findAllByOtpExpirationTimeBeforeAndIsActiveFalse(now);
        if (!expiredUsers.isEmpty()) {
            userRepository.deleteAll(expiredUsers);
            System.out.println("Deleted expired users: " + expiredUsers.size());
        }
    }

}
