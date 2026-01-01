package app.storage;

import app.domain.Operation;
import app.domain.OperationType;
import app.domain.Wallet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileStorageExportImportTest {

    @TempDir
    Path tempDir;

    @Test
    void exportAndImport_walletRoundTrip() {
        FileStorage storage = new FileStorage(tempDir);

        Wallet w = new Wallet("ivan");
        w.getOperations().add(new Operation(OperationType.INCOME, "Salary", new BigDecimal("10"), "x"));

        Path out = tempDir.resolve("export.json");
        storage.exportWallet(w, out);

        Wallet imported = storage.importWallet(out);
        assertEquals("ivan", imported.getOwnerLogin());
        assertEquals(1, imported.getOperations().size());
        assertEquals(new BigDecimal("10"), imported.getOperations().get(0).getAmount());
    }
}
