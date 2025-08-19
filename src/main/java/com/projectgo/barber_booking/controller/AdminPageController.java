package com.projectgo.barber_booking.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminPageController {

    @GetMapping("/admin")
    public String admin() {
        return "admin_dashboard"; // ไม่ต้องใส่ .html
    }
}
