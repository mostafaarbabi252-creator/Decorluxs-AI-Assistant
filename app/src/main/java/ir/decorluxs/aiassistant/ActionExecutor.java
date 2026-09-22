package ir.decorluxs.aiassistant;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;

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
        activity.startActivity(Intent.createChooser(i, "Share with"));
    }

    public void openUrl(String url) {
        try {
            activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (ActivityNotFoundException ignored) { }
    }
}
