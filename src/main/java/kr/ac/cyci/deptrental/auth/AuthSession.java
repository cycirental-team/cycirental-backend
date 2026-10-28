package kr.ac.cyci.deptrental.auth;

import java.time.Instant;
import jakarta.persistence.*;

@Entity
@Table(name = "auth_sessions")
public class AuthSession {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    protected AuthSession() { }
    public AuthSession(String tokenHash, User user, Instant expiresAt) {
        this.tokenHash = tokenHash; this.user = user; this.expiresAt = expiresAt;
    }
    public boolean isExpired(Instant now) { return !expiresAt.isAfter(now); }
}
