package com.projectgo.barber_booking;

import com.projectgo.barber_booking.model.User;
import com.projectgo.barber_booking.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@SpringBootApplication(
  scanBasePackages = "com.projectgo.barber_booking",
  exclude = {
    SecurityAutoConfiguration.class,
    UserDetailsServiceAutoConfiguration.class
  }
)
public class BarberbookingApplication {
    public static void main(String[] args) {
        SpringApplication.run(BarberbookingApplication.class, args);
    }


    /** สร้างหรือรีเซ็ตรายการ admin ทุกครั้งที่สตาร์ทแอป */
    @Bean
    public CommandLineRunner initAdmin(
        UserRepository userRepository,
        BCryptPasswordEncoder encoder
    ) {
        return args -> {
            User admin = userRepository
                           .findByUsername("admin")
                           .orElse(new User());
            admin.setUsername("admin");
            admin.setPassword(encoder.encode("password"));
            admin.setRole("ADMIN");
            userRepository.save(admin);
            System.out.println("✅ Admin user seeded/reset!");
        };
    }
}
