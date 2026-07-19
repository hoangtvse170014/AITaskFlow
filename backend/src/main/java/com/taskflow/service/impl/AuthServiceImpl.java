package com.taskflow.service.impl;

import com.taskflow.dto.request.GoogleAuthRequest;
import com.taskflow.dto.request.LoginRequest;
import com.taskflow.dto.request.RefreshTokenRequest;
import com.taskflow.dto.request.RegisterRequest;
import com.taskflow.dto.response.AuthResponse;
import com.taskflow.dto.response.GoogleUserInfo;
import com.taskflow.dto.response.UserResponse;
import com.taskflow.entity.Role;
import com.taskflow.entity.User;
import com.taskflow.entity.Workspace;
import com.taskflow.entity.WorkspaceMember;
import com.taskflow.exception.BadRequestException;
import com.taskflow.exception.OAuth2AuthenticationException;
import com.taskflow.exception.ResourceNotFoundException;
import com.taskflow.repository.RoleRepository;
import com.taskflow.repository.UserRepository;
import com.taskflow.repository.WorkspaceMemberRepository;
import com.taskflow.repository.WorkspaceRepository;
import com.taskflow.security.JwtTokenProvider;
import com.taskflow.service.AuthService;
import com.taskflow.service.GoogleService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final AuthenticationManager authenticationManager;
    private final GoogleService googleService;

    @Value("${app.auth.frontend-base-url:http://localhost:3000}")
    private String frontendBaseUrl;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already registered");
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .build();

        user = userRepository.save(user);

        String defaultWorkspaceName = user.getFullName() + "'s Workspace";
        String defaultSlug = generateSlug(defaultWorkspaceName);

        int counter = 1;
        String slug = defaultSlug;
        while (workspaceRepository.existsBySlug(slug)) {
            slug = defaultSlug + "-" + counter++;
        }

        Workspace workspace = Workspace.builder()
                .name(defaultWorkspaceName)
                .slug(slug)
                .description("Personal workspace")
                .owner(user)
                .build();

        workspace = workspaceRepository.save(workspace);

        Role ownerRole = roleRepository.findByNameWithPermissions(Role.OWNER)
                .orElseThrow(() -> new ResourceNotFoundException("Owner role not found"));

        WorkspaceMember ownerMember = WorkspaceMember.builder()
                .workspace(workspace)
                .user(user)
                .role(ownerRole)
                .joinedAt(java.time.LocalDateTime.now())
                .build();

        workspaceMemberRepository.save(ownerMember);

        String accessToken = tokenProvider.generateAccessToken(user.getEmail());
        String refreshToken = tokenProvider.generateRefreshToken(user.getEmail());

        return AuthResponse.builder()
                .user(UserResponse.fromEntityWithWorkspace(user, workspace.getId().toString()))
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .workspaceId(workspace.getId().toString())
                .build();
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", request.getEmail()));

        // Get user's workspace
        String workspaceId = workspaceMemberRepository.findByUserId(user.getId()).stream()
                .findFirst()
                .map(wm -> wm.getWorkspace().getId().toString())
                .orElse(null);

        String accessToken = tokenProvider.generateAccessToken(user.getEmail());
        String refreshToken = tokenProvider.generateRefreshToken(user.getEmail());

        return AuthResponse.builder()
                .user(UserResponse.fromEntityWithWorkspace(user, workspaceId))
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .workspaceId(workspaceId)
                .build();
    }

    @Override
    public AuthResponse refreshToken(String refreshToken) {
        if (!tokenProvider.validateRefreshToken(refreshToken)) {
            throw new BadRequestException("Invalid refresh token");
        }

        String email = tokenProvider.getEmailFromToken(refreshToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        // Get user's workspace
        String workspaceId = workspaceMemberRepository.findByUserId(user.getId()).stream()
                .findFirst()
                .map(wm -> wm.getWorkspace().getId().toString())
                .orElse(null);

        String newAccessToken = tokenProvider.generateAccessToken(email);
        String newRefreshToken = tokenProvider.generateRefreshToken(email);

        return AuthResponse.builder()
                .user(UserResponse.fromEntityWithWorkspace(user, workspaceId))
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .workspaceId(workspaceId)
                .build();
    }

    @Override
    public AuthResponse getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        String workspaceId = workspaceMemberRepository.findByUserId(user.getId()).stream()
                .findFirst()
                .map(wm -> wm.getWorkspace().getId().toString())
                .orElse(null);

        return AuthResponse.builder()
                .user(UserResponse.fromEntityWithWorkspace(user, workspaceId))
                .build();
    }

    @Override
    @Transactional
    public AuthResponse authenticateWithGoogle(GoogleAuthRequest request) {
        if (request.getCredential() == null || request.getCredential().isBlank()) {
            throw new OAuth2AuthenticationException("Missing Google credential");
        }

        GoogleUserInfo googleUser;
        if (request.getClientId() != null && !request.getClientId().isBlank()) {
            googleUser = googleService.verifyIdToken(request.getCredential(), request.getClientId());
        } else {
            String accessToken = extractAccessToken(request.getCredential());
            googleUser = googleService.fetchUserInfo(accessToken);
        }

        String email = googleUser.getEmail();
        String avatarUrl = googleUser.getPicture();

        Optional<User> existingUser = userRepository.findByEmailAndProvider(email, "GOOGLE");

        User user;
        String workspaceId;

        if (existingUser.isPresent()) {
            user = existingUser.get();

            if (avatarUrl != null && !avatarUrl.equals(user.getAvatarUrl())) {
                user.setAvatarUrl(avatarUrl);
                user.setProviderId(googleUser.getSub());
            }
        } else {
            Optional<User> localUser = userRepository.findByEmail(email);

            if (localUser.isPresent()) {
                user = localUser.get();

                if (user.getProvider() == null || "LOCAL".equalsIgnoreCase(user.getProvider())) {
                    user.setProvider("GOOGLE");
                    user.setProviderId(googleUser.getSub());
                    if (avatarUrl != null) {
                        user.setAvatarUrl(avatarUrl);
                    }
                }
            } else {
                user = User.builder()
                        .email(email)
                        .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                        .fullName(resolveFullName(googleUser))
                        .avatarUrl(avatarUrl)
                        .provider("GOOGLE")
                        .providerId(googleUser.getSub())
                        .build();

                user = userRepository.save(user);

                String defaultWorkspaceName = user.getFullName() + "'s Workspace";
                String defaultSlug = generateSlug(defaultWorkspaceName);

                int counter = 1;
                String slug = defaultSlug;
                while (workspaceRepository.existsBySlug(slug)) {
                    slug = defaultSlug + "-" + counter++;
                }

                Workspace workspace = Workspace.builder()
                        .name(defaultWorkspaceName)
                        .slug(slug)
                        .description("Personal workspace")
                        .owner(user)
                        .build();

                workspaceRepository.save(workspace);

                Role ownerRole = roleRepository.findByNameWithPermissions(Role.OWNER)
                        .orElseThrow(() -> new ResourceNotFoundException("Owner role not found"));

                WorkspaceMember ownerMember = WorkspaceMember.builder()
                        .workspace(workspace)
                        .user(user)
                        .role(ownerRole)
                        .joinedAt(java.time.LocalDateTime.now())
                        .build();

                workspaceMemberRepository.save(ownerMember);
            }
        }

        user = userRepository.save(user);

        workspaceId = workspaceMemberRepository.findByUserId(user.getId()).stream()
                .findFirst()
                .map(wm -> wm.getWorkspace().getId().toString())
                .orElse(null);

        String accessToken = tokenProvider.generateAccessToken(user.getEmail());
        String refreshToken = tokenProvider.generateRefreshToken(user.getEmail());

        return AuthResponse.builder()
                .user(UserResponse.fromEntityWithWorkspace(user, workspaceId))
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .workspaceId(workspaceId)
                .build();
    }

    private String resolveFullName(GoogleUserInfo googleUser) {
        if (googleUser.getName() != null && !googleUser.getName().isBlank()) {
            return googleUser.getName();
        }
        StringBuilder name = new StringBuilder();
        if (googleUser.getGivenName() != null) {
            name.append(googleUser.getGivenName());
        }
        if (googleUser.getFamilyName() != null && !googleUser.getFamilyName().isBlank()) {
            if (name.length() > 0) {
                name.append(" ");
            }
            name.append(googleUser.getFamilyName());
        }
        if (name.length() > 0) {
            return name.toString();
        }
        return googleUser.getEmail();
    }

    private String extractAccessToken(String credential) {
        if (credential.startsWith("ya29.")) {
            return credential;
        }
        throw new OAuth2AuthenticationException("Unsupported Google credential format");
    }

    private String generateSlug(String name) {
        String normalized = Normalizer.normalize(name, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        String slug = pattern.matcher(normalized).replaceAll("");
        slug = slug.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
        return slug;
    }
}
