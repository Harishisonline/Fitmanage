package com.ironpump.fitmanage.util;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

public final class CurrencyUtils {
    private static final NumberFormat INR = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
    private static final NumberFormat USD = NumberFormat.getCurrencyInstance(Locale.US);
    private static final NumberFormat EUR = NumberFormat.getCurrencyInstance(Locale.GERMANY);

    private CurrencyUtils() {}

    public static String format(BigDecimal amount, String currency) {
        if (amount == null) return "";
        return switch (currency == null ? "INR" : currency.toUpperCase()) {
            case "USD" -> USD.format(amount);
            case "EUR" -> EUR.format(amount);
            default    -> INR.format(amount);
        };
    }
}
