package com.appointmentscheduling.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalTime;

public class AvailabilitySlotDto {

    @JsonFormat(pattern = "HH:mm")
    private LocalTime startTime;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime endTime;

    private String status; // "AVAILABLE" or "BOOKED"

    public AvailabilitySlotDto() {
    }

    public AvailabilitySlotDto(LocalTime startTime, LocalTime endTime, String status) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
