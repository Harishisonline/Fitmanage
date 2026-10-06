package com.ironpump.fitmanage.model;

import java.time.LocalDateTime;

public class ActivityLog {
    private long activityId;
    private Integer userId;
    private Integer memberId;
    private Integer trainerId;
    private String action;
    private String detail;
    private String ip;
    private LocalDateTime createdAt;

    public long getActivityId() { return activityId; }
    public void setActivityId(long activityId) { this.activityId = activityId; }
    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }
    public Integer getMemberId() { return memberId; }
    public void setMemberId(Integer memberId) { this.memberId = memberId; }
    public Integer getTrainerId() { return trainerId; }
    public void setTrainerId(Integer trainerId) { this.trainerId = trainerId; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
