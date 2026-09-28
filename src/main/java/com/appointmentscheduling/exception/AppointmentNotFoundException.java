package com.appointmentscheduling.exception;

public class AppointmentNotFoundException extends RuntimeException {
    public AppointmentNotFoundException(String message) {
        super(message);
    }

    public AppointmentNotFoundException(Long id) {
        super("Appointment not found with id: " + id);
    }
}
