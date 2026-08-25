package cn.leftshine.apkexport.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;
import java.io.File;

public final class Settings {
    private static final String KEY_CUSTOM_FILENAME_FORMAT = "custom_filename_format";
    private static final String KEY_CUSTOM_EXPORT_PATH = "custom_export_path";
    private static SharedPreferences preferences;
    private static String defaultExportPath;

    private Settings() {}

    public static void init(Context context) {
        Context applicationContext = context.getApplicationContext();
        preferences = PreferenceManager.getDefaultSharedPreferences(applicationContext);
        defaultExportPath = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "APKExport"
        ).getPath();
    }


    public static String getCustomFileNameFormat() {
        return preferences.getString(KEY_CUSTOM_FILENAME_FORMAT, "#N-#P-#V");
    }

    public static void setCustomFileNameFormat(String format) {
        preferences.edit().putString(KEY_CUSTOM_FILENAME_FORMAT, format).apply();
    }

    public static String getCustomExportPath() {
        return preferences.getString(KEY_CUSTOM_EXPORT_PATH, defaultExportPath);
    }

    public static void setCustomExportPath(String path) {
        preferences.edit().putString(KEY_CUSTOM_EXPORT_PATH, path).apply();
    }
}