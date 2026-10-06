package com.ironpump.fitmanage.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Membership {
    private int membershipId;
    private int memberId;
    private int planId;
    private Integer trainerId;
    private LocalDate startDate;
    private LocalDate endDate;
    private Status status = Status.ACTIVE;
    private Integer renewalOfId;
    private LocalDateTime createdAt;

    public enum Status { ACTIVE, EXPIRED, CANCELLED }

    public int getMembershipId() { return membershipId; }
    public void setMembershipId(int membershipId) { this.membershipId = membershipId; }
    public int getMemberId() { return memberId; }
    public void setMemberId(int memberId) { this.memberId = memberId; }
    public int getPlanId() { return planId; }
    public void setPlanId(int planId) { this.planId = planId; }
    public Integer getTrainerId() { return trainerId; }
    public void setTrainerId(Integer trainerId) { this.trainerId = trainerId; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public Integer getRenewalOfId() { return renewalOfId; }
    public void setRenewalOfId(Integer renewalOfId) { this.renewalOfId = renewalOfId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    /** Status the operator should see. Stored EXPIRED is not trusted when the dates say otherwise. */
    public String displayStatus() {
        if (status == Status.CANCELLED) return "CANCELLED";
        LocalDate today = LocalDate.now();
        if (endDate == null || endDate.isBefore(today)) return "EXPIRED";
        if (startDate != null && startDate.isAfter(today)) return "UPCOMING";
        if (!endDate.isAfter(today.plusDays(7))) return "EXPIRING";
        return "ACTIVE";
    }
}
