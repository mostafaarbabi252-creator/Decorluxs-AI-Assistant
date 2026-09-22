package ir.decorluxs.aiassistant;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.widget.Toast;

public class ActionExecutor {
    private final Activity activity;

    public ActionExecutor(Activity activity) {
        this.activity = activity;
    }

    public void openPackage(String packageName, String fallbackUrl) {
        Intent launch = activity.getPackageManager().getLaunchIntentForPackage(packageName);
        if (launch != null) {
            activity.startActivity(launch);
            return;
        }
        activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(fallbackUrl)));
    }

    public void openSettings() {
        activity.startActivity(new Intent(Settings.ACTION_SETTINGS));
    }

    public void shareText(String text) {
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_TEXT, text);
        activity.startActivity(Intent.createChooser(i, "اشتراک‌گذاری"));
    }

    public void copyText(String text) {
        ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText("DECORLUXS AI", text));
            Toast.makeText(activity, "متن کپی شد", Toast.LENGTH_SHORT).show();
        }
    }

    public void sendToWhatsAppBusiness(String text) {
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_TEXT, text);
        i.setPackage("com.whatsapp.w4b");
        try {
            activity.startActivity(i);
        } catch (ActivityNotFoundException e) {
            shareText(text);
        }
    }

    public void copyAndOpenInstagram(String text) {
        copyText(text);
        openPackage("com.instagram.android", "https://www.instagram.com/decorluxs/");
    }

    public void openUrl(String url) {
        try {
            activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (ActivityNotFoundException ignored) { }
    }
}
