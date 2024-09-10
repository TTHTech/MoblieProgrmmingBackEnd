package org.example.apigateway.service;

import org.example.apigateway.model.User;
import org.example.apigateway.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
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
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    public boolean validateUser(String username, String password) {
        Optional<User> userOpt = findByUsername(username);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            System.out.println("Username input: " + username);
            System.out.println("Password input: " + password);
            System.out.println("Password from DB (encrypted): " + user.getPassword());

            boolean isPasswordMatch = passwordEncoder.matches(password, user.getPassword());
            System.out.println("Password matches: " + isPasswordMatch);

            return isPasswordMatch;
        } else {
            System.out.println("User not found for username: " + username);
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
            user.setOtpExpirationTime(LocalDateTime.now().plusMinutes(10));
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

}
