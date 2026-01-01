package app.util;

import app.cli.AppException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class MoneyTest {

    @Test
    void parse_acceptsDot() {
        assertEquals(new BigDecimal("10.50"), Money.parse("10.50"));
    }

    @Test
    void parse_acceptsComma() {
        assertEquals(new BigDecimal("10.50"), Money.parse("10,50"));
    }

    @Test
    void parse_throwsOnInvalid() {
        assertThrows(AppException.class, () -> Money.parse("abc"));
    }

    @Test
    void parsePositive_throwsOnZero() {
        assertThrows(AppException.class, () -> Money.parsePositive("0"));
    }

    @Test
    void parsePositive_throwsOnNegative() {
        assertThrows(AppException.class, () -> Money.parsePositive("-1"));
    }

    @Test
    void parseNonNegative_allowsZero() {
        assertEquals(new BigDecimal("0"), Money.parseNonNegative("0"));
    }
}
