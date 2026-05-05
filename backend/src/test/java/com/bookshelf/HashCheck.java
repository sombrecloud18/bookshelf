package com.bookshelf;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertTrue;

class HashCheck {
    @Test
    void seedAdminHashMatchesAdminPassword() {
        BCryptPasswordEncoder enc = new BCryptPasswordEncoder();
        String hash = "$2a$10$f8O0f5OywnGAWk6s4DmUj.iq1Zqx9uWlFTi9vECfmcL0QcvRTcPwa";
        assertTrue(enc.matches("admin123", hash),
                "Seeded admin hash MUST verify with password 'admin123'");
    }
}
