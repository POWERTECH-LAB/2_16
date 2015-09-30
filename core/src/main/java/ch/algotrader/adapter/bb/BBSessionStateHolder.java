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
package ch.algotrader.adapter.bb;

import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang.Validate;

import ch.algotrader.adapter.DataFeedSessionStateHolder;
import ch.algotrader.adapter.ExternalSessionStateHolder;
import ch.algotrader.enumeration.ConnectionState;
import ch.algotrader.event.dispatch.EventDispatcher;
import ch.algotrader.vo.SessionEventVO;

/**
 * Default implementation of {@link ExternalSessionStateHolder} that keeps track of
 * FIX connection state state.
 *
 * @author <a href="mailto:okalnichevski@algotrader.ch">Oleg Kalnichevski</a>
 *
 * @version $Revision$ $Date$
 */
public class BBSessionStateHolder implements ExternalSessionStateHolder, DataFeedSessionStateHolder {

    private final String name;
    private final EventDispatcher eventDispatcher;
    private final AtomicReference<ConnectionState> connState;
    private final String feedType;

    public BBSessionStateHolder(final String name, final EventDispatcher eventDispatcher, final String feedType) {
        Validate.notEmpty(name, "Session name is null");
        Validate.notNull(eventDispatcher, "PlatformEventDispatcher is null");
        Validate.notEmpty(name, "Feed type is null");

        this.name = name;
        this.eventDispatcher = eventDispatcher;
        this.feedType = feedType;
        this.connState = new AtomicReference<>(ConnectionState.DISCONNECTED);
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void onCreate() {

        if (this.connState.compareAndSet(ConnectionState.DISCONNECTED, ConnectionState.CONNECTED)) {

            SessionEventVO event = new SessionEventVO(ConnectionState.CONNECTED, this.name);
            this.eventDispatcher.broadcast(event);
        }
    }

    @Override
    public void onLogon() {

          if (this.connState.compareAndSet(ConnectionState.CONNECTED, ConnectionState.LOGGED_ON)) {

              SessionEventVO event = new SessionEventVO(ConnectionState.LOGGED_ON, this.name);
              this.eventDispatcher.broadcast(event);
          }
    }

    @Override
    public void onLogoff() {

        ConnectionState previousState = this.connState.getAndSet(ConnectionState.CONNECTED);
        if (previousState.compareTo(ConnectionState.LOGGED_ON) >= 0) {

            SessionEventVO event = new SessionEventVO(ConnectionState.CONNECTED, this.name);
            this.eventDispatcher.broadcast(event);
        }
    }

    @Override
    public boolean onSubscribe() {

        if (this.connState.compareAndSet(ConnectionState.LOGGED_ON, ConnectionState.SUBSCRIBED)) {

            SessionEventVO event = new SessionEventVO(ConnectionState.SUBSCRIBED, this.name);
            this.eventDispatcher.broadcast(event);
            return true;
        } else {

            return false;
        }
    }

    @Override
    public boolean isLoggedOn() {

        return this.connState.get().compareTo(ConnectionState.LOGGED_ON) >= 0;
    }

    @Override
    public boolean isSubscribed() {

        return this.connState.get().compareTo(ConnectionState.SUBSCRIBED) >= 0;
    }

    @Override
    public ConnectionState getConnectionState() {

        return this.connState.get();
    }

    @Override
    public String getFeedType() {
        return this.feedType;
    }

}
