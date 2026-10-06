package com.ironpump.fitmanage.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class Member {
    private int memberId;
    private String memberCode;
    private String firstName;
    private String lastName;
    private LocalDate dob;
    private Gender gender;
    private String phone;
    private String email;
    private String address;
    private String emergencyName;
    private String emergencyPhone;
    private Integer heightCm;
    private BigDecimal weightKg;
    private String medicalNotes;
    private Integer assignedTrainerId;  // initial trainer assignment, also tracked on memberships
    private Status status = Status.ACTIVE;
    private LocalDate joinedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum Gender { MALE, FEMALE, OTHER }
    public enum Status { ACTIVE, INACTIVE }

    public int getMemberId() { return memberId; }
    public void setMemberId(int memberId) { this.memberId = memberId; }
    public String getMemberCode() { return memberCode; }
    public void setMemberCode(String memberCode) { this.memberCode = memberCode; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public LocalDate getDob() { return dob; }
    public void setDob(LocalDate dob) { this.dob = dob; }
    public Gender getGender() { return gender; }
    public void setGender(Gender gender) { this.gender = gender; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getEmergencyName() { return emergencyName; }
    public void setEmergencyName(String emergencyName) { this.emergencyName = emergencyName; }
    public String getEmergencyPhone() { return emergencyPhone; }
    public void setEmergencyPhone(String emergencyPhone) { this.emergencyPhone = emergencyPhone; }
    public Integer getHeightCm() { return heightCm; }
    public void setHeightCm(Integer heightCm) { this.heightCm = heightCm; }
    public BigDecimal getWeightKg() { return weightKg; }
    public void setWeightKg(BigDecimal weightKg) { this.weightKg = weightKg; }
    public String getMedicalNotes() { return medicalNotes; }
    public void setMedicalNotes(String medicalNotes) { this.medicalNotes = medicalNotes; }
    public Integer getTrainerId() { return assignedTrainerId; }
    public void setTrainerId(Integer assignedTrainerId) { this.assignedTrainerId = assignedTrainerId; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public LocalDate getJoinedAt() { return joinedAt; }
    public void setJoinedAt(LocalDate joinedAt) { this.joinedAt = joinedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getFullName() { return firstName + " " + lastName; }
}
