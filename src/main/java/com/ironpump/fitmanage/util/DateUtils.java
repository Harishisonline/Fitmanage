package com.ironpump.fitmanage.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Single source of date formatting. Add new patterns here, not in views. */
public final class DateUtils {
    private DateUtils() {}

    public static final DateTimeFormatter DEFAULT_DATE     = DateTimeFormatter.ofPattern("dd MMM yyyy");
    public static final DateTimeFormatter DEFAULT_DATETIME = DateTimeFormatter.ofPattern("dd MMM yyyy · HH:mm");
    public static final DateTimeFormatter ISO_DATE         = DateTimeFormatter.ISO_LOCAL_DATE;
    public static final DateTimeFormatter ISO_DATETIME     = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public static String format(LocalDate d) { return d == null ? "" : d.format(DEFAULT_DATE); }
    public static String format(LocalDateTime dt) { return dt == null ? "" : dt.format(DEFAULT_DATETIME); }
    public static String formatDateOnly(LocalDateTime dt) { return dt == null ? "" : dt.toLocalDate().format(DEFAULT_DATE); }
}
