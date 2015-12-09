package cn.hiroz.uninstallfeedback;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.text.TextUtils;
import android.util.AndroidRuntimeException;
import android.util.Log;

import java.io.File;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

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
        try {
            //通过ps查找弹卸载反馈的进程
            String processName = context.getPackageName() + ":feedback";
            Process p = Runtime.getRuntime().exec("ps");
            p.waitFor();
            Scanner scanner = new Scanner(p.getInputStream());
            List<String> tags = new ArrayList<>();

            int pidRow = -1;
            while (scanner.hasNextLine()) {
                String scannerStr = scanner.nextLine();
                if (scannerStr.contains(processName) || scannerStr.toLowerCase().contains("pid")) {
                    while (scannerStr.contains("  ")) {
                        scannerStr = scannerStr.replaceAll("  ", " ").trim();
                    }
                    String pidStr = null;
                    int pid = -1;
                    if (scannerStr.toLowerCase().contains("pid")) {
                        tags = Arrays.asList(scannerStr.toLowerCase().split(" "));
                        pidRow = tags.indexOf("pid");//pid所在的列号
                    } else if (pidRow != -1){
                        pidStr = scannerStr.split(" ")[pidRow];
                        if (!TextUtils.isEmpty(pidStr)) {
                            pid = Integer.valueOf(pidStr);
                            android.os.Process.killProcess(pid);
                            Log.d("DaemonThread", scannerStr + "\npidRow:" + pidRow + ", kill pid:" + pid);
                        }
                    }
                }
            }
            return true;
        } catch (Exception e) {
            Log.d("DaemonThread", "cancel Exception => " + e.getMessage());
            if (BuildConfig.DEBUG) {
                e.printStackTrace();
            }
        }
        return false;
    }

    private static int wrapInit(int isFork, String dirStr, String data, String processName) {
        try {
            return init(isFork, dirStr, data, Build.BRAND, Build.VERSION.SDK_INT, processName);
        } catch (Throwable t) {
            if (BuildConfig.DEBUG) {
                t.printStackTrace();
            }
        }
        return 0;
    }

    private static native int init(int isFork, String dirStr, String data, String brand, int apiLevel, String processName);

//    private static native int countProcess(String processName);

    private static int countProcess(String processName) {
        int count = 0;
        try {
            Process p = Runtime.getRuntime().exec("ps");
            p.waitFor();
            Scanner scanner = new Scanner(p.getInputStream());
            while (scanner.hasNextLine()) {
                if (scanner.nextLine().contains(processName)) {
                    count++;
                }
            }
//            int[] pids = (int[]) android.os.Process.class.getMethod("getPids", String.class, int[].class)
//                .invoke(null, "/proc", null);
        } catch (Exception e) {
            if (BuildConfig.DEBUG) {
                e.printStackTrace();
            }
        }
        android.util.Log.e("DaemonThread", "countProcess => " + count);
        return count;
//        int i = android.os.Process.getUidForName(processName);
//        android.util.Log.e("DaemonThread", "i => " + i);
//        return i > 0 ? 1 : 0;
    }

    public static void openUrlWhenUninstall(Context context, String openUrl) {
        FeedbackUtils.context = context;
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

        processId = openUrlWhenUninstallViaForkProcess(context, openUrl);
//        openUrlWhenUninstallViaAppProcess(context, openUrl);
    }

    public static int openUrlWhenUninstallViaForkProcess(Context context, String openUrl) {
        return asyncOpenUrlWhenUninstall(context.getApplicationInfo().dataDir, openUrl, context.getPackageName() + ":feedback");
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
                    String.format("app_process / %s --nice-name=%s --daemon &", AppProcessEntry.class.getName(), context.getPackageName() + ":feedback")
            );
        } catch (Throwable e) {
            if (BuildConfig.DEBUG) {
                e.printStackTrace();
            }
            return false;
        }
        return true;
    }

    static int syncOpenUrlWhenUninstall(String dirStr, String openUrl, String processName) {
        return wrapInit(0, dirStr, openUrl, processName);
    }

    static int asyncOpenUrlWhenUninstall(String dirStr, String openUrl, String processName) {
        return wrapInit(1, dirStr, openUrl, processName);
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
