package kr.ac.cyci.deptrental.auth;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(name = "login_id", nullable = false, unique = true, length = 50)
    private String loginId;
    @Column(name = "password", nullable = false)
    private String passwordHash;
    @Column(name = "user_name", nullable = false, length = 50)
    private String displayName;
    @Column(name = "student_number", unique = true, length = 12)
    private String studentNumber;

    @Column(name = "is_admin", nullable = false)
    private boolean admin = false;

    @Column(name="must_change_password", nullable=false)
    private boolean mustChangePassword;
    @Column(name="temporary_password_expires_at")
    private java.time.Instant temporaryPasswordExpiresAt;
    public boolean mustChangePassword() { return mustChangePassword; }
    public boolean temporaryPasswordExpired(java.time.Instant now) {
        return mustChangePassword && (temporaryPasswordExpiresAt == null || !temporaryPasswordExpiresAt.isAfter(now));
    }
    public void issueTemporaryPassword(String hash, java.time.Instant expiry) {
        passwordHash=hash; mustChangePassword=true; temporaryPasswordExpiresAt=expiry;
    }
    @Column(name="phone_number", length=11)
    private String phoneNumber;
    public String getPhoneNumber() { return phoneNumber; }
    protected User() { }
    public User(String loginId, String passwordHash, String displayName, String studentNumber, String phoneNumber) {
        this.loginId = loginId;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.studentNumber = studentNumber;
        this.phoneNumber = phoneNumber;
    }
    public Long getId() { return id; }
    public String getLoginId() { return loginId; }
    public String getPasswordHash() { return passwordHash; }
    public String getDisplayName() { return displayName; }
    public String getStudentNumber() { return studentNumber; }
    public boolean isAdmin() { return admin; }
    public void changePassword(String passwordHash) { this.passwordHash = passwordHash; mustChangePassword=false; temporaryPasswordExpiresAt=null; }
}
