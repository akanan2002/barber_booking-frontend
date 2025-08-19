package com.projectgo.barber_booking.repository;

import com.projectgo.barber_booking.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // ใช้สำหรับล็อกอิน/ดึงผู้ใช้ปัจจุบัน
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);

    // ใช้เช็คความซ้ำตอนแก้ไขโปรไฟล์
    boolean existsByEmailAndIdNot(String email, Long id);
    boolean existsByUsernameAndIdNot(String username, Long id);

    // เผื่อกรณีต้องตรวจซ้ำแบบทั่วไป
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
}
