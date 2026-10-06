package com.ironpump.fitmanage.util;

import com.ironpump.fitmanage.config.AppConfig;
import com.ironpump.fitmanage.model.Member;
import com.ironpump.fitmanage.model.Payment;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;

/**
 * Renders a payment receipt as a PDF using PDFBox 3.x. A4 portrait, 1 page.
 * Two entry points:
 *   - writePdf(Path, String[])   — write a list of lines, lines starting with "#" are bold.
 *   - writeReceipt(Path, member, payment, cfg) — high-level: render a branded receipt.
 * Returns the path to the written file. Used by PaymentReceiptDialog.
 */
public final class ReceiptRenderer {
    private static final Logger log = LoggerFactory.getLogger(ReceiptRenderer.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    private ReceiptRenderer() {}

    public static Path writeReceipt(Path target, Member m, Payment p, AppConfig cfg) {
        String gymName = cfg == null ? "FitManage" : cfg.getOrDefault("app.gym.name", "FitManage");
        String addr    = cfg == null ? ""          : cfg.getOrDefault("app.gym.address", "");
        String phone   = cfg == null ? ""          : cfg.getOrDefault("app.gym.phone", "");
        String email   = cfg == null ? ""          : cfg.getOrDefault("app.gym.email", "");
        String footer  = cfg == null ? "This is a computer-generated receipt and does not require a signature."
                : cfg.getOrDefault("app.gym.receipt_footer", "This is a computer-generated receipt and does not require a signature.");
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                PDType1Font body = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
                PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                float y = page.getMediaBox().getHeight() - 48;
                cs.setNonStrokingColor(1f, 1f, 1f);
                cs.addRect(36, 36, page.getMediaBox().getWidth() - 72, page.getMediaBox().getHeight() - 72);
                cs.fill();
                cs.setNonStrokingColor(0.08f, 0.10f, 0.12f);
                y = text(cs, bold, 16, 56, y, strip(gymName));
                y = text(cs, body, 11, 56, y, strip(addr));
                y = text(cs, body, 11, 56, y, strip(phone + "  " + email));
                y -= 8;
                y = text(cs, bold, 13, 56, y, strip("Receipt " + p.getReceiptNo()));
                y = text(cs, body, 11, 56, y, strip("Date: " + p.getPaymentDate().format(DATE_FMT)));
                y -= 10;
                y = text(cs, bold, 12, 56, y, "Payment receipt");
                y = text(cs, body, 11, 56, y, "Thank you for training with us.");
                y -= 8;
                y = text(cs, bold, 11, 56, y, "MEMBER");
                y = text(cs, body, 11, 56, y, strip("Name: " + m.getFullName()));
                y = text(cs, body, 11, 56, y, strip("Member ID: " + m.getMemberCode()));
                y = text(cs, body, 11, 56, y, strip("Phone: " + m.getPhone()));
                y = text(cs, body, 11, 56, y, strip("Email: " + (m.getEmail() == null ? "" : m.getEmail())));
                y -= 8;
                y = text(cs, bold, 11, 56, y, "Description                         Amount");
                y = text(cs, body, 11, 56, y, strip("Membership payment                  Rs. " + p.getAmount().abs()));
                y = text(cs, body, 11, 56, y, "GST (0%)                            Rs. 0.00");
                y = text(cs, bold, 12, 56, y, strip("Total paid                          Rs. " + p.getAmount()));
                y -= 8;
                y = text(cs, bold, 11, 56, y, "PAYMENT");
                y = text(cs, body, 11, 56, y, strip("Type: " + p.getPaymentType()));
                y = text(cs, body, 11, 56, y, strip("Method: " + p.getMethod()));
                y = text(cs, body, 11, 56, y, strip("Reference: " + (p.getReference() == null ? "" : p.getReference())));
                y -= 16;
                text(cs, body, 10, 56, y, strip(footer));
            }
            if (target.getParent() != null) Files.createDirectories(target.getParent());
            doc.save(target.toFile());
            log.info("Receipt PDF written: {}", target);
            return target;
        } catch (IOException e) {
            throw new com.ironpump.fitmanage.exception.FitManageException("Could not write receipt PDF: " + target, e);
        }
    }

    private static float text(PDPageContentStream cs, PDType1Font font, float size, float x, float y, String line) throws IOException {
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        cs.showText(line == null ? "" : line);
        cs.endText();
        return y - (size + 6);
    }

    public static Path writePdf(Path target, String[] lines) {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                PDType1Font body = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
                PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                float y = page.getMediaBox().getHeight() - 60;
                for (String l : lines) {
                    cs.beginText();
                    cs.setFont(l != null && l.startsWith("#") ? bold : body, 12);
                    cs.newLineAtOffset(50, y);
                    cs.showText(strip(l == null ? "" : (l.startsWith("#") ? l.substring(1) : l)));
                    cs.endText();
                    y -= 18;
                }
            }
            Files.createDirectories(target.getParent());
            doc.save(target.toFile());
            log.info("Receipt PDF written: {}", target);
            return target;
        } catch (IOException e) {
            throw new com.ironpump.fitmanage.exception.FitManageException(
                    "Could not write receipt PDF: " + target, e);
        }
    }

    private static String strip(String s) {
        // PDFBox built-in fonts (WinAnsiEncoding) do NOT support all Unicode. Strip non-WinAnsi.
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\u20B9' || c == '\u20AC' || c == '\u00A3') { out.append("Rs."); continue; }
            if (c == '\u2022') { out.append("*"); continue; }
            if (c == '\u2013' || c == '\u2014') { out.append('-'); continue; }
            if (c < 0x20 || (c > 0x7E && c < 0xA0)) out.append('?');
            else out.append(c);
        }
        return out.toString();
    }
}
