package com.ironpump.fitmanage.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class TrainerPayment {
    private int trainerPaymentId;
    private int trainerId;
    private BigDecimal amount;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private LocalDate paidOn;
    private Payment.Method method;
    private String reference;
    private String notes;
    private int createdBy;
    private LocalDateTime createdAt;

    public int getTrainerPaymentId() { return trainerPaymentId; }
    public void setTrainerPaymentId(int trainerPaymentId) { this.trainerPaymentId = trainerPaymentId; }
    public int getTrainerId() { return trainerId; }
    public void setTrainerId(int trainerId) { this.trainerId = trainerId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public LocalDate getPeriodStart() { return periodStart; }
    public void setPeriodStart(LocalDate periodStart) { this.periodStart = periodStart; }
    public LocalDate getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(LocalDate periodEnd) { this.periodEnd = periodEnd; }
    public LocalDate getPaidOn() { return paidOn; }
    public void setPaidOn(LocalDate paidOn) { this.paidOn = paidOn; }
    public Payment.Method getMethod() { return method; }
    public void setMethod(Payment.Method method) { this.method = method; }
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public int getCreatedBy() { return createdBy; }
    public void setCreatedBy(int createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
