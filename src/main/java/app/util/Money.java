package app.util;

import app.cli.AppException;

import java.math.BigDecimal;

public final class Money {
    private Money() {}

    public static BigDecimal parsePositive(String s) {
        BigDecimal v = parse(s);
        requirePositive(v, "Сумма");
        return v;
    }

    public static BigDecimal parseNonNegative(String s) {
        BigDecimal v = parse(s);
        requireNonNegative(v, "Сумма");
        return v;
    }

    public static BigDecimal parse(String s) {
        if (s == null) throw new AppException("Сумма не задана.");
        try {
            return new BigDecimal(s.replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new AppException("Некорректная сумма: " + s);
        }
    }

    public static void requirePositive(BigDecimal v, String name) {
        if (v == null || v.compareTo(BigDecimal.ZERO) <= 0) {
            throw new AppException(name + " должна быть > 0");
        }
    }

    public static void requireNonNegative(BigDecimal v, String name) {
        if (v == null || v.compareTo(BigDecimal.ZERO) < 0) {
            throw new AppException(name + " должна быть >= 0");
        }
    }
}
