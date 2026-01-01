package app.storage;

import app.domain.User;
import app.domain.Wallet;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.nio.file.*;
import java.util.Collections;
import java.util.List;

public class FileStorage {
    private final Path dataDir;
    private final Path walletsDir;
    private final Path usersFile;
    private final ObjectMapper om;

    public FileStorage(Path dataDir) {
        this.dataDir = dataDir;
        this.walletsDir = dataDir.resolve("wallets");
        this.usersFile = dataDir.resolve("users.json");

        this.om = new ObjectMapper();
        this.om.registerModule(new JavaTimeModule());

        ensureDirs();
    }

    private void ensureDirs() {
        try {
            Files.createDirectories(dataDir);
            Files.createDirectories(walletsDir);
        } catch (IOException e) {
            throw new RuntimeException("Cannot create data directories: " + e.getMessage(), e);
        }
    }

    public List<User> loadUsers() {
        if (!Files.exists(usersFile)) return Collections.emptyList();
        try {
            return om.readValue(usersFile.toFile(), new TypeReference<List<User>>() {});
        } catch (Exception e) {
            throw new RuntimeException("Cannot read users.json: " + e.getMessage(), e);
        }
    }

    public void saveUsers(List<User> users) {
        try {
            om.writerWithDefaultPrettyPrinter().writeValue(usersFile.toFile(), users);
        } catch (Exception e) {
            throw new RuntimeException("Cannot write users.json: " + e.getMessage(), e);
        }
    }

    public Wallet loadWallet(String login) {
        Path file = walletsDir.resolve(login + ".json");
        if (!Files.exists(file)) return null;
        try {
            return om.readValue(file.toFile(), Wallet.class);
        } catch (Exception e) {
            throw new RuntimeException("Cannot read wallet for " + login + ": " + e.getMessage(), e);
        }
    }

    public void saveWallet(Wallet wallet) {
        Path file = walletsDir.resolve(wallet.getOwnerLogin() + ".json");
        try {
            om.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), wallet);
        } catch (Exception e) {
            throw new RuntimeException("Cannot write wallet for " + wallet.getOwnerLogin() + ": " + e.getMessage(), e);
        }
    }

    public void saveWallets(Iterable<Wallet> wallets) {
        for (Wallet w : wallets) saveWallet(w);
    }

    public void exportWallet(Wallet wallet, Path path) {
        try {
            if (path.getParent() != null) Files.createDirectories(path.getParent());
            om.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), wallet);
        } catch (Exception e) {
            throw new RuntimeException("Cannot export wallet to " + path + ": " + e.getMessage(), e);
        }
    }

    public Wallet importWallet(Path path) {
        if (!Files.exists(path)) {
            throw new RuntimeException("Import file not found: " + path);
        }
        try {
            return om.readValue(path.toFile(), Wallet.class);
        } catch (Exception e) {
            throw new RuntimeException("Cannot import wallet from " + path + ": " + e.getMessage(), e);
        }
    }

}
