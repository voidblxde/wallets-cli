# Система управления личными финансами (CLI)

Консольное backend-приложение для учёта личных финансов: доходы, расходы, бюджеты, статистика, уведомления, сохранение данных и переводы между пользователями.

---

## Требования

- Java 17+
- Maven 3.9+

Проверка окружения:

```bash
mvn -v
```

---

## Сборка и запуск

### Запуск приложения
```bash
mvn -DskipTests compile exec:java
```

### Запуск тестов
```bash
mvn test
```

### Отчёт покрытия
После запуска тестов:
```
target/site/jacoco/index.html
```

---

## Хранение данных

- Во время работы данные хранятся в памяти.
- При выходе из приложения (`exit`) данные сохраняются:
    - `data/users.json`
    - `data/wallets/<login>.json`
- При авторизации данные загружаются из файла, если отсутствуют в памяти текущего запуска.

---

## Команды

### Справка
```
help
```

### Авторизация
```
register <login> <password>
login <login> <password>
logout
whoami
```

### Операции
```
income <category> <amount> [comment...]
expense <category> <amount> [comment...]
list [income|expense] [category=<name>] [from=YYYY-MM-DD] [to=YYYY-MM-DD]
```

Примеры:
```
income Зарплата 20000
expense Еда 500 "покупка продуктов"
list expense category=Еда
list income from=2026-01-01 to=2026-12-31
```

### Бюджеты
```
budget set <category> <limit>
budget remove <category>
budget show
```

Пример:
```
budget set Еда 4000
budget show
budget remove Еда
```

### Категории
```
category list
```

### Статистика
```
stats total
stats income-by-category
stats expense-by-category
stats categories <cat1,cat2,...> [type=income|expense]
```

### Отчёты
```
report console
report file <path>
```

### Импорт / экспорт
```
export <path>
import <path>
```

### Переводы
```
transfer <toLogin> <amount> [comment...]
```

### Выход
```
exit
```

---

## Архитектура проекта

```
app/
 ├── cli         — обработка команд и CLI
 ├── services    — бизнес-логика
 ├── storage     — хранение данных (JSON)
 ├── domain      — модели (User, Wallet, Operation)
 └── util        — валидация и утилиты
```

---

## Валидация и уведомления

Проверяется:
- корректность логина и пароля;
- корректность числовых значений;
- пустые аргументы;
- существование категорий.

Уведомления:
- превышение 80% по бюджету
- превышение бюджета;
- превышение расходов над доходами.

---

## Тестирование

Используется **JUnit 5**.

Запуск:
```bash
mvn test
```

Отчёт покрытия:
```
target/site/jacoco/index.html
```
