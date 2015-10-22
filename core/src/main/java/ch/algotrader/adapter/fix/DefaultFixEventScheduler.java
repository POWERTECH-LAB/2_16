/***********************************************************************************
 * AlgoTrader Enterprise Trading Framework
 *
 * Copyright (C) 2015 AlgoTrader GmbH - All rights reserved
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
 * Aeschstrasse 6
 * 8834 Schindellegi
 ***********************************************************************************/
package ch.algotrader.adapter.fix;

import ch.algotrader.esper.Engine;
import quickfix.Session;
import quickfix.SessionID;

/**
 * Default {@link FixEventScheduler} implementation based on Esper {@link ch.algotrader.esper.Engine} .
 *
 * @author <a href="mailto:aflury@algotrader.ch">Andy Flury</a>
 * @version $Revision$ $Date$
 */
public class DefaultFixEventScheduler implements FixEventScheduler {

    private final Engine serverEngine;

    public DefaultFixEventScheduler(final Engine serverEngine) {
        this.serverEngine = serverEngine;
    }

    @Override
    public void scheduleLogon(final SessionID sessionId, final EventPattern eventTime) {

        Object[] logonParams = {eventTime.getMinute(), eventTime.getHour(), eventTime.getDay(), eventTime.getSecond()};

        this.serverEngine.deployStatement("server-prepared", "FIX_SESSION", sessionId.getSessionQualifier() + "_LOGON", logonParams, new Object() {
            @SuppressWarnings("unused")
            public void update() {
                Session session = Session.lookupSession(sessionId);
                session.logon();
            }
        });
    }

    @Override
    public void scheduleLogout(final SessionID sessionId, final EventPattern eventTime) {

        Object[] logoutParams = {eventTime.getMinute(), eventTime.getHour(), eventTime.getDay(), eventTime.getSecond()};

        this.serverEngine.deployStatement("server-prepared", "FIX_SESSION", sessionId.getSessionQualifier() + "_LOGOUT", logoutParams, new Object() {
            @SuppressWarnings("unused")
            public void update() {
                Session session = Session.lookupSession(sessionId);
                session.logout();
            }
        });
    }
}
