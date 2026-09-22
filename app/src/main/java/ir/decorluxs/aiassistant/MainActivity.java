package ir.decorluxs.aiassistant;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
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

        Button send = findViewById(R.id.sendButton);
        send.setOnClickListener(v -> sendMessage());

        findViewById(R.id.openWhatsAppButton).setOnClickListener(v ->
                actionExecutor.openPackage("com.whatsapp.w4b", "https://www.whatsapp.com/business/"));
        findViewById(R.id.openInstagramButton).setOnClickListener(v ->
                actionExecutor.openPackage("com.instagram.android", "https://www.instagram.com/decorluxs/"));
        findViewById(R.id.shareButton).setOnClickListener(v -> {
            String text = input.getText().toString().trim();
            actionExecutor.shareText(text.isEmpty() ? "DECORLUXS" : text);
        });
        findViewById(R.id.settingsButton).setOnClickListener(v -> showBackendDialog());

        addMessage("DECORLUXS AI", "سلام 👋 من دستیار امن کسب‌وکار تو هستم. می‌تونی برای کپشن، پاسخ مشتری، ایده محتوا و کارهای روزمره ازم کمک بگیری.", false);
        checkBackend();
    }

    private String backendUrl() {
        return prefs.getString("backend_url", "http://10.0.2.2:8787");
    }

    private void checkBackend() {
        status.setText("Checking secure backend…");
        pool.submit(() -> {
            try {
                String health = new ApiClient(backendUrl()).health();
                runOnUiThread(() -> {
                    status.setText("Secure backend: " + health);
                    status.setTextColor(Color.parseColor("#6ED6A0"));
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    status.setText("Backend offline — tap Settings to set URL");
                    status.setTextColor(Color.parseColor("#FF7C7C"));
                });
            }
        });
    }

    private void sendMessage() {
        String message = input.getText().toString().trim();
        if (message.isEmpty()) return;
        input.setText("");
        addMessage("You", message, true);
        addMessage("DECORLUXS AI", "…", false);

        pool.submit(() -> {
            try {
                JSONObject response = new ApiClient(backendUrl()).chat(message);
                String reply = response.optString("reply", "No response");
                JSONObject action = response.optJSONObject("action");
                runOnUiThread(() -> {
                    replaceLastAssistant(reply);
                    if (action != null && action.optBoolean("requiresApproval", true)) {
                        showApproval(action);
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> replaceLastAssistant("اتصال به سرور برقرار نشد. از Settings آدرس Backend را بررسی کن."));
            }
        });
    }

    private void showApproval(JSONObject action) {
        String type = action.optString("type", "ACTION");
        String description = action.optString("description", "Requested action");
        new AlertDialog.Builder(this)
                .setTitle("تأیید عملیات")
                .setMessage(description + "\n\nنوع: " + type + "\n\nبدون تأیید شما اجرا نمی‌شود.")
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
            default: addMessage("System", "این عملیات هنوز به اجراکننده امن متصل نشده است.", false);
        }
    }

    private void showBackendDialog() {
        EditText box = new EditText(this);
        box.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        box.setText(backendUrl());
        int p = (int) (16 * getResources().getDisplayMetrics().density);
        box.setPadding(p, p, p, p);

        new AlertDialog.Builder(this)
                .setTitle("Backend URL")
                .setMessage("آدرس سرور امن را وارد کن. کلید Gemini و Meta فقط روی سرور می‌مانند.")
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
        v.setText(who + "\n" + text);
        v.setTextColor(Color.parseColor(user ? "#F5F5F5" : "#D7B46A"));
        v.setTextSize(15);
        int p = (int) (12 * getResources().getDisplayMetrics().density);
        v.setPadding(p, p, p, p);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, p / 2);
        v.setLayoutParams(lp);
        v.setBackgroundColor(Color.parseColor(user ? "#292929" : "#151515"));
        chatContainer.addView(v);
        chatScroll.post(() -> chatScroll.fullScroll(View.FOCUS_DOWN));
    }

    private void replaceLastAssistant(String text) {
        int count = chatContainer.getChildCount();
        if (count == 0) return;
        View last = chatContainer.getChildAt(count - 1);
        if (last instanceof TextView) ((TextView) last).setText("DECORLUXS AI\n" + text);
    }

    @Override
    protected void onDestroy() {
        pool.shutdownNow();
        super.onDestroy();
    }
}
