package kr.ac.cyci.deptrental.auth;

import java.time.Instant;
import jakarta.persistence.*;

@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetToken {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
    @Column(name = "used_at")
    private Instant usedAt;

    protected PasswordResetToken() { }
    public PasswordResetToken(String tokenHash, User user, Instant expiresAt) {
        this.tokenHash = tokenHash; this.user = user; this.expiresAt = expiresAt;
    }
    public boolean isUsable(Instant now) { return usedAt == null && expiresAt.isAfter(now); }
    public User getUser() { return user; }
    public void markUsed(Instant usedAt) { this.usedAt = usedAt; }
}
