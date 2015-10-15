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

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.DisposableBean;

import com.ib.client.EClientSocket;

import ch.algotrader.enumeration.ConnectionState;
import ch.algotrader.enumeration.InitializingServiceType;
import ch.algotrader.service.InitializationPriority;
import ch.algotrader.service.InitializingServiceI;

/**
 * Represents on IB (socket) connection.
 *
 * @author <a href="mailto:aflury@algotrader.ch">Andy Flury</a>
 *
 * @version $Revision$ $Date$
 */
@InitializationPriority(value = InitializingServiceType.BROKER_INTERFACE)
public final class IBSession extends EClientSocket implements InitializingServiceI, DisposableBean {

    private static final Logger LOGGER = LogManager.getLogger(IBSession.class);

    private final int clientId;
    private final String host;
    private final int port;
    private final IBSessionStateHolder sessionStateHolder;
    private final AtomicBoolean terminated;

    public IBSession(final int clientId, final String host, final int port, final IBSessionStateHolder sessionStateHolder, final AbstractIBMessageHandler messageHandler) {

        super(messageHandler);

        Validate.notNull(host, "host may not be null");
        Validate.isTrue(port != 0, "port may not be 0");
        Validate.notNull(sessionStateHolder, "IBSessionStateHolder is null");

        this.clientId = clientId;
        this.host = host;
        this.port = port;
        this.sessionStateHolder = sessionStateHolder;
        this.terminated = new AtomicBoolean(false);
    }

    @Override
    public void init() {
        connect();
    }

    @Override
    public void destroy() {

        shutdown();
    }

    public void shutdown() {

        if (this.terminated.compareAndSet(false, true)) {
            if (isConnected()) {
                eDisconnect();
            }
        }
    }

    public boolean isTerminated() {

        return this.terminated.get();
    }

    public int getClientId() {
        return this.clientId;
    }

    public boolean isLoggedOn() {
        return this.sessionStateHolder.isLoggedOn();
    }

    public ConnectionState getConnectionState() {
        return this.sessionStateHolder.getConnectionState();
    }

    /**
     * (re)connects to TWS / IB Gateway
     */
    public void connect() {

        if (isConnected()) {
            eDisconnect();

            sleep();
        }

        waitAndConnect();

        if (isConnected() && !isTerminated()) {
            this.sessionStateHolder.onConnect();

            // in case there is no 2104 message from the IB Gateway (Market data farm connection is OK)
            // manually invoke initSubscriptions after some time if there is marketDataService
            this.sessionStateHolder.onLogon(true);
        }
    }

    /**
     * disconnects from TWS / IB Gateway
     */
    public void disconnect() {

        if (isConnected()) {
            eDisconnect();
            this.sessionStateHolder.onDisconnect();
        }
    }

    private void sleep() {

        try {
            Thread.sleep(10000);
        } catch (InterruptedException e1) {
            try {
                // during eDisconnect this thread get's interrupted so sleep again
                Thread.sleep(10000);
            } catch (InterruptedException e2) {
                LOGGER.error("problem sleeping", e2);
            }
        }
    }

    private void waitAndConnect() {

        while (!isTerminated()) {

            Socket socket = new Socket();
            try {
                socket.connect(new InetSocketAddress(this.host, this.port), 5000);
                eConnect(socket, this.clientId);
                return;
            } catch (ConnectException e) {
                // do nothing, gateway is down
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("please start IB Gateway / TWS on port: {}", this.port);
                }
            } catch (IOException e) {
                LOGGER.error("connection error", e);
            }
            try {
                socket.close();
            } catch (IOException ignore) {
            }

            sleep();
        }

    }

}
