package app.storage;

import app.domain.Wallet;

import java.util.*;

public class WalletRepository {
    private final Map<String, Wallet> wallets = new HashMap<>();

    public Optional<Wallet> find(String login) {
        return Optional.ofNullable(wallets.get(login));
    }

    public Wallet require(String login) {
        Wallet w = wallets.get(login);
        if (w == null) throw new IllegalStateException("Wallet not loaded for: " + login);
        return w;
    }

    public void upsert(Wallet wallet) {
        wallets.put(wallet.getOwnerLogin(), wallet);
    }

    public Collection<Wallet> getAll() {
        return wallets.values();
    }
}
