package com.projectgo.barber_booking.repository;

import com.projectgo.barber_booking.model.ServiceReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

/**
 * Repository สำหรับรีวิวบริการ
 * ฟิลด์ใน JPQL ต้องตรงกับ entity ServiceReview เช่น: id, bookingId, username, service, rating, createdAt
 */
public interface ServiceReviewRepository extends JpaRepository<ServiceReview, Long> {

    /** มีรีวิวของ booking นี้อยู่หรือยัง (ไม่ระบุผู้ใช้) */
    boolean existsByBookingId(Long bookingId);

    /** มีรีวิวของ booking นี้จากผู้ใช้นี้หรือยัง (ไว้เช็ค “ยังไม่เคยรีวิว”) */
    boolean existsByBookingIdAndUsername(Long bookingId, String username);

    /** ดึงรายการรีวิวของบริการ ตามเวลา (ใหม่สุดก่อน) */
    @Query("""
           select r
           from ServiceReview r
           where r.service = :service
           order by r.createdAt desc
           """)
    Page<ServiceReview> findByServiceOrderByCreatedAtDesc(@Param("service") String service, Pageable pageable);

    /** สรุปค่าเฉลี่ยเรตติ้งและจำนวนรีวิวของบริการ (Projection: avg, cnt) */
    @Query("""
           select coalesce(avg(r.rating), 0.0) as avg,
                  count(r.id)                as cnt
           from ServiceReview r
           where r.service = :service
           """)
    AvgCountRow avgAndCount(@Param("service") String service);

    /** ดึงรีวิวทั้งหมดที่มี bookingId อยู่ในชุดที่ระบุ (ใช้ตอน map หา id ที่ถูกรีวิวแล้ว) */
    List<ServiceReview> findAllByBookingIdIn(Collection<Long> bookingIds);

    /** ดึงเฉพาะ bookingId ที่ถูกรีวิวแล้ว (ทุกผู้ใช้) จากชุดที่ระบุ */
    @Query("""
           select r.bookingId
           from ServiceReview r
           where r.bookingId in :ids
           """)
    List<Long> findReviewedBookingIds(@Param("ids") Collection<Long> ids);

    /** ดึงเฉพาะ bookingId ที่ผู้ใช้คนนี้รีวิวแล้ว จากชุดที่ระบุ */
    @Query("""
           select r.bookingId
           from ServiceReview r
           where r.username = :username
             and r.bookingId in :ids
           """)
    List<Long> findReviewedBookingIdsByUsername(@Param("username") String username,
                                                @Param("ids") Collection<Long> ids);

    /** สรุปรีวิวตามบริการ (Projection: service, avg, cnt) สำหรับหน้า overview */
    @Query("""
           select r.service                         as service,
                  coalesce(avg(r.rating), 0.0)     as avg,
                  count(r.id)                      as cnt
           from ServiceReview r
           group by r.service
           order by r.service asc
           """)
    List<ServiceSummaryRow> avgByService();

    /* ===== Projections ===== */

    interface AvgCountRow {
        Double getAvg();
        Long getCnt();
    }

    interface ServiceSummaryRow {
        String getService();
        Double getAvg();
        Long getCnt();
    }
}
