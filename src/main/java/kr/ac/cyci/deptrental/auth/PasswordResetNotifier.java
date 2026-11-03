package kr.ac.cyci.deptrental.auth;

/** A future verified recovery flow must resolve a secure delivery channel.
 * A student number is an identifier, NOT proof of identity or an email address.
 * Never return or log the raw reset token. */
public interface PasswordResetNotifier {
    void send(String studentNumber, String resetToken);
}
