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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.bloomberglp.blpapi.Event;
import com.bloomberglp.blpapi.EventHandler;
import com.bloomberglp.blpapi.Message;
import com.bloomberglp.blpapi.Session;

/**
 * Abstract Bloomberg MessageHandler.
 *
 * @author <a href="mailto:aflury@algotrader.ch">Andy Flury</a>
 *
 * @version $Revision: 5941 $ $Date: 2013-05-31 13:23:59 +0200 (Fr, 31 Mai 2013) $
 */
public abstract class BBMessageHandler implements EventHandler {

    private static final Logger LOGGER = LogManager.getLogger(BBMessageHandler.class);

    private final Object lock = new Object();
    private boolean running;

    @Override
    public void processEvent(Event event, Session session) {

        try {
            internalProcessEvent(session, event);
        } catch (Exception e) {
            LOGGER.error("problem processing event", e);
            for (Message msg : event) {
                LOGGER.error("correlationID: {}, {}", msg.correlationID().value(), msg);
            }
        }
    }

    public boolean processEvent(Session session) throws InterruptedException {

        Event event = session.nextEvent();

        return internalProcessEvent(session, event);
    }

    private boolean internalProcessEvent(Session session, Event event) {

        switch (event.eventType().intValue()) {
            case Event.EventType.Constants.SESSION_STATUS:
                synchronized (this.lock) {
                    processSessionStatus(event, session);
                }
                return false;
            case Event.EventType.Constants.SERVICE_STATUS:
                synchronized (this.lock) {
                    processServiceStatus(event, session);
                }
                return false;
            case Event.EventType.Constants.ADMIN:
                synchronized (this.lock) {
                    processAdminEvent(event, session);
                }
                return false;
            case Event.EventType.Constants.PARTIAL_RESPONSE:
                processResponseEvent(event, session);
                return false;
            case Event.EventType.Constants.RESPONSE:
                processResponseEvent(event, session);
                return true;
            case Event.EventType.Constants.SUBSCRIPTION_STATUS:
                synchronized (this.lock) {
                    processSubscriptionStatus(event, session);
                }
                return false;
            case Event.EventType.Constants.SUBSCRIPTION_DATA:
                processSubscriptionDataEvent(event, session);
                return false;
            default:
                processMiscEvents(event, session);
                return false;
        }
    }

    private void processSessionStatus(Event event, Session session) {

        for (Message msg : event) {

            if (msg.messageType() == BBConstants.SESSION_CONNECTION_UP) {
                LOGGER.info("session connection up");
            } else if (msg.messageType() == BBConstants.SESSION_CONNECTION_DOWN) {
                LOGGER.info("session connection down");
            } else if (msg.messageType() == BBConstants.SESSION_STARTED) {
                this.running = true;
                LOGGER.info("session started");
            } else if (msg.messageType() == BBConstants.SESSION_TERMINATED) {
                this.running = false;
                LOGGER.info("session terminated");
            } else if (msg.messageType() == BBConstants.SESSION_STARTUP_FAILURE) {
                LOGGER.error(msg);
            } else {
                throw new IllegalStateException("unknown messageType " + msg.messageType());
            }
        }
    }

    private void processServiceStatus(Event event, Session session) {

        for (Message msg : event) {
            if (msg.messageType() == BBConstants.SERVICE_OPENED) {
                String serviceName = msg.getElementAsString("serviceName");
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("service has been opened {}", serviceName);
                }
            } else {
                throw new IllegalArgumentException("unknown msgType " + msg.messageType());
            }
        }
    }

    private void processAdminEvent(Event event, Session session) {

        for (Message msg : event) {
            LOGGER.info(msg);
        }
    }

    private void processMiscEvents(Event event, Session session) {

        for (Message msg : event) {
            throw new IllegalArgumentException("unknown msgType " + msg.messageType());
        }
    }

    public boolean isRunning() {
        return this.running;
    }

    protected void processSubscriptionStatus(Event event, Session session) {

        // To be overwritten by subclasses
    }

    protected void processSubscriptionDataEvent(Event event, Session session) {

        // To be overwritten by subclasses
    }

    protected void processResponseEvent(Event event, Session session) {

        // To be overwritten by subclasses
    }
}
