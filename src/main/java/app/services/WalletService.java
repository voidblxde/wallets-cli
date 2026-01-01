package app.services;

import app.cli.AppException;
import app.domain.*;
import app.storage.WalletRepository;
import app.util.Money;
import app.util.Validation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class WalletService {
    private final AuthService auth;
    private final WalletRepository wallets;

    public WalletService(AuthService auth, WalletRepository wallets) {
        this.auth = auth;
        this.wallets = wallets;
    }

    public List<String> addOperation(OperationType type, String category, BigDecimal amount, String comment) {
        Validation.requireNonBlank(category, "Категория");
        Money.requirePositive(amount, "Сумма");

        Wallet w = currentWallet();
        Operation op = new Operation(type, category, amount, normalizeComment(comment));
        w.getOperations().add(op);

        return buildAlerts(w, op);
    }

    public void setBudget(String category, BigDecimal limit) {
        Validation.requireNonBlank(category, "Категория");
        Money.requireNonNegative(limit, "Бюджет");

        Wallet w = currentWallet();
        w.getBudgets().put(category, new CategoryBudget(category, limit));
    }

    public void removeBudget(String category) {
        Validation.requireNonBlank(category, "Категория");
        Wallet w = currentWallet();
        w.getBudgets().remove(category);
    }

    public List<String> listCategories() {
        Wallet w = currentWallet();

        // категории из операций
        Set<String> categories = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (Operation op : w.getOperations()) {
            if (op.getCategory() != null && !op.getCategory().isBlank()) {
                categories.add(op.getCategory());
            }
        }
        // категории из бюджетов
        categories.addAll(w.getBudgets().keySet());

        return new ArrayList<>(categories);
    }

    public List<Operation> list(OperationType type, String category, LocalDate from, LocalDate to) {
        Wallet w = currentWallet();
        return w.getOperations().stream()
                .filter(op -> type == null || op.getType() == type)
                .filter(op -> category == null || category.isBlank() || op.getCategory().equalsIgnoreCase(category.trim()))
                .filter(op -> inDateRange(op, from, to))
                .sorted(Comparator.comparing(Operation::getCreatedAt))
                .toList();
    }

    public String formatTotals(LocalDate from, LocalDate to) {
        Wallet w = currentWallet();
        BigDecimal income = sum(w, OperationType.INCOME, null, from, to);
        BigDecimal expense = sum(w, OperationType.EXPENSE, null, from, to);
        BigDecimal balance = income.subtract(expense);

        return """
Итого:
  Общий доход: %s
  Общие расходы: %s
  Баланс: %s
""".formatted(income, expense, balance);
    }

    public String formatByCategory(OperationType type, LocalDate from, LocalDate to) {
        Wallet w = currentWallet();
        Map<String, BigDecimal> map = sumByCategory(w, type, from, to);
        if (map.isEmpty()) return "Нет данных.";

        StringBuilder sb = new StringBuilder(type == OperationType.INCOME ? "Доходы по категориям:\n" : "Расходы по категориям:\n");
        map.forEach((k, v) -> sb.append("  ").append(k).append(": ").append(v).append("\n"));
        return sb.toString();
    }

    public String formatBudgetsReport() {
        Wallet w = currentWallet();
        if (w.getBudgets().isEmpty()) return "Бюджеты не заданы.";

        StringBuilder sb = new StringBuilder("Бюджет по категориям:\n");
        for (String cat : w.getBudgets().keySet()) {
            sb.append("  ").append(formatBudgetLine(cat)).append("\n");
        }
        return sb.toString();
    }

    public String formatBudgetLine(String category) {
        Wallet w = currentWallet();
        CategoryBudget b = w.getBudgets().get(category);
        if (b == null) return category + ": бюджет не задан";

        BigDecimal spent = sum(w, OperationType.EXPENSE, category, null, null);
        BigDecimal remaining = b.getLimit().subtract(spent);

        return "%s: %s, Оставшийся бюджет: %s".formatted(category, b.getLimit(), remaining);
    }

    public CategoriesStatsResult statsForCategories(List<String> categories, OperationType type, LocalDate from, LocalDate to) {
        Wallet w = currentWallet();
        Set<String> existing = w.getOperations().stream()
                .map(Operation::getCategory)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        List<String> missing = new ArrayList<>();
        Map<String, BigDecimal> by = new LinkedHashMap<>();
        BigDecimal total = BigDecimal.ZERO;

        for (String c : categories) {
            String key = c.trim();
            if (key.isEmpty()) continue;

            if (!existing.contains(key.toLowerCase())) {
                missing.add(key);
                continue;
            }

            BigDecimal s = sum(w, type, key, from, to);
            by.put(key, s);
            total = total.add(s);
        }

        return new CategoriesStatsResult(total, by, missing);
    }

    public record CategoriesStatsResult(BigDecimal total, Map<String, BigDecimal> byCategory, List<String> missing) {}

    // ---- helpers ----

    private Wallet currentWallet() {
        String login = auth.requireCurrentLogin();
        return wallets.require(login);
    }

    private boolean inDateRange(Operation op, LocalDate from, LocalDate to) {
        LocalDate d = op.getCreatedAt().toLocalDate();
        if (from != null && d.isBefore(from)) return false;
        if (to != null && d.isAfter(to)) return false;
        return true;
    }

    private BigDecimal sum(Wallet w, OperationType type, String category, LocalDate from, LocalDate to) {
        return w.getOperations().stream()
                .filter(op -> op.getType() == type)
                .filter(op -> category == null || op.getCategory().equalsIgnoreCase(category))
                .filter(op -> inDateRange(op, from, to))
                .map(Operation::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Map<String, BigDecimal> sumByCategory(Wallet w, OperationType type, LocalDate from, LocalDate to) {
        Map<String, BigDecimal> map = new LinkedHashMap<>();
        for (Operation op : w.getOperations()) {
            if (op.getType() != type) continue;
            if (!inDateRange(op, from, to)) continue;
            map.merge(op.getCategory(), op.getAmount(), BigDecimal::add);
        }
        return map;
    }

    private List<String> buildAlerts(Wallet w, Operation lastOp) {
        List<String> alerts = new ArrayList<>();

        if (lastOp.getType() == OperationType.EXPENSE) {
            CategoryBudget b = w.getBudgets().get(lastOp.getCategory());
            if (b != null) {
                BigDecimal spent = sum(w, OperationType.EXPENSE, lastOp.getCategory(), null, null);
                if (spent.compareTo(b.getLimit()) > 0) {
                    alerts.add("Превышен бюджет по категории '" + lastOp.getCategory()
                            + "': лимит=" + b.getLimit() + ", потрачено=" + spent);
                }
            }
        }

        BigDecimal income = sum(w, OperationType.INCOME, null, null, null);
        BigDecimal expense = sum(w, OperationType.EXPENSE, null, null, null);
        if (expense.compareTo(income) > 0) {
            alerts.add("Расходы превысили доходы: доход=" + income + ", расход=" + expense);
        }

        return alerts;
    }

    private String normalizeComment(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
