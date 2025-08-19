package com.projectgo.barber_booking.controller;

import com.projectgo.barber_booking.model.ServiceReview;
import com.projectgo.barber_booking.repository.ServiceReviewRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * หน้า “รีวิวรวม” และ “รายการรีวิวของบริการหนึ่ง ๆ”
 * - GET /reviews                   : รวมทุกบริการ (สรุป avg + count)
 * - GET /reviews/service/{service} : รายการรีวิวของบริการนั้น ๆ (แบ่งหน้า)
 *
 * ต้องมีเมธอดใน ServiceReviewRepository:
 *   Page<ServiceReview> findByServiceOrderByCreatedAtDesc(String, Pageable)
 *   AvgCountRow        avgAndCount(String)
 *   List<ServiceSummaryRow> avgByService()
 */
@Controller
public class ReviewBrowseController {

    private final ServiceReviewRepository reviewRepo;

    public ReviewBrowseController(ServiceReviewRepository reviewRepo) {
        this.reviewRepo = reviewRepo;
    }

    /** รีวิวรวมทุกบริการ (สรุปค่าเฉลี่ยและจำนวนรีวิว) */
    @GetMapping("/reviews")
    public String reviewsOverview(Model model) {
        List<ServiceReviewRepository.ServiceSummaryRow> rows = reviewRepo.avgByService();

        List<ServiceSummary> summaries = new ArrayList<>();
        for (ServiceReviewRepository.ServiceSummaryRow r : rows) {
            String service = r.getService();
            double avg = (r.getAvg() == null) ? 0.0 : r.getAvg();
            long count = (r.getCnt() == null) ? 0L : r.getCnt();
            summaries.add(new ServiceSummary(service, avg, count));
        }

        model.addAttribute("summaries", summaries);
        return "reviews"; // templates/reviews.html
    }

    /** รายการรีวิวของบริการหนึ่ง ๆ (รองรับ page เริ่ม 1 หรือ 0 ก็ได้) */
    @GetMapping("/reviews/service/{service}")
    public String reviewsByService(@PathVariable("service") String service,
                                   @RequestParam(defaultValue = "0") int page,
                                   Model model) {

        int size = 8;
        int pageIndex = (page > 0) ? page - 1 : 0; // ให้ 1-based ก็ใช้ได้

        // ดึงรายการรีวิวแบบแบ่งหน้า
        Page<ServiceReview> p =
                reviewRepo.findByServiceOrderByCreatedAtDesc(service, PageRequest.of(pageIndex, size));

        // ค่าเฉลี่ยและจำนวนรีวิวแบบ Projection (ไม่ต้องแคสต์ Object[])
        ServiceReviewRepository.AvgCountRow ac = reviewRepo.avgAndCount(service);
        double avg = (ac == null || ac.getAvg() == null) ? 0.0 : ac.getAvg();
        long count = (ac == null || ac.getCnt() == null) ? 0L : ac.getCnt();

        // เผื่อกรณี count = 0 แต่หน้ารีวิวมีข้อมูล (กันพลาดจาก DB function)
        if (count == 0 && p.getTotalElements() > 0) {
            count = p.getTotalElements();
        }

        model.addAttribute("service", service);
        model.addAttribute("avg", avg);
        model.addAttribute("count", count);
        model.addAttribute("page", p);

        return "reviews_service"; // templates/reviews_service.html
    }

    /** DTO สำหรับหน้า overview */
    public static class ServiceSummary {
        private final String service;
        private final double avg;
        private final long count;

        public ServiceSummary(String service, double avg, long count) {
            this.service = service;
            this.avg = avg;
            this.count = count;
        }
        public String getService() { return service; }
        public double getAvg() { return avg; }
        public long getCount() { return count; }
        public int getAvgInt() { return (int) Math.round(avg); } // ใช้ทำดาว
    }
}
