package xin.xldl.timecalculator;

import android.app.Application;
import android.content.Context;
import com.jakewharton.threetenabp.AndroidThreeTen;

public class TimeCalculatorApp extends Application {

    private static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        context = this;

        // 初始化ThreeTenABP库
        AndroidThreeTen.init(this);

        // 可选：设置全局异常处理器
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            // 在这里处理未捕获的异常
            throwable.printStackTrace();
        });
    }

    public static Context getAppContext() {
        return context;
    }
}