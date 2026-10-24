package dev.laibach.launcher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The censor's pen for text: cuts a text into words and blacks out what the
 * state prefers unread. Addresses, numbers, names and a short list of
 * unsuitable words go first; the owner lifts or adds bars by hand. Plain Java,
 * nothing leaves the phone.
 */
final class TextCensor {
    private TextCensor() {}

    /** Why a word was blacked out: the owner's hand, or one of the censor's rules. */
    static final int NONE = 0, HAND = 1, CONTACT = 2, NUMBER = 3, NAME = 4, UNSUITABLE = 5;

    /** A word of the text, from start to end, and whether it is blacked out. */
    static final class Word {
        final int start, end;
        final int rule;
        boolean redacted;

        Word(int start, int end, int rule) {
            this.start = start;
            this.end = end;
            this.rule = rule;
            this.redacted = rule != NONE;
        }
    }

    /** Links, addresses, handles, telephone numbers, then words. In that order, so the longer catch wins. */
    private static final Pattern TOKENS = Pattern.compile(
            "(?<contact>(?:https?://|www\\.)\\S+|[\\w.+-]+@[\\w-]+(?:\\.[\\w-]+)+|@\\w{2,}|\\+?\\d[\\d ()./-]{5,}\\d)"
                    + "|(?<word>[\\p{L}\\p{N}][\\p{L}\\p{N}'’-]*)");

    /** Words the state finds unsuitable, in English, German and Slovenian. A stem of five letters or more matches its forms. */
    private static final Set<String> UNSUITABLE_WORDS = new HashSet<>(Arrays.asList(
            "freedom", "free", "truth", "love", "protest", "strike", "resist", "opinion", "vote", "election", "elections",
            "democracy", "secret", "escape", "border", "rights", "union", "dissent", "justice",
            "freiheit", "frei", "wahrheit", "liebe", "streik", "widerstand", "meinung", "wahl", "wahlen",
            "demokratie", "geheim", "flucht", "grenze", "rechte", "gerechtigkeit",
            "svoboda", "svobodo", "svobode", "resnica", "resnico", "ljubezen", "stavka", "upor", "mnenje",
            "volitve", "demokracija", "skrivnost", "pobeg", "meja", "pravice", "pravica"));

    private static final String[] UNSUITABLE_STEMS = {"freedom", "protest", "resist", "democra", "escap",
            "dissent", "freiheit", "wahrheit", "streik", "widerst", "demokrat", "geheim", "svobod", "resnic",
            "ljubez", "stavk", "demokra", "skrivn"};

    /** Cuts text into words and applies the censor's rules. */
    static List<Word> read(String text) {
        List<Word> words = new ArrayList<>();
        List<Boolean> capitalised = new ArrayList<>();
        int midSentence = 0, capitalMid = 0;
        Matcher m = TOKENS.matcher(text);
        while (m.find()) {
            if (m.group("contact") != null) {
                words.add(new Word(m.start(), m.end(), CONTACT));
                capitalised.add(false);
                continue;
            }
            String w = m.group("word");
            int rule = NONE;
            if (w.chars().anyMatch(Character::isDigit)) rule = NUMBER;
            else if (unsuitable(w)) rule = UNSUITABLE;
            boolean starts = sentenceStart(text, m.start());
            boolean capital = !starts && w.length() > 1 && Character.isUpperCase(w.charAt(0))
                    && !w.equals(w.toUpperCase(Locale.ROOT));
            if (!starts) {
                midSentence++;
                if (capital) capitalMid++;
            }
            words.add(new Word(m.start(), m.end(), rule));
            capitalised.add(capital && rule == NONE);
        }
        // Names: capitalised words inside a sentence. Not in a language that capitalises every
        // noun, where the rule would black out half the text.
        if (capitalMid * 4 <= midSentence) {
            for (int i = 0; i < words.size(); i++) {
                if (capitalised.get(i)) {
                    Word w = words.get(i);
                    words.set(i, new Word(w.start, w.end, NAME));
                }
            }
        }
        return words;
    }

    private static boolean unsuitable(String word) {
        String w = word.toLowerCase(Locale.ROOT);
        if (UNSUITABLE_WORDS.contains(w)) return true;
        for (String stem : UNSUITABLE_STEMS) if (w.startsWith(stem)) return true;
        return false;
    }

    /** Whether the word at i opens the text, a line or a sentence. */
    private static boolean sentenceStart(String text, int i) {
        for (int j = i - 1; j >= 0; j--) {
            char c = text.charAt(j);
            if (c == '\n') return true;
            if (Character.isWhitespace(c) || c == '"' || c == '“' || c == '„' || c == '(' || c == '\'' || c == '‘') continue;
            return c == '.' || c == '!' || c == '?' || c == ':' || c == '…';
        }
        return true;
    }

    /** The word at offset in the text, or null. */
    static Word at(List<Word> words, int offset) {
        for (Word w : words) if (offset >= w.start && offset < w.end) return w;
        return null;
    }

    static int count(List<Word> words) {
        int n = 0;
        for (Word w : words) if (w.redacted) n++;
        return n;
    }

    /** The text with every blacked-out word replaced by a bar as long as the word. */
    static String redact(String text, List<Word> words) {
        StringBuilder out = new StringBuilder(text);
        for (Word w : words) {
            if (!w.redacted) continue;
            for (int i = w.start; i < w.end; i++) {
                char c = text.charAt(i);
                // Bars over letters; a space inside a telephone number or link stays a space.
                if (!Character.isWhitespace(c)) out.setCharAt(i, '█');
            }
        }
        return out.toString();
    }
}
