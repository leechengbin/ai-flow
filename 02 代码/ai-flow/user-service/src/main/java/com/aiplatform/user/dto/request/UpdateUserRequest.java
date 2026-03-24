package com.aiplatform.user.dto.request;

import com.aiplatform.user.domain.enums.UserStatus;
import jakarta.validation.constraints.Email;
import lombok.Data;

@Data
public class UpdateUserRequest {
    @Email(message = "Invalid email format")
    private String email;

    private String phone;

    private String fullName;

    private UserStatus status;
}