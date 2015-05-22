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
package ch.algotrader.adapter.ib;

import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.algotrader.enumeration.ConnectionState;
import ch.algotrader.enumeration.FeedType;
import ch.algotrader.event.dispatch.EventDispatcher;
import ch.algotrader.service.MarketDataService;
import ch.algotrader.vo.SessionEventVO;

/**
 * @author <a href="mailto:aflury@algotrader.ch">Andy Flury</a>
 *
 * @version $Revision$ $Date$
 */
public class DefaultIBSessionStateHolder implements IBSessionStateHolder {

    private static final Logger logger = LogManager.getLogger(DefaultIBSessionStateHolder.class.getName());

    private final String name;
    private final EventDispatcher eventDispatcher;
    private final MarketDataService marketDataService;
    private final AtomicReference<ConnectionState> connState;

    public DefaultIBSessionStateHolder(final String name, final EventDispatcher eventDispatcher, final MarketDataService marketDataService) {

        Validate.notEmpty(name, "Session name is null");
        Validate.notNull(eventDispatcher, "PlatformEventDispatcher is null");
        Validate.notNull(marketDataService, "MarketDataService is null");

        this.name = name;
        this.eventDispatcher = eventDispatcher;
        this.marketDataService = marketDataService;
        this.connState = new AtomicReference<>(ConnectionState.DISCONNECTED);
    }

    @Override
    public void onConnect() {

        if (compareAndSet(ConnectionState.DISCONNECTED, ConnectionState.CONNECTED)) {

            SessionEventVO event = new SessionEventVO(ConnectionState.CONNECTED, this.name);
            this.eventDispatcher.broadcast(event);
        };
    }

    @Override
    public void onDisconnect() {

        ConnectionState previousState = this.connState.getAndSet(ConnectionState.DISCONNECTED);
        if (previousState.compareTo(ConnectionState.DISCONNECTED) > 0) {

            this.connState.set(ConnectionState.DISCONNECTED);
            logger.debug("change state to " + ConnectionState.DISCONNECTED);

            SessionEventVO event = new SessionEventVO(ConnectionState.DISCONNECTED, this.name);
            this.eventDispatcher.broadcast(event);
        }

    }

    /**
     * logon with subscriptions either maintained or not-maintained
     * returns true if a re-subscription is necessary
     */
    @Override
    public void onLogon(boolean maintained) {

        if (onLogon0(maintained)) {

            SessionEventVO event = new SessionEventVO(ConnectionState.LOGGED_ON, this.name);
            this.eventDispatcher.broadcast(event);

            if (this.marketDataService.isSupportedFeed(FeedType.IB)) {
                this.marketDataService.initSubscriptions(FeedType.IB);
            }
        }
    }

    private synchronized boolean onLogon0(boolean maintained) {

        if (maintained) {

            // if we have previously been IDLE no need to re-subscribe
            if (compareAndSet(ConnectionState.IDLE, ConnectionState.SUBSCRIBED)) {
                return false;
            }

            // if we have been CONNECTED we need to resubscribe
            return compareAndSet(ConnectionState.CONNECTED, ConnectionState.LOGGED_ON);

        } else {

            // re-subscribe if we have been previously CONNECTED or IDLE
            return (compareAndSet(ConnectionState.IDLE, ConnectionState.LOGGED_ON) || compareAndSet(ConnectionState.CONNECTED, ConnectionState.LOGGED_ON));
        }
    }

    @Override
    public void onLogoff() {

        if (onLogoff0()) {

            SessionEventVO event = new SessionEventVO(ConnectionState.CONNECTED, this.name);
            this.eventDispatcher.broadcast(event);
        };
    }

    private synchronized boolean onLogoff0() {

        return compareAndSet(ConnectionState.LOGGED_ON, ConnectionState.CONNECTED) || compareAndSet(ConnectionState.SUBSCRIBED, ConnectionState.IDLE);
    }

    /**
     * returns true if there is a need to subscribe
     */
    @Override
    public boolean subscribe() {

        if (compareAndSet(ConnectionState.LOGGED_ON, ConnectionState.SUBSCRIBED)) {

            SessionEventVO event = new SessionEventVO(ConnectionState.SUBSCRIBED, this.name);
            this.eventDispatcher.broadcast(event);
            return true;
        } else {

            return false;
        }
    }

    @Override
    public ConnectionState getConnectionState() {

        return this.connState.get();
    }

    @Override
    public boolean isConnected() {

        return this.connState.get().getValue() >= ConnectionState.CONNECTED.getValue();
    }

    @Override
    public boolean isLoggedOn() {

        return this.connState.get().getValue() >= ConnectionState.LOGGED_ON.getValue();
    }

    @Override
    public boolean isSubscribed() {

        return this.connState.get().getValue() >= ConnectionState.SUBSCRIBED.getValue();
    }

    private boolean compareAndSet(ConnectionState before, ConnectionState after) {

        boolean success = this.connState.compareAndSet(before, after);

        if (success) {
            logger.debug("change state from " + before + " to " + after);
        }

        return success;
    }
}

