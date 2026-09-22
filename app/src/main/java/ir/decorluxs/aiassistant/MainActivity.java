package ir.decorluxs.aiassistant;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
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
    private TextView channelStatus;
    private Button sendButton;
    private Button whatsappButton;
    private Button instagramButton;
    private SharedPreferences prefs;
    private ActionExecutor actionExecutor;
    private String lastAssistantReply = "";

    private volatile boolean whatsappReady = false;
    private volatile boolean instagramReady = false;
    private volatile boolean whatsappConnectConfigured = false;
    private volatile boolean metaOAuthConfigured = false;

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
        channelStatus = findViewById(R.id.channelStatusText);
        sendButton = findViewById(R.id.sendButton);
        whatsappButton = findViewById(R.id.openWhatsAppButton);
        instagramButton = findViewById(R.id.openInstagramButton);

        sendButton.setOnClickListener(v -> sendMessage());
        input.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage();
                return true;
            }
            return false;
        });

        whatsappButton.setOnClickListener(v -> onWhatsAppClicked());
        instagramButton.setOnClickListener(v -> onInstagramClicked());

        findViewById(R.id.shareButton).setOnClickListener(v -> {
            String text = lastAssistantReply.trim();
            if (text.isEmpty()) text = "هنوز پاسخی برای اشتراک‌گذاری وجود ندارد.";
            actionExecutor.shareText(text);
        });
        findViewById(R.id.settingsButton).setOnClickListener(v -> showBackendDialog());
        channelStatus.setOnClickListener(v -> checkBackend());

        findViewById(R.id.customerReplyButton).setOnClickListener(v ->
                showQuickTool(
                        "پاسخ مشتری",
                        "پیام مشتری را اینجا وارد کن",
                        "به پیام مشتری زیر یک پاسخ حرفه‌ای، کوتاه، صمیمی و فروش‌محور برای برند دکورلوکس بده. فقط متن آماده ارسال را بنویس و توضیح اضافه نده:\n\n"
                ));
        findViewById(R.id.captionButton).setOnClickListener(v ->
                showQuickTool(
                        "کپشن اینستاگرام",
                        "محصول و ویژگی‌هایش را بنویس",
                        "برای محصول زیر یک کپشن فارسی شیک، طبیعی و فروش‌محور برای اینستاگرام دکورلوکس بنویس. یک هوک کوتاه، مزیت محصول، CTA و حداکثر دو هشتگ مرتبط داشته باشد. فقط متن آماده انتشار را بده:\n\n"
                ));
        findViewById(R.id.storyButton).setOnClickListener(v ->
                showQuickTool(
                        "استوری سریالی",
                        "محصول یا موضوع استوری را بنویس",
                        "برای موضوع زیر 4 استوری سریالی کوتاه و طبیعی برای پیج دکورلوکس بنویس. استوری اول هوک، دوم همذات‌پنداری یا مسئله، سوم معرفی مزیت، چهارم CTA باشد. متن‌ها کوتاه و قابل کپی باشند:\n\n"
                ));
        findViewById(R.id.contentIdeaButton).setOnClickListener(v ->
                showQuickTool(
                        "ایده ریلز",
                        "محصول یا هدف محتوا را بنویس",
                        "برای موضوع زیر 5 ایده ریلز واقعی و قابل اجرا برای پیج دکورلوکس بده. برای هر ایده یک هوک و یک توضیح خیلی کوتاه اجرا بنویس. ایده‌ها فروش‌محور اما غیرکلیشه‌ای باشند:\n\n"
                ));

        addMessage("DECORLUXS AI", "سلام 👋 من دستیار هوشمند کسب‌وکار دکورلوکس هستم. می‌تونی مستقیم باهام چت کنی یا از ابزارهای سریع بالا استفاده کنی.", false);
        handleDeepLink(getIntent());
        checkBackend();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (status != null) checkBackend();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleDeepLink(intent);
    }

    private void handleDeepLink(Intent intent) {
        if (intent == null || intent.getData() == null) return;
        Uri data = intent.getData();
        if (!"decorluxaipro".equals(data.getScheme()) || !"meta-connected".equals(data.getHost())) return;

        String token = data.getQueryParameter("token");
        if (token != null && !token.isEmpty()) {
            prefs.edit().putString("automation_admin_token", token).apply();
        }

        new AlertDialog.Builder(this)
                .setTitle("اتصال Meta انجام شد ✅")
                .setMessage("اتصال حساب ثبت شد. وضعیت واتساپ و اینستاگرام دوباره بررسی می‌شود.")
                .setPositiveButton("باشه", null)
                .show();

        checkBackend();
    }

    private String backendUrl() {
        return prefs.getString("backend_url", "https://decorlux-backend-production.up.railway.app");
    }

    private void checkBackend() {
        status.setText("● در حال بررسی اتصال امن…");
        status.setTextColor(Color.parseColor("#A9A9A9"));
        channelStatus.setText("در حال بررسی واتساپ و اینستاگرام…");
        channelStatus.setTextColor(Color.parseColor("#A9A9A9"));

        pool.submit(() -> {
            try {
                ApiClient api = new ApiClient(backendUrl());
                api.health();
                JSONObject automation = api.automationStatus();

                whatsappReady = automation.optBoolean("whatsappReady", false);
                instagramReady = automation.optBoolean("instagramReady", false);
                whatsappConnectConfigured = automation.optBoolean("whatsappConnectConfigured", false);
                metaOAuthConfigured = automation.optBoolean("metaOAuthConfigured", false);

                String networkLine =
                        "واتساپ: " + (whatsappReady ? "آماده" : "نیاز به اتصال")
                        + "   •   اینستاگرام: " + (instagramReady ? "آماده" : "نیاز به اتصال");

                runOnUiThread(() -> {
                    status.setText("● سرور امن متصل است");
                    status.setTextColor(Color.parseColor("#64D39B"));
                    if (!whatsappReady && !instagramReady) {
                        channelStatus.setText("واتساپ و اینستاگرام: حالت دستی آماده   •   لمس برای تازه‌سازی");
                    } else {
                        channelStatus.setText(networkLine + "   •   لمس برای تازه‌سازی");
                    }
                    channelStatus.setTextColor(Color.parseColor(
                            whatsappReady || instagramReady ? "#D6B05E" : "#A9A9A9"
                    ));
                    whatsappButton.setText(whatsappReady ? "واتساپ" : "واتساپ دستی");
                    instagramButton.setText(instagramReady ? "اینستاگرام" : "اینستاگرام دستی");
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    status.setText("● اتصال سرور برقرار نیست");
                    status.setTextColor(Color.parseColor("#FF737A"));
                    channelStatus.setText("وضعیت شبکه‌ها در دسترس نیست");
                    channelStatus.setTextColor(Color.parseColor("#A9A9A9"));
                });
            }
        });
    }

    private void onWhatsAppClicked() {
        if (whatsappReady) {
            actionExecutor.openPackage("com.whatsapp.w4b", "https://www.whatsapp.com/business/");
            return;
        }

        String[] options = lastAssistantReply.trim().isEmpty()
                ? new String[]{"باز کردن WhatsApp Business", "اتصال API رسمی (بعداً)"}
                : new String[]{"ارسال آخرین جواب به واتساپ", "باز کردن WhatsApp Business", "اتصال API رسمی (بعداً)"};

        new AlertDialog.Builder(this)
                .setTitle("واتساپ")
                .setMessage("تا وقتی Meta Cloud API در دسترس نباشه، حالت دستی امن فعاله؛ ارسال نهایی همیشه با تأیید خودت انجام می‌شه.")
                .setItems(options, (d, which) -> {
                    if (!lastAssistantReply.trim().isEmpty()) {
                        if (which == 0) {
                            actionExecutor.sendToWhatsAppBusiness(lastAssistantReply);
                            return;
                        }
                        if (which == 1) {
                            actionExecutor.openPackage("com.whatsapp.w4b", "https://www.whatsapp.com/business/");
                            return;
                        }
                        actionExecutor.openUrl(backendUrl() + "/setup/meta-whatsapp");
                        return;
                    }

                    if (which == 0) {
                        actionExecutor.openPackage("com.whatsapp.w4b", "https://www.whatsapp.com/business/");
                    } else {
                        actionExecutor.openUrl(backendUrl() + "/setup/meta-whatsapp");
                    }
                })
                .setNegativeButton("لغو", null)
                .show();
    }

    private void onInstagramClicked() {
        if (instagramReady) {
            actionExecutor.openPackage("com.instagram.android", "https://www.instagram.com/decorluxs/");
            return;
        }

        String[] options = lastAssistantReply.trim().isEmpty()
                ? new String[]{"باز کردن اینستاگرام", "اتصال API رسمی (بعداً)"}
                : new String[]{"کپی آخرین جواب و باز کردن اینستاگرام", "فقط باز کردن اینستاگرام", "اتصال API رسمی (بعداً)"};

        new AlertDialog.Builder(this)
                .setTitle("اینستاگرام")
                .setMessage("فعلاً حالت دستی فعاله. متن AI کپی می‌شه و اینستاگرام باز می‌شه؛ ارسال نهایی با خودته.")
                .setItems(options, (d, which) -> {
                    if (!lastAssistantReply.trim().isEmpty()) {
                        if (which == 0) {
                            actionExecutor.copyAndOpenInstagram(lastAssistantReply);
                            return;
                        }
                        if (which == 1) {
                            actionExecutor.openPackage("com.instagram.android", "https://www.instagram.com/decorluxs/");
                            return;
                        }
                        if (metaOAuthConfigured) {
                            actionExecutor.openUrl(backendUrl() + "/auth/meta/start?device=android");
                        } else {
                            actionExecutor.openUrl(backendUrl() + "/setup/meta-whatsapp");
                        }
                        return;
                    }

                    if (which == 0) {
                        actionExecutor.openPackage("com.instagram.android", "https://www.instagram.com/decorluxs/");
                    } else if (metaOAuthConfigured) {
                        actionExecutor.openUrl(backendUrl() + "/auth/meta/start?device=android");
                    } else {
                        actionExecutor.openUrl(backendUrl() + "/setup/meta-whatsapp");
                    }
                })
                .setNegativeButton("لغو", null)
                .show();
    }

    private void sendMessage() {
        String message = input.getText().toString().trim();
        if (message.isEmpty() || !sendButton.isEnabled()) return;
        input.setText("");
        sendPreparedMessage(message, message);
    }

    private void sendPreparedMessage(String displayText, String prompt) {
        if (!sendButton.isEnabled()) return;

        setSending(true);
        addMessage("شما", displayText, true);
        addMessage("DECORLUXS AI", "در حال فکر کردن…", false);

        pool.submit(() -> {
            try {
                JSONObject response = new ApiClient(backendUrl()).chat(prompt);
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

    private void showQuickTool(String title, String hint, String promptPrefix) {
        EditText box = new EditText(this);
        box.setHint(hint);
        box.setMinLines(3);
        box.setMaxLines(7);
        box.setGravity(Gravity.RIGHT | Gravity.TOP);
        box.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG_RTL);
        box.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        int p = dp(14);
        box.setPadding(p, p, p, p);

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(box)
                .setNegativeButton("لغو", null)
                .setPositiveButton("بساز", (d, w) -> {
                    String detail = box.getText().toString().trim();
                    if (!detail.isEmpty()) {
                        sendPreparedMessage(title + "\n" + detail, promptPrefix + detail);
                    }
                })
                .show();
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
                .setMessage("آدرس سرور امن DecorLux را وارد کن.")
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
        lastAssistantReply = text;
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
