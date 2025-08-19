package com.projectgo.barber_booking.dto;

import jakarta.validation.constraints.*;

public class PasswordChangeForm {
  @NotBlank(message="กรอกรหัสผ่านเดิม")
  private String currentPassword;

  @NotBlank @Size(min=8, message="อย่างน้อย 8 ตัวอักษร")
  private String newPassword;

  @NotBlank(message="ยืนยันรหัสผ่านใหม่")
  private String confirmPassword;

  public boolean matches(){
    return newPassword != null && newPassword.equals(confirmPassword);
  }

  // getters/setters
  public String getCurrentPassword(){ return currentPassword; }
  public void setCurrentPassword(String v){ this.currentPassword = v; }
  public String getNewPassword(){ return newPassword; }
  public void setNewPassword(String v){ this.newPassword = v; }
  public String getConfirmPassword(){ return confirmPassword; }
  public void setConfirmPassword(String v){ this.confirmPassword = v; }
}
