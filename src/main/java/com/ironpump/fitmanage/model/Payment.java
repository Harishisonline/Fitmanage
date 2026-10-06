package com.ironpump.fitmanage.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Payment {
    private int paymentId;
    private String receiptNo;
    private int memberId;
    private Integer membershipId;
    private BigDecimal amount;
    private Method method;
    private String reference;
    private LocalDateTime paymentDate;
    private String notes;
    private int createdBy;
    private LocalDateTime createdAt;

    public enum Method { CASH, UPI, CARD, BANK_TRANSFER, CHEQUE }
    public enum Type { PAYMENT, REFUND }

    private Type paymentType = Type.PAYMENT;

    public int getPaymentId() { return paymentId; }
    public void setPaymentId(int paymentId) { this.paymentId = paymentId; }
    public String getReceiptNo() { return receiptNo; }
    public void setReceiptNo(String receiptNo) { this.receiptNo = receiptNo; }
    public int getMemberId() { return memberId; }
    public void setMemberId(int memberId) { this.memberId = memberId; }
    public Integer getMembershipId() { return membershipId; }
    public void setMembershipId(Integer membershipId) { this.membershipId = membershipId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public Type getPaymentType() { return paymentType == null ? Type.PAYMENT : paymentType; }
    public void setPaymentType(Type paymentType) { this.paymentType = paymentType; }
    public Method getMethod() { return method; }
    public void setMethod(Method method) { this.method = method; }
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
    public LocalDateTime getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDateTime paymentDate) { this.paymentDate = paymentDate; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public int getCreatedBy() { return createdBy; }
    public void setCreatedBy(int createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
