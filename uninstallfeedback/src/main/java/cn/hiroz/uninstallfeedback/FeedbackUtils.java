package cn.hiroz.uninstallfeedback;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.util.AndroidRuntimeException;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Created by hiro on 5/11/15.
 */
public class FeedbackUtils {

    private static final String TAG = "FeedbackUtils";
    private static int processId = 0;

    static {
        try {
            System.loadLibrary("uninstall-feedback");
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public static boolean cancel() {
        if (processId > 0) {
            try {
                Runtime.getRuntime().exec("kill " + processId);
                return true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    private static int wrapInit(int isFork, String dirStr, String activity, String action, String data, String brand) {
        try {
            return init(isFork, dirStr, activity, action, data, brand);
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return 0;
    }

    private static native int init(int isFork, String dirStr, String activity, String action, String data, String brand);

    public static void openUrlWhenUninstall(Context context, String openUrl) {
        processId = openUrlWhenUninstallViaForkProcess(context, openUrl);
        openUrlWhenUninstallViaAppProcess(context, openUrl);
    }

    public static int openUrlWhenUninstallViaForkProcess(Context context, String openUrl) {
        return asyncOpenUrlWhenUninstall(context.getApplicationInfo().dataDir, openUrl);
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

    static int syncOpenUrlWhenUninstall(String dirStr, String openUrl) {
        return wrapInit(0, dirStr, null, "android.intent.action.VIEW", openUrl, Build.BRAND);
    }

    static int asyncOpenUrlWhenUninstall(String dirStr, String openUrl) {
        return wrapInit(1, dirStr, null, "android.intent.action.VIEW", openUrl, Build.BRAND);
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

    static String getDexpath(Context context) {
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
