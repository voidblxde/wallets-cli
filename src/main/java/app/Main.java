package app;

import app.cli.CommandLoop;
import app.services.*;
import app.storage.*;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

public class Main {
    public static void main(String[] args) {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(System.err, true, StandardCharsets.UTF_8));

        Path dataDir = Path.of("data");

        FileStorage storage = new FileStorage(dataDir);
        UserRepository userRepo = new UserRepository();
        WalletRepository walletRepo = new WalletRepository();

        storage.loadUsers().forEach(userRepo::upsert);

        AuthService authService = new AuthService(userRepo, walletRepo, storage);
        WalletService walletService = new WalletService(authService, walletRepo);
        TransferService transferService = new TransferService(authService, userRepo, walletRepo, storage);
        ReportService reportService = new ReportService();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                storage.saveUsers(userRepo.getAll());
                storage.saveWallets(walletRepo.getAll());
                reportService.close();
            } catch (Exception ignored) {}
        }));

        new CommandLoop(
                authService,
                walletService,
                transferService,
                reportService,
                storage,
                userRepo,
                walletRepo
        ).run();
    }
}
