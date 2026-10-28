package kr.ac.cyci.deptrental.auth;

/** Infrastructure boundary: replace this bean with an email/SMS provider implementation in deployment. */
public interface PasswordResetNotifier {
    void send(String email, String resetToken);
}
