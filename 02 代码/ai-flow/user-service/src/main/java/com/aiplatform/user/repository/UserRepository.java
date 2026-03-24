package com.aiplatform.user.repository;

import com.aiplatform.user.domain.User;
import com.aiplatform.user.domain.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    List<User> findByStatus(UserStatus status);
    List<User> findByIdIn(List<String> ids);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}