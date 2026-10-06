package com.ironpump.fitmanage.util;

import com.ironpump.fitmanage.exception.FitManageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Writes rows to a CSV file. Every field is quoted (RFC 4180). The file starts
 * with a UTF-8 BOM so Excel and WPS detect the encoding. Phones and dates that
 * must survive a spreadsheet go through {@link #text} and {@link #date}: those
 * cells are Excel text formulas, so a leading + is kept and a date is not
 * rewritten as a locale serial.
 */
public final class CsvExporter {
    private static final Logger log = LoggerFactory.getLogger(CsvExporter.class);

    private CsvExporter() {}

    /**
     * Spreadsheet text cell. Excel and WPS evaluate {@code ="value"} to the
     * literal string, which keeps a leading {@code +} and stops scientific notation.
     */
    public static String text(String value) {
        if (value == null) value = "";
        return "=\"" + value + "\"";
    }

    /** ISO-8601 date kept as text so a spreadsheet does not reformat it. */
    public static String date(LocalDate d) {
        return d == null ? "" : text(d.toString());
    }

    /** Calendar day of a timestamp, same treatment as {@link #date(LocalDate)}. */
    public static String date(LocalDateTime dt) {
        return dt == null ? "" : text(dt.toLocalDate().toString());
    }

    public static Path export(Path file, String[] header, List<String[]> rows) {
        try {
            if (file.getParent() != null) Files.createDirectories(file.getParent());
            try (BufferedWriter w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                w.write('\ufeff');
                w.write(joinRow(header));
                w.newLine();
                for (String[] row : rows) { w.write(joinRow(row)); w.newLine(); }
                log.info("Exported {} rows to {}", rows.size(), file);
                return file;
            }
        } catch (IOException e) {
            throw new FitManageException("Could not write CSV: " + file, e);
        }
    }

    public static Path export(String filename, String[] header, List<String[]> rows) {
        Path dir = Path.of(System.getProperty("user.home"), "fitmanage-exports");
        return export(dir.resolve(filename), header, rows);
    }

    private static String joinRow(String[] row) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < row.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(escape(row[i] == null ? "" : row[i]));
        }
        return sb.toString();
    }

    /** Quote every field. Embedded quotes are doubled, per RFC 4180. */
    private static String escape(String v) {
        return "\"" + v.replace("\"", "\"\"") + "\"";
    }

    /** Convenience: filename like members-2026-05-05.csv. */
    public static String datedName(String prefix) {
        return prefix + "-" + LocalDate.now() + ".csv";
    }
}
