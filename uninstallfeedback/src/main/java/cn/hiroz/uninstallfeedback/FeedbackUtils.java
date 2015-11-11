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
    private static Context context;

    static {
        try {
            System.loadLibrary("uninstall-feedback");
        } catch (Throwable e) {
            if (BuildConfig.DEBUG) {
                e.printStackTrace();
            }
        }
    }

    public static boolean cancel() {
        if (processId > 0) {
            try {
                Runtime.getRuntime().exec("kill -9 " + processId);
                Runtime.getRuntime().exec("killall -9 '" + context.getPackageName() + ":feedback'");
                return true;
            } catch (IOException e) {
                if (BuildConfig.DEBUG) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    private static int wrapInit(int isFork, String dirStr, String data) {
        try {
            return init(isFork, dirStr, data, Build.BRAND, Build.VERSION.SDK_INT);
        } catch (Throwable t) {
            if (BuildConfig.DEBUG) {
                t.printStackTrace();
            }
        }
        return 0;
    }

    private static native int init(int isFork, String dirStr, String data, String brand, int apiLevel);

    private static native int countProcess(String processName);

    public static void openUrlWhenUninstall(Context context, String openUrl) {
        int countProcess = 0;
        try {
            countProcess = countProcess(context.getPackageName() + ":feedback");
        } catch (Throwable t) {
            if (BuildConfig.DEBUG) {
                t.printStackTrace();
            }
        }
        if (countProcess > 0) {
            return;
        }
        FeedbackUtils.context = context;
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
                    String.format("export %s=%s", "PACKAGE_NAME", context.getPackageName()),
                    String.format("export %s=%s", "LD_LIBRARY_PATH", System.getenv("LD_LIBRARY_PATH") + ":" + dirStr + "/lib"),
                    String.format("app_process / %s --nice-name=%s --daemon &", AppProcessEntry.class.getName(), context.getPackageName())
            );
        } catch (Throwable e) {
            if (BuildConfig.DEBUG) {
                e.printStackTrace();
            }
            return false;
        }
        return true;
    }

    static int syncOpenUrlWhenUninstall(String dirStr, String openUrl) {
        return wrapInit(0, dirStr, openUrl);
    }

    static int asyncOpenUrlWhenUninstall(String dirStr, String openUrl) {
        return wrapInit(1, dirStr, openUrl);
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
            if (BuildConfig.DEBUG) {
                e.printStackTrace();
            }
        }
        return dexPath.toString();
    }

}
