package cn.codec.server.utils;

import android.app.Activity;
import android.app.AndroidAppHelper;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;


import java.io.FileInputStream;
import java.io.FileOutputStream;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;


public class Helper {
    private static Handler handler = null;

    public synchronized static void post(Runnable call) {
        if (handler == null) handler = new Handler(Looper.getMainLooper());
        handler.post(call);
    }

    public static Object log(String... args) {
        String tag = "Helper";
        String info = String.join(" ", args);
        Log.d(tag, info);
        System.out.println(tag + info);
        return args[args.length - 1];
    }

    public static void toast(Context context, String msg) {
        post(() -> Toast.makeText((Context) context, msg, Toast.LENGTH_LONG).show());
    }


    public static String filePath(String name) {
        String path = getSystemContext().getFilesDir() + "/" + name;
        log("filePath:", path);
        return path;
    }

    public static void write(XC_LoadPackage.LoadPackageParam lp, String name, String json) {
        FileOutputStream out;
        try {
            out = new FileOutputStream(filePath(name));
            out.write(json.getBytes());
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String read(XC_LoadPackage.LoadPackageParam lp, String name) {
        try {
            StringBuilder sb = new StringBuilder("");
            FileInputStream input = new FileInputStream(filePath(name));
            byte[] temp = new byte[1024];
            int len = 0;
            while ((len = input.read(temp)) > 0) {
                sb.append(new String(temp, 0, len));
            }
            input.close();
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public interface MethodCall {
        void doTask(XC_MethodHook.MethodHookParam params);
    }

    public static Context getSystemContext() {
        Application application = AndroidAppHelper.currentApplication();
        log("application:", application + "");
        Context context = application.getBaseContext();
        log("context:", context + "");
        return context;
    }


    public static void onActivityCreate(MethodCall methodCall) {
        XposedHelpers.findAndHookMethod(Activity.class, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                methodCall.doTask(param);
            }
        });
    }

    public static void onActivityResume(MethodCall methodCall) {
        XposedHelpers.findAndHookMethod(Activity.class, "onResume", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                methodCall.doTask(param);
            }
        });
    }

    public static void sp_put(XC_LoadPackage.LoadPackageParam lp, Pair<String, String> pair) {
        String pre = read(lp, Pair.presister);
        if (pre != null) pair.merge(pre);
        write(lp, Pair.presister, pair.toString());
    }

    public static void sp_put(XC_LoadPackage.LoadPackageParam lp, String key, String value) {
        sp_put(lp, new Pair<String, String>().add(key, value));
    }

    public static String sp_get(XC_LoadPackage.LoadPackageParam lp, String key, String sec) {
        String json = read(lp, Pair.presister);
        if (json != null) return (String) Pair.from(json).get(key);
        if (sec != null) {
            sp_put(lp, key, sec);
            return sec;
        }
        return null;
    }

}
