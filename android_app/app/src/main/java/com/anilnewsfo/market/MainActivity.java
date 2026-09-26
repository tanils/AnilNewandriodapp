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
    private boolean dashboardVisible = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUi();
        loadFeed();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(20, 18, 20, 10);

        TextView title = new TextView(this);
        title.setText("ANILNEWSFO");
        title.setTextSize(28);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Market Intelligence • News • F&O • AI");
        subtitle.setTextSize(13);
        root.addView(subtitle);

        status = new TextView(this);
        status.setText("Loading latest market intelligence…");
        status.setTextSize(12);
        status.setPadding(0, 6, 0, 10);
        root.addView(status);

        ScrollView scroll = new ScrollView(this);
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        content = body;

        renderDashboard();

        scroll.addView(body);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
    }

    private void renderDashboard() {
        dashboardVisible = true;
        content.removeAllViews();

        LinearLayout liveRow = new LinearLayout(this);
        liveRow.setOrientation(LinearLayout.HORIZONTAL);
        liveRow.addView(dashboardCard("📡", "LIVE MARKET", "Market structure & session status", "live"),
                new LinearLayout.LayoutParams(0, 112, 1));
        liveRow.addView(dashboardCard("📰", "NEWS", "Important market-moving news", "news"),
                new LinearLayout.LayoutParams(0, 112, 1));
        content.addView(liveRow);

        LinearLayout fnoRow = new LinearLayout(this);
        fnoRow.setOrientation(LinearLayout.HORIZONTAL);
        fnoRow.addView(dashboardCard("📊", "F&O", "OI • PCR • options • setups", "fno"),
                new LinearLayout.LayoutParams(0, 112, 1));
        fnoRow.addView(dashboardCard("🔥", "BREAKOUTS", "Technical trigger watch", "breakouts"),
                new LinearLayout.LayoutParams(0, 112, 1));
        content.addView(fnoRow);

        LinearLayout aiRow = new LinearLayout(this);
        aiRow.setOrientation(LinearLayout.HORIZONTAL);
        aiRow.addView(dashboardCard("🤖", "AI ANALYSIS", "Cross-check & trade evidence", "ai"),
                new LinearLayout.LayoutParams(0, 112, 1));
        aiRow.addView(dashboardCard("⚡", "NEWS IMPACT", "Reaction vs headline", "impact"),
                new LinearLayout.LayoutParams(0, 112, 1));
        content.addView(aiRow);

        Button refresh = new Button(this);
        refresh.setText("↻  REFRESH LIVE FEED");
        refresh.setTextSize(13);
        refresh.setOnClickListener(v -> loadFeed());
        LinearLayout.LayoutParams refreshParams = new LinearLayout.LayoutParams(-1, 56);
        refreshParams.setMargins(0, 10, 0, 8);
        content.addView(refresh, refreshParams);
    }

    private Button dashboardCard(String icon, String title, String subtitle, String section) {
        Button b = new Button(this);
        b.setAllCaps(false);
        b.setText(icon + "  " + title + "\n" + subtitle);
        b.setTextSize(12);
        b.setGravity(Gravity.CENTER);
        b.setOnClickListener(v -> loadSection(section));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, 112, 1);
        p.setMargins(5, 5, 5, 5);
        b.setLayoutParams(p);
        return b;
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
                    renderDashboard();
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
        dashboardVisible = false;
        clear();

        LinearLayout sectionHeader = new LinearLayout(this);
        sectionHeader.setOrientation(LinearLayout.HORIZONTAL);
        sectionHeader.setGravity(Gravity.CENTER_VERTICAL);
        sectionHeader.setPadding(0, 4, 0, 8);

        Button back = new Button(this);
        back.setText("← HOME");
        back.setTextSize(14);
        back.setAllCaps(false);
        back.setOnClickListener(v -> renderDashboard());
        sectionHeader.addView(back, new LinearLayout.LayoutParams(120, 56));

        TextView selected = new TextView(this);
        selected.setText(sectionTitle(section));
        selected.setTextSize(18);
        selected.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        selected.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams selectedParams = new LinearLayout.LayoutParams(0, 56, 1);
        selectedParams.setMargins(8, 0, 4, 0);
        sectionHeader.addView(selected, selectedParams);

        content.addView(sectionHeader);

        if (latest == null) {
            showMessage("No feed loaded yet.");
            return;
        }

        if ("news".equals(section)) {
            heading("📰 NEWS / MARKET INTELLIGENCE");
            JSONArray news = latest.optJSONArray("news");
            if (news == null || news.length() == 0) {
                card("No current/recent news in this pipeline run.");
                return;
            }
            for (int i = 0; i < news.length(); i++) {
                JSONObject n = news.optJSONObject(i);
                if (n == null) continue;
                card(newsCardText(n));
            }
        } else if ("impact".equals(section)) {
            heading("⚡ NEWS IMPACT");
            card("How the headline is behaving in the market. This is evidence, not a prediction.");
            JSONArray news = latest.optJSONArray("news");
            if (news == null || news.length() == 0) {
                card("No news-impact data available.");
                return;
            }
            for (int i = 0; i < Math.min(10, news.length()); i++) {
                JSONObject n = news.optJSONObject(i);
                if (n == null) continue;
                String symbols = n.optString("symbols", "MARKET/SECTOR");
                String impact = n.optString("news_type", "context").toUpperCase();
                String freshness = n.optString("freshness", "unknown");
                card(symbols + "\n\nIMPACT CLASS: " + impact +
                    "\nFRESHNESS: " + freshness +
                    "\n\n" + n.optString("headline", "") +
                    "\n\nMarket reaction is shown only where market data is available; no reaction is invented.");
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
                card(
                    "#" + x.optInt("rank") + "  " + x.optString("symbol") + "\n\n" +
                    "Spot: ₹" + x.optString("price", "n/a") +
                    " | Change: " + x.optString("change_pct", "n/a") + "%\n" +
                    "Setup score: " + x.optString("setup_quality_score", "n/a") + "/100\n" +
                    "Status: " + (ready ? "CHAIN READY" : "CHAIN UNAVAILABLE") + "\n\n" +
                    x.optString("option_summary", "Detailed option-chain data unavailable.")
                );
                if (ready) renderOptionLegs(x);
            }
        } else if ("breakouts".equals(section)) {
            heading("🔥 TECHNICAL BREAKOUT WATCH");
            JSONArray breakouts = latest.optJSONArray("breakouts");
            if (breakouts == null || breakouts.length() == 0) {
                card("No technical breakout watch candidates in this run.");
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
        } else if ("ai".equals(section)) {
            heading("🤖 AI ANALYSIS");
            JSONArray analyses = latest.optJSONArray("ai_analyses");
            String aiStatus = latest.optString("ai_status", "unknown");
            card("AI STATUS: " + aiStatus.toUpperCase() +
                "\n\nAI output is cross-checked against supplied news, technicals and option-chain evidence. Missing evidence stays unavailable.");
            if (analyses == null || analyses.length() == 0) {
                card("No AI provider returned an analysis for this pipeline run.");
                return;
            }
            for (int i = 0; i < analyses.length(); i++) {
                JSONObject item = analyses.optJSONObject(i);
                if (item != null) card(item.optString("provider", "AI") + "\n\n" + item.optString("analysis", ""));
            }
        } else {
            heading("📡 LIVE MARKET INTELLIGENCE");
            JSONObject regime = latest.optJSONObject("market_regime");
            if (regime != null) {
                card("MARKET STRUCTURE\n" + regime.optString("label", "DATA_UNAVAILABLE"));
                JSONObject indices = regime.optJSONObject("indices");
                if (indices != null) {
                    card("NIFTY / BANK NIFTY\n" + indices.toString().replace("{", "").replace("}", "").replace(",", "\n"));
                }
            }
            card("Last pipeline update\n" + latest.optString("generated_at_utc", "unknown") +
                "\n\nPhase: " + latest.optString("phase", "unknown") +
                "\n\nUse NEWS for catalysts, F&O for option evidence, BREAKOUTS for technical triggers and AI ANALYSIS for cross-checking.");
        }
    }

    private String sectionTitle(String section) {
        if ("news".equals(section)) return "📰 NEWS";
        if ("fno".equals(section)) return "📊 F&O";
        if ("breakouts".equals(section)) return "🔥 BREAKOUTS";
        if ("ai".equals(section)) return "🤖 AI ANALYSIS";
        if ("impact".equals(section)) return "⚡ NEWS IMPACT";
        return "📡 LIVE MARKET";
    }

    private String newsCardText(JSONObject n) {
        return n.optString("symbols", "MARKET/SECTOR") + "\n\n" +
            n.optString("headline", "") + "\n\n" +
            "Type: " + n.optString("news_type", "context") +
            " | Freshness: " + n.optString("freshness", "unknown") + "\n" +
            "Source: " + n.optString("source", "unknown") + "\n" +
            "Published: " + n.optString("published", "unknown") + "\n\n" +
            n.optString("summary", "");
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

    @Override
    public void onBackPressed() {
        if (!dashboardVisible) {
            renderDashboard();
            return;
        }
        super.onBackPressed();
    }

    private void showMessage(String text) {
        clear();
        card(text);
    }
}
