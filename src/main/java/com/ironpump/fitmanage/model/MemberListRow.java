package com.ironpump.fitmanage.model;

import java.time.LocalDate;

/** One members-list row with status derived from the current membership. */
public class MemberListRow {
    private Member member;
    private String derivedStatus;
    private String planName;
    private String trainerName;
    private LocalDate expiresOn;

    public Member getMember() { return member; }
    public void setMember(Member member) { this.member = member; }
    public String getDerivedStatus() { return derivedStatus; }
    public void setDerivedStatus(String derivedStatus) { this.derivedStatus = derivedStatus; }
    public String getPlanName() { return planName; }
    public void setPlanName(String planName) { this.planName = planName; }
    public String getTrainerName() { return trainerName; }
    public void setTrainerName(String trainerName) { this.trainerName = trainerName; }
    public LocalDate getExpiresOn() { return expiresOn; }
    public void setExpiresOn(LocalDate expiresOn) { this.expiresOn = expiresOn; }
}
