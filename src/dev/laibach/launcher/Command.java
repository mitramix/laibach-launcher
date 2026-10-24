package dev.laibach.launcher;

import android.graphics.Typeface;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * The broadcast ticker and the command line: a programme requested by name,
 * the first match launched on GO. Under the programmes, or in the terminal
 * formation over them.
 */
final class Command extends HomePart {
    private Ticker ticker;
    private EditText search;

    Command(HomeActivity home) {
        super(home);
    }

    void build(LinearLayout root, View monument) {
        Formation formation = home.formation;
        ticker = new Ticker(home, v);
        ticker.setOnClickListener(view -> home.callUps.show());
        if (!formation.has(Formation.TICKER)) ticker.setVisibility(View.GONE);
        root.addView(ticker, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(18)));
        LinearLayout cmd = row();
        cmd.setGravity(Gravity.CENTER_VERTICAL);
        TextView prompt = type(15, v.accent, 25, 1000);
        prompt.setText(t("COMMAND ▶", "BEFEHL ▶"));
        prompt.setLetterSpacing(0.18f);
        cmd.addView(prompt);
        search = new EditText(home);
        search.setTypeface(home.flex);
        search.setFontVariationSettings("'wdth' 70, 'wght' 650");
        search.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        search.setLetterSpacing(0.08f);
        search.setTextColor(v.ink);
        search.setHint(t("REQUEST A PROGRAMME", "PROGRAMM BEANTRAGEN"));
        search.setHintTextColor(v.muted);
        search.setSingleLine(true);
        search.setBackground(null);
        search.setPadding(dp(12), dp(12), 0, dp(10));
        search.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_FILTER
                | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        search.setImeOptions(EditorInfo.IME_ACTION_GO | EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { home.register.applyFilter(); }
        });
        search.setOnEditorActionListener((view, action, event) -> {
            if (!home.register.shown.isEmpty()) home.counter.launch(home.register.shown.get(0), null);
            return true;
        });
        // While a command is typed, the monument steps aside for the list.
        search.setOnFocusChangeListener((view, focused) ->
                monument.setVisibility(focused ? View.GONE : View.VISIBLE));
        cmd.addView(search, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        if (formation.mono) {
            prompt.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
            prompt.setText("$");
            search.setTypeface(Typeface.MONOSPACE);
        }
        if (formation.commandTop) {
            // The terminal takes orders first: under the masthead, above the programmes.
            root.addView(cmd, 3);
            root.addView(rule(v.accent), 4, ruleParams(dp(2), 0, dp(6)));
        } else {
            root.addView(rule(v.accent), ruleParams(dp(2), 0, 0));
            root.addView(cmd);
        }
    }

    /** What is typed, trimmed; empty before the line is drawn. */
    String query() {
        return search == null ? "" : search.getText().toString().trim();
    }

    /** The ticker's message, from the news last received. */
    void broadcast(String message) {
        ticker.setMessage(message);
    }

    /** The line cleared, unfocused and the keyboard put away. */
    void clear() {
        if (search.length() > 0) search.setText("");
        search.clearFocus();
        home.getSystemService(InputMethodManager.class).hideSoftInputFromWindow(search.getWindowToken(), 0);
    }
}
