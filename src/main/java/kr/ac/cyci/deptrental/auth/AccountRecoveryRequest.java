package kr.ac.cyci.deptrental.auth;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="password_reset_requests")
public class AccountRecoveryRequest {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="user_id",nullable=false) private User user;
    @Column(name="requested_at",nullable=false) private Instant requestedAt;
    @Column(nullable=false,length=16) private String status="PENDING";
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="processed_by") private User processedBy;
    @Column(name="processed_at") private Instant processedAt;
    protected AccountRecoveryRequest() { }
    public AccountRecoveryRequest(User user) { this.user=user; requestedAt=Instant.now(); }
    public Long getId(){return id;}
    public User getUser(){return user;}
    public Instant getRequestedAt(){return requestedAt;}
    public String getStatus(){return status;}
    public void finish(User admin, boolean approve) { status=approve?"COMPLETED":"REJECTED"; processedBy=admin; processedAt=Instant.now(); }
}
