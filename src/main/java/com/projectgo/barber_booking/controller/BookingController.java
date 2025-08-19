package com.projectgo.barber_booking.controller;

import com.projectgo.barber_booking.model.Booking;
import com.projectgo.barber_booking.model.ServiceReview;
import com.projectgo.barber_booking.repository.BookingRepository;
import com.projectgo.barber_booking.repository.ServiceReviewRepository;
import com.projectgo.barber_booking.service.NotificationService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

/** ✅ Controller ฝั่งลูกค้า (Customer Booking Flow) */
@Controller
public class BookingController {

    private final BookingRepository bookingRepository;
    private final NotificationService notificationService;
    private final ServiceReviewRepository reviewRepository;

    public BookingController(BookingRepository bookingRepository,
                             NotificationService notificationService,
                             ServiceReviewRepository reviewRepository) {
        this.bookingRepository = bookingRepository;
        this.notificationService = notificationService;
        this.reviewRepository = reviewRepository;
    }

    // STEP 1: หน้าเลือกบริการ
    @GetMapping({"/booking", "/booking/select-service", "/select_service"})
    public String showSelectServicePage(Model model, Principal principal) {
        model.addAttribute("activePage", "booking");
        return "select_service";
    }

    // STEP 2: หน้าเลือกวันเวลา
    @GetMapping("/booking/select-time")
    public String selectTime(@RequestParam String service, Model model) {
        model.addAttribute("selectedService", service);
        return "select_time";
    }

    // STEP 3: หน้ายืนยันการจอง
    @GetMapping("/booking/confirm")
    public String confirmBooking(@RequestParam String service,
                                 @RequestParam String date,
                                 @RequestParam String time,
                                 @RequestParam String barber,
                                 @RequestParam(required = false) String note,
                                 Model model) {
        model.addAttribute("service", service);
        model.addAttribute("date", date);
        model.addAttribute("time", time);
        model.addAttribute("barber", barber);
        model.addAttribute("note", note);
        return "confirm_booking";
    }

