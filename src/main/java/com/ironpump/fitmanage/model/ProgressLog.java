package com.ironpump.fitmanage.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class ProgressLog {
    private int logId;
    private int memberId;
    private LocalDate logDate;
    private BigDecimal weightKg;
    private Integer chestCm;
    private Integer waistCm;
    private BigDecimal bodyFatPct;
    private String notes;
    private Integer loggedBy;
    private LocalDateTime createdAt;

    public int getLogId() { return logId; }
    public void setLogId(int logId) { this.logId = logId; }
    public int getMemberId() { return memberId; }
    public void setMemberId(int memberId) { this.memberId = memberId; }
    public LocalDate getLogDate() { return logDate; }
    public void setLogDate(LocalDate logDate) { this.logDate = logDate; }
    public BigDecimal getWeightKg() { return weightKg; }
    public void setWeightKg(BigDecimal weightKg) { this.weightKg = weightKg; }
    public Integer getChestCm() { return chestCm; }
    public void setChestCm(Integer chestCm) { this.chestCm = chestCm; }
    public Integer getWaistCm() { return waistCm; }
    public void setWaistCm(Integer waistCm) { this.waistCm = waistCm; }
    public BigDecimal getBodyFatPct() { return bodyFatPct; }
    public void setBodyFatPct(BigDecimal bodyFatPct) { this.bodyFatPct = bodyFatPct; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public Integer getLoggedBy() { return loggedBy; }
    public void setLoggedBy(Integer loggedBy) { this.loggedBy = loggedBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
