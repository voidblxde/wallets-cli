package app.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;

public final class PasswordHasher {
    private static final SecureRandom RNG = new SecureRandom();

    private PasswordHasher() {}

    public static String newSalt() {
        byte[] b = new byte[16];
        RNG.nextBytes(b);
        return toHex(b);
    }

    public static String hash(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] input = (salt + ":" + password).getBytes(StandardCharsets.UTF_8);
            return toHex(md.digest(input));
        } catch (Exception e) {
            throw new RuntimeException("Hash error: " + e.getMessage(), e);
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
