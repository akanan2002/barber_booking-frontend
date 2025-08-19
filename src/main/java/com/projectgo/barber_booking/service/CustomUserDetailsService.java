package com.projectgo.barber_booking.service;

import com.projectgo.barber_booking.model.User;
import com.projectgo.barber_booking.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<User> opt = userRepository.findByUsername(username);
        User u = opt.orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        // รองรับหลายสิทธิ์คั่นด้วย comma และ normalize ให้เป็น ROLE_*
        String roleField = u.getRole() == null ? "" : u.getRole().trim();
        if (roleField.isEmpty()) roleField = "USER";

        List<SimpleGrantedAuthority> authorities = Arrays.stream(roleField.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(this::ensureRolePrefix)                    // เติม ROLE_ ถ้ายังไม่มี + upper-case
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        return org.springframework.security.core.userdetails.User
                .withUsername(u.getUsername())
                .password(u.getPassword())
                .authorities(authorities)
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(false)
                .build();
    }

    private String ensureRolePrefix(String role) {
        String r = role.toUpperCase();
        return r.startsWith("ROLE_") ? r : "ROLE_" + r;
    }
}
