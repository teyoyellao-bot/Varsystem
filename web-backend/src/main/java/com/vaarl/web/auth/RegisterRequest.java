package com.vaarl.web.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank @Size(max = 120) String fullName,
    @NotBlank @Email @Size(max = 255) String email,
    @NotBlank @Size(min = 3, max = 60) String username,
    @NotBlank @Size(max = 40) String phone,
    @Size(max = 160) String shopName,
    @NotBlank String role,
    @NotBlank @Size(min = 8, max = 72) String password,
    @NotBlank String confirmPassword
) {}
