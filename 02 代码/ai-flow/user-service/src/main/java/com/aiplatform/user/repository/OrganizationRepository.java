package com.aiplatform.user.repository;

import com.aiplatform.user.domain.Organization;
import com.aiplatform.user.domain.OrgType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, String> {
    Optional<Organization> findByCode(String code);
    List<Organization> findByOrgType(OrgType orgType);
    List<Organization> findByParentId(String parentId);
    List<Organization> findByLevel(Integer level);
    List<Organization> findByManagerId(String managerId);
}