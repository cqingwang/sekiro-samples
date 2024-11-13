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
            HashMap<String, Action> handler = build(lp);
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

    public HashMap<String, Action> build(XC_LoadPackage.LoadPackageParam lp) {

        HashMap<String, Action> handlers = new HashMap<>();

        switch (lp.packageName) {
            case Pair.ddmc: {
                handlers.put("live", (request, response) -> {
                    Pair<String, Object> json = new Pair<String, Object>();
                    json.put("id", getClientId());
                    json.put("status", "done");
                    json.put("ext", null);
                    response.success(json.toString());
                });

                handlers.put("report", (request, response) -> {
                    Pair<String, Object> json = new Pair<String, Object>();
                    json.put("id", getClientId());
                    json.put("status", "done");
                    json.put("ext", Helper.sp_read(lp));

                    response.success(json.toString());
                });

            }
        }

        return handlers;
    }

    public String getClientId() {
        String tmpid = new Date().getTime() + "";
        String loaded = Helper.sp_get(lp, Pair.clientId, tmpid);
        Helper.log("clientId->", loaded);
        if (loaded.equals(tmpid)) Helper.sp_put(lp, Pair.clientId, loaded);
        return loaded;
    }

    public void setClientId(String clientId) {
        if (clientId == null || clientId.trim().length() == 0) return;
        clientId = clientId.trim();
        String pre = getClientId();
        if (!clientId.equals(pre)) {
            Helper.log("setClientId:", clientId);
            Helper.sp_put(lp, Pair.clientId, clientId);
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
