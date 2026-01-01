package app.cli;

import app.domain.Operation;
import app.domain.OperationType;
import app.services.*;
import app.storage.FileStorage;
import app.storage.UserRepository;
import app.storage.WalletRepository;
import app.util.Money;
import app.util.TablePrinter;
import app.util.Validation;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class CommandLoop {
    private final AuthService auth;
    private final WalletService wallet;
    private final TransferService transfer;
    private final ReportService report;
    private final FileStorage storage;
    private final UserRepository userRepo;
    private final WalletRepository walletRepo;

    private boolean running = true;

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public CommandLoop(
            AuthService auth,
            WalletService wallet,
            TransferService transfer,
            ReportService report,
            FileStorage storage,
            UserRepository userRepo,
            WalletRepository walletRepo
    ) {
        this.auth = auth;
        this.wallet = wallet;
        this.transfer = transfer;
        this.report = report;
        this.storage = storage;
        this.userRepo = userRepo;
        this.walletRepo = walletRepo;
    }

    public void run() {
        printHelp();
        try (var br = new java.io.BufferedReader(new java.io.InputStreamReader(System.in))) {
            while (running) {
                System.out.print("> ");
                String line = br.readLine();
                if (line == null) break;

                line = line.trim();
                if (line.isEmpty()) continue;

                try {
                    handle(line);
                } catch (AppException e) {
                    report.println("Ошибка: " + e.getMessage());
                    report.println("Подсказка: help");
                } catch (Exception e) {
                    report.println("Неожиданная ошибка: " + e.getClass().getSimpleName() + ": " + e.getMessage());
                    report.println("Подсказка: help");
                }
            }
        } catch (Exception e) {
            report.println("Фатальная ошибка: " + e.getMessage());
        } finally {
            report.close();
        }
    }

    private void handle(String line) {
        ParsedCommand cmd = CommandParser.parse(line);

        switch (cmd.name()) {
            case "help" -> printHelp();

            case "register" -> {
                requireArgs(cmd, 2);
                auth.register(cmd.arg(0), cmd.arg(1));
                report.println("Ок: пользователь зарегистрирован.");
            }

            case "login" -> {
                requireArgs(cmd, 2);
                auth.login(cmd.arg(0), cmd.arg(1));
                report.println("Ок: вход выполнен как " + auth.requireCurrentLogin());
            }

            case "logout" -> {
                auth.logout();
                report.println("Ок: выход выполнен.");
            }

            case "whoami" -> report.println(auth.currentLogin().map(s -> "Вы: " + s).orElse("Не авторизован."));

            case "income" -> {
                requireLoggedIn();
                requireArgs(cmd, 2);

                String category = Validation.requireNonBlank(cmd.arg(0), "Категория");
                var amount = Money.parsePositive(cmd.arg(1));
                String comment = cmd.restFrom(2);

                var alerts = wallet.addOperation(OperationType.INCOME, category, amount, comment);
                report.println("Ок: доход добавлен.");
                printAlerts(alerts);
            }

            case "expense" -> {
                requireLoggedIn();
                requireArgs(cmd, 2);

                String category = Validation.requireNonBlank(cmd.arg(0), "Категория");
                var amount = Money.parsePositive(cmd.arg(1));
                String comment = cmd.restFrom(2);

                var alerts = wallet.addOperation(OperationType.EXPENSE, category, amount, comment);
                report.println("Ок: расход добавлен.");
                printAlerts(alerts);
            }

            case "list" -> {
                requireLoggedIn();

                OperationType type = null;
                if (!cmd.args().isEmpty()) {
                    String first = cmd.arg(0).toLowerCase(Locale.ROOT);
                    if (first.equals("income")) type = OperationType.INCOME;
                    else if (first.equals("expense")) type = OperationType.EXPENSE;
                }

                String category = cmd.kv().get("category");
                LocalDate from = parseDateOrNull(cmd.kv().get("from"));
                LocalDate to = parseDateOrNull(cmd.kv().get("to"));

                List<Operation> ops = wallet.list(type, category, from, to);
                printOperationsTable(ops);
            }

            case "budget" -> {
                requireLoggedIn();
                requireArgs(cmd, 1);
                String sub = cmd.arg(0).toLowerCase(Locale.ROOT);

                if (sub.equals("set")) {
                    requireArgs(cmd, 3);
                    String category = Validation.requireNonBlank(cmd.arg(1), "Категория");
                    var limit = Money.parseNonNegative(cmd.arg(2));

                    wallet.setBudget(category, limit);
                    report.println("Ок: бюджет установлен.");
                    printBudgetTable();
                } else if (sub.equals("show")) {
                    printBudgetTable();
                } else if (sub.equals("remove")) {
                    requireArgs(cmd, 2);
                    String category = Validation.requireNonBlank(cmd.arg(1), "Категория");
                    wallet.removeBudget(category);
                    report.println("Ок: бюджет удалён (если был задан).");
                    printBudgetTable();
                } else {
                    throw new AppException("Неизвестная подкоманда budget: " + sub);
                }
            }

            case "category" -> {
                requireLoggedIn();
                requireArgs(cmd, 1);
                String sub = cmd.arg(0).toLowerCase(Locale.ROOT);

                if (sub.equals("list")) {
                    var cats = wallet.listCategories();
                    if (cats.isEmpty()) {
                        report.println("Категорий нет.");
                    } else {
                        report.println("Категории:");
                        for (String c : cats) report.println("  - " + c);
                    }
                } else {
                    throw new AppException("Неизвестная подкоманда category: " + sub);
                }
            }

            case "stats" -> {
                requireLoggedIn();
                requireArgs(cmd, 1);

                String sub = cmd.arg(0).toLowerCase(Locale.ROOT);
                LocalDate from = parseDateOrNull(cmd.kv().get("from"));
                LocalDate to = parseDateOrNull(cmd.kv().get("to"));

                switch (sub) {
                    case "total" -> report.println(wallet.formatTotals(from, to));

                    case "income-by-category" -> printByCategoryTable(OperationType.INCOME, from, to);

                    case "expense-by-category" -> printByCategoryTable(OperationType.EXPENSE, from, to);

                    case "categories" -> {
                        requireArgs(cmd, 2);
                        List<String> cats = Arrays.stream(cmd.arg(1).split(","))
                                .map(String::trim).filter(s -> !s.isEmpty()).toList();

                        String typeStr = cmd.kv().getOrDefault("type", "expense");
                        OperationType t = typeStr.equalsIgnoreCase("income") ? OperationType.INCOME : OperationType.EXPENSE;

                        WalletService.CategoriesStatsResult r = wallet.statsForCategories(cats, t, from, to);

                        if (!r.missing().isEmpty()) {
                            report.println("Предупреждение: категории не найдены: " + String.join(", ", r.missing()));
                        }

                        report.println("Сумма (" + t + ") по выбранным категориям: " + r.total());

                        // Таблица по выбранным категориям
                        List<List<String>> rows = new ArrayList<>();
                        rows.add(List.of("Категория", "Сумма"));
                        r.byCategory().forEach((k, v) -> rows.add(List.of(k, v.stripTrailingZeros().toPlainString())));
                        printTable(rows);
                    }

                    default -> throw new AppException("Неизвестная подкоманда stats: " + sub);
                }
            }

            case "report" -> {
                requireArgs(cmd, 1);
                String sub = cmd.arg(0).toLowerCase(Locale.ROOT);

                if (sub.equals("console")) {
                    report.setConsole();
                    report.println("Ок: вывод в консоль.");
                } else if (sub.equals("file")) {
                    requireArgs(cmd, 2);
                    Path p = Path.of(cmd.arg(1));
                    report.setFile(p);
                    report.println("Ок: вывод в файл: " + p);
                } else {
                    throw new AppException("Неизвестная подкоманда report: " + sub);
                }
            }

            case "transfer" -> {
                requireLoggedIn();
                requireArgs(cmd, 2);
                String toLogin = Validation.requireNonBlank(cmd.arg(0), "Получатель");
                var amount = Money.parsePositive(cmd.arg(1));
                String comment = cmd.restFrom(2);

                var alerts = transfer.transfer(toLogin, amount, comment);
                report.println("Ок: перевод выполнен.");
                printAlerts(alerts);
            }

            case "export" -> {
                requireLoggedIn();
                requireArgs(cmd, 1);

                String login = auth.requireCurrentLogin();
                var w = walletRepo.require(login);
                var path = Path.of(cmd.arg(0));

                storage.exportWallet(w, path);
                report.println("Ок: экспорт выполнен: " + path);
            }

            case "import" -> {
                requireLoggedIn();
                requireArgs(cmd, 1);

                String login = auth.requireCurrentLogin();
                var path = Path.of(cmd.arg(0));

                var imported = storage.importWallet(path);
                imported.setOwnerLogin(login);

                walletRepo.upsert(imported);
                storage.saveWallet(imported);

                report.println("Ок: импорт выполнен: " + path);
            }

            case "exit", "quit" -> {
                saveAll();
                report.println("Данные сохранены. Пока!");
                running = false;
            }

            default -> throw new AppException("Неизвестная команда: " + cmd.name());
        }
    }

    private void saveAll() {
        storage.saveUsers(userRepo.getAll());
        storage.saveWallets(walletRepo.getAll());
    }

    private void printHelp() {
        report.setConsole();
        report.println("""
=====================================
        УЧЁТ ЛИЧНЫХ ФИНАНСОВ (CLI)
=====================================

АВТОРИЗАЦИЯ
  register <login> <password>         Регистрация
  login    <login> <password>         Вход
  logout                               Выход
  whoami                               Текущий пользователь

ОПЕРАЦИИ
  income  <category> <amount> [comment...]
  expense <category> <amount> [comment...]
  list [income|expense] [category=<name>] [from=YYYY-MM-DD] [to=YYYY-MM-DD]

БЮДЖЕТЫ
  budget set    <category> <limit>
  budget remove <category>
  budget show

КАТЕГОРИИ
  category list

СТАТИСТИКА
  stats total [from=...] [to=...]
  stats income-by-category [from=...] [to=...]
  stats expense-by-category [from=...] [to=...]
  stats categories <cat1,cat2,...> [type=income|expense] [from=...] [to=...]

ОТЧЁТЫ
  report console
  report file <path>

ИМПОРТ/ЭКСПОРТ
  export <path>
  import <path>

ПЕРЕВОДЫ
  transfer <toLogin> <amount> [comment...]

ВЫХОД
  exit

ПРИМЕРЫ
  register ivan 1234
  login ivan 1234
  income Зарплата 20000
  expense Еда 500 "покупка продуктов"
  budget set Еда 4000
  stats total
  list expense category=Еда
  export exports/ivan.json
""");
    }

    private void printAlerts(List<String> alerts) {
        for (String a : alerts) report.println("⚠ " + a);
    }

    private void requireArgs(ParsedCommand cmd, int n) {
        if (cmd.args().size() < n) throw new AppException("Недостаточно аргументов.");
    }

    private void requireLoggedIn() {
        auth.requireCurrentLogin();
    }

    private LocalDate parseDateOrNull(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return LocalDate.parse(s.trim());
        } catch (Exception e) {
            throw new AppException("Некорректная дата: " + s + " (формат YYYY-MM-DD)");
        }
    }

    // -------- Pretty output helpers --------

    private void printOperationsTable(List<Operation> ops) {
        if (ops.isEmpty()) {
            report.println("Операций нет.");
            return;
        }

        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("Дата", "Тип", "Категория", "Сумма", "Контрагент", "Комментарий"));

        for (Operation op : ops) {
            rows.add(List.of(
                    op.getCreatedAt().format(DT_FMT),
                    op.getType().name(),
                    op.getCategory(),
                    op.getAmount().stripTrailingZeros().toPlainString(),
                    op.getCounterpartyLogin() == null ? "" : op.getCounterpartyLogin(),
                    op.getComment() == null ? "" : op.getComment()
            ));
        }

        printTable(rows);
    }

    private void printBudgetTable() {
        String reportText = wallet.formatBudgetsReport();
        if (reportText.equals("Бюджеты не заданы.")) {
            report.println(reportText);
            return;
        }

        var cats = wallet.listCategories();
        List<String> budgetCats = new ArrayList<>();
        for (String c : cats) {
            String line = wallet.formatBudgetLine(c);
            if (!line.endsWith("бюджет не задан")) budgetCats.add(c);
        }

        if (budgetCats.isEmpty()) {
            report.println("Бюджеты не заданы.");
            return;
        }

        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("Категория", "Лимит", "Остаток"));

        for (String cat : budgetCats) {

            String line = wallet.formatBudgetLine(cat);
            String[] parts = line.split(": ", 2);
            String rest = parts.length == 2 ? parts[1] : "";
            String[] restParts = rest.split(", Оставшийся бюджет: ");
            String limit = restParts.length > 0 ? restParts[0] : "";
            String remaining = restParts.length > 1 ? restParts[1] : "";
            rows.add(List.of(cat, limit, remaining));
        }

        printTable(rows);
    }

    private void printByCategoryTable(OperationType type, LocalDate from, LocalDate to) {
        String text = wallet.formatByCategory(type, from, to);
        if (text.equals("Нет данных.")) {
            report.println(text);
            return;
        }


        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("Категория", "Сумма"));

        String[] lines = text.split("\\R");
        for (String ln : lines) {
            ln = ln.trim();
            if (ln.isEmpty()) continue;
            if (ln.startsWith("Доходы") || ln.startsWith("Расходы")) continue;
            // "cat: amount"
            int idx = ln.indexOf(':');
            if (idx <= 0) continue;
            String cat = ln.substring(0, idx).trim();
            String amount = ln.substring(idx + 1).trim();
            rows.add(List.of(cat, amount));
        }

        printTable(rows);
    }

    private void printTable(List<List<String>> rows) {
        for (String line : TablePrinter.format(rows)) {
            report.println(line);
        }
    }
}
