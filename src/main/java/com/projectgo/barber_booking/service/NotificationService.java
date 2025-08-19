package com.projectgo.barber_booking.service;

import com.projectgo.barber_booking.model.Booking;
import com.projectgo.barber_booking.model.User;
import com.projectgo.barber_booking.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final JavaMailSender mailSender;
    private final UserRepository userRepo;
    private final RestTemplate rest = new RestTemplate();

    @Value("${notify.email.enabled:false}")
    private boolean emailEnabled;

    @Value("${notify.line.enabled:false}")
    private boolean lineEnabled;

    @Value("${notify.line.token:}")
    private String lineToken;

    // ผู้ส่งอีเมล (ไม่ตั้งจะ fallback เป็น spring.mail.username)
    @Value("${app.mail.from:${spring.mail.username}}")
    private String fromAddress;

    public NotificationService(JavaMailSender mailSender, UserRepository userRepo) {
        this.mailSender = mailSender;
        this.userRepo = userRepo;
    }

    /** แจ้งที่ร้านผ่าน LINE Notify เมื่อมีการจองใหม่ */
    public void sendNewBookingAlert(Booking b) {
        if (!lineEnabled || lineToken == null || lineToken.isBlank()) return;

        String msg = """
                🔔 มีการจองใหม่
                • ผู้ใช้: %s
                • บริการ: %s
                • ช่าง: %s
                • วันที่: %s เวลา: %s
                • สถานะ: %s
                """.formatted(
                nullSafe(b.getUsername()),
                nullSafe(b.getService()),
                nullSafe(b.getBarber()),
                b.getDate(),
                b.getTime(),
                nullSafe(b.getStatus())
        );
        sendLineNotify(msg);
    }

    /** ส่งอีเมลยืนยันทันทีหลังสร้างการจอง */
    public void sendBookingConfirmation(Booking b) {
        if (!emailEnabled) return;
        Optional<User> ou = userRepo.findByUsername(b.getUsername());
        ou.map(User::getEmail)
          .filter(e -> e != null && !e.isBlank())
          .ifPresent(email -> {
              String subject = "[ร้านตัดผม] ยืนยันการจองของคุณ";
              String text = """
                      สวัสดีคุณ %s,

                      ระบบได้รับการจองของคุณเรียบร้อยแล้ว 🎉

                      รายละเอียดการจอง:
                      • บริการ: %s
                      • ช่าง: %s
                      • วันที่: %s เวลา: %s
                      • หมายเหตุ: %s
                      • สถานะปัจจุบัน: %s

                      ขอบคุณที่ใช้บริการครับ/ค่ะ
                      """.formatted(
                      nullSafeName(ou.get()),
                      nullSafe(b.getService()),
                      nullSafe(b.getBarber()),
                      b.getDate(),
                      b.getTime(),
                      nullSafe(b.getNote()),
                      nullSafe(b.getStatus())
              );
              sendEmail(email, subject, text);
          });
    }

    /** ส่งอีเมลให้ลูกค้า + LINE ให้ร้าน เมื่อมีการเปลี่ยนสถานะ */
    public void sendStatusChange(Booking b, String oldStatus, String newStatus) {
        // LINE ให้ร้าน
        if (lineEnabled && lineToken != null && !lineToken.isBlank()) {
            String msg = """
                    🔄 เปลี่ยนสถานะการจอง
                    • ID: %d
                    • ผู้ใช้: %s
                    • บริการ: %s
                    • ช่าง: %s
                    • วันที่: %s เวลา: %s
                    • %s → %s
                    """.formatted(
                    b.getId(),
                    nullSafe(b.getUsername()),
                    nullSafe(b.getService()),
                    nullSafe(b.getBarber()),
                    b.getDate(),
                    b.getTime(),
                    nullSafe(oldStatus),
                    nullSafe(newStatus)
            );
            sendLineNotify(msg);
        }

        // Email ให้ลูกค้า
        if (emailEnabled) {
            Optional<User> ou = userRepo.findByUsername(b.getUsername());
            ou.map(User::getEmail)
              .filter(e -> e != null && !e.isBlank())
              .ifPresent(email -> {
                  String subject = "[ร้านตัดผม] อัปเดตสถานะการจอง: " + newStatus;
                  String text = """
                          สวัสดีคุณ %s,

                          สถานะการจองของคุณถูกเปลี่ยนเป็น: %s

                          รายละเอียดการจอง:
                          • บริการ: %s
                          • ช่าง: %s
                          • วันที่: %s เวลา: %s
                          • หมายเหตุ: %s

                          ขอบคุณที่ใช้บริการครับ/ค่ะ
                          """.formatted(
                          nullSafeName(ou.get()),
                          newStatus,
                          nullSafe(b.getService()),
                          nullSafe(b.getBarber()),
                          b.getDate(),
                          b.getTime(),
                          nullSafe(b.getNote())
                  );
                  sendEmail(email, subject, text);
              });
        }
    }

    /* ---------------- helpers ---------------- */

    private void sendEmail(String to, String subject, String text) {
        try {
            if (to == null || to.isBlank()) {
                log.warn("ข้ามการส่งอีเมล: address ว่าง");
                return;
            }
            SimpleMailMessage m = new SimpleMailMessage();
            if (fromAddress != null && !fromAddress.isBlank()) {
                m.setFrom(fromAddress);
            }
            m.setTo(to);
            m.setSubject(subject);
            m.setText(text);
            mailSender.send(m);
            log.info("✅ ส่งอีเมลไปยัง {}", to);
        } catch (Exception ex) {
            log.warn("ส่งอีเมลล้มเหลวไปยัง {}: {}", to, ex.getMessage());
        }
    }

    private void sendLineNotify(String message) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.setBearerAuth(lineToken);

            // เข้ารหัสสำหรับ x-www-form-urlencoded (ขึ้นบรรทัดใช้ %0A)
            String encoded = URLEncoder.encode(message, StandardCharsets.UTF_8);
            String body = "message=" + encoded;

            HttpEntity<String> req = new HttpEntity<>(body, headers);
            ResponseEntity<String> res = rest.postForEntity(
                    "https://notify-api.line.me/api/notify", req, String.class);

            if (!res.getStatusCode().is2xxSuccessful()) {
                log.warn("ส่ง LINE Notify ไม่สำเร็จ: HTTP {} | {}", res.getStatusCode(), res.getBody());
            }
        } catch (Exception ex) {
            log.warn("ส่ง LINE Notify ล้มเหลว: {}", ex.getMessage());
        }
    }

    private String nullSafe(Object o) { return o == null ? "-" : o.toString(); }

    private String nullSafeName(User u) {
        if (u == null) return "-";
        if (u.getFullName() != null && !u.getFullName().isBlank()) return u.getFullName();
        return u.getUsername();
    }
}
