package app.services;

import app.cli.AppException;
import app.domain.User;
import app.domain.Wallet;
import app.storage.FileStorage;
import app.storage.UserRepository;
import app.storage.WalletRepository;
import app.util.PasswordHasher;
import app.util.Validation;

import java.util.Optional;

public class AuthService {
    private final UserRepository users;
    private final WalletRepository wallets;
    private final FileStorage storage;

    private String currentLogin;

    public AuthService(UserRepository users, WalletRepository wallets, FileStorage storage) {
        this.users = users;
        this.wallets = wallets;
        this.storage = storage;
    }

    public Optional<String> currentLogin() {
        return Optional.ofNullable(currentLogin);
    }

    public String requireCurrentLogin() {
        if (currentLogin == null) throw new AppException("Нужно авторизоваться (login).");
        return currentLogin;
    }

    public void register(String login, String password) {
        Validation.requireLogin(login);
        Validation.requirePassword(password);

        if (users.exists(login)) throw new AppException("Пользователь уже существует: " + login);

        String salt = PasswordHasher.newSalt();
        String hash = PasswordHasher.hash(password, salt);

        users.upsert(new User(login, hash, salt));

        Wallet w = new Wallet(login);
        wallets.upsert(w);

        storage.saveWallet(w);
        storage.saveUsers(users.getAll());
    }

    public void login(String login, String password) {
        Validation.requireLogin(login);
        Validation.requirePassword(password);

        User u = users.find(login).orElseThrow(() -> new AppException("Пользователь не найден: " + login));
        String expected = PasswordHasher.hash(password, u.getSalt());
        if (!expected.equals(u.getPasswordHash())) throw new AppException("Неверный пароль.");

        currentLogin = login;

        Wallet current = wallets.find(login).orElse(null);
        if (current != null) {
            currentLogin = login;
            return;
        }

        Wallet loaded = storage.loadWallet(login);
        if (loaded == null) loaded = new Wallet(login);
        loaded.setOwnerLogin(login);
        wallets.upsert(loaded);

        currentLogin = login;

    }

    public void logout() {
        currentLogin = null;
    }
}
