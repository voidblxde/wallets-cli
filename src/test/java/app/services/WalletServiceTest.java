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

class WalletServiceTest {

    @TempDir
    Path tempDir;

    private FileStorage storage;
    private UserRepository users;
    private WalletRepository wallets;
    private AuthService auth;
    private WalletService wallet;

    @BeforeEach
    void setup() {
        storage = new FileStorage(tempDir);
        users = new UserRepository();
        wallets = new WalletRepository();
        auth = new AuthService(users, wallets, storage);
        wallet = new WalletService(auth, wallets);

        auth.register("ivan", "1234");
        auth.login("ivan", "1234");
    }

    @Test
    void addIncomeAndExpense_affectsTotals() {
        wallet.addOperation(OperationType.INCOME, "Salary", new BigDecimal("100"), null);
        wallet.addOperation(OperationType.EXPENSE, "Food", new BigDecimal("30"), null);

        String totals = wallet.formatTotals(null, null);
        assertTrue(totals.contains("Общий доход"));
        assertTrue(totals.contains("100"));
        assertTrue(totals.contains("30"));
    }

    @Test
    void byCategory_incomeSumsCorrectly() {
        wallet.addOperation(OperationType.INCOME, "Salary", new BigDecimal("10"), null);
        wallet.addOperation(OperationType.INCOME, "Salary", new BigDecimal("15"), null);

        String rep = wallet.formatByCategory(OperationType.INCOME, null, null);
        assertTrue(rep.contains("Salary"));
        assertTrue(rep.contains("25"));
    }

    @Test
    void setBudget_andFormatLine_remainingPositive() {
        wallet.setBudget("Food", new BigDecimal("100"));
        wallet.addOperation(OperationType.EXPENSE, "Food", new BigDecimal("40"), null);

        String line = wallet.formatBudgetLine("Food");
        assertTrue(line.contains("Оставшийся бюджет"));
        assertTrue(line.contains("60"));
    }

    @Test
    void budgetOverspend_triggersAlert() {
        wallet.setBudget("Food", new BigDecimal("50"));
        var alerts = wallet.addOperation(OperationType.EXPENSE, "Food", new BigDecimal("60"), null);

        assertTrue(alerts.stream().anyMatch(a -> a.contains("Превышен бюджет")));
    }

    @Test
    void expensesOverIncome_triggersAlert() {
        wallet.addOperation(OperationType.INCOME, "Salary", new BigDecimal("10"), null);
        var alerts = wallet.addOperation(OperationType.EXPENSE, "Food", new BigDecimal("20"), null);

        assertTrue(alerts.stream().anyMatch(a -> a.contains("Расходы превысили доходы")));
    }

    @Test
    void statsForCategories_reportsMissing() {
        wallet.addOperation(OperationType.EXPENSE, "Food", new BigDecimal("10"), null);
        var r = wallet.statsForCategories(java.util.List.of("Food", "Missing"), OperationType.EXPENSE, null, null);

        assertEquals(new BigDecimal("10"), r.total());
        assertTrue(r.missing().contains("Missing"));
    }

    @Test
    void listCategories_containsOpsAndBudgets() {
        wallet.addOperation(OperationType.EXPENSE, "Food", new BigDecimal("10"), null);
        wallet.setBudget("Utilities", new BigDecimal("100"));

        var cats = wallet.listCategories();
        assertTrue(cats.contains("Food"));
        assertTrue(cats.contains("Utilities"));
    }

    @Test
    void removeBudget_removesIfExists() {
        wallet.setBudget("Food", new BigDecimal("100"));
        wallet.removeBudget("Food");

        String rep = wallet.formatBudgetsReport();
        assertTrue(rep.contains("Бюджеты не заданы") || !rep.contains("Food"));
    }
}
