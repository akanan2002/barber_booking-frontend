package com.projectgo.barber_booking.service;

import com.projectgo.barber_booking.model.User;
import com.projectgo.barber_booking.repository.UserRepository;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {
  private final UserRepository repo;
  public CustomUserDetailsService(UserRepository repo) {
    this.repo = repo;
  }

  @Override
  public UserDetails loadUserByUsername(String username)
      throws UsernameNotFoundException {
    User user = repo.findByUsername(username)
                    .orElseThrow(() ->
                      new UsernameNotFoundException("User not found"));
    return new org.springframework.security.core.userdetails.User(
      user.getUsername(),
      user.getPassword(),
      List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole()))
    );
  }
}
