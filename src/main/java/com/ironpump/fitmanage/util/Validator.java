package com.ironpump.fitmanage.util;

import com.ironpump.fitmanage.exception.ValidationException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

/**
 * One place for all field-level validation rules. Every form and every service
 * calls into this class so the same rule fires whether the user typed it or the
 * service is re-validating.
 */
public final class Validator {
    private static final Pattern PHONE   = Pattern.compile("^\\+?\\d{10,15}$");
    private static final Pattern EMAIL   = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final String[] COMMON_BAD = {
            "password", "12345678", "123456789", "1234567890", "qwerty", "qwerty123",
            "admin", "admin123", "letmein", "welcome", "iloveyou", "abc123", "monkey", "dragon"
    };

    private Validator() {}

    public static String requireText(String field, String value) {
        if (value == null || value.trim().isEmpty())
            throw new ValidationException(field, "Field is required: " + field);
        return value.trim();
    }

    public static String requireMax(String field, String value, int max) {
        String v = requireText(field, value);
        if (v.length() > max) throw new ValidationException(field, "Field " + field + " max length is " + max);
        return v;
    }

    public static String phone(String field, String value) {
        requireText(field, value);
        String stripped = value.replaceAll("[\\s-]", "");
        if (!PHONE.matcher(stripped).matches())
            throw new ValidationException(field, "Phone must be 10-15 digits, optional + prefix.");
        return stripped;
    }

    public static String email(String field, String value) {
        if (value == null || value.isBlank()) return null;
        if (!EMAIL.matcher(value.trim()).matches())
            throw new ValidationException(field, "Invalid email format.");
        return value.trim();
    }

    public static LocalDate pastDate(String field, String value) {
        requireText(field, value);
        try {
            LocalDate d = LocalDate.parse(value.trim());
            if (!d.isBefore(LocalDate.now())) throw new ValidationException(field, "Date must be in the past.");
            return d;
        } catch (DateTimeParseException e) {
            throw new ValidationException(field, "Invalid date (YYYY-MM-DD).");
        }
    }

    public static LocalDate date(String field, String value) {
        requireText(field, value);
        try { return LocalDate.parse(value.trim()); }
        catch (DateTimeParseException e) { throw new ValidationException(field, "Invalid date."); }
    }

    public static BigDecimal positiveMoney(String field, String value) {
        requireText(field, value);
        try {
            BigDecimal v = new BigDecimal(value.trim());
            if (v.signum() <= 0) throw new ValidationException(field, "Amount must be greater than zero.");
            return v;
        } catch (NumberFormatException e) {
            throw new ValidationException(field, "Invalid amount.");
        }
    }

    public static int intInRange(String field, String value, int min, int max) {
        try {
            int v = Integer.parseInt(value.trim());
            if (v < min || v > max) throw new ValidationException(field, "Must be between " + min + " and " + max);
            return v;
        } catch (NumberFormatException e) { throw new ValidationException(field, "Invalid integer."); }
    }

    public static String password(String value) {
        requireText("password", value);
        if (value.length() < 8)
            throw new ValidationException("password", "Password must be at least 8 characters.");
        boolean hasLetter = value.chars().anyMatch(Character::isLetter);
        boolean hasDigit  = value.chars().anyMatch(Character::isDigit);
        if (!hasLetter || !hasDigit)
            throw new ValidationException("password", "Password must contain at least one letter and one digit.");
        for (String bad : COMMON_BAD)
            if (value.equalsIgnoreCase(bad))
                throw new ValidationException("password", "That password is too common. Pick another.");
        return value;
    }
}
