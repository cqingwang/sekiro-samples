package cn.codec.server.rpcserver;

import java.util.Date;
import java.util.HashMap;

import cn.codec.server.utils.Helper;
import cn.codec.server.utils.Pair;
import cn.iinti.sekiro3.business.api.SekiroClient;
import cn.iinti.sekiro3.business.api.interfaze.ActionHandler;
import cn.iinti.sekiro3.business.api.interfaze.SekiroRequest;
import cn.iinti.sekiro3.business.api.interfaze.SekiroResponse;

public abstract class Channel {
    public static final String clientId = "clientId";

    private final String packageName;
    private final String host;
    private final int port;
    private boolean running = false;
    private SekiroClient client = null;

    public interface Action {
        void doTask(SekiroRequest request, SekiroResponse response);
    }

    public Channel(String packageName, String host, int port) {
        this.packageName = packageName;
        this.host = host;
        this.port = port;
    }

    public SekiroClient newClient() {
        SekiroClient newClient = new SekiroClient(packageName, getClientId(), host, port);
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

    public abstract String presister(String key, String sec);

    public abstract void storage(String key, String value);

    public abstract String read();

    public HashMap<String, Action> build() {
        HashMap<String, Action> handlers = new HashMap<>();
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
            json.put("ext", read());

            response.success(json.toString());
        });
        return handlers;
    }

    public String getClientId() {
        String tmpid = new Date().getTime() + "";
        String loaded = presister(clientId, tmpid);
        Helper.log("clientId->", loaded);
        if (loaded.equals(tmpid)) storage(clientId, loaded);
        return loaded;
    }

    public void setClientId(String clientId) {
        if (clientId == null || clientId.trim().length() == 0) return;
        clientId = clientId.trim();
        String pre = getClientId();
        if (!clientId.equals(pre)) {
            Helper.log("setClientId:", clientId);
            storage(clientId, clientId);
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
        Helper.log("Starting ...");
        running = true;
        new Thread(() -> {
            this.client = newClient();
            this.client.start();
        }).start();
    }
}
