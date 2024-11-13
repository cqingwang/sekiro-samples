package cn.codec.server.utils;

import java.util.Date;
import java.util.HashMap;

import cn.iinti.sekiro3.business.api.SekiroClient;
import cn.iinti.sekiro3.business.api.interfaze.ActionHandler;
import cn.iinti.sekiro3.business.api.interfaze.SekiroRequest;
import cn.iinti.sekiro3.business.api.interfaze.SekiroResponse;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class Channel {
    private final XC_LoadPackage.LoadPackageParam lp;
    private boolean running = false;
    private SekiroClient client = null;

    interface Action {
        void doTask(SekiroRequest request, SekiroResponse response);
    }

    public Channel(XC_LoadPackage.LoadPackageParam lp) {
        this.lp = lp;
    }

    public SekiroClient newClient() {
        SekiroClient newClient = new SekiroClient(lp.packageName, getClientId(), Pair.host, Pair.port);
        newClient.setupSekiroRequestInitializer((sekiroRequest, registry) -> {
            HashMap<String, Action> handler = build();
            for (String action : handler.keySet()) {
                registry.registerSekiroHandler(new ActionHandler() {
                    @Override
                    public String action() {
                        return action;
                    }

                    @Override
                    public void handleRequest(SekiroRequest r, SekiroResponse p) {
                        handler.get(action).doTask(r, p);
                    }
                });
            }
        });
        return newClient;
    }

    public HashMap<String, Action> build() {
        HashMap<String, Action> handlers = new HashMap<>();
        handlers.put("live", (request, response) -> {
            Pair<String, Object> json = new Pair<String, Object>();
            json.put("id", getClientId());
            json.put("name", lp.packageName);
            switch (lp.packageName) {
                case Pair.ddmc: {
                    json.put("status", "done");
                    response.success(json.toString());
                    return;
                }
                default: {
                    json.put("status", "not_match");
                    response.success(json.toString());
                }
            }
        });
        return handlers;
    }

    public void lp_put(String key, String value) {
        Helper.sp_put(this.lp, key, value);
    }

    public String lp_get(String key, String sec) {
        return Helper.sp_get(this.lp, key, sec);
    }

    public String getClientId() {
        String loaded = lp_get(Pair.clientId, null);
        if (loaded != null && loaded.length() > 0) {
            Helper.log("clientId->[real]", loaded);
            return loaded;
        }
        String tmpid = new Date().getTime() + "";
        Helper.log("clientId->[tmp]", tmpid);
        lp_put(Pair.clientId, tmpid);
        return tmpid;
    }

    public void setClientId(String clientId) {
        String pre = getClientId();
        lp_put(Pair.clientId, clientId);
        if (pre != null && !pre.equals(clientId)) {
            restart();
        }
    }

    private void stop() {
        Helper.log("Stopping");
        if (!running || this.client == null) return;
        try {
            this.client.destroy(3);
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.client = null;
        this.running = false;
    }

    private void restart() {
        stop();
        start();
    }

    public void start() {
        if (running) {
            Helper.log("Already running");
            return;
        }
        Helper.log("Starting...");
        running = true;
        new Thread(() -> {
            this.client = newClient();
            this.client.start();
        }).start();
    }
}
