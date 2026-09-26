package com.anilnewsfo.market;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends Activity {
    // This repository is the single source of truth for the Android market feed.
    private static final String FEED_URL =
        "https://raw.githubusercontent.com/tanils/AnilNewandriodapp/main/data/app_feed.json";

    private LinearLayout content;
    private TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUi();
        loadFeed();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24, 20, 24, 12);

        TextView title = new TextView(this);
        title.setText("ANILNEWSFO • MARKET INTELLIGENCE");
        title.setTextSize(26);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        root.addView(title);

        status = new TextView(this);
        status.setText("Loading latest market intelligence…");
        status.setTextSize(13);
        root.addView(status);

        LinearLayout tabs = new LinearLayout(this);
        tabs.setOrientation(LinearLayout.HORIZONTAL);

        Button news = tabButton("📰 NEWS");
        Button fno = tabButton("📊 F&O");
        Button breakout = tabButton("🔥 BREAKOUTS");
        Button refresh = tabButton("↻");

        tabs.addView(news, new LinearLayout.LayoutParams(0, 56, 1));
        tabs.addView(fno, new LinearLayout.LayoutParams(0, 56, 1));
        tabs.addView(breakout, new LinearLayout.LayoutParams(0, 56, 1));
        tabs.addView(refresh, new LinearLayout.LayoutParams(56, 56));

        root.addView(tabs);

        ScrollView scroll = new ScrollView(this);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        news.setOnClickListener(v -> loadSection("news"));
        fno.setOnClickListener(v -> loadSection("fno"));
        breakout.setOnClickListener(v -> loadSection("breakouts"));
        refresh.setOnClickListener(v -> loadFeed());

        setContentView(root);
    }

    private Button tabButton(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(11);
        return b;
    }

    private JSONObject latest;

    private void loadFeed() {
        status.setText("Refreshing latest pipeline output…");
        new Thread(() -> {
            try {
                // Add a cache-buster so the app always asks for the current repository feed.
                String json = fetchUrl(FEED_URL + "?v=" + System.currentTimeMillis());
                if (json == null || json.trim().isEmpty()) {
                    throw new Exception("No app feed is available from the Android repository.");
                }
                latest = new JSONObject(json);
                runOnUiThread(() -> {
                    status.setText("Updated: " + latest.optString("generated_at_utc", "unknown"));
                    loadSection("news");
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    status.setText("Feed unavailable. Tap ↻ to retry.");
                    showMessage("Unable to load latest pipeline data.\n" + e.getMessage());
                });
            }
        }).start();
    }


    private String fetchUrl(String url) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(10000);
        c.setReadTimeout(15000);
        c.setRequestMethod("GET");
        int code = c.getResponseCode();
        if (code < 200 || code >= 300) {
            c.disconnect();
            return null;
        }
        StringBuilder out = new StringBuilder();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream()))) {
            String line;
            while ((line = r.readLine()) != null) out.append(line);
        } finally {
            c.disconnect();
        }
        return out.toString();
    }

    private void clear() {
        content.removeAllViews();
    }

    private void heading(String text) {
        TextView h = new TextView(this);
        h.setText(text);
        h.setTextSize(20);
        h.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        h.setPadding(0, 20, 0, 10);
        content.addView(h);
    }

    private void card(String text) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextSize(15);
        v.setPadding(18, 18, 18, 18);
        v.setBackgroundResource(android.R.drawable.dialog_holo_light_frame);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.setMargins(0, 0, 0, 12);
        content.addView(v, p);
    }

    private void loadSection(String section) {
        clear();
        if (latest == null) {
            showMessage("No feed loaded yet.");
            return;
        }

        if ("news".equals(section)) {
            heading("📰 NEWS / MARKET INTELLIGENCE");
            JSONObject regime = latest.optJSONObject("market_regime");
            if (regime != null) {
                card("MARKET STRUCTURE\n" + regime.optString("label", "DATA_UNAVAILABLE")
                    + "\n\nThis is a description of supplied market structure, not a forecast.");
            }
            JSONArray news = latest.optJSONArray("news");
            if (news == null || news.length() == 0) {
                card("No current/recent news in this pipeline run.");
                return;
            }
            for (int i = 0; i < news.length(); i++) {
                JSONObject n = news.optJSONObject(i);
                if (n == null) continue;
                String symbols = n.optString("symbols", "MARKET");
                card(
                    symbols + "\n\n" +
                    n.optString("headline", "") + "\n\n" +
                    "Type: " + n.optString("news_type", "context") +
                    " | Freshness: " + n.optString("freshness", "unknown") + "\n" +
                    "Source: " + n.optString("source", "unknown") + "\n" +
                    "Published: " + n.optString("published", "unknown") + "\n\n" +
                    n.optString("summary", "")
                );
            }
        } else if ("fno".equals(section)) {
            heading("📊 F&O / OPTIONS");
            JSONArray fno = latest.optJSONArray("fno_candidates");
            if (fno == null || fno.length() == 0) {
                card("No F&O candidates were produced.");
                return;
            }
            for (int i = 0; i < fno.length(); i++) {
                JSONObject x = fno.optJSONObject(i);
                if (x == null) continue;
                boolean ready = x.optBoolean("option_chain_available", false);
                String chain = ready ? "CHAIN READY" : "CHAIN UNAVAILABLE";
                card(
                    "#" + x.optInt("rank") + "  " + x.optString("symbol") + "\n\n" +
                    "Spot: ₹" + x.optString("price", "n/a") +
                    " | Change: " + x.optString("change_pct", "n/a") + "%\n" +
                    "Setup score: " + x.optString("setup_quality_score", "n/a") + "/100\n" +
                    "Status: " + chain + "\n\n" +
                    x.optString("option_summary", "Detailed option-chain data unavailable.")
                );
                if (ready) {
                    renderOptionLegs(x);
                }
            }
            JSONArray analyses = latest.optJSONArray("ai_analyses");
            if (analyses != null && analyses.length() > 0) {
                heading("🤖 AI ANALYSIS");
                for (int i = 0; i < analyses.length(); i++) {
                    JSONObject item = analyses.optJSONObject(i);
                    if (item != null) card(item.optString("provider", "AI") + "\n\n" + item.optString("analysis", ""));
                }
            } else {
                card("AI analysis is currently unavailable. The app will not invent a CE/PE trade without sufficient evidence.");
            }
        } else {
            heading("🔥 TECHNICAL BREAKOUT WATCH");
            JSONArray breakouts = latest.optJSONArray("breakouts");
            if (breakouts == null || breakouts.length() == 0) {
                card("No technical breakout watch candidates in this run.\n\nThe scanner checks previous-day high/low, volume confirmation and EMA structure.");
                return;
            }
            for (int i = 0; i < breakouts.length(); i++) {
                JSONObject b = breakouts.optJSONObject(i);
                if (b == null) continue;
                card(
                    b.optString("symbol", "") + "  |  " + b.optString("status", "WATCH") + "\n\n" +
                    b.optString("pattern", "") + "\n" +
                    "Price: ₹" + b.optString("price", "n/a") +
                    " | Change: " + b.optString("change_pct", "n/a") + "%\n" +
                    "Trigger: ₹" + b.optString("trigger", "n/a") +
                    " | Invalidation: ₹" + b.optString("invalidation", "n/a") + "\n" +
                    "Distance: " + b.optString("distance_pct", "n/a") + "%" +
                    " | Volume: " + b.optString("volume_ratio", "n/a") + "x\n\n" +
                    b.optString("reason", "")
                );
            }
        }
    }

    private void renderOptionLegs(JSONObject x) {
        JSONArray calls = x.optJSONArray("calls");
        JSONArray puts = x.optJSONArray("puts");
        if ((calls == null || calls.length() == 0) && (puts == null || puts.length() == 0)) return;
        StringBuilder s = new StringBuilder("NEAR-SPOT OPTION DATA\n");
        appendLegs(s, "CALLS", calls);
        appendLegs(s, "PUTS", puts);
        card(s.toString());
    }

    private void appendLegs(StringBuilder s, String title, JSONArray legs) {
        if (legs == null) return;
        s.append("\n").append(title).append("\n");
        int limit = Math.min(3, legs.length());
        for (int i = 0; i < limit; i++) {
            JSONObject leg = legs.optJSONObject(i);
            if (leg == null) continue;
            s.append("Strike ").append(leg.optString("strike", "n/a"))
             .append(" | LTP ").append(leg.optString("lastPrice", "n/a"))
             .append(" | OI ").append(leg.optString("openInterest", "n/a"))
             .append(" | IV ").append(leg.optString("impliedVolatility", "n/a"))
             .append("\n");
        }
    }

    private void showMessage(String text) {
        clear();
        card(text);
    }
}
