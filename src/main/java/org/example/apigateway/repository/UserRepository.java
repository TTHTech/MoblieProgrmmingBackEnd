package org.example.apigateway.repository;

import org.example.apigateway.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Optional<User> findById(Long id);
    List<User> findAllByOtpExpirationTimeBeforeAndIsActiveFalse(LocalDateTime expirationTime);
}
