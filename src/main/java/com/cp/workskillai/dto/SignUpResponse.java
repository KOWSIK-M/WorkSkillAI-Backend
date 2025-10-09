package com.cp.workskillai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SignUpResponse {
    private boolean success;
    private String message;
    private String userId;
    private String email;
    private String role;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
}