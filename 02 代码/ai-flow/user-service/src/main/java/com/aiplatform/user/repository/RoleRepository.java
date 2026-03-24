package com.aiplatform.user.repository;

import com.aiplatform.user.domain.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, String> {
    Optional<Role> findByCode(String code);
    boolean existsByCode(String code);
    List<Role> findByIdIn(List<String> ids);
}