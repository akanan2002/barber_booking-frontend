package com.projectgo.barber_booking.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true) // ไม่ให้ username ซ้ำ และห้าม null
    private String username;

    @Column(nullable = false) // รหัสผ่านห้าม null
    private String password;

    @Column(nullable = false) // บทบาทของผู้ใช้ เช่น USER / ADMIN
    private String role;

    @Column(name = "full_name") // ชื่อเต็ม (แสดงใน profile)
    private String fullName;

    @Column(nullable = true) // อีเมลสามารถเป็น null ได้
    private String email;

    @Column(name = "avatar_url", nullable = true)
    private String avatarUrl;

    public String getAvatarUrl() {
        return avatarUrl;
    }
    
    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }
    


    @Column(name = "created_at")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    // ✅ Constructor แบบไม่รวม id และ createdAt (ใช้ตอนสมัครสมาชิก)
    public User(String username, String password, String role) {
        this.username = username;
        this.password = password;
        this.role = role;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    public void setLastName(String lastName) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setLastName'");
    }

    public String getLastName() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getLastName'");
    }
    

}
