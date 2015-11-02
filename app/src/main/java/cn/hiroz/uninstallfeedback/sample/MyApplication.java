package cn.hiroz.uninstallfeedback.sample;

import android.app.Application;
import android.os.Build;
import android.os.Environment;
import cn.hiroz.uninstallfeedback.FeedbackUtils;

import java.io.IOException;

/**
 * Created by hiro on 5/11/15.
 */
public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        android.util.Log.e("DaemonThread", "Build Brand => " + Build.BRAND);
//        FeedbackUtils.openUrlWhenUninstall(this, "http://www.baidu.com");
        int a = FeedbackUtils.openUrlWhenUninstallViaForkProcess(this, "http://www.baidu.com");
        android.util.Log.e("DaemonThread", "new thread => " + a);
        try {
            Thread.sleep(1000);
            Runtime.getRuntime().exec("kill " + a);
            //Runtime.getRuntime().exec("")
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
