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
package ch.algotrader.adapter.bb;

import com.bloomberglp.blpapi.Service;
import com.bloomberglp.blpapi.Session;
import com.bloomberglp.blpapi.SessionOptions;

/**
 * Represents a Bloomberg Session
 *
 * @author <a href="mailto:andyflury@gmail.com">Andy Flury</a>
 *
 * @version $Revision: 5941 $ $Date: 2013-05-31 13:23:59 +0200 (Fr, 31 Mai 2013) $
 */
public final class BBSession extends Session {

    private Service service;
    private String serviceName;
    private BBMessageHandler messageHandler;

    public BBSession(String serviceName, SessionOptions sessionOptions, BBMessageHandler messageHandler) {

        super(sessionOptions, messageHandler);
        this.serviceName = serviceName;
        this.messageHandler = messageHandler;
    }

    public BBSession(String serviceName, SessionOptions sessionOptions) {

        super(sessionOptions);
        this.serviceName = serviceName;
    }

    public Service getService() {

        if (this.service == null) {
            this.service = getService("//blp/" + this.serviceName);
        }

        return this.service;
    }

    public String getServiceName() {

        return this.serviceName;
    }

    public BBMessageHandler getMessageHandler() {

        return this.messageHandler;
    }

    // returns true if the session has not received a SESSION_TERMINATED event (and there is a messageHandler)
    public boolean isRunning() {

        return this.messageHandler != null && this.messageHandler.isRunning();
    }
}
