package com.projectgo.barber_booking.controller;

import com.projectgo.barber_booking.model.Booking;
import com.projectgo.barber_booking.repository.BookingRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.Optional;

@Controller
public class ReviewPageController {

    private final BookingRepository bookingRepository;

    public ReviewPageController(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    /** หน้าเขียนรีวิวจาก bookingId (คงไว้) */
    @GetMapping("/reviews/new")
    public String newReview(@RequestParam("bookingId") Long bookingId,
                            Model model,
                            Principal principal,
                            RedirectAttributes ra) {
        Optional<Booking> opt = bookingRepository.findById(bookingId);
        if (opt.isEmpty()) {
            ra.addFlashAttribute("error", "ไม่พบการจองที่ระบุ");
            return "redirect:/all-bookings";
        }

        Booking b = opt.get();

        // อนุญาตเฉพาะเจ้าของหรือ admin ตามที่ต้องการ
        if (principal != null) {
            String me = principal.getName();
            if (!me.equals(b.getUsername()) && !"admin".equalsIgnoreCase(me)) {
                ra.addFlashAttribute("error", "คุณไม่มีสิทธิ์เขียนรีวิวสำหรับการจองนี้");
                return "redirect:/all-bookings";
            }
        }

        model.addAttribute("booking", b);
        return "review_new"; // templates/review_new.html
    }
}
