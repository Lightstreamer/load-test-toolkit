package com.lightstreamer.load_test.client.utils;

import com.lightstreamer.oneway_client.ItemUpdate;
import com.lightstreamer.oneway_client.Subscription;
import com.lightstreamer.oneway_client.SubscriptionListener;

public class BaseSubscriptionListener implements SubscriptionListener {

    @Override
    public void onClearSnapshot(String itemName, int itemPos) {
    }

    @Override
    public void onCommandSecondLevelItemLostUpdates(int lostUpdates, String key) {
    }

    @Override
    public void onCommandSecondLevelSubscriptionError(int code, String message, String key) {
    }

    @Override
    public void onEndOfSnapshot(String itemName, int itemPos) {
    }

    @Override
    public void onItemLostUpdates(String itemName, int itemPos, int lostUpdates) {
    }

    @Override
    public void onItemUpdate(ItemUpdate itemUpdate) {
    }

    @Override
    public void onListenEnd(Subscription subscription) {
    }

    @Override
    public void onListenStart(Subscription subscription) {
    }

    @Override
    public void onSubscription() {
    }

    @Override
    public void onSubscriptionError(int code, String message) {
    }

    @Override
    public void onUnsubscription() {
    }

    @Override
    public void onRealMaxFrequency(String frequency) {
    }

}
