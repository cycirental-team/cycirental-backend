package kr.ac.cyci.deptrental.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Safe default that never exposes a reset token in an API response or application log. */
@Component
public class UnavailablePasswordResetNotifier implements PasswordResetNotifier {
    private static final Logger log = LoggerFactory.getLogger(UnavailablePasswordResetNotifier.class);
    @Override public void send(String email, String resetToken) {
        log.warn("Password reset requested for {} but no email provider is configured", email);
    }
}
