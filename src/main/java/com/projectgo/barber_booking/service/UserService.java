package com.projectgo.barber_booking.service;

import java.util.NoSuchElementException;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projectgo.barber_booking.dto.PasswordChangeForm;
import com.projectgo.barber_booking.dto.ProfileUpdateForm;
import com.projectgo.barber_booking.model.User;
import com.projectgo.barber_booking.repository.UserRepository;


@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository repo;
  private final PasswordEncoder encoder;

  // --- เพิ่มเมธอดนี้ เพื่อให้ Controller ดึง user ไปโชว์วันที่สมัครได้ ---
  public User getById(Long id) {
    return repo.findById(id).orElseThrow(() -> new NoSuchElementException("user not found"));
  }

  private User requireUserByLogin(String login){
    return repo.findByEmail(login)
        .orElseGet(() -> repo.findByUsername(login)
            .orElseThrow(() -> new BadCredentialsException("User not found")));
  }

  public Long currentUserId(Authentication auth){
    if (auth == null) throw new BadCredentialsException("Unauthenticated");
    Object principal = auth.getPrincipal();
    String login = (principal instanceof UserDetails)
        ? ((UserDetails) principal).getUsername()
        : auth.getName();
    return requireUserByLogin(login).getId();
  }

  public ProfileUpdateForm toForm(Long userId){
    User u = getById(userId);
    ProfileUpdateForm f = new ProfileUpdateForm();
    f.setFullName(u.getFullName() == null ? "" : u.getFullName());
    f.setEmail(u.getEmail());
    return f;
  }

  @Transactional
  public void updateProfile(Long userId, ProfileUpdateForm f){
    User u = getById(userId);

    // อัปเดตเฉพาะฟิลด์ที่ "เปลี่ยนจริง"
    String newFull = f.getFullName() == null ? "" : f.getFullName().trim();
    String curFull = u.getFullName() == null ? "" : u.getFullName().trim();
    if (!newFull.isEmpty() && !newFull.equals(curFull)) {
      u.setFullName(newFull);
    }

    String newEmailRaw = f.getEmail() == null ? "" : f.getEmail().trim();
    String curEmailRaw = u.getEmail() == null ? "" : u.getEmail().trim();
    // เช็กอีเมลซ้ำ "เฉพาะตอนเปลี่ยน"
    if (!newEmailRaw.isEmpty() && !newEmailRaw.equalsIgnoreCase(curEmailRaw)) {
      if (repo.existsByEmailAndIdNot(newEmailRaw, userId)) {
        throw new IllegalArgumentException("EMAIL_TAKEN");
      }
      u.setEmail(newEmailRaw);
    }
    // ถ้าไม่เปลี่ยนอะไรเลย ก็จะไม่โยน error — ผ่านได้
  }

  @Transactional
  public void changePassword(Long userId, PasswordChangeForm f){
    if (!f.matches()) throw new IllegalArgumentException("PW_MISMATCH");
    User u = repo.findById(userId).orElseThrow();
    if (!encoder.matches(f.getCurrentPassword(), u.getPassword()))
      throw new BadCredentialsException("BAD_CURRENT");
    u.setPassword(encoder.encode(f.getNewPassword()));
  }
  
}
