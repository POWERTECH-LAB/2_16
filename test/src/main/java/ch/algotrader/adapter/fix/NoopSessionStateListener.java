/***********************************************************************************
 * AlgoTrader Enterprise Trading Framework
 *
 * Copyright (C) 2014 AlgoTrader GmbH - All rights reserved
 *
 * All information contained herein is, and remains the property of AlgoTrader GmbH.
 * The intellectual and technical concepts contained herein are proprietary to
 * AlgoTrader GmbH. Modification, translation, reverse engineering, decompilation,
 * disassembly or reproduction of this material is strictly forbidden unless prior
 * written permission is obtained from AlgoTrader GmbH
 *
 * Fur detailed terms and conditions consult the file LICENSE.txt or contact
 *
 * AlgoTrader GmbH
 * Badenerstrasse 16
 * 8004 Zurich
 ***********************************************************************************/
package ch.algotrader.adapter.fix;

import quickfix.SessionStateListener;

public class NoopSessionStateListener implements SessionStateListener {

    @Override
    public void onConnect() {
    }

    @Override
    public void onDisconnect() {
    }

    @Override
    public void onLogon() {
    }

    @Override
    public void onLogout() {
    }

    @Override
    public void onReset() {
    }

    @Override
    public void onRefresh() {
    }

    @Override
    public void onMissedHeartBeat() {
    }

    @Override
    public void onHeartBeatTimeout() {
    }

}
