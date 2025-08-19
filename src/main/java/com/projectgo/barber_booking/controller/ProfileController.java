package com.projectgo.barber_booking.controller;

import com.projectgo.barber_booking.dto.PasswordChangeForm;
import com.projectgo.barber_booking.dto.ProfileUpdateForm;
import com.projectgo.barber_booking.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

  private final UserService userService;

  @GetMapping({"/edit", "/edit/"})
  public String edit(Model model, Authentication auth){
    Long id = userService.currentUserId(auth);
    model.addAttribute("form", userService.toForm(id));
    model.addAttribute("user", userService.getById(id)); // สำหรับวันที่สมัคร
    return "profile_edit";
  }

  @PostMapping({"/edit", "/edit/"})
  public String update(@Valid @ModelAttribute("form") ProfileUpdateForm form,
                       BindingResult br,
                       Authentication auth,
                       RedirectAttributes ra){
    if (br.hasErrors()) return "profile_edit";
    Long id = userService.currentUserId(auth);
    try {
      userService.updateProfile(id, form);
      ra.addFlashAttribute("ok","อัปเดตข้อมูลแล้ว");
      return "redirect:/profile";
    } catch (IllegalArgumentException e){
      if ("EMAIL_TAKEN".equals(e.getMessage())) {
        br.rejectValue("email","dup","อีเมลนี้ถูกใช้แล้ว");
      } else {
        br.reject("err","เกิดข้อผิดพลาด");
      }
      return "profile_edit";
    }
  }

  @GetMapping({"/password", "/password/"})
  public String password(Model model){
    model.addAttribute("form", new PasswordChangeForm());
    return "profile_password";
  }
  
  @PostMapping({"/password", "/password/"})
  public String changePassword(@Valid @ModelAttribute("form") PasswordChangeForm form,
                               BindingResult br,
                               Authentication auth,
                               RedirectAttributes ra){
    if (!form.matches()){
      br.rejectValue("confirmPassword","mismatch","รหัสผ่านไม่ตรงกัน");
      return "profile_password";
    }
    Long id = userService.currentUserId(auth);
    try{
      userService.changePassword(id, form);
      ra.addFlashAttribute("ok","เปลี่ยนรหัสผ่านเรียบร้อย");
      return "redirect:/profile";
    }catch (BadCredentialsException e){
      br.rejectValue("currentPassword","bad","รหัสผ่านเดิมไม่ถูกต้อง");
      return "profile_password";
    }
  }  
}
