/***********************************************************************************
 * AlgoTrader Enterprise Trading Framework
 *
 * Copyright (C) 2013 Flury Trading - All rights reserved
 *
 * All information contained herein is, and remains the property of Flury Trading.
 * The intellectual and technical concepts contained herein are proprietary to
 * Flury Trading. Modification, translation, reverse engineering, decompilation,
 * disassembly or reproduction of this material is strictly forbidden unless prior
 * written permission is obtained from Flury Trading
 *
 * Fur detailed terms and conditions consult the file LICENSE.txt or contact
 *
 * Flury Trading
 * Badenerstrasse 16
 * 8004 Zurich
 ***********************************************************************************/
package ch.algotrader.adapter.fix;

import java.util.concurrent.atomic.AtomicReference;

import ch.algotrader.enumeration.ConnectionState;

/**
 * Default implementation of {@link FixSessionLifecycle} that keeps track of
 * FIX connection state state.
 *
 * @author <a href="mailto:okalnichevski@algotrader.ch">Oleg Kalnichevski</a>
 *
 * @version $Revision$ $Date$
 */
public class DefaultFixSessionLifecycle implements FixSessionLifecycle {

    private final AtomicReference<ConnectionState> connState;

    public DefaultFixSessionLifecycle() {

        this.connState = new AtomicReference<ConnectionState>(ConnectionState.DISCONNECTED);
    }

    @Override
    public void create() {

        this.connState.compareAndSet(ConnectionState.DISCONNECTED, ConnectionState.CONNECTED);
    }

    @Override
    public void logon() {

          this.connState.compareAndSet(ConnectionState.CONNECTED, ConnectionState.LOGGED_ON);
    }

    @Override
    public void logoff() {

        this.connState.set(ConnectionState.CONNECTED);
    }

    @Override
    public boolean subscribe() {

        return this.connState.compareAndSet(ConnectionState.LOGGED_ON, ConnectionState.SUBSCRIBED);
    }

    @Override
    public boolean isLoggedOn() {

        return this.connState.get().getValue() >= ConnectionState.LOGGED_ON.getValue();
    }

    @Override
    public boolean isSubscribed() {

        return this.connState.get().getValue() >= ConnectionState.SUBSCRIBED.getValue();
    }

    @Override
    public ConnectionState getConnectionState() {

        return this.connState.get();
    }
}
