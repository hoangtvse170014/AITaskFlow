package com.taskflow.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "invitations", indexes = {
    @Index(name = "idx_invitations_workspace_email", columnList = "workspace_id, email"),
    @Index(name = "idx_invitations_token", columnList = "token", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Invitation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(nullable = false, length = 50)
    private String role;

    @Column(nullable = false, unique = true, length = 100)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private InvitationStatus status = InvitationStatus.PENDING;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invited_by")
    private User invitedBy;

    public enum InvitationStatus {
        PENDING,
        ACCEPTED,
        EXPIRED,
        REVOKED
    }
}
