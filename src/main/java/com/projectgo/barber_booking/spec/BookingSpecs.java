package com.projectgo.barber_booking.spec;

import com.projectgo.barber_booking.model.Booking;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

public class BookingSpecs {

    private static Specification<Booking> alwaysTrue() {
        return (root, query, cb) -> cb.conjunction();
    }

    /** สถานะตรงเป๊ะ (ถ้าไม่ใส่ คืน spec ว่าง) */
    public static Specification<Booking> hasStatus(String status) {
        return (root, query, cb) ->
                (status == null || status.isBlank())
                        ? cb.conjunction()
                        : cb.equal(root.get("status"), status);
    }

    /** ค้นหาช่างแบบ LIKE ไม่สนตัวพิมพ์ (ถ้าไม่ใส่ คืน spec ว่าง) */
    public static Specification<Booking> hasBarber(String barber) {
        return (root, query, cb) -> {
            if (barber == null || barber.isBlank()) return cb.conjunction();
            String like = "%" + barber.trim().toLowerCase() + "%";
            return cb.like(cb.lower(root.get("barber")), like);
        };
    }

    /** เลือกวันเดียว */
    public static Specification<Booking> onDate(LocalDate date) {
        return (root, query, cb) ->
                (date == null) ? cb.conjunction() : cb.equal(root.get("date"), date);
    }

    /** ช่วงวันที่ (ถ้าให้ทั้ง start/end จะใช้ between, ให้ข้างเดียวจะใช้ >= หรือ <=) */
    public static Specification<Booking> betweenDates(LocalDate start, LocalDate end) {
        return (root, query, cb) -> {
            if (start == null && end == null) return cb.conjunction();
            if (start != null && end != null) return cb.between(root.get("date"), start, end);
            if (start != null) return cb.greaterThanOrEqualTo(root.get("date"), start);
            return cb.lessThanOrEqualTo(root.get("date"), end);
        };
    }

    /** ค้นหาอิสระหลายคอลัมน์: id / username / service / barber / note */
    public static Specification<Booking> freeText(String q) {
        return (root, query, cb) -> {
            if (q == null || q.isBlank()) return cb.conjunction();

            String like = "%" + q.trim().toLowerCase() + "%";
            Long idEq = null;
            try { idEq = Long.parseLong(q.trim()); } catch (NumberFormatException ignored) {}

            var or = cb.disjunction();
            if (idEq != null) {
                or = cb.or(or, cb.equal(root.get("id"), idEq));
            }
            or = cb.or(or, cb.like(cb.lower(root.get("username")), like));
            or = cb.or(or, cb.like(cb.lower(root.get("service")), like));
            or = cb.or(or, cb.like(cb.lower(root.get("barber")), like));
            or = cb.or(or, cb.like(cb.lower(root.get("note")), like));
            return or;
        };
    }

    /**
     * รวมทุกฟิลเตอร์:
     * - ถ้าใส่ date เดี่ยว จะเพิกเฉย start/end
     * - มี free-text q ค้นหลายคอลัมน์
     */
    public static Specification<Booking> byFilters(
            String status,
            String barber,
            LocalDate date,
            LocalDate start,
            LocalDate end,
            String q
    ) {
        Specification<Booking> spec = alwaysTrue()
                .and(hasStatus(status))
                .and(hasBarber(barber))
                .and(freeText(q));

        // ถ้ามี date เดี่ยว ให้ใช้ onDate() และไม่ใช้ช่วง
        if (date != null) {
            spec = spec.and(onDate(date));
        } else {
            spec = spec.and(betweenDates(start, end));
        }
        return spec;
    }
}
