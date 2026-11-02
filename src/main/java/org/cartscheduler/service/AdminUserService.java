package org.cartscheduler.service;

import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.cartscheduler.entity.AdminUser;
import org.cartscheduler.repository.AdminUserRepository;
import org.cartscheduler.web.form.AdminUserForm;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminUserService {
    private final AdminUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public List<AdminUser> findAll() {
        return userRepository.findAll(Sort.by(Sort.Order.asc("username").ignoreCase()));
    }

    public AdminUser getRequired(long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Nie znaleziono użytkownika o ID " + id + "."));
    }

    @Transactional
    public AdminUser create(AdminUserForm form) {
        validateUniqueValues(form, null);
        AdminUser user = new AdminUser();
        copyValues(form, user);
        user.setPassword(passwordEncoder.encode(form.getPassword()));
        return userRepository.save(user);
    }

    @Transactional
    public AdminUser update(long id, AdminUserForm form) {
        AdminUser user = getRequired(id);
        validateUniqueValues(form, id);
        copyValues(form, user);
        if (hasText(form.getPassword())) {
            user.setPassword(passwordEncoder.encode(form.getPassword()));
        }
        return userRepository.save(user);
    }

    @Transactional
    public void delete(long id) {
        userRepository.delete(getRequired(id));
    }

    @Transactional
    public void recordSuccessfulLogin(long id) {
        userRepository.findById(id).ifPresent(user -> user.setDateLastLogin(LocalDateTime.now()));
    }

    private void validateUniqueValues(AdminUserForm form, Long currentId) {
        String username = normalized(form.getUsername());
        userRepository.findByUsernameIgnoreCase(username)
                .filter(existing -> currentId == null || existing.getId() != currentId)
                .ifPresent(existing -> {
                    throw new FieldValidationException("username", "Taka nazwa użytkownika jest już zajęta.");
                });

        String email = normalized(form.getEmail());
        userRepository.findByEmailIgnoreCase(email)
                .filter(existing -> currentId == null || existing.getId() != currentId)
                .ifPresent(existing -> {
                    throw new FieldValidationException("email", "Taki adres e-mail jest już używany.");
                });
    }

    private void copyValues(AdminUserForm form, AdminUser user) {
        user.setUsername(normalized(form.getUsername()));
        user.setName(normalized(form.getName()));
        user.setEmail(normalized(form.getEmail()));
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String normalized(String value) {
        return value == null ? null : value.strip();
    }
}
