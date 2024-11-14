package cn.codec.server.mod;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import cn.codec.server.rpcserver.Channel;
import cn.codec.server.utils.Network;
import cn.codec.server.utils.Pair;
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

                if (useinfo.pddid != null && useinfo.pddid.length() > 0) {
                    Helper.toast(useinfo.pddid);
                    rpc.setClientId(useinfo.pddid);
                    Helper.sp_put(lp, "userid", useinfo.userid);
                }


                if (useinfo.userid == null) {
                    Helper.toast("请登录多多买菜账号:" + useinfo.pddid);
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

    public static void enter(XC_LoadPackage.LoadPackageParam lp) {
        Helper.onAppAttach(params -> {
            Helper.log("onAppAttach -> " + params);
            Channel rpc = new Channel(lp.packageName, Pair.host, Pair.port) {
                @Override
                public String read() {
                    return Helper.sp_read(lp);
                }

                @Override
                public String presister(String key, String sec) {
                    return Helper.sp_get(lp, key, sec);
                }

                @Override
                public void storage(String key, String value) {
                    Helper.sp_put(lp, key, value);
                }

                @Override
                public HashMap<String, Action> build() {
                    HashMap<String, Action> handlers = super.build();
                    //添加自定义处理
                    return handlers;
                }
            };
            rpc.start();
            new DDMC().listenPddid(rpc, lp);
        });
    }

}
