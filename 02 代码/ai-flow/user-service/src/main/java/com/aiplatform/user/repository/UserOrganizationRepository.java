package com.aiplatform.user.repository;

import com.aiplatform.user.domain.UserOrganization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserOrganizationRepository extends JpaRepository<UserOrganization, String> {
    List<UserOrganization> findByUserId(String userId);
    List<UserOrganization> findByOrganizationId(String organizationId);
    Optional<UserOrganization> findByUserIdAndOrganizationId(String userId, String organizationId);
    boolean existsByUserIdAndOrganizationId(String userId, String organizationId);
    void deleteByUserIdAndOrganizationId(String userId, String organizationId);
}