package com.ironpump.fitmanage.model;

import java.math.BigDecimal;

/** Membership joined to its plan and the people on it. */
public class MembershipView {
    private Membership membership;
    private String planName;
    private int durationDays;
    private BigDecimal price = BigDecimal.ZERO;
    private String trainerName;
    private String memberName;
    private String memberCode;
    private String memberPhone;

    public Membership getMembership() { return membership; }
    public void setMembership(Membership membership) { this.membership = membership; }
    public String getPlanName() { return planName; }
    public void setPlanName(String planName) { this.planName = planName; }
    public int getDurationDays() { return durationDays; }
    public void setDurationDays(int durationDays) { this.durationDays = durationDays; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public String getTrainerName() { return trainerName; }
    public void setTrainerName(String trainerName) { this.trainerName = trainerName; }
    public String getMemberName() { return memberName; }
    public void setMemberName(String memberName) { this.memberName = memberName; }
    public String getMemberCode() { return memberCode; }
    public void setMemberCode(String memberCode) { this.memberCode = memberCode; }
    public String getMemberPhone() { return memberPhone; }
    public void setMemberPhone(String memberPhone) { this.memberPhone = memberPhone; }
}
