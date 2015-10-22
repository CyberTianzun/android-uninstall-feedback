package cn.hiroz.uninstallfeedback;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.util.AndroidRuntimeException;
import android.util.Log;

import java.io.File;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Created by hiro on 5/11/15.
 */
public class FeedbackUtils {

    private static final String TAG = "FeedbackUtils";

    static {
        System.loadLibrary("uninstall-feedback");
    }

    public static void startActionWhenUninstall(Context context, String action, String data) {
        String dirStr = context.getApplicationInfo().dataDir;
        init(1, dirStr, null, action, data, Build.BRAND);
    }

    public static void startActivityWhenUninstall(Context context, String packageName, String activityName) {
        String dirStr = context.getApplicationInfo().dataDir;
        init(1, dirStr, String.format("%s/%s", packageName, activityName), null, null, Build.BRAND);
    }

    private static native void init(int isFork, String dirStr, String activity, String action, String data, String brand);

    public static void openUrlWhenUninstall(Context context, String openUrl) {
        if (Build.BRAND.equals("OPPO") || Build.BRAND.equals("samsung")) {
            openUrlWhenUninstallViaForkProcess(context, openUrl);
        } else {
            openUrlWhenUninstallViaAppProcess(context, openUrl);
        }
    }

    public static void openUrlWhenUninstallViaForkProcess(Context context, String openUrl) {
        String dirStr = context.getApplicationInfo().dataDir;
        String activity = "com.android.browser/com.android.browser.BrowserActivity";
        String action = "android.intent.action.VIEW";
        init(1, dirStr, activity, action, openUrl, Build.BRAND);
    }

    public static boolean openUrlWhenUninstallViaAppProcess(Context context, String openUrl) {
        String dirStr = context.getApplicationInfo().dataDir;
        try {
            exec(
                    "sh",
                    "export CLASSPATH=$CLASSPATH:" + getDexpath(context),
                    "cd " + context.getFilesDir().getParent(),
                    String.format("export %s=%s", "FEEDBACK_URL", "'" + openUrl + "'"),
                    String.format("export %s=%s", "PATH", System.getenv("PATH")),
                    String.format("export %s=%s", "DATA_DIR", dirStr),
                    String.format("export %s=%s", "LD_LIBRARY_PATH", System.getenv("LD_LIBRARY_PATH") + ":" + dirStr + "/lib"),
                    String.format("app_process / %s --nice-name=%s --daemon &", AppProcessEntry.class.getName(), context.getPackageName())
            );
        } catch (Throwable e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    static void syncOpenUrlWhenUninstall(String dirStr, String openUrl) {
        init(1, dirStr, "com.android.browser/com.android.browser.BrowserActivity", "android.intent.action.VIEW", openUrl, Build.BRAND);
    }

    static int exec(String shell, String... cmds) {
        try {
            ProcessBuilder builder = new ProcessBuilder().command(shell).redirectErrorStream(true).directory(new File("/"));
            OutputStream stdIn = builder.start().getOutputStream();
            for (String cmd : cmds) {
                if (!cmd.endsWith("\n")) {
                    cmd += "\n";
                }
                stdIn.write(cmd.getBytes());
                stdIn.flush();
            }
            return 0;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new AndroidRuntimeException(e);
        }
    }

    private static String getDexpath(Context context) {
        StringBuilder dexPath = new StringBuilder();
        boolean IS_VM_MULTIDEX_CAPABLE;
        ApplicationInfo applicationInfo = context.getApplicationInfo();
        dexPath.append(applicationInfo.sourceDir);
        try {
            Class Mclass = Class.forName("android.support.multidex.MultiDex");
            Field field = Mclass.getDeclaredField("SECONDARY_FOLDER_NAME");
            field.setAccessible(true);
            String path = (String) field.get(null);
            Log.d(TAG, "path:" + path);
            File file = new File(context.getApplicationInfo().dataDir, path);

            Class a = Class.forName("android.support.multidex.MultiDex");
            Field field_a = a.getDeclaredField("IS_VM_MULTIDEX_CAPABLE");
            field_a.setAccessible(true);
            IS_VM_MULTIDEX_CAPABLE = (Boolean)field_a.get(null);
            if(IS_VM_MULTIDEX_CAPABLE){
                //支持，解压
                Class b_class = Class.forName("android.support.multidex.MultiDex");
                Method b_method = b_class.getDeclaredMethod("clearOldDexDir", Context.class);
                b_method.setAccessible(true);
                b_method.invoke(null, context);

                Class c_class = Class.forName("android.support.multidex.MultiDexExtractor");
                Method c_method = c_class.getDeclaredMethod("load", Context.class, ApplicationInfo.class, File.class, boolean.class);
                c_method.setAccessible(true);
                c_method.invoke(null, context, applicationInfo, file, false);
            }

            for (File f : file.listFiles()) {
                if (f.isFile() && f.getName().endsWith(".zip")) {
                    if (dexPath.length() > 0) {
                        dexPath.append(":");
                    }
                    dexPath.append(f.getAbsolutePath());
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return dexPath.toString();
    }

}
