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
package ch.algotrader.adapter.fix.fix44;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import ch.algotrader.util.MyLogger;
import quickfix.FieldNotFound;
import quickfix.SessionID;
import quickfix.field.MDReqID;
import quickfix.field.MDReqRejReason;
import quickfix.field.Text;
import quickfix.fix44.MarketDataRequestReject;
import quickfix.fix44.QuoteStatusReport;

/**
 * Base Fix4.4 market data message handler. Needs to be overwritten by specific broker interfaces.
 *
 * @author <a href="mailto:aflury@algotrader.ch">Andy Flury</a>
 *
 * @version $Revision: 6665 $ $Date: 2014-01-11 18:17:51 +0100 (Sa, 11 Jan 2014) $
 */
public abstract class AbstractFix44MarketDataMessageHandler extends AbstractFix44MessageHandler {

    private static Logger LOGGER = MyLogger.getLogger(AbstractFix44MarketDataMessageHandler.class.getName());

    public void onMessage(MarketDataRequestReject requestReject, SessionID sessionID) throws FieldNotFound {

        if (LOGGER.isEnabledFor(Level.WARN)) {

            StringBuilder buf = new StringBuilder();
            MDReqID reqID = requestReject.getMDReqID();
            buf.append("Subscription request for '").append(reqID.getValue()).append("' was rejected");

            if (requestReject.isSetField(Text.FIELD))  {

                buf.append("; reason given: ").append(requestReject.getText().getValue());
            } else if (requestReject.isSetField(MDReqRejReason.FIELD)) {

                buf.append("; code: ").append(requestReject.getMDReqRejReason().getValue());
            }

            LOGGER.warn(buf.toString());
        }
    }

    public void onMessage(QuoteStatusReport quoteStatusReport, SessionID sessionID) throws FieldNotFound {

        if (LOGGER.isTraceEnabled()) {

            LOGGER.trace("Quote status report: " + quoteStatusReport.getSymbol().getValue());
        }
    }

}
