package cn.hiroz.uninstallfeedback;

import android.text.TextUtils;
import android.util.Log;

import java.lang.reflect.Method;

/**
 * Created by hiro on 10/21/15.
 */
public class AppProcessEntry {

    public static void main(String[] args) {
        final String dataDir = System.getenv("DATA_DIR"),
                     feedBackUrl = System.getenv("FEEDBACK_URL"),
                     packageName = System.getenv("PACKAGE_NAME");
        setProcessName(packageName + ":feedback");
        if (TextUtils.isEmpty(dataDir)) {
            Log.e("DaemonThread", "DATA_DIR is empty, DaemonThread exit.");
            return;
        }
        if (TextUtils.isEmpty(feedBackUrl)) {
            Log.e("DaemonThread", "FEEDBACK_URL is empty, DaemonThread exit.");
            return ;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                FeedbackUtils.syncOpenUrlWhenUninstall(dataDir, feedBackUrl, packageName + ":feedback");
            }
        }).start();
    }

    public static void setProcessName(String name) {
        try {
            Class<android.os.Process> clazz = android.os.Process.class;
            Method method = clazz.getMethod("setArgV0", String.class);
            method.invoke(null, name);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
