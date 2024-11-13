package cn.codec.server.rpcserver;

import com.google.gson.Gson;


import java.util.HashMap;
import java.util.Map;

import cn.iinti.sekiro3.business.api.SekiroClient;
import cn.iinti.sekiro3.business.api.interfaze.ActionHandler;
import cn.iinti.sekiro3.business.api.interfaze.SekiroRequest;
import cn.iinti.sekiro3.business.api.interfaze.SekiroResponse;
import okhttp3.OkHttpClient;

public class RequestApi {
    private static OkHttpClient _client = null;
    public static Map<String, HashMap<String, Object>> cached = new HashMap<>();

    public static synchronized OkHttpClient getInstance() {
        if (_client == null) {
            _client = new OkHttpClient();
        }
        return _client;
    }

    public static HashMap<String, Object> use(String packageName) {
        if (!cached.containsKey(packageName)) {
            cached.put(packageName, new HashMap<>());
        }
        return cached.get(packageName);
    }


    public static void startRpc(String packageName) {
        final String proc=packageName;
        new Thread(() -> {
            // xposed环境下使用sekiro
            new SekiroClient("test", "xpid", "m.olless.com", 8010)
                    .setupSekiroRequestInitializer((sekiroRequest, handlerRegistry) ->
                            // 注册一个接口，名为testAction
                            handlerRegistry.registerSekiroHandler(new ActionHandler() {
                                @Override
                                public String action() {
                                    return "testAction";
                                }

                                @Override
                                public void handleRequest(SekiroRequest sekiroRequest, SekiroResponse sekiroResponse) {
                                    // 接口处理逻辑，我们不做任何处理，直接返回字符串：ok
                                    String params = new Gson().toJson(sekiroRequest);
                                    String pkg = sekiroRequest.getString("pkg");
                                    String sateKey = sekiroRequest.getString("stateKey");

                                    System.out.println("params:"+params);
                                    if (pkg != null) {
                                        HashMap<String, Object> state = use(proc);
                                        if (state != null) {
                                            sekiroResponse.success(state.get(sateKey));
                                            return;
                                        }
                                        sekiroResponse.success("not_found");
                                    }
                                    sekiroResponse.success("ok");
                                }
                            })
                    ).start();
        }).start();

    }


}
