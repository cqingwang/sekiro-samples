package cn.codec.server.rpcserver;

import android.app.Activity;

import cn.codec.server.mod.DDMC;
import cn.codec.server.utils.Channel;
import cn.codec.server.utils.Helper;
import cn.codec.server.utils.Pair;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class TheEnter implements IXposedHookLoadPackage {
    public static XC_LoadPackage.LoadPackageParam loadPackageParam;


    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        TheEnter.loadPackageParam = loadPackageParam;

        //一般情况下只在主进程中启动sekiro服务
        // xposed本身支持多个进程注入hook，所以这里大部分情况判断下，只过滤到主进程
        if (!loadPackageParam.packageName.equals(loadPackageParam.processName)) return;

        // 请注意，一般sekiro只作用于特定的app
        switch (loadPackageParam.packageName) {
            case Pair.ddmc: {
                Channel rpc = new Channel(loadPackageParam);
                Helper.onActivityResume(params -> {
                    Helper.log("onActivityResume" + params);
                    rpc.start();
                });
                new DDMC().listenPddid(rpc, loadPackageParam);
            }
        }


    }


}
