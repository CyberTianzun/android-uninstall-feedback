package cn.hiroz.uninstallfeedback;

import android.content.Context;
import android.os.Build;

/**
 * Created by hiro on 5/11/15.
 */
public class FeedbackUtils {

    static {
        System.loadLibrary("uninstall-feedback");
    }

    public static void openUrlWhenUninstall(Context context, String openUrl) {
        String dirStr = context.getApplicationInfo().dataDir;
        String activity = "com.android.browser/com.android.browser.BrowserActivity";
        String action = "android.intent.action.VIEW";
        String data = openUrl;
        init(dirStr, activity, action, data, Build.BRAND);
    }

    public static void startActionWhenUninstall(Context context, String action, String data) {
        String dirStr = context.getApplicationInfo().dataDir;
        init(dirStr, null, action, data, Build.BRAND);
    }

    public static void startActivityWhenUninstall(Context context, String packageName, String activityName) {
        String dirStr = context.getApplicationInfo().dataDir;
        init(dirStr, String.format("%s/%s", packageName, activityName), null, null, Build.BRAND);
    }

    private static native void init(String dirStr, String activity, String action, String data, String brand);

}
