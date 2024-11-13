package cn.codec.server.mod;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Timer;
import java.util.TimerTask;

import cn.codec.server.utils.Network;
import cn.codec.server.utils.Channel;
import cn.codec.server.utils.Helper;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class DDMC {
    public String latestKey = "";

    public void listenPddid(Channel rpc, XC_LoadPackage.LoadPackageParam lp) {
        final Class<?> PddActivityThread = XposedHelpers.findClass("android.app.PddActivityThread", lp.classLoader);
        final Class<?> SecureMmkvClass = XposedHelpers.findClass("com.aimi.android.common.c.b", lp.classLoader);
        final Class<?> PddApp = XposedHelpers.findClass("com.xunmeng.pinduoduo.basekit.a.a.a", lp.classLoader);

        TimerTask task = new TimerTask() { // from class: com.virjar.sekiro.demo.XposedMain.1
            @Override // java.util.TimerTask, java.lang.Runnable
            public void run() {
                final Object context = XposedHelpers.callStaticMethod(PddActivityThread, "getApplication", new Object[0]);
                Object pddId = XposedHelpers.callStaticMethod(SecureMmkvClass, "b", new Object[0]);
                Helper.log("pddId:" + pddId);
                Object PddAppIntance = XposedHelpers.callStaticMethod(PddApp, "a", new Object[0]);
                Object IPDDUserInstance = XposedHelpers.callMethod(PddAppIntance, "c", new Object[0]);
                Object userInfoString = XposedHelpers.callMethod(IPDDUserInstance, "b", new Object[0]);
                Helper.log("uid =" + userInfoString);

                if (userInfoString == null || userInfoString.toString().isEmpty()) {
                    Helper.toast((Context) context, "请登录多多买菜账号");
                    return;
                }
                Helper.log("userId =", userInfoString.toString(), "latestKey =", latestKey);
                try {
                    if (DDMC.this.latestKey.isEmpty() || !DDMC.this.latestKey.equals(pddId.toString())) {
                        rpc.setClientId(pddId + "");
                        rpc.lp_put("userid", userInfoString + "");
                        DDMC.this.savePddid(userInfoString.toString(), pddId.toString());
                    }
                } catch (JSONException e) {
                    Helper.log("saveYdId|fail: " + e.getMessage());
                    throw new RuntimeException(e);
                }
            }
        };
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(task, 5000L, 20000L);

    }


    public void savePddid(String userId, final String key) throws JSONException {

        String url = "http://localhost:80/api/ddmc/saveIdByUserId";

        JSONObject jsonBody = new JSONObject();
        jsonBody.put("userId", userId);
        jsonBody.put("key", key);
        Network.postJson(url, jsonBody.toString(), (call, response, e) -> {
            if (e != null) return Helper.log("savePddid|failed:" + e.getMessage());

            if (response.isSuccessful()) {
                DDMC.this.latestKey = key;
                return Helper.log("savePddid|success:" + response.body().string());
            }
            return Helper.log("savePddid|other:" + response.body().string());
        });

    }
}
