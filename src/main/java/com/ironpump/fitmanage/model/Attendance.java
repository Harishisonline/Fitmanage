package com.ironpump.fitmanage.model;

import java.time.LocalDateTime;

public class Attendance {
    private long attendanceId;
    private int memberId;
    private LocalDateTime checkInAt;
    private LocalDateTime checkOutAt;
    private Integer trainerId;
    private String notes;
    private LocalDateTime createdAt;
    private String memberName;
    private String memberCode;
    private String planName;
    private String trainerName;

    public long getAttendanceId() { return attendanceId; }
    public void setAttendanceId(long attendanceId) { this.attendanceId = attendanceId; }
    public int getMemberId() { return memberId; }
    public void setMemberId(int memberId) { this.memberId = memberId; }
    public LocalDateTime getCheckInAt() { return checkInAt; }
    public void setCheckInAt(LocalDateTime checkInAt) { this.checkInAt = checkInAt; }
    public LocalDateTime getCheckOutAt() { return checkOutAt; }
    public void setCheckOutAt(LocalDateTime checkOutAt) { this.checkOutAt = checkOutAt; }
    public Integer getTrainerId() { return trainerId; }
    public void setTrainerId(Integer trainerId) { this.trainerId = trainerId; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getMemberName() { return memberName; }
    public void setMemberName(String memberName) { this.memberName = memberName; }
    public String getMemberCode() { return memberCode; }
    public void setMemberCode(String memberCode) { this.memberCode = memberCode; }
    public String getPlanName() { return planName; }
    public void setPlanName(String planName) { this.planName = planName; }
    public String getTrainerName() { return trainerName; }
    public void setTrainerName(String trainerName) { this.trainerName = trainerName; }
}
