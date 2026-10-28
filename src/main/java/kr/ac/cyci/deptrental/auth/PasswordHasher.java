package kr.ac.cyci.deptrental.auth;

import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import org.springframework.stereotype.Component;

@Component
public class PasswordHasher {
    private static final int ITERATIONS = 210_000;
    private static final int KEY_LENGTH = 256;
    private final SecureRandom random = new SecureRandom();

    public String hash(String password) {
        byte[] salt = new byte[16]; random.nextBytes(salt);
        return ITERATIONS + ":" + Base64.getEncoder().encodeToString(salt) + ":" +
                Base64.getEncoder().encodeToString(derive(password.toCharArray(), salt, ITERATIONS));
    }
    public boolean matches(String password, String encoded) {
        try {
            String[] parts = encoded.split(":");
            int iterations = Integer.parseInt(parts[0]);
            byte[] salt = Base64.getDecoder().decode(parts[1]);
            byte[] expected = Base64.getDecoder().decode(parts[2]);
            return java.security.MessageDigest.isEqual(expected, derive(password.toCharArray(), salt, iterations));
        } catch (RuntimeException e) { return false; }
    }
    private byte[] derive(char[] password, byte[] salt, int iterations) {
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(new PBEKeySpec(password, salt, iterations, KEY_LENGTH)).getEncoded();
        } catch (Exception e) { throw new IllegalStateException("Password hashing is unavailable", e); }
    }
}
