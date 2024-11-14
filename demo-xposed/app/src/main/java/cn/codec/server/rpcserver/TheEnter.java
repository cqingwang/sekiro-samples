package cn.codec.server.rpcserver;



import cn.codec.server.mod.DDMC;
import cn.codec.server.utils.Pair;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class TheEnter implements IXposedHookLoadPackage {
    public static XC_LoadPackage.LoadPackageParam loadPackageParam;


    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lp) throws Throwable {
        TheEnter.loadPackageParam = lp;

        //一般情况下只在主进程中启动sekiro服务
        if (!loadPackageParam.packageName.equals(lp.processName)) return;

        //         // xposed本身支持多个进程注入hook，所以这里大部分情况判断下，只过滤到主进程
        switch (loadPackageParam.packageName) {
            case Pair.ddmc: {
                DDMC.enter(lp);
            }
        }


    }


}