    // STEP 4: บันทึกการจอง
    @PostMapping("/booking/submit")
    public String saveBookingToDB(@RequestParam String service,
                                  @RequestParam String date,
                                  @RequestParam String time,
                                  @RequestParam String barber,
                                  @RequestParam(required = false) String note,
                                  Principal principal,
                                  RedirectAttributes ra) {
        if (principal == null) {
            return "redirect:/login";
        }

        LocalDate d;
        LocalTime t;
        try {
            d = LocalDate.parse(date);           // คาดหวังรูปแบบ yyyy-MM-dd
            t = parseTimeFlexible(time);         // รองรับ HH:mm และ HH:mm:ss
        } catch (DateTimeParseException ex) {
            ra.addFlashAttribute("errorMessage", "รูปแบบวัน-เวลาไม่ถูกต้อง");
            return "redirect:/booking/select-time?service=" + url(service);
        }

        try {
            // กันเวลาชน ถ้าใน repository มีเมธอดนี้
            try {
                if (hasExistsMethod() &&
                    bookingRepository.existsByBarberAndDateAndTime(barber, d, t)) {
                    ra.addFlashAttribute("errorMessage", "ช่วงเวลานี้ถูกจองแล้ว กรุณาเลือกเวลาอื่น");
                    return "redirect:/booking/select-time?service=" + url(service);
                }
            } catch (NoSuchMethodError | RuntimeException ignored) {}

            Booking booking = new Booking();
            booking.setService(service);
            booking.setDate(d);
            booking.setTime(t);
            booking.setBarber(barber);
            booking.setNote(note);
            booking.setUsername(principal.getName());
            booking.setStatus("รอดำเนินการ");

            Booking saved = bookingRepository.save(booking);

            // แจ้งเตือน (ไม่กระทบกรณีปิดใช้ NotificationService)
            try {
                notificationService.sendBookingConfirmation(saved);
                notificationService.sendNewBookingAlert(saved);
            } catch (Exception ignore) {}

            ra.addFlashAttribute("successMessage", "จองสำเร็จ! ระบบได้ส่งอีเมลยืนยันให้แล้ว");
            return "redirect:/booking/success";
        } catch (DataIntegrityViolationException dup) {
            ra.addFlashAttribute("errorMessage", "ช่วงเวลานี้ถูกจองแล้ว กรุณาเลือกเวลาอื่น");
            return "redirect:/booking/select-time?service=" + url(service);
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "บันทึกไม่สำเร็จ กรุณาลองใหม่");
            return "redirect:/booking/select-time?service=" + url(service);
        }
    }

    @GetMapping("/booking/success")
    public String bookingSuccess() {
        return "booking_success";
    }

    /** หน้า “รายละเอียดการจอง” (ไม่ผูกกับข้อมูลผู้ใช้เพื่อความเข้ากันได้เดิม) */
    @GetMapping("/booking/{id}")
    public String viewBooking(@PathVariable Long id, Model model) {
        Booking b = bookingRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("booking", b);
        // NOTE: เสริมแฟล็ก canReview ตามเงื่อนไขเดียวกับหน้า detail อีกตัว
        boolean canReview = "เสร็จสิ้น".equals(b.getStatus());
        model.addAttribute("canReview", canReview);
        return "booking_detail";
    }

    // STEP 5: รายการจองของผู้ใช้ + ส่ง reviewedIds ไป UI
    @GetMapping("/all-bookings")
    public String showAllBookings(Model model, Principal principal) {
        if (principal == null) return "redirect:/login";

        String username = principal.getName();
        List<Booking> bookings = Optional.ofNullable(bookingRepository.findAllByUsername(username))
                .orElseGet(Collections::emptyList);

        Set<Long> reviewedIds = Collections.emptySet();
        if (!bookings.isEmpty()) {
            List<Long> ids = bookings.stream().map(Booking::getId).toList();
            reviewedIds = reviewRepository.findAllByBookingIdIn(ids).stream()
                    .map(ServiceReview::getBookingId)
                    .collect(Collectors.toSet());
        }

        model.addAttribute("username", username);
        model.addAttribute("bookings", bookings);
        model.addAttribute("reviewedIds", reviewedIds);
        return "user_bookings";
    }

    // STEP 6: หน้า “รายละเอียดการจอง” ที่ล็อกอินและเป็นเจ้าของเท่านั้น
    @GetMapping("/booking/detail/{id}")
    public String showBookingDetail(@PathVariable("id") Long id,
                                    Model model,
                                    Principal principal,
                                    RedirectAttributes ra) {
        if (principal == null) {
            return "redirect:/login";
        }

        Optional<Booking> opt = bookingRepository.findById(id);
        if (opt.isEmpty()) {
            ra.addFlashAttribute("errorMessage", "ไม่พบรายการจองที่ร้องขอ");
            return "redirect:/all-bookings";
        }

        Booking booking = opt.get();

        // อนุญาตเฉพาะเจ้าของ
        if (!principal.getName().equals(booking.getUsername())) {
            ra.addFlashAttribute("errorMessage", "คุณไม่มีสิทธิ์เข้าถึงรายการนี้");
            return "redirect:/all-bookings";
        }

        // ขึ้นปุ่ม “ให้รีวิว” เฉพาะเมื่อ เสร็จสิ้น และผู้ใช้นี้ยังไม่เคยรีวิวใบนี้
        boolean alreadyReviewed = reviewRepository.existsByBookingIdAndUsername(
                booking.getId(), principal.getName()
        );
        boolean canReview = "เสร็จสิ้น".equals(booking.getStatus()) && !alreadyReviewed;

        model.addAttribute("booking", booking);
        model.addAttribute("canReview", canReview);   // ใช้ใน booking_detail.html -> th:if="${canReview}"
        return "booking_detail";
    }

    // ---------- Utils ----------
    private static final DateTimeFormatter TIME_FLEX =
            new DateTimeFormatterBuilder()
                    .appendPattern("H:mm")
                    .optionalStart().appendPattern(":ss").optionalEnd()
                    .toFormatter();

    private LocalTime parseTimeFlexible(String s) { return LocalTime.parse(s, TIME_FLEX); }

    private String url(String s) {
        return s == null ? "" : URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    /** เช็คว่ามีเมธอด existsByBarberAndDateAndTime อยู่ใน BookingRepository หรือไม่ (ไม่พังของเดิม) */
    private boolean hasExistsMethod() {
        try {
            bookingRepository.getClass()
                    .getMethod("existsByBarberAndDateAndTime", String.class, LocalDate.class, LocalTime.class);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }
}
