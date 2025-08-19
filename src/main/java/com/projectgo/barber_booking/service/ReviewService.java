package com.projectgo.barber_booking.service;

import com.projectgo.barber_booking.model.Booking;
import com.projectgo.barber_booking.model.ServiceReview;
import com.projectgo.barber_booking.repository.BookingRepository;
import com.projectgo.barber_booking.repository.ServiceReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ServiceReviewRepository reviews;
    private final BookingRepository bookings;
    private final JdbcTemplate jdbc;

    /** อนุญาตเฉพาะเจ้าของ booking และสถานะ 'เสร็จสิ้น' */
    private boolean canReview(Long bookingId, String username) {
        Boolean ok = jdbc.queryForObject(
                "select exists(select 1 from bookings where id=? and username=? and status='เสร็จสิ้น')",
                Boolean.class, bookingId, username
        );
        return Boolean.TRUE.equals(ok);
    }

    /** สร้างรีวิว (กัน race ระหว่างตรวจ exists กับ save) */
    @Transactional
    public ServiceReview create(String username, Long bookingId, int rating, String comment) {
        if (!canReview(bookingId, username)) {
            throw new IllegalStateException("รีวิวได้เฉพาะการจองที่เสร็จสิ้นของคุณ");
        }
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("คะแนนต้องอยู่ระหว่าง 1 ถึง 5");
        }
        if (reviews.existsByBookingId(bookingId)) {
            throw new IllegalStateException("การจองนี้รีวิวแล้ว");
        }

        Booking b = bookings.findById(bookingId).orElseThrow();

        ServiceReview r = new ServiceReview();
        r.setBookingId(bookingId);
        r.setUsername(username);
        r.setService(b.getService());
        r.setBarber(b.getBarber());
        r.setRating(rating);
        r.setComment(comment);

        return reviews.save(r);
    }

    public Page<ServiceReview> listByService(String service, int page, int size) {
        return reviews.findByServiceOrderByCreatedAtDesc(service, PageRequest.of(page, size));
    }

    /** สรุปคะแนนเฉลี่ยและจำนวนรีวิวของบริการ */
    public double[] summary(String service) {
        // ใช้ SQL ตรง ๆ เพื่อให้ชนิดข้อมูลนิ่ง: avg = double, total = long
        return jdbc.queryForObject(
            """
            select coalesce(avg(rating), 0.0) as avg_rating,
                   count(*)::bigint        as total
            from service_reviews
            where service = ?
            """,
            (rs, n) -> new double[] {
                Math.round(rs.getDouble("avg_rating") * 10) / 10.0,  // ปัดทศนิยม 1 ตำแหน่ง
                rs.getLong("total")
            },
            service
        );
      }
      
}
