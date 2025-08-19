package com.projectgo.barber_booking.controller;

import com.projectgo.barber_booking.model.ServiceReview;
import com.projectgo.barber_booking.service.ReviewService;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.FieldError;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping(value = "/api", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Validated
public class ReviewRestController {

    private final ReviewService service;

    /** Payload สำหรับสร้างรีวิว */
    public record CreateReq(
            @NotNull Long bookingId,
            @NotNull @Min(1) @Max(5) Integer rating,
            String comment
    ) {}

    /** สร้างรีวิว (ต้องล็อกอิน - บังคับโดย SecurityConfig) */
    @PostMapping(value = "/reviews", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> create(@Valid @RequestBody CreateReq req,
                                    @AuthenticationPrincipal(expression = "username") String username) {
        if (username == null || username.isBlank()) {
            // กันกรณีหลุดการตั้งค่า security
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "unauthorized"));
        }

        ServiceReview r = service.create(username, req.bookingId(), req.rating(), req.comment());

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(r.getId())
                .toUri();

        return ResponseEntity.created(location).body(Map.of("id", r.getId()));
    }

    /** อ่านรายการรีวิวของบริการ (แบ่งหน้า) */
    @GetMapping("/services/{name}/reviews")
    public Map<String, Object> list(@PathVariable("name") String serviceName,
                                    @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "10") int size) {
        int p = Math.max(page, 0);
        int s = Math.max(1, Math.min(size, 50)); // จำกัด size 1..50

        Page<ServiceReview> result = service.listByService(serviceName, p, s);

        Map<String, Object> resp = new HashMap<>();
        resp.put("content", result.getContent());
        resp.put("page", result.getNumber());
        resp.put("size", result.getSize());
        resp.put("totalElements", result.getTotalElements());
        resp.put("totalPages", result.getTotalPages());
        return resp;
    }

    /** สรุปคะแนนเฉลี่ย + จำนวนรีวิวของบริการ */
    @GetMapping("/services/{name}/rating")
    public Map<String, Object> rating(@PathVariable("name") String serviceName) {
        double[] s = service.summary(serviceName); // [avg, total]
        double avg = s[0];
        long total = (long) s[1];

        // ปัดทศนิยม 1 ตำแหน่งให้อ่านง่าย (เช่น 4.2)
        double rounded = BigDecimal.valueOf(avg).setScale(1, RoundingMode.HALF_UP).doubleValue();

        return Map.of("avgRating", rounded, "total", total);
    }

    /* ------------------- Error handlers (ให้ตอบกลับเป็น JSON สวยงาม) ------------------- */

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleInvalidBody(MethodArgumentNotValidException ex) {
        Map<String, Object> errors = new HashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            errors.put(fe.getField(), fe.getDefaultMessage());
        }
        return Map.of(
                "message", "validation_failed",
                "errors", errors
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleConstraint(ConstraintViolationException ex) {
        return Map.of(
                "message", "validation_failed",
                "errors", ex.getConstraintViolations()
                        .stream()
                        .collect(
                                java.util.stream.Collectors.toMap(
                                        v -> v.getPropertyPath().toString(),
                                        jakarta.validation.ConstraintViolation::getMessage,
                                        (a, b) -> a
                                )
                        )
        );
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleBadRequest(RuntimeException ex) {
        return Map.of("message", ex.getMessage());
    }
}
