package com.projectgo.barber_booking.controller;

import com.projectgo.barber_booking.dto.AdminStatsDTO;
import com.projectgo.barber_booking.repository.BookingRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/stats")
public class AdminStatsController {

    private final BookingRepository repo;

    public AdminStatsController(BookingRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public AdminStatsDTO getStats(@RequestParam(required = false)
                                  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate d = (date != null) ? date : LocalDate.now();

        long total     = repo.countByDate(d);
        long pending   = repo.countByStatusAndDate("รอดำเนินการ", d);
        long confirmed = repo.countByStatusAndDate("ยืนยันแล้ว", d);
        long done      = repo.countByStatusAndDate("เสร็จสิ้น", d);
        long canceled  = repo.countByStatusAndDate("ยกเลิก", d);

        return new AdminStatsDTO(d, total, pending, confirmed, done, canceled);
    }
}
