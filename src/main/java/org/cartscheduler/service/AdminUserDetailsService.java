package org.cartscheduler.service;

import lombok.RequiredArgsConstructor;
import org.cartscheduler.impl.AdminUserDetails;
import org.cartscheduler.repository.AdminUserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminUserDetailsService implements UserDetailsService {
    private final AdminUserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsernameIgnoreCase(username)
                .map(AdminUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("Administrator not found: " + username));
    }
}
