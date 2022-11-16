/*
 *  Copyright (c) Lightstreamer Srl
 *  
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *      https://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */


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
