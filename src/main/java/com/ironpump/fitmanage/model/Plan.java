package com.ironpump.fitmanage.model;

import java.math.BigDecimal;

public class Plan {
    private int planId;
    private String name;
    private int durationDays;
    private BigDecimal price;
    private String description;
    private boolean active = true;
    private java.time.LocalDateTime createdAt;

    public int getPlanId() { return planId; }
    public void setPlanId(int planId) { this.planId = planId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getDurationDays() { return durationDays; }
    public void setDurationDays(int durationDays) { this.durationDays = durationDays; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public java.time.LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(java.time.LocalDateTime createdAt) { this.createdAt = createdAt; }
}
