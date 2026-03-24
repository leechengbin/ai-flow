package com.aiplatform.user.repository;

import com.aiplatform.user.domain.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, String> {
    Optional<Permission> findByCode(String code);
    List<Permission> findByResource(String resource);
    List<Permission> findByResourceAndAction(String resource, String action);
}