package com.projectgo.barber_booking.dto;

import jakarta.validation.constraints.*;

public class ProfileUpdateForm {

  @NotBlank(message = "กรอกชื่อ-นามสกุล")
  private String fullName;

  @NotBlank(message = "กรอกอีเมล")
  @Email(message = "อีเมลไม่ถูกต้อง")
  private String email;

  // getters/setters
  public String getFullName() { return fullName; }
  public void setFullName(String fullName) { this.fullName = fullName; }

  public String getEmail() { return email; }
  public void setEmail(String email) { this.email = email; }
}
