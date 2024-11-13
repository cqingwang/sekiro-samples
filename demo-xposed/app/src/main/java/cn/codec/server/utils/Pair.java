package cn.codec.server.utils;

import android.support.annotation.NonNull;

import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Map;

public class Pair<K, V> extends HashMap<K, V> {
    public static final String presister = "mod_presister.txt";

    public static final String clientId = "clientId";
    public static final String host = "m.olless.com";
    public static final int port = 8010;

    public static final String ddmc = "com.xunmeng.station";

    Pair<K, V> add(K k, V v) {
        put(k, v);
        return this;
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }


    public static Pair from(String json) {
        return new Gson().fromJson(json, Pair.class);
    }


    public Pair<K, V> merge(String json) {
        putAll(from(json));
        return this;
    }


}
