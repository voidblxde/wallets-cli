package app.services;

import app.domain.OperationType;
import app.storage.FileStorage;
import app.storage.UserRepository;
import app.storage.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class TransferServiceTest {

    @TempDir
    Path tempDir;

    private FileStorage storage;
    private UserRepository users;
    private WalletRepository wallets;
    private AuthService auth;
    private WalletService wallet;
    private TransferService transfer;

    @BeforeEach
    void setup() {
        storage = new FileStorage(tempDir);
        users = new UserRepository();
        wallets = new WalletRepository();
        auth = new AuthService(users, wallets, storage);
        wallet = new WalletService(auth, wallets);
        transfer = new TransferService(auth, users, wallets, storage);

        auth.register("ivan", "1234");
        auth.register("petr", "1234");
        auth.login("ivan", "1234");
    }

    @Test
    void transfer_createsExpenseForSender() {
        transfer.transfer("petr", new BigDecimal("1000"), "test");

        var ops = wallets.require("ivan").getOperations();
        assertTrue(ops.stream().anyMatch(o -> o.getType() == OperationType.EXPENSE && o.getAmount().equals(new BigDecimal("1000"))));
    }

    @Test
    void transfer_createsIncomeForReceiver() {
        transfer.transfer("petr", new BigDecimal("1000"), "test");

        var ops = wallets.require("petr").getOperations();
        assertTrue(ops.stream().anyMatch(o -> o.getType() == OperationType.INCOME && o.getAmount().equals(new BigDecimal("1000"))));
    }

    @Test
    void transfer_persistsReceiverWallet_toAvoidReloginLoss() {
        transfer.transfer("petr", new BigDecimal("1000"), "test");

        auth.logout();
        auth.login("petr", "1234"); // должен увидеть данные из файла или памяти

        String totals = wallet.formatTotals(null, null);
        assertTrue(totals.contains("1000"));
    }

    @Test
    void transfer_rejectsSelfTransfer() {
        assertThrows(app.cli.AppException.class, () -> transfer.transfer("ivan", new BigDecimal("1"), null));
    }
}
