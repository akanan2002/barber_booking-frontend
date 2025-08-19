package com.projectgo.barber_booking.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String service;

    @Column(nullable = false)
    private LocalDate date;      // ✅ ใช้ LocalDate

    @Column(nullable = false)
    private LocalTime time;      // ✅ ใช้ LocalTime ให้ตรงกับที่ใช้งานในเทมเพลต

    @Column(nullable = false)
    private String barber;

    @Column(length = 500)
    private String note;

    @Column(nullable = false)
    private String status = "รอดำเนินการ";

    public Booking() {}

    public Booking(String username, String service, LocalDate date, LocalTime time) {
        this.username = username;
        this.service = service;
        this.date = date;
        this.time = time;
        this.status = "รอดำเนินการ";
    }

    @PrePersist
    public void prePersist() {
        if (status == null || status.isBlank()) status = "รอดำเนินการ";
    }

    // --- Getter/Setter ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getService() { return service; }
    public void setService(String service) { this.service = service; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    // เผื่อกรณีรับค่าเป็นสตริง (เช่นจากฟอร์ม/JSON ISO-8601: yyyy-MM-dd)
    public void setDate(String date) { this.date = LocalDate.parse(date); }

    public LocalTime getTime() { return time; }
    public void setTime(LocalTime time) { this.time = time; }
    // เผื่อกรณีรับค่าเป็นสตริง (รูปแบบ HH:mm หรือ HH:mm:ss)
    public void setTime(String time) { this.time = LocalTime.parse(time); }

    public String getBarber() { return barber; }
    public void setBarber(String barber) { this.barber = barber; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
