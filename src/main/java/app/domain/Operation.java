package app.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class Operation {
    private UUID id;
    private OperationType type;
    private String category;
    private BigDecimal amount;
    private LocalDateTime createdAt;
    private String comment;

    // transfers
    private String counterpartyLogin;
    private UUID transferId;

    public Operation() {}

    public Operation(OperationType type, String category, BigDecimal amount, String comment) {
        this.id = UUID.randomUUID();
        this.type = type;
        this.category = category;
        this.amount = amount;
        this.createdAt = LocalDateTime.now();
        this.comment = comment;
    }

    public UUID getId() { return id; }
    public OperationType getType() { return type; }
    public String getCategory() { return category; }
    public BigDecimal getAmount() { return amount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getComment() { return comment; }
    public String getCounterpartyLogin() { return counterpartyLogin; }
    public UUID getTransferId() { return transferId; }

    public void setId(UUID id) { this.id = id; }
    public void setType(OperationType type) { this.type = type; }
    public void setCategory(String category) { this.category = category; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setComment(String comment) { this.comment = comment; }
    public void setCounterpartyLogin(String counterpartyLogin) { this.counterpartyLogin = counterpartyLogin; }
    public void setTransferId(UUID transferId) { this.transferId = transferId; }
}
