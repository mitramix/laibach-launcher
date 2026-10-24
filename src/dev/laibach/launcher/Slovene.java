package dev.laibach.launcher;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Slovenian versions of the English texts, looked up by the exact English.
 * A text without an entry here stays in English.
 *
 * The table is slovene.txt beside this class, packed into the app as it is:
 * one text a line, the English and the Slovenian split by a tab, with \n for
 * a line break and \\ for a backslash. Lines starting with # are notes, and
 * blank lines are left out. It is read once, the first time Slovenian is asked
 * for.
 */
final class Slovene {
    private static Map<String, String> table;

    private Slovene() {}

    static String of(String en) {
        String sl = table().get(en);
        return sl == null ? en : sl;
    }

    private static synchronized Map<String, String> table() {
        if (table == null) table = read();
        return table;
    }

    private static Map<String, String> read() {
        Map<String, String> out = new HashMap<>(1024);
        try (InputStream in = Slovene.class.getResourceAsStream("slovene.txt")) {
            if (in == null) return out;
            BufferedReader lines = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            for (String line; (line = lines.readLine()) != null; ) {
                int tab = line.indexOf('\t');
                if (line.startsWith("#") || tab < 0) continue;
                out.put(unescape(line.substring(0, tab)), unescape(line.substring(tab + 1)));
            }
        } catch (IOException e) {
            // A damaged table leaves the texts in English.
        }
        return out;
    }

    private static String unescape(String s) {
        if (s.indexOf('\\') < 0) return s;
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char n = s.charAt(++i);
                b.append(n == 'n' ? '\n' : n);
            } else {
                b.append(c);
            }
        }
        return b.toString();
    }
}
