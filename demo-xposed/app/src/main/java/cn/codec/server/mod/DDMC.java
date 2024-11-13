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

    static class Userinfo {
        String pddid;
        String userid;

        public Userinfo(String pddid, String userid) {
            this.pddid = pddid;
            this.userid = userid;
        }
    }

    public static Userinfo getinfo(XC_LoadPackage.LoadPackageParam lp) {
        final Class<?> PddActivityThread = XposedHelpers.findClass("android.app.PddActivityThread", lp.classLoader);
        final Class<?> SecureMmkvClass = XposedHelpers.findClass("com.aimi.android.common.c.b", lp.classLoader);
        final Class<?> PddApp = XposedHelpers.findClass("com.xunmeng.pinduoduo.basekit.a.a.a", lp.classLoader);

        final Object context = XposedHelpers.callStaticMethod(PddActivityThread, "getApplication", new Object[0]);
        String pddId = (String) XposedHelpers.callStaticMethod(SecureMmkvClass, "b", new Object[0]);
        Helper.log("pddId:" + pddId);
        Object PddAppIntance = XposedHelpers.callStaticMethod(PddApp, "a", new Object[0]);
        Object IPDDUserInstance = XposedHelpers.callMethod(PddAppIntance, "c", new Object[0]);
        String userid = (String) XposedHelpers.callMethod(IPDDUserInstance, "b", new Object[0]);
        Helper.log("uid =" + userid);
        return new Userinfo(pddId, userid);
    }

    public void listenPddid(Channel rpc, XC_LoadPackage.LoadPackageParam lp) {
        TimerTask task = new TimerTask() { // from class: com.virjar.sekiro.demo.XposedMain.1
            @Override // java.util.TimerTask, java.lang.Runnable
            public void run() {
                Userinfo useinfo = getinfo(lp);
                if (useinfo == null) return;

                rpc.setClientId(useinfo.pddid);
                Helper.sp_put(lp, "userid", useinfo.userid);

                if (useinfo.userid == null) {
                    Helper.toast(Helper.getSystemContext(), "请登录多多买菜账号");
                    return;
                }
                Helper.log("userId =", useinfo.userid, "latestKey =", latestKey);
                try {
                    if (DDMC.this.latestKey.isEmpty() || !DDMC.this.latestKey.equals(useinfo.pddid)) {
                        DDMC.this.savePddid(useinfo);
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


    public void savePddid(Userinfo userinfo) throws JSONException {

        String url = "http://localhost:80/api/ddmc/saveIdByUserId";

        JSONObject jsonBody = new JSONObject();
        jsonBody.put("userId", userinfo.userid);
        jsonBody.put("pddid", userinfo.pddid);
        Network.postJson(url, jsonBody.toString(), (call, response, e) -> {
            if (e != null) return Helper.log("savePddid|failed:" + e.getMessage());
            if (response.isSuccessful()) {
                DDMC.this.latestKey = userinfo.pddid;
                return Helper.log("savePddid|success:" + response.body().string());
            }
            return Helper.log("savePddid|other:" + response.body().string());
        });

    }
}
