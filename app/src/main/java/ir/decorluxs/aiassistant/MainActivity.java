package ir.decorluxs.aiassistant;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private final ExecutorService pool = Executors.newFixedThreadPool(3);
    private LinearLayout chatContainer;
    private ScrollView chatScroll;
    private EditText input;
    private TextView status;
    private Button sendButton;
    private SharedPreferences prefs;
    private ActionExecutor actionExecutor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences("decorluxs_ai", MODE_PRIVATE);
        actionExecutor = new ActionExecutor(this);
        chatContainer = findViewById(R.id.chatContainer);
        chatScroll = findViewById(R.id.chatScroll);
        input = findViewById(R.id.messageInput);
        status = findViewById(R.id.statusText);
        sendButton = findViewById(R.id.sendButton);

        sendButton.setOnClickListener(v -> sendMessage());
        input.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage();
                return true;
            }
            return false;
        });

        findViewById(R.id.openWhatsAppButton).setOnClickListener(v ->
                actionExecutor.openPackage("com.whatsapp.w4b", "https://www.whatsapp.com/business/"));
        findViewById(R.id.openInstagramButton).setOnClickListener(v ->
                actionExecutor.openPackage("com.instagram.android", "https://www.instagram.com/decorluxs/"));
        findViewById(R.id.shareButton).setOnClickListener(v -> {
            String text = input.getText().toString().trim();
            actionExecutor.shareText(text.isEmpty() ? "DECORLUXS" : text);
        });
        findViewById(R.id.settingsButton).setOnClickListener(v -> showBackendDialog());

        addMessage("DECORLUXS AI", "سلام 👋 من دستیار هوشمند کسب‌وکار دکورلوکس هستم. برای پاسخ مشتری، کپشن، ایده محتوا و کارهای روزمره کنارت هستم.", false);
        checkBackend();
    }

    private String backendUrl() {
        return prefs.getString("backend_url", "https://decorlux-backend-production.up.railway.app");
    }

    private void checkBackend() {
        status.setText("● در حال بررسی اتصال امن…");
        status.setTextColor(Color.parseColor("#A9A9A9"));
        pool.submit(() -> {
            try {
                new ApiClient(backendUrl()).health();
                runOnUiThread(() -> {
                    status.setText("● سرور امن متصل است");
                    status.setTextColor(Color.parseColor("#64D39B"));
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    status.setText("● اتصال سرور برقرار نیست");
                    status.setTextColor(Color.parseColor("#FF737A"));
                });
            }
        });
    }

    private void sendMessage() {
        String message = input.getText().toString().trim();
        if (message.isEmpty() || !sendButton.isEnabled()) return;

        input.setText("");
        setSending(true);
        addMessage("شما", message, true);
        addMessage("DECORLUXS AI", "در حال فکر کردن…", false);

        pool.submit(() -> {
            try {
                JSONObject response = new ApiClient(backendUrl()).chat(message);
                String reply = response.optString("reply", "پاسخی دریافت نشد.");
                JSONObject action = response.optJSONObject("action");
                runOnUiThread(() -> {
                    replaceLastAssistant(reply);
                    setSending(false);
                    if (action != null && action.optBoolean("requiresApproval", true)) {
                        showApproval(action);
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    replaceLastAssistant("درخواست انجام نشد. اتصال اینترنت یا سرور را بررسی کن و دوباره امتحان کن.");
                    setSending(false);
                });
            }
        });
    }

    private void setSending(boolean sending) {
        sendButton.setEnabled(!sending);
        sendButton.setText(sending ? "…" : "ارسال");
        input.setEnabled(!sending);
    }

    private void showApproval(JSONObject action) {
        String type = action.optString("type", "ACTION");
        String description = action.optString("description", "عملیات درخواستی");
        new AlertDialog.Builder(this)
                .setTitle("تأیید عملیات")
                .setMessage(description + "\n\nنوع: " + type + "\n\nاین عملیات بدون تأیید شما اجرا نمی‌شود.")
                .setNegativeButton("رد", null)
                .setPositiveButton("تأیید", (d, w) -> executeApprovedAction(action))
                .show();
    }

    private void executeApprovedAction(JSONObject action) {
        String type = action.optString("type", "");
        String value = action.optString("value", "");
        switch (type) {
            case "OPEN_URL": actionExecutor.openUrl(value); break;
            case "SHARE_TEXT": actionExecutor.shareText(value); break;
            case "OPEN_WHATSAPP": actionExecutor.openPackage("com.whatsapp.w4b", "https://www.whatsapp.com/business/"); break;
            case "OPEN_INSTAGRAM": actionExecutor.openPackage("com.instagram.android", "https://www.instagram.com/decorluxs/"); break;
            case "OPEN_SETTINGS": actionExecutor.openSettings(); break;
            default: addMessage("سیستم", "این عملیات هنوز به اجراکننده امن متصل نشده است.", false);
        }
    }

    private void showBackendDialog() {
        EditText box = new EditText(this);
        box.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        box.setText(backendUrl());
        box.setSelectAllOnFocus(true);
        int p = dp(16);
        box.setPadding(p, p, p, p);

        new AlertDialog.Builder(this)
                .setTitle("تنظیمات سرور")
                .setMessage("آدرس سرور امن DecorLux را وارد کن. کلیدهای سرویس‌های هوش مصنوعی و شبکه‌های اجتماعی فقط روی سرور نگهداری می‌شوند.")
                .setView(box)
                .setNegativeButton("لغو", null)
                .setPositiveButton("ذخیره", (d, w) -> {
                    prefs.edit().putString("backend_url", box.getText().toString().trim()).apply();
                    checkBackend();
                })
                .show();
    }

    private void addMessage(String who, String text, boolean user) {
        TextView v = new TextView(this);
        v.setText(formatMessage(who, text, user));
        v.setTextSize(15);
        v.setLineSpacing(0f, 1.12f);
        v.setGravity(Gravity.RIGHT);
        v.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG_RTL);
        v.setMaxWidth((int) (getResources().getDisplayMetrics().widthPixels * 0.88f));

        int horizontal = dp(14);
        int vertical = dp(11);
        v.setPadding(horizontal, vertical, horizontal, vertical);

        GradientDrawable bubble = new GradientDrawable();
        bubble.setShape(GradientDrawable.RECTANGLE);
        bubble.setColor(Color.parseColor(user ? "#242424" : "#111111"));
        bubble.setCornerRadius(dp(18));
        bubble.setStroke(dp(1), Color.parseColor(user ? "#373737" : "#66532E"));
        v.setBackground(bubble);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.gravity = user ? Gravity.END : Gravity.START;
        lp.setMargins(0, 0, 0, dp(9));
        v.setLayoutParams(lp);

        chatContainer.addView(v);
        chatScroll.post(() -> chatScroll.fullScroll(View.FOCUS_DOWN));
    }

    private SpannableStringBuilder formatMessage(String who, String text, boolean user) {
        SpannableStringBuilder out = new SpannableStringBuilder();
        int labelStart = 0;
        out.append(who);
        int labelEnd = out.length();
        out.setSpan(
                new ForegroundColorSpan(Color.parseColor(user ? "#D9D9D9" : "#D6B05E")),
                labelStart,
                labelEnd,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );
        out.setSpan(
                new StyleSpan(Typeface.BOLD),
                labelStart,
                labelEnd,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );
        out.append("\n");
        int bodyStart = out.length();
        out.append(text);
        out.setSpan(
                new ForegroundColorSpan(Color.parseColor("#F5F1E8")),
                bodyStart,
                out.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );
        return out;
    }

    private void replaceLastAssistant(String text) {
        int count = chatContainer.getChildCount();
        if (count == 0) return;
        View last = chatContainer.getChildAt(count - 1);
        if (last instanceof TextView) {
            ((TextView) last).setText(formatMessage("DECORLUXS AI", text, false));
        }
        chatScroll.post(() -> chatScroll.fullScroll(View.FOCUS_DOWN));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDestroy() {
        pool.shutdownNow();
        super.onDestroy();
    }
}
