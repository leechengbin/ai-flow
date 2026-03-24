package com.aiplatform.user.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_organizations",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "organization_id", "role_in_org"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserOrganization {
    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(name = "role_in_org", length = 50)
    private String roleInOrg;

    @Column(name = "user_title", length = 50)
    private String userTitle;

    @Column(updatable = false)
    private LocalDateTime joinedAt;

    @PrePersist
    protected void onCreate() {
        joinedAt = LocalDateTime.now();
        if (id == null) id = UUID.randomUUID().toString();
    }
}