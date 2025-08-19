package com.projectgo.barber_booking.controller;

import com.projectgo.barber_booking.model.User;
import com.projectgo.barber_booking.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.*;

@Controller
public class HomeController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    // --- [0] หน้า Admin (สำหรับ Thymeleaf view) ---
    // SecurityConfig จะอนุญาตเฉพาะ ROLE_ADMIN เข้าหน้านี้
    // @GetMapping("/admin")
    // public String adminPage() {
    //     return "admin_dashboard";
    // }

    // --- [1] หน้าสมัครสมาชิก ---
    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        model.addAttribute("user", new User());
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@ModelAttribute("user") User user,
                               @RequestParam("confirmPassword") String confirmPassword,
                               Model model) {

        String username   = Optional.ofNullable(user.getUsername()).orElse("").trim();
        String rawPassword = Optional.ofNullable(user.getPassword()).orElse("");

        // ตรวจสอบความถูกต้องขั้นพื้นฐาน
        if (username.isEmpty() || rawPassword.isEmpty()) {
            model.addAttribute("error", "กรุณากรอกชื่อผู้ใช้และรหัสผ่าน");
            return "register";
        }
        if (!rawPassword.equals(confirmPassword)) {
            model.addAttribute("error", "รหัสผ่านไม่ตรงกัน");
            return "register";
        }
        if (userRepository.findByUsername(username).isPresent()) {
            model.addAttribute("error", "มีชื่อผู้ใช้นี้ในระบบแล้ว");
            return "register";
        }

        // เข้ารหัสรหัสผ่าน + ตั้ง role เริ่มต้นเป็น USER
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole("USER"); // จะถูกเติม ROLE_ โดย CustomUserDetailsService
        userRepository.save(user);

        return "redirect:/login?registered";
    }

    // --- [2] หน้า Login ---
    @GetMapping("/login")
    public String showLoginForm(@RequestParam(value = "error",      required = false) String error,
                                @RequestParam(value = "logout",     required = false) String logout,
                                @RequestParam(value = "registered", required = false) String registered,
                                Model model) {
        if (error != null)      model.addAttribute("errorMessage",   "ชื่อผู้ใช้หรือรหัสผ่านไม่ถูกต้อง");
        if (logout != null)     model.addAttribute("successMessage", "ออกจากระบบเรียบร้อย");
        if (registered != null) model.addAttribute("successMessage", "สมัครสมาชิกสำเร็จ! กรุณาเข้าสู่ระบบ");
        return "login";
    }

// --- [3] หน้า Home ---
@GetMapping({"/", "/home", "/home/"})
public String showHomePage(Model model, Principal principal) {
    if (principal != null) {
        model.addAttribute("username", principal.getName());
    }
    model.addAttribute("activePage", "home"); 
    return "home";
}


    // --- [4] หน้า Profile ---
    @GetMapping("/profile")
    public String userProfile(Model model, Principal principal, HttpServletRequest request) {
        if (principal == null) {
            return "redirect:/login";
        }

        String username = principal.getName();
        Optional<User> userOpt = userRepository.findByUsername(username);
        model.addAttribute("user", userOpt.orElseGet(User::new)); // กัน null

        // ✅ รับ flash attribute (เช่น successMessage) จาก session → ส่งไป Thymeleaf
        String successMessage = (String) request.getSession().getAttribute("successMessage");
        if (successMessage != null) {
            model.addAttribute("successMessage", successMessage);
            request.getSession().removeAttribute("successMessage");
        }

        return "profile";
    }

    // --- [5] Upload รูปโปรไฟล์ ---
    @PostMapping(
        value = "/upload-avatar",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseBody
    public ResponseEntity<Map<String, Object>> uploadAvatar(@RequestParam("avatar") MultipartFile file,
                                                            Principal principal) {
        Map<String, Object> resp = new HashMap<>();

        if (principal == null) {
            resp.put("message", "unauthorized");
            return ResponseEntity.status(401).body(resp);
        }
        if (file == null || file.isEmpty()) {
            resp.put("message", "ไม่พบไฟล์อัปโหลด");
            return ResponseEntity.badRequest().body(resp);
        }

        // ✅ จำกัดขนาดไฟล์ (เช่น 3MB)
        long MAX_SIZE = 3L * 1024 * 1024;
        if (file.getSize() > MAX_SIZE) {
            resp.put("message", "ขนาดไฟล์ต้องไม่เกิน 3MB");
            return ResponseEntity.badRequest().body(resp);
        }

        // ✅ อนุญาตเฉพาะไฟล์รูปภาพบางชนิด
        String contentType = Optional.ofNullable(file.getContentType()).orElse("");
        boolean isImage = contentType.startsWith("image/");
        if (!isImage) {
            resp.put("message", "ไฟล์ที่อัปโหลดต้องเป็นรูปภาพเท่านั้น");
            return ResponseEntity.badRequest().body(resp);
        }

        Set<String> allowedExt = Set.of("jpg", "jpeg", "png", "gif", "webp");
        String original = Optional.ofNullable(file.getOriginalFilename()).orElse("avatar");
        String baseName = Paths.get(original).getFileName().toString(); // กัน path traversal
        String ext = "";
        int dot = baseName.lastIndexOf('.');
        if (dot >= 0 && dot < baseName.length() - 1) {
            ext = baseName.substring(dot + 1).toLowerCase(Locale.ROOT);
        }
        if (!ext.isEmpty() && !allowedExt.contains(ext)) {
            resp.put("message", "อนุญาตเฉพาะไฟล์: " + allowedExt);
            return ResponseEntity.badRequest().body(resp);
        }
        if (ext.isEmpty()) {
            // เดา ext จาก content-type เบื้องต้น
            if (contentType.equals("image/png")) ext = "png";
            else if (contentType.equals("image/jpeg")) ext = "jpg";
            else if (contentType.equals("image/gif")) ext = "gif";
            else if (contentType.equals("image/webp")) ext = "webp";
            else ext = "jpg";
        }

        String username = principal.getName();
        User user = userRepository.findByUsername(username).orElseThrow();

        // 📁 เก็บไฟล์สำหรับ dev: classpath:/static/uploads/
        // หมายเหตุ: เมื่อแพ็กเป็น JAR แนะนำย้ายไปโฟลเดอร์ภายนอกและทำ ResourceHandler ใน WebConfig
        Path uploadDir = Paths.get("src/main/resources/static/uploads");
        try {
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            // ลบไฟล์เก่า (ถ้ามี)
            String oldUrl = user.getAvatarUrl();
            if (oldUrl != null && !oldUrl.isBlank() && oldUrl.startsWith("/uploads/")) {
                Path oldFilePath = Paths.get("src/main/resources/static" + oldUrl);
                Files.deleteIfExists(oldFilePath);
            }

            // ตั้งชื่อไฟล์ใหม่แบบสุ่ม
            String filename = UUID.randomUUID() + "." + ext;
            Path filePath = uploadDir.resolve(filename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // อัปเดต URL ใน DB (เสิร์ฟผ่าน /uploads/**)
            String publicUrl = "/uploads/" + filename;
            user.setAvatarUrl(publicUrl);
            userRepository.save(user);

            resp.put("message", "อัปโหลดสำเร็จ");
            resp.put("avatarUrl", publicUrl);
            return ResponseEntity.ok(resp);

        } catch (IOException e) {
            resp.put("message", "อัปโหลดไม่สำเร็จ: " + e.getMessage());
            return ResponseEntity.internalServerError().body(resp);
        }
    }
}
