package com.aiplatform.user.dto.response;

import com.aiplatform.user.domain.entity.User;
import com.aiplatform.user.domain.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private String id;
    private String username;
    private String email;
    private String phone;
    private String fullName;
    private UserStatus status;
    private List<RoleDto> roles;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static UserDto from(User user) {
        return UserDto.builder()
            .id(user.getId())
            .username(user.getUsername())
            .email(user.getEmail())
            .phone(user.getPhone())
            .fullName(user.getFullName())
            .status(user.getStatus())
            .roles(user.getRoles().stream().map(RoleDto::from).toList())
            .createdAt(user.getCreatedAt())
            .updatedAt(user.getUpdatedAt())
            .build();
    }
}