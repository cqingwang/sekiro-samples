package cn.codec.server.rpcserver;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class DDMCEnter {
    public String latestKey = "";

    public void getPddid(XC_LoadPackage.LoadPackageParam lp) {
        final Class<?> PddActivityThread = XposedHelpers.findClass("android.app.PddActivityThread", lp.classLoader);
        final Class<?> SecureMmkvClass = XposedHelpers.findClass("com.aimi.android.common.c.b", lp.classLoader);
        final Class<?> PddApp = XposedHelpers.findClass("com.xunmeng.pinduoduo.basekit.a.a.a", lp.classLoader);

        TimerTask task = new TimerTask() { // from class: com.virjar.sekiro.demo.XposedMain.1
            @Override // java.util.TimerTask, java.lang.Runnable
            public void run() {
                final Object context = XposedHelpers.callStaticMethod(PddActivityThread, "getApplication", new Object[0]);
                Object pddId = XposedHelpers.callStaticMethod(SecureMmkvClass, "b", new Object[0]);
                System.out.println("fingerToken pddId:" + pddId);
                Object PddAppIntance = XposedHelpers.callStaticMethod(PddApp, "a", new Object[0]);
                Object IPDDUserInstance = XposedHelpers.callMethod(PddAppIntance, "c", new Object[0]);
                Object userInfoString = XposedHelpers.callMethod(IPDDUserInstance, "b", new Object[0]);
                System.out.println("fingerToken uid:" + userInfoString);
                if (userInfoString == null || userInfoString.toString().isEmpty()) {
                    new Thread(new Runnable() {
                        @Override // java.lang.Runnable
                        public void run() {
                            Handler mainHandler = new Handler(Looper.getMainLooper());
                            mainHandler.post(new Runnable() { // from class: com.virjar.sekiro.demo.XposedMain.1.1.1
                                @Override // java.lang.Runnable
                                public void run() {
                                    Toast.makeText((Context) context, "请登录多多买菜账号", Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                    }).start();
                    return;
                }
                System.out.println("fingerToken userInfo.userId = " + userInfoString.toString() + " latestKey= " + DDMCEnter.this.latestKey);
                try {
                    if (DDMCEnter.this.latestKey.isEmpty() || !DDMCEnter.this.latestKey.equals(pddId.toString())) {
                        RequestApi.use(lp.packageName).put("pddid", pddId + "|" + userInfoString);
                        DDMCEnter.this.savePddid(userInfoString.toString(), pddId.toString());
                    }
                } catch (JSONException e) {
                    System.out.println("fingerToken saveYdId fail: " + e.getMessage());
                    throw new RuntimeException(e);
                }
            }
        };
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(task, 5000L, 20000L);

    }


    public void savePddid(String userId, final String key) throws JSONException {
        String httpAt = "http://localhost:80/api/ddmc/saveIdByUserId";

        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        JSONObject jsonBody = new JSONObject();
        jsonBody.put("userId", userId);
        jsonBody.put("key", key);
        RequestBody body = RequestBody.create(JSON, jsonBody.toString());
        Request request = new Request.Builder().url(httpAt).header("Accept", "application/json").post(body).build();
        RequestApi.getInstance().newCall(request).enqueue(new Callback() { // from class: com.virjar.sekiro.demo.XposedMain.2
            @Override // okhttp3.Callback
            public void onFailure(Call call, IOException e) {
                System.out.println("saveIdByUserId|failed:" + e.getMessage());
            }

            @Override // okhttp3.Callback
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    System.out.println("saveIdByUserId|success:" + response.body().string());
                    DDMCEnter.this.latestKey = key;
                    return;
                }
                System.out.println("saveIdByUserId|fail:" + response.body().string());
            }
        });
    }
}
