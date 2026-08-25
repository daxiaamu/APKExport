package cn.leftshine.apkexport;

import android.app.Application;
import cn.leftshine.apkexport.utils.Settings;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Settings.init(this);
    }
}