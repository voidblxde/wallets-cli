package app.services;

import app.cli.AppException;
import app.domain.Operation;
import app.domain.OperationType;
import app.domain.Wallet;
import app.storage.FileStorage;
import app.storage.UserRepository;
import app.storage.WalletRepository;
import app.util.Money;
import app.util.Validation;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TransferService {

    private final AuthService auth;
    private final UserRepository users;
    private final WalletRepository wallets;
    private final FileStorage storage;

    public TransferService(AuthService auth, UserRepository users, WalletRepository wallets, FileStorage storage) {
        this.auth = auth;
        this.users = users;
        this.wallets = wallets;
        this.storage = storage;
    }

    public List<String> transfer(String toLogin, BigDecimal amount, String comment) {
        String fromLogin = auth.requireCurrentLogin();

        Validation.requireLogin(toLogin);
        Money.requirePositive(amount, "Сумма");
        if (toLogin.equals(fromLogin)) throw new AppException("Нельзя перевести самому себе.");
        if (!users.exists(toLogin)) throw new AppException("Получатель не найден: " + toLogin);

        Wallet from = wallets.require(fromLogin);
        Wallet to = wallets.find(toLogin).orElseGet(() -> {
            Wallet loaded = storage.loadWallet(toLogin);
            if (loaded == null) loaded = new Wallet(toLogin);
            loaded.setOwnerLogin(toLogin);
            wallets.upsert(loaded);
            return loaded;
        });

        UUID transferId = UUID.randomUUID();

        Operation out = new Operation(OperationType.EXPENSE, "Transfer", amount, normalize(comment));
        out.setCounterpartyLogin(toLogin);
        out.setTransferId(transferId);

        Operation in = new Operation(OperationType.INCOME, "Transfer", amount, normalize(comment));
        in.setCounterpartyLogin(fromLogin);
        in.setTransferId(transferId);

        from.getOperations().add(out);
        to.getOperations().add(in);

        wallets.upsert(from);
        wallets.upsert(to);
        storage.saveWallet(from);
        storage.saveWallet(to);

        return new ArrayList<>();
    }

    private String normalize(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
