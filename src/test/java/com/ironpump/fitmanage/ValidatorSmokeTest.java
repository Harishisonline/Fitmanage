package com.ironpump.fitmanage;

import com.ironpump.fitmanage.auth.PasswordHasher;
import com.ironpump.fitmanage.util.Validator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ValidatorSmokeTest {

    @Test
    void password_accepts_strong_input() {
        assertDoesNotThrow(() -> Validator.password("MyStr0ng!Pass"));
    }

    @Test
    void password_rejects_weak_input() {
        assertThrows(RuntimeException.class, () -> Validator.password("password"));
        assertThrows(RuntimeException.class, () -> Validator.password("12345678"));
        assertThrows(RuntimeException.class, () -> Validator.password(""));
    }

    @Test
    void phone_normalises_in_indian_style() {
        // 10-digit input
        String normalised = Validator.phone("phone", "9876543210");
        assertNotNull(normalised);
        assertTrue(normalised.length() >= 10);
    }

    @Test
    void email_basic_check() {
        assertEquals("me@you.com", Validator.email("email", "me@you.com"));
        assertThrows(RuntimeException.class, () -> Validator.email("email", "not-an-email"));
    }

    @Test
    void bcrypt_round_trip_works() {
        String hash = PasswordHasher.hash("hello123");
        assertNotNull(hash);
        assertTrue(hash.startsWith("$2a$"), "expected bcrypt prefix, got " + hash);
        assertTrue(PasswordHasher.verify("hello123", hash));
        assertFalse(PasswordHasher.verify("hello124", hash));
    }
}
