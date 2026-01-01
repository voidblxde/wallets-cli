package app.domain;

import java.math.BigDecimal;

public class CategoryBudget {
    private String category;
    private BigDecimal limit;

    public CategoryBudget() {}

    public CategoryBudget(String category, BigDecimal limit) {
        this.category = category;
        this.limit = limit;
    }

    public String getCategory() { return category; }
    public BigDecimal getLimit() { return limit; }

    public void setCategory(String category) { this.category = category; }
    public void setLimit(BigDecimal limit) { this.limit = limit; }
}
