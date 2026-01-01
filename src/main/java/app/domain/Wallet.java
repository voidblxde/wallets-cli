package app.domain;

import java.util.*;

public class Wallet {
    private String ownerLogin;
    private List<Operation> operations = new ArrayList<>();
    private Map<String, CategoryBudget> budgets = new LinkedHashMap<>();

    public Wallet() {}

    public Wallet(String ownerLogin) {
        this.ownerLogin = ownerLogin;
    }

    public String getOwnerLogin() { return ownerLogin; }
    public List<Operation> getOperations() { return operations; }
    public Map<String, CategoryBudget> getBudgets() { return budgets; }

    public void setOwnerLogin(String ownerLogin) { this.ownerLogin = ownerLogin; }
    public void setOperations(List<Operation> operations) { this.operations = operations; }
    public void setBudgets(Map<String, CategoryBudget> budgets) { this.budgets = budgets; }
}
