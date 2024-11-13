package cn.codec.server.utils;


import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Network {
    public interface HttpCallback {
        Object done(Call call, Response response, IOException e) throws IOException;
    }

    private static OkHttpClient _client = null;

    public static synchronized OkHttpClient getInstance() {
        if (_client == null) {
            _client = new OkHttpClient();
        }
        return _client;
    }

    public static void postJson(String url, String json, HttpCallback callback) {
        RequestBody body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), json);

        Request request = new Request.Builder().url(url).header("Accept", "application/json").post(body).build();
        Network.getInstance().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                try {
                    callback.done(call, null, e);
                } catch (Exception ex) {
                    Helper.log("HttpHelper|", e.getMessage());
                    e.printStackTrace();
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                callback.done(call, response, null);
            }
        });
    }
}
