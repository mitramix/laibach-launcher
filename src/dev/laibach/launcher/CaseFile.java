package dev.laibach.launcher;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * The case file: the papers, the weekly report and the whole permanent record
 * printed as a PDF to Download/Laibach.
 */
final class CaseFile {
    private CaseFile() {}

    /** The case file as a PDF in Download/Laibach: the papers, the weekly report and the whole record. */
    static boolean caseFile(Context c, boolean german) {
        Locale locale = Lang.locale(german);
        Pdf pdf = new Pdf();
        pdf.text(t(german, "THE LAIBACH COLLECTIVE · CASE FILE", "DAS LAIBACH-KOLLEKTIV · FALLAKTE"), 20, true, Pdf.RED);
        pdf.text(t(german, "CONFIDENTIAL. A WORK OF FICTION, PRINTED BY THE LAUNCHER FOR ITS OWNER.",
                "VERTRAULICH. EIN WERK DER FIKTION, VOM LAUNCHER FÜR SEINEN BESITZER GEDRUCKT."), 8, true, Pdf.GREY);
        pdf.gap(10);
        pdf.text(t(german, "CITIZEN № ", "BÜRGER NR. ") + Citizen.number(c), 14, true, Pdf.INK);
        pdf.text(t(german, "STANDING: ", "ANSEHEN: ") + Lang.pick(german, Citizen.STANDING[Citizen.grade(c)]), 10, true, Pdf.INK);
        pdf.text(t(german, "PRINTED ", "GEDRUCKT ")
                + new SimpleDateFormat(t(german, "d MMMM yyyy, HH:mm", "d. MMMM yyyy, HH:mm"), locale)
                .format(new Date()).toUpperCase(locale), 10, false, Pdf.INK);
        pdf.gap(14);
        pdf.heading(Report.title(german).toUpperCase(locale));
        for (String line : Report.lines(c, german)) pdf.text(line, 10, false, Pdf.INK);
        pdf.gap(14);
        List<String[]> entries = Record.entries(c);
        pdf.heading(t(german, "PERMANENT RECORD", "STÄNDIGE AKTE") + " ("
                + (Record.on(c) ? String.valueOf(entries.size()) : t(german, "NOT KEPT", "NICHT GEFÜHRT")) + ")");
        SimpleDateFormat when = new SimpleDateFormat(t(german, "d MMM yyyy HH:mm", "d. MMM yyyy HH:mm"), locale);
        for (String[] e : entries) {
            String time;
            try {
                time = when.format(new Date(Long.parseLong(e[0]))).toUpperCase(locale);
            } catch (NumberFormatException ex) {
                time = "—";
            }
            pdf.text(time + "   " + (german ? e[2] : Lang.en(e[1])), 9, false, Pdf.INK);
        }
        pdf.gap(14);
        pdf.text(t(german, "THE FILE IS COMPLETE. THE FILE IS NEVER COMPLETE.",
                "DIE AKTE IST VOLLSTÄNDIG. DIE AKTE IST NIE VOLLSTÄNDIG."), 9, true, Pdf.RED);
        PdfDocument doc = pdf.finish();

        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, "laibach-case-file-" + Exports.stamp() + ".pdf");
        values.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/" + Exports.FOLDER);
        boolean ok = Exports.write(c, MediaStore.Downloads.EXTERNAL_CONTENT_URI, values, out -> {
            try {
                doc.writeTo(out);
                return true;
            } catch (java.io.IOException e) {
                return false;
            }
        });
        doc.close();
        return ok;
    }

    /** A4 pages filled from the top, a new page when one is full. */
    private static final class Pdf {
        static final int INK = 0xFF111111, RED = 0xFFB3121B, GREY = 0xFF6B6B6B;
        private static final int W = 595, H = 842, MARGIN = 48;
        private final PdfDocument doc = new PdfDocument();
        private final TextPaint paint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        private PdfDocument.Page page;
        private float y;
        private int number;

        Pdf() {
            newPage();
        }

        private void newPage() {
            if (page != null) doc.finishPage(page);
            page = doc.startPage(new PdfDocument.PageInfo.Builder(W, H, ++number).create());
            y = MARGIN;
            Paint foot = new Paint(Paint.ANTI_ALIAS_FLAG);
            foot.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
            foot.setTextSize(7);
            foot.setColor(GREY);
            foot.setLetterSpacing(0.15f);
            page.getCanvas().drawText("LAIBACH LIFE · " + number, MARGIN, H - MARGIN / 2f, foot);
            Paint rule = new Paint();
            rule.setColor(RED);
            page.getCanvas().drawRect(MARGIN, MARGIN / 2f, W - MARGIN, MARGIN / 2f + 3, rule);
        }

        void text(String s, float size, boolean bold, int color) {
            paint.setTypeface(Typeface.create("sans-serif-condensed", bold ? Typeface.BOLD : Typeface.NORMAL));
            paint.setTextSize(size);
            paint.setColor(color);
            paint.setLetterSpacing(0.04f);
            StaticLayout layout = StaticLayout.Builder.obtain(s, 0, s.length(), paint, W - 2 * MARGIN)
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL).setLineSpacing(0, 1.15f).build();
            if (y + layout.getHeight() > H - MARGIN) newPage();
            Canvas canvas = page.getCanvas();
            canvas.save();
            canvas.translate(MARGIN, y);
            layout.draw(canvas);
            canvas.restore();
            y += layout.getHeight() + 2;
        }

        void heading(String s) {
            if (y + 40 > H - MARGIN) newPage();
            Paint rule = new Paint();
            rule.setColor(RED);
            page.getCanvas().drawRect(MARGIN, y, W - MARGIN, y + 1.5f, rule);
            y += 6;
            text(s, 12, true, INK);
            y += 4;
        }

        void gap(float points) {
            y += points;
        }

        PdfDocument finish() {
            doc.finishPage(page);
            page = null;
            return doc;
        }
    }

    private static String t(boolean german, String en, String de) {
        return german ? de : Lang.en(en);
    }
}
