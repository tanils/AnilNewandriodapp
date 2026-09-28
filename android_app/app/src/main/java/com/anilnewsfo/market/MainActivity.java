package com.anilnewsfo.market;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Typeface;
import android.view.Gravity;
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

        TextView master = new TextView(this);
        String phase = latest == null ? "WAITING FOR FEED" : latest.optString("phase", "UNKNOWN");
        String strategy = latest == null ? "Waiting for pipeline output." : latest.optString("strategy", "Top F&O momentum + catalyst + technical + OI/options analysis.");
        master.setText("🧠 DAILY MASTER ANALYSIS\\nPhase: " + phase.toUpperCase() + "\\nCapital: ₹25,000 • Liquid F&O + NIFTY + BANKNIFTY\\n" + strategy + "\\nNo forced trade • catalyst + price + OI + invalidation required");
        master.setTextSize(13);
        master.setPadding(12, 12, 12, 12);
        master.setBackgroundResource(android.R.drawable.dialog_holo_light_frame);
        content.addView(master);

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
            heading("📰 NEWS");
            JSONArray news = latest.optJSONArray("news");
            card("IMPORTANT MARKET NEWS\n" + (news == null ? "0" : news.length()) +
                " items in this feed\n\nTap any section from Home to switch between intelligence views.");

            if (news == null || news.length() == 0) {
                card("NO NEWS\nNo current/recent news was supplied by the latest pipeline run.");
                return;
            }

            for (int i = 0; i < news.length(); i++) {
                JSONObject n = news.optJSONObject(i);
                if (n == null) continue;
                card(
                    "#" + (i + 1) + "  " + n.optString("symbols", "MARKET/SECTOR") + "\n\n" +
                    n.optString("headline", "Headline unavailable") + "\n\n" +
                    "TYPE: " + n.optString("news_type", "context").toUpperCase() +
                    "   •   FRESHNESS: " + n.optString("freshness", "unknown").toUpperCase() + "\n" +
                    "SOURCE: " + n.optString("source", "unknown") + "\n" +
                    "PUBLISHED: " + n.optString("published", "unknown") + "\n\n" +
                    n.optString("summary", "Summary unavailable.")
                );
            }
        } else if ("impact".equals(section)) {
            heading("⚡ NEWS IMPACT");
            card("CATALYST → MARKET REACTION\nThis screen separates the headline from observable market reaction. Missing market data remains unavailable.");

            JSONArray news = latest.optJSONArray("news");
            if (news == null || news.length() == 0) {
                card("NO IMPACT DATA\nNo news items were supplied.");
                return;
            }

            for (int i = 0; i < Math.min(10, news.length()); i++) {
                JSONObject n = news.optJSONObject(i);
                if (n == null) continue;

                String symbols = n.optString("symbols", "MARKET/SECTOR");
                String type = n.optString("news_type", "context").toUpperCase();
                String freshness = n.optString("freshness", "unknown").toUpperCase();

                card(
                    symbols + "\n\n" +
                    "IMPACT CLASS: " + type + "\n" +
                    "FRESHNESS: " + freshness + "\n\n" +
                    n.optString("headline", "Headline unavailable") + "\n\n" +
                    "OBSERVED REACTION: Not available in this feed run.\n" +
                    "No reaction is inferred when market evidence is missing."
                );
            }
        } else if ("fno".equals(section)) {
            heading("📊 F&O / OPTIONS");
            JSONArray fno = latest.optJSONArray("fno_candidates");

            if (fno == null || fno.length() == 0) {
                card("NO F&O SETUPS\nThe latest pipeline run did not produce F&O candidates.");
                return;
            }

            card("F&O WATCHLIST\n" + fno.length() +
                " candidate(s) ranked by the pipeline. A score is evidence for review, not a trade instruction.");

            if (movers != null && movers.length() > 0) {
                StringBuilder moverText = new StringBuilder("TOP F&O MOVERS\\n");
                for (int i = 0; i < Math.min(30, movers.length()); i++) {
                    JSONObject m = movers.optJSONObject(i);
                    if (m == null) continue;
                    moverText.append("#").append(m.optInt("mover_rank", i + 1)).append(" ")
                        .append(m.optString("symbol", "UNKNOWN")).append("  ")
                        .append(m.optString("direction", "")).append("  ")
                        .append(m.optString("change_pct", "n/a")).append("%\\n");
                }
                card(moverText.toString().trim());
            }

            for (int i = 0; i < fno.length(); i++) {
                JSONObject x = fno.optJSONObject(i);
                if (x == null) continue;

                boolean ready = x.optBoolean("option_chain_available", false);
                String chainStatus = ready ? "CHAIN READY" : "CHAIN UNAVAILABLE";

                card(
                    "RANK #" + x.optInt("rank") + "  •  " + x.optString("symbol", "UNKNOWN") + "\n\n" +
                    "SPOT: ₹" + x.optString("price", "n/a") +
                    "    CHANGE: " + x.optString("change_pct", "n/a") + "%\n" +
                    "SETUP QUALITY: " + x.optString("setup_quality_score", "n/a") + "/100\n" +
                    "OPTION CHAIN: " + chainStatus + "\n\n" +
                    x.optString("option_summary", "Detailed option-chain data unavailable.") +
                    (x.optString("chain_warning", "").isEmpty() ? "" : "\n\nWARNING: " + x.optString("chain_warning"))
                );

                if (ready) renderOptionLegs(x);
            }
        } else if ("breakouts".equals(section)) {
            heading("🔥 BREAKOUT WATCH");
            card("TECHNICAL TRIGGER SCANNER\nChecks supplied price structure, previous-day levels, volume and EMA/RSI evidence. A watch signal is not a guaranteed breakout.");

            JSONArray breakouts = latest.optJSONArray("breakouts");
            if (breakouts == null || breakouts.length() == 0) {
                card("NO ACTIVE BREAKOUTS\nNo candidates met the scanner conditions in the latest pipeline run. This is preferable to inventing a setup.");
                return;
            }

            for (int i = 0; i < breakouts.length(); i++) {
                JSONObject b = breakouts.optJSONObject(i);
                if (b == null) continue;
                card(
                    b.optString("symbol", "UNKNOWN") + "  •  " +
                    b.optString("status", "WATCH").toUpperCase() + "\n\n" +
                    b.optString("pattern", "Pattern unavailable") + "\n\n" +
                    "PRICE: ₹" + b.optString("price", "n/a") +
                    "    CHANGE: " + b.optString("change_pct", "n/a") + "%\n" +
                    "TRIGGER: ₹" + b.optString("trigger", "n/a") +
                    "    INVALIDATION: ₹" + b.optString("invalidation", "n/a") + "\n" +
                    "DISTANCE: " + b.optString("distance_pct", "n/a") + "%" +
                    "    VOLUME: " + b.optString("volume_ratio", "n/a") + "x\n\n" +
                    "WHY: " + b.optString("reason", "Reason unavailable.")
                );
            }
        } else if ("ai".equals(section)) {
            heading("🤖 AI ANALYSIS");
            String aiStatus = latest.optString("ai_status", "unknown");
            JSONArray analyses = latest.optJSONArray("ai_analyses");
            JSONArray models = latest.optJSONArray("available_models");

            card(
                "AI ENGINE STATUS\n" +
                aiStatus.toUpperCase() + "\n\n" +
                "AI output is used as a cross-check against supplied news, technicals and F&O evidence. Missing evidence is never filled with guesses."
            );

            if (models != null && models.length() > 0) {
                StringBuilder modelText = new StringBuilder("AVAILABLE MODELS\n");
                for (int i = 0; i < models.length(); i++) {
                    modelText.append("• ").append(models.optString(i)).append("\n");
                }
                card(modelText.toString().trim());
            }

            if (analyses == null || analyses.length() == 0) {
                card("NO AI ANALYSIS\nNo AI provider returned an analysis for this pipeline run. The app will not manufacture a CE/PE or buy/sell call.");
                return;
            }

            for (int i = 0; i < analyses.length(); i++) {
                JSONObject item = analyses.optJSONObject(i);
                if (item != null) {
                    card(
                        "PROVIDER: " + item.optString("provider", "AI") + "\n\n" +
                        item.optString("analysis", "Analysis unavailable.")
                    );
                }
            }
        } else {
            heading("📡 LIVE MARKET");
            JSONObject regime = latest.optJSONObject("market_regime");

            if (regime == null) {
                card("MARKET STRUCTURE\nUnavailable in the latest feed.");
            } else {
                card(
                    "MARKET STRUCTURE\n" +
                    regime.optString("label", "DATA_UNAVAILABLE") + "\n\n" +
                    "This describes supplied market structure; it is not a forecast."
                );

                JSONObject indices = regime.optJSONObject("indices");
                if (indices != null) {
                    card("INDEX SNAPSHOT\n" + formatObjectLines(indices));
                }
            }

            card(
                "PIPELINE STATUS\n" +
                "Phase: " + latest.optString("phase", "unknown").toUpperCase() + "\n" +
                "Updated: " + latest.optString("generated_at_utc", "unknown") + "\n\n" +
                "Use NEWS for catalysts • F&O for derivatives evidence • BREAKOUTS for technical triggers • AI ANALYSIS for cross-checking."
            );
        }
    }

    private String formatObjectLines(JSONObject obj) {
        if (obj == null) return "Unavailable";
        StringBuilder out = new StringBuilder();
        java.util.Iterator<String> keys = obj.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            Object value = obj.opt(key);
            if (out.length() > 0) out.append("\n");
            out.append(key.replace("_", " ").toUpperCase())
               .append(": ")
               .append(String.valueOf(value));
        }
        return out.toString();
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
