package com.operations.auth.config;

import com.operations.auth.model.TenantDocument;
import com.operations.auth.model.UserDocument;
import com.operations.auth.repository.TenantRepository;
import com.operations.auth.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class SeedTenantAndUserConfig {

  @Bean
  CommandLineRunner seedTenantAndUser(
      TenantRepository tenantRepository,
      UserRepository userRepository,
      PasswordEncoder passwordEncoder,
      @Value("${seed.tenant.id}") String tenantId,
      @Value("${seed.user.email}") String email,
      @Value("${seed.user.password}") String password,
      @Value("${seed.user.role}") String role) {
    return args -> {
      tenantRepository.findById(tenantId).orElseGet(() -> {
        TenantDocument tenant = new TenantDocument();
        tenant.setId(tenantId);
        tenant.setName(tenantId);
        tenant.setActive(true);
        tenant.setCreatedAt(Instant.now());
        return tenantRepository.save(tenant);
      });

      userRepository.findByTenantIdAndEmailIgnoreCase(tenantId, email).orElseGet(() -> {
        UserDocument user = new UserDocument();
        user.setTenantId(tenantId);
        user.setEmail(email.toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRoles(List.of(role));
        user.setActive(true);
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());
        return userRepository.save(user);
      });
    };
  }
}
