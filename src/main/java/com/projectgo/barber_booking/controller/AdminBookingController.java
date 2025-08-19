package com.projectgo.barber_booking.controller;

import com.projectgo.barber_booking.model.Booking;
import com.projectgo.barber_booking.repository.BookingRepository;
import com.projectgo.barber_booking.service.NotificationService;
import com.projectgo.barber_booking.spec.BookingSpecs;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/admin/bookings")
public class AdminBookingController {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private NotificationService notificationService;

    // ✅ สถานะที่อนุญาต
    private static final Set<String> ALLOWED_STATUSES =
            Set.of("รอดำเนินการ", "ยืนยันแล้ว", "ยกเลิก", "เสร็จสิ้น");

    private boolean isValidStatus(String s) {
        return s != null && ALLOWED_STATUSES.contains(s);
    }

    // ✅ CREATE
    @PostMapping
    public ResponseEntity<Booking> createBooking(@RequestBody Booking booking) {
        if (booking.getStatus() == null || booking.getStatus().isBlank()) {
            booking.setStatus("รอดำเนินการ");
        } else if (!isValidStatus(booking.getStatus())) {
            return ResponseEntity.badRequest().<Booking>build();
        }

        Booking saved = bookingRepository.save(booking);

        // 🔔 แจ้งเตือน
        notificationService.sendNewBookingAlert(saved);   // LINE (ถ้าเปิดใช้)
        notificationService.sendBookingConfirmation(saved); // อีเมลยืนยันถึงลูกค้า (ถ้าเปิดใช้)

        return ResponseEntity.ok(saved);
    }

    // ✅ LIST + FILTERS + ค้นหาอิสระ 'q'
    @GetMapping
    public List<Booking> getAllBookings(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String barber,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false, name = "startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false, name = "endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false, name = "q") String q
    ) {
        Specification<Booking> spec = BookingSpecs.byFilters(status, barber, date, startDate, endDate, q);
        Sort sort = Sort.by(Sort.Order.asc("date"), Sort.Order.asc("time"));
        return bookingRepository.findAll(spec, sort);
    }

    // ✅ GET BY ID
    @GetMapping("/{id}")
    public ResponseEntity<Booking> getBookingById(@PathVariable Long id) {
        return bookingRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ✅ UPDATE (ถ้าเปลี่ยนสถานะ ให้แจ้งเตือน)
    @PutMapping("/{id}")
    public ResponseEntity<Booking> updateBooking(@PathVariable Long id, @RequestBody Booking updated) {
        if (updated.getStatus() != null && !isValidStatus(updated.getStatus())) {
            return ResponseEntity.badRequest().<Booking>build();
        }

        return bookingRepository.findById(id)
                .map(b -> {
                    String oldStatus = b.getStatus();

                    b.setUsername(updated.getUsername());
                    b.setService(updated.getService());
                    b.setDate(updated.getDate());
                    b.setTime(updated.getTime());
                    b.setBarber(updated.getBarber());
                    b.setNote(updated.getNote());
                    if (updated.getStatus() != null) {
                        b.setStatus(updated.getStatus());
                    }

                    Booking saved = bookingRepository.save(b);

                    if (updated.getStatus() != null && !updated.getStatus().equals(oldStatus)) {
                        notificationService.sendStatusChange(saved, oldStatus, saved.getStatus());
                    }
                    return saved;
                })
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // ✅ PATCH STATUS
    public static class StatusDTO { public String status; }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Booking> patchStatus(@PathVariable Long id, @RequestBody StatusDTO body) {
        if (body == null || !isValidStatus(body.status)) {
            return ResponseEntity.badRequest().<Booking>build();
        }
        return bookingRepository.findById(id).map(b -> {
            String oldStatus = b.getStatus();
            if (body.status.equals(oldStatus)) {
                return ResponseEntity.ok(b); // ไม่มีการเปลี่ยน
            }
            b.setStatus(body.status);
            Booking saved = bookingRepository.save(b);
            notificationService.sendStatusChange(saved, oldStatus, saved.getStatus());
            return ResponseEntity.ok(saved);
        }).orElse(ResponseEntity.notFound().build());
    }

    // ✅ DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBooking(@PathVariable Long id) {
        if (!bookingRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        bookingRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // ✅ EXPORT CSV (รองรับตัวกรองและ q)
    @GetMapping(value = "/export", produces = "text/csv; charset=UTF-8")
    public void exportCsv(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String barber,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false, name = "startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false, name = "endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false, name = "q") String q,
            HttpServletResponse response
    ) throws Exception {

        Specification<Booking> spec = BookingSpecs.byFilters(status, barber, date, startDate, endDate, q);
        Sort sort = Sort.by(Sort.Order.asc("date"), Sort.Order.asc("time"));
        List<Booking> list = bookingRepository.findAll(spec, sort);

        response.setContentType("text/csv; charset=UTF-8");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Content-Disposition", "attachment; filename=bookings.csv");

        try (PrintWriter out = response.getWriter()) {
            out.write("\uFEFF"); // BOM สำหรับ Excel
            out.println("ID,Username,Service,Date,Time,Barber,Status,Note");
            for (Booking b : list) {
                out.printf("%s,%s,%s,%s,%s,%s,%s,%s%n",
                        csv(b.getId()),
                        csv(b.getUsername()),
                        csv(b.getService()),
                        csv(b.getDate()),
                        csv(b.getTime()),
                        csv(b.getBarber()),
                        csv(b.getStatus()),
                        csv(b.getNote()));
            }
            out.flush();
        }
    }

    private String csv(Object v) {
        if (v == null) return "";
        String s = String.valueOf(v).replace("\"", "\"\"");
        if (s.contains(",") || s.contains("\n")) s = "\"" + s + "\"";
        return s;
    }
}
