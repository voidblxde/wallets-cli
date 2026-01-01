package app.util;

import app.cli.AppException;

public final class Validation {
    private Validation() {}

    public static String requireNonBlank(String s, String field) {
        if (s == null || s.isBlank()) throw new AppException(field + " не должно быть пустым.");
        return s;
    }

    public static void requireLogin(String login) {
        requireNonBlank(login, "Логин");
        if (login.contains(" ")) throw new AppException("Логин не должен содержать пробелы.");
    }

    public static void requirePassword(String password) {
        requireNonBlank(password, "Пароль");
        if (password.length() < 4) throw new AppException("Пароль слишком короткий (минимум 4 символа).");
    }
}
