package com.projectgo.barber_booking.controller;

import com.projectgo.barber_booking.model.Booking;
import com.projectgo.barber_booking.repository.BookingRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@RestController
@RequestMapping("/api/bookings")
public class BookingRestController {

    private final BookingRepository bookingRepository;

    public BookingRestController(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    // สร้างการจอง (เดิม)
    @PostMapping
    public Booking createBooking(@RequestBody Booking booking) {
        return bookingRepository.save(booking);
    }

    // ----- 🔧 ฟังก์ชันที่เพิ่มเข้ามา -----

    // 1) อัปเดตสถานะ (เช่น เสร็จสิ้น/ยกเลิก/ยืนยันแล้ว)
    @PatchMapping("/{id}/status")
    public ResponseEntity<Booking> updateStatus(@PathVariable Long id,
                                                @RequestBody StatusDTO body) {
        Booking b = bookingRepository.findById(id).orElse(null);
        if (b == null) return ResponseEntity.notFound().build();

        String status = body.status();
        if (status == null || status.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        b.setStatus(status.trim());
        return ResponseEntity.ok(bookingRepository.save(b));
    }

    // 2) อัปเดตวันและเวลา (endpoint หลัก)
    @PatchMapping("/{id}/schedule")
    public ResponseEntity<Booking> updateSchedule(@PathVariable Long id,
                                                  @RequestBody ScheduleDTO body) {
        Booking b = bookingRepository.findById(id).orElse(null);
        if (b == null) return ResponseEntity.notFound().build();

        // รองรับได้ทั้ง "yyyy-MM-dd" (จาก <input type=date>) และ "dd/MM/yyyy"
        if (body.date() != null && !body.date().isBlank()) {
            b.setDate(parseDate(body.date().trim()));
        }
        // รองรับได้ทั้ง "HH:mm" และ "HH:mm:ss"
        if (body.time() != null && !body.time().isBlank()) {
            b.setTime(parseTime(body.time().trim()));
        }
        return ResponseEntity.ok(bookingRepository.save(b));
    }

    // 2.1) alias ให้ตรงกับหน้าเดิมที่เรียก /{id}/time  ✅ แก้ 404 ได้ทันที
    @PatchMapping("/{id}/time")
    public ResponseEntity<Booking> updateTimeAlias(@PathVariable Long id,
                                                   @RequestBody ScheduleDTO body) {
        return updateSchedule(id, body);
    }

    // 3) ลบ/ยกเลิกการจอง (ถ้าหน้าคุณเรียก DELETE)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!bookingRepository.existsById(id)) return ResponseEntity.notFound().build();
        bookingRepository.deleteById(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    // ---------- helpers ----------
    private static LocalDate parseDate(String s) {
        try {
            return LocalDate.parse(s); // ISO: yyyy-MM-dd
        } catch (DateTimeParseException ignore) {
        }
        // fallback: dd/MM/yyyy
        DateTimeFormatter f = DateTimeFormatter.ofPattern("dd/MM/uuuu");
        return LocalDate.parse(s, f);
    }

    private static LocalTime parseTime(String s) {
        try {
            return LocalTime.parse(s); // HH:mm:ss หรือ HH:mm ก็เข้าได้ส่วนใหญ่
        } catch (DateTimeParseException ignore) {
        }
        // fallback: H:mm
        DateTimeFormatter f = DateTimeFormatter.ofPattern("H:mm");
        return LocalTime.parse(s, f);
    }

    // ---------- DTO ----------
    public record StatusDTO(String status) {}
    public record ScheduleDTO(String date, String time) {}
}
