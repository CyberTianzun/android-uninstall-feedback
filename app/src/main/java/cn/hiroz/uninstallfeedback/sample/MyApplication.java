package cn.hiroz.uninstallfeedback.sample;

import android.app.Application;
import cn.hiroz.uninstallfeedback.FeedbackUtils;

/**
 * Created by hiro on 5/11/15.
 */
public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        FeedbackUtils.openUrlWhenUninstall(this, "http://www.baidu.com");
    }
}
