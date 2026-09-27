package com.dellybizz.notificationvault;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Space;
import android.widget.TextView;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends Activity {
    private static final int BG = Color.rgb(246, 246, 243);
    private static final int CARD = Color.WHITE;
    private static final int TEXT = Color.rgb(24, 24, 24);
    private static final int MUTED = Color.rgb(105, 105, 100);
    private static final int BORDER = Color.rgb(224, 224, 218);
    private static final int GOOD_BG = Color.rgb(235, 246, 231);
    private static final int GOOD_TEXT = Color.rgb(54, 92, 48);
    private static final int WARN_BG = Color.rgb(250, 239, 222);
    private static final int WARN_TEXT = Color.rgb(127, 79, 20);
    private static final int BAD_BG = Color.rgb(255, 236, 234);
    private static final int BAD_TEXT = Color.rgb(141, 45, 37);

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private TokenVault tokenVault;
    private ApiClient apiClient;

    private LinearLayout page;
    private EditText tokenInput;
    private Button connectButton;
    private Button refreshButton;
    private LinearLayout profileArea;
    private TextView connectionStatus;
    private TextView helperText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        tokenVault = new TokenVault(this);
        apiClient = new ApiClient(BuildConfig.API_BASE_URL);
        buildScreen();

        if (tokenVault.hasToken()) {
            showEnrolledState();
            refreshProfile(false);
        } else {
            showEnrollmentState();
        }
    }

    @Override
    protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }

    private void buildScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);

        page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(20), dp(28), dp(20), dp(48));
        scroll.addView(page, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT
        ));

        TextView eyebrow = label("NOTIFICATION VAULT", 11, MUTED, true);
        eyebrow.setLetterSpacing(0.12f);
        page.addView(eyebrow);

        TextView title = label("Device client", 32, TEXT, true);
        title.setPadding(0, dp(4), 0, 0);
        page.addView(title);

        TextView subtitle = label(
                "Connect this phone or Chromebook to the admin-controlled notification archive.",
                15,
                MUTED,
                false
        );
        subtitle.setPadding(0, dp(8), 0, dp(22));
        page.addView(subtitle);

        LinearLayout connectionCard = card();
        TextView connectionTitle = label("Device enrollment", 18, TEXT, true);
        connectionCard.addView(connectionTitle);

        connectionStatus = chip("Not connected", BAD_BG, BAD_TEXT);
        LinearLayout.LayoutParams statusParams = wrapParams();
        statusParams.topMargin = dp(10);
        connectionCard.addView(connectionStatus, statusParams);

        helperText = label(
                "Create this device in the admin dashboard, then paste the one-time device token here.",
                13,
                MUTED,
                false
        );
        helperText.setPadding(0, dp(12), 0, dp(12));
        connectionCard.addView(helperText);

        tokenInput = new EditText(this);
        tokenInput.setHint("nv1.… device token");
        tokenInput.setSingleLine(true);
        tokenInput.setTextSize(14);
        tokenInput.setTextColor(TEXT);
        tokenInput.setHintTextColor(Color.rgb(150, 150, 145));
        tokenInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        tokenInput.setPadding(dp(12), dp(12), dp(12), dp(12));
        tokenInput.setBackground(rounded(Color.rgb(250, 250, 248), BORDER, 10));
        connectionCard.addView(tokenInput, matchWrapParams());

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        actions.setPadding(0, dp(10), 0, 0);

        connectButton = button("Connect device", true);
        connectButton.setOnClickListener(view -> connectDevice());
        actions.addView(connectButton, new LinearLayout.LayoutParams(0, dp(46), 1f));

        Space gap = new Space(this);
        actions.addView(gap, new LinearLayout.LayoutParams(dp(8), 1));

        refreshButton = button("Refresh", false);
        refreshButton.setOnClickListener(view -> refreshProfile(true));
        actions.addView(refreshButton, new LinearLayout.LayoutParams(dp(116), dp(46)));

        connectionCard.addView(actions);
        page.addView(connectionCard, matchWrapMarginBottom(dp(14)));

        profileArea = new LinearLayout(this);
        profileArea.setOrientation(LinearLayout.VERTICAL);
        page.addView(profileArea, matchWrapMarginBottom(dp(14)));

        LinearLayout settingsCard = card();
        settingsCard.addView(label("Settings", 18, TEXT, true));

        TextView endpoint = label("Server\n" + BuildConfig.API_BASE_URL, 13, MUTED, false);
        endpoint.setPadding(0, dp(12), 0, dp(12));
        settingsCard.addView(endpoint);

        Button adminButton = button("Open admin dashboard", false);
        adminButton.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(BuildConfig.API_BASE_URL));
            startActivity(intent);
        });
        settingsCard.addView(adminButton, matchWrapMarginBottom(dp(8)));

        Button clearButton = button("Forget token on this device", false);
        clearButton.setTextColor(BAD_TEXT);
        clearButton.setOnClickListener(view -> {
            tokenVault.clear();
            tokenInput.setText("");
            profileArea.removeAllViews();
            showEnrollmentState();
        });
        settingsCard.addView(clearButton);

        page.addView(settingsCard);
        setContentView(scroll);
    }

    private void connectDevice() {
        String token = tokenInput.getText().toString().trim();
        if (!looksLikeToken(token)) {
            showMessage("That does not look like a Notification Vault device token.", true);
            return;
        }

        setBusy(true);
        helperText.setText("Checking the token with the server…");
        executor.execute(() -> {
            try {
                DeviceProfile profile = apiClient.fetchProfile(token);
                tokenVault.save(token);
                runOnUiThread(() -> {
                    tokenInput.setText("");
                    showEnrolledState();
                    renderProfile(profile);
                    showMessage("Connected securely. Server permissions loaded.", false);
                    setBusy(false);
                });
            } catch (Exception exception) {
                runOnUiThread(() -> {
                    showMessage(safeError(exception), true);
                    helperText.setText("The token was not stored. Check it and try again.");
                    setBusy(false);
                });
            }
        });
    }

    private void refreshProfile(boolean userInitiated) {
        setBusy(true);
        if (userInitiated) helperText.setText("Refreshing permissions from the server…");
        executor.execute(() -> {
            try {
                String token = tokenVault.load();
                if (token == null || token.isEmpty()) throw new IllegalStateException("No saved token.");
                DeviceProfile profile = apiClient.fetchProfile(token);
                runOnUiThread(() -> {
                    showEnrolledState();
                    renderProfile(profile);
                    if (userInitiated) showMessage("Permissions refreshed.", false);
                    setBusy(false);
                });
            } catch (ApiClient.ApiException exception) {
                runOnUiThread(() -> {
                    if (exception.statusCode == 401) {
                        tokenVault.clear();
                        profileArea.removeAllViews();
                        showEnrollmentState();
                        showMessage("This device token was revoked or is no longer valid.", true);
                    } else {
                        showMessage(safeError(exception), true);
                    }
                    setBusy(false);
                });
            } catch (Exception exception) {
                runOnUiThread(() -> {
                    showMessage(safeError(exception), true);
                    setBusy(false);
                });
            }
        });
    }

    private void renderProfile(DeviceProfile profile) {
        profileArea.removeAllViews();
        boolean wide = getResources().getConfiguration().smallestScreenWidthDp >= 600;

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(wide ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);

        LinearLayout identity = card();
        identity.addView(label("This device", 16, TEXT, true));
        TextView name = label(profile.name, 22, TEXT, true);
        name.setPadding(0, dp(14), 0, dp(4));
        identity.addView(name);
        identity.addView(label(capitalize(profile.type), 13, MUTED, false));

        LinearLayout record = card();
        record.addView(label("Record permission", 16, TEXT, true));
        TextView recordChip = profile.canRecord
                ? chip("Allowed", GOOD_BG, GOOD_TEXT)
                : chip("Blocked by admin", BAD_BG, BAD_TEXT);
        LinearLayout.LayoutParams chipParams = wrapParams();
        chipParams.topMargin = dp(14);
        record.addView(recordChip, chipParams);
        TextView recordHelp = label(
                profile.canRecord
                        ? "Phase 2 will allow this device to upload notifications."
                        : "The server will reject notification uploads from this device.",
                13,
                MUTED,
                false
        );
        recordHelp.setPadding(0, dp(10), 0, 0);
        record.addView(recordHelp);

        if (wide) {
            LinearLayout.LayoutParams left = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            left.setMarginEnd(dp(7));
            row.addView(identity, left);
            LinearLayout.LayoutParams right = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            right.setMarginStart(dp(7));
            row.addView(record, right);
        } else {
            row.addView(identity, matchWrapMarginBottom(dp(12)));
            row.addView(record, matchWrapParams());
        }
        profileArea.addView(row, matchWrapMarginBottom(dp(14)));

        LinearLayout viewCard = card();
        viewCard.addView(label("View permission", 16, TEXT, true));
        if (profile.viewSources.isEmpty()) {
            TextView none = chip("No notification sources allowed", WARN_BG, WARN_TEXT);
            LinearLayout.LayoutParams noneParams = wrapParams();
            noneParams.topMargin = dp(12);
            viewCard.addView(none, noneParams);
            TextView explanation = label(
                    "This device may not view notification history until the admin grants at least one source device.",
                    13,
                    MUTED,
                    false
            );
            explanation.setPadding(0, dp(10), 0, 0);
            viewCard.addView(explanation);
        } else {
            TextView heading = label("May view notifications recorded by:", 13, MUTED, false);
            heading.setPadding(0, dp(12), 0, dp(4));
            viewCard.addView(heading);
            for (DeviceProfile.ViewSource source : profile.viewSources) {
                TextView sourceView = label("•  " + source.name + "  ·  " + capitalize(source.type), 14, TEXT, false);
                sourceView.setPadding(0, dp(7), 0, dp(7));
                viewCard.addView(sourceView);
            }
        }
        profileArea.addView(viewCard);
    }

    private void showEnrollmentState() {
        connectionStatus.setText("Not connected");
        connectionStatus.setTextColor(BAD_TEXT);
        connectionStatus.setBackground(rounded(BAD_BG, BAD_BG, 999));
        helperText.setText("Create this device in the admin dashboard, then paste the one-time device token here.");
        tokenInput.setVisibility(View.VISIBLE);
        connectButton.setVisibility(View.VISIBLE);
        refreshButton.setVisibility(View.GONE);
    }

    private void showEnrolledState() {
        connectionStatus.setText("Connected");
        connectionStatus.setTextColor(GOOD_TEXT);
        connectionStatus.setBackground(rounded(GOOD_BG, GOOD_BG, 999));
        helperText.setText("The credential is encrypted with Android Keystore. Refresh to fetch current admin permissions.");
        tokenInput.setVisibility(View.GONE);
        connectButton.setVisibility(View.GONE);
        refreshButton.setVisibility(View.VISIBLE);
    }

    private void showMessage(String text, boolean error) {
        helperText.setText(text);
        helperText.setTextColor(error ? BAD_TEXT : GOOD_TEXT);
        helperText.postDelayed(() -> helperText.setTextColor(MUTED), 3500);
    }

    private void setBusy(boolean busy) {
        connectButton.setEnabled(!busy);
        refreshButton.setEnabled(!busy);
        tokenInput.setEnabled(!busy);
    }

    private boolean looksLikeToken(String token) {
        return token.startsWith("nv1.") && token.split("\\.").length == 3 && token.length() > 40;
    }

    private String safeError(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.trim().isEmpty()) return "Something went wrong. Try again.";
        return message;
    }

    private LinearLayout card() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(18), dp(18), dp(18), dp(18));
        layout.setBackground(rounded(CARD, BORDER, 16));
        return layout;
    }

    private TextView label(String text, int sp, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(sp);
        view.setTextColor(color);
        view.setLineSpacing(0f, 1.12f);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private TextView chip(String text, int backgroundColor, int textColor) {
        TextView view = label(text, 12, textColor, true);
        view.setPadding(dp(10), dp(6), dp(10), dp(6));
        view.setBackground(rounded(backgroundColor, backgroundColor, 999));
        return view;
    }

    private Button button(String text, boolean primary) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(13);
        button.setAllCaps(false);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setTextColor(primary ? Color.WHITE : TEXT);
        button.setBackground(rounded(primary ? TEXT : CARD, primary ? TEXT : BORDER, 10));
        button.setPadding(dp(14), 0, dp(14), 0);
        return button;
    }

    private GradientDrawable rounded(int fill, int stroke, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(dp(radiusDp));
        drawable.setStroke(dp(1), stroke);
        return drawable;
    }

    private LinearLayout.LayoutParams matchWrapParams() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
    }

    private LinearLayout.LayoutParams matchWrapMarginBottom(int margin) {
        LinearLayout.LayoutParams params = matchWrapParams();
        params.bottomMargin = margin;
        return params;
    }

    private LinearLayout.LayoutParams wrapParams() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private String capitalize(String value) {
        if (value == null || value.isEmpty()) return "Other";
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }
}
