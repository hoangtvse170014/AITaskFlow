package com.taskflow.service;

import com.taskflow.dto.request.GoogleAuthRequest;
import com.taskflow.dto.request.LoginRequest;
import com.taskflow.dto.request.RegisterRequest;
import com.taskflow.dto.response.AuthResponse;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refreshToken(String refreshToken);

    AuthResponse getCurrentUser();

    AuthResponse authenticateWithGoogle(GoogleAuthRequest request);
}
