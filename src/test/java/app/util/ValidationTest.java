package app.util;

import app.cli.AppException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ValidationTest {

    @Test
    void requireLogin_rejectsBlank() {
        assertThrows(AppException.class, () -> Validation.requireLogin("  "));
    }

    @Test
    void requireLogin_rejectsSpaces() {
        assertThrows(AppException.class, () -> Validation.requireLogin("a b"));
    }

    @Test
    void requirePassword_rejectsTooShort() {
        assertThrows(AppException.class, () -> Validation.requirePassword("123"));
    }

    @Test
    void requireNonBlank_ok() {
        assertEquals("x", Validation.requireNonBlank("x", "field"));
    }
}
