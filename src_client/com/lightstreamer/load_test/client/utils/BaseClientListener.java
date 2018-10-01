package com.lightstreamer.load_test.client.utils;

import com.lightstreamer.oneway_client.ClientListener;
import com.lightstreamer.oneway_client.LightstreamerClient;

public class BaseClientListener implements ClientListener {

    @Override
    public void onListenEnd(LightstreamerClient client) {
    }

    @Override
    public void onListenStart(LightstreamerClient client) {
    }

    @Override
    public void onServerError(int errorCode, String errorMessage) {
    }

    @Override
    public void onStatusChange(String status) {
    }

    @Override
    public void onPropertyChange(String property) {
    }

}
