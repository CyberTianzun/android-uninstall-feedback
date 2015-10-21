package cn.hiroz.uninstallfeedback;

import android.text.TextUtils;
import android.util.Log;

/**
 * Created by hiro on 10/21/15.
 */
public class AppProcessEntry {

    public static void main(String[] args) {
        String dataDir = System.getenv("DATA_DIR"), feedBackUrl = System.getenv("FEEDBACK_URL");
        if (TextUtils.isEmpty(dataDir)) {
            Log.e("DaemonThread", "DATA_DIR is empty, DaemonThread exit.");
            return;
        }
        if (TextUtils.isEmpty(feedBackUrl)) {
            Log.e("DaemonThread", "FEEDBACK_URL is empty, DaemonThread exit.");
            return ;
        }
        FeedbackUtils.syncOpenUrlWhenUninstall(dataDir, feedBackUrl);
        System.exit(0);
    }

}
