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

import ch.algotrader.entity.trade.Fill;
import ch.algotrader.entity.trade.Order;
import ch.algotrader.entity.trade.OrderStatus;
import ch.algotrader.esper.EngineLocator;
import ch.algotrader.service.LookupService;
import ch.algotrader.util.MyLogger;
import quickfix.FieldNotFound;
import quickfix.SessionID;
import quickfix.field.ClOrdID;
import quickfix.field.OrigClOrdID;
import quickfix.field.Text;
import quickfix.fix44.ExecutionReport;
import quickfix.fix44.OrderCancelReject;

/**
 * Abstract FIX44 order message handler implementing generic functionality common to all broker specific
 * interfaces..
 *
 * @author <a href="mailto:okalnichevski@algotrader.ch">Oleg Kalnichevski</a>
 *
 * @version $Revision$ $Date$
 */
public abstract class AbstractFix44OrderMessageHandler extends AbstractFix44MessageHandler {

    private static Logger LOGGER = MyLogger.getLogger(AbstractFix44OrderMessageHandler.class.getName());

    private LookupService lookupService;

    public void setLookupService(final LookupService lookupService) {
        this.lookupService = lookupService;
    }

    public LookupService getLookupService() {
        return lookupService;
    }

    protected abstract boolean discardReport(ExecutionReport executionReport) throws FieldNotFound;

    protected abstract boolean isOrderRejected(ExecutionReport executionReport) throws FieldNotFound;

    protected abstract OrderStatus createStatus(ExecutionReport executionReport, Order order) throws FieldNotFound;

    protected abstract Fill createFill(ExecutionReport executionReport, Order order) throws FieldNotFound;

    public void onMessage(final ExecutionReport executionReport, final SessionID sessionID) throws FieldNotFound {

        if (discardReport(executionReport)) {

            return;
        }

        String intId = executionReport.getClOrdID().getValue();

        if (isOrderRejected(executionReport)) {

            if (LOGGER.isEnabledFor(Level.ERROR)) {

                StringBuilder buf = new StringBuilder();
                buf.append("Order with int ID ").append(intId).append(" has been rejected");
                if (executionReport.isSetField(Text.FIELD)) {

                    buf.append("; reason given: ").append(executionReport.getText().getValue());
                }
                LOGGER.error(buf.toString());
            }
            return;
        }

        // get the order from the OpenOrderWindow
        Order order = getLookupService().getOpenOrderByRootIntId(intId);
        if (order == null) {

            if (LOGGER.isEnabledFor(Level.ERROR)) {

                LOGGER.error("Order with int ID " + intId + " matching the execution report could not be found");
            }
            return;
        }

        OrderStatus orderStatus = createStatus(executionReport, order);
        orderStatus.setOrder(order);

        EngineLocator.instance().getBaseEngine().sendEvent(orderStatus);

        Fill fill = createFill(executionReport, order);
        if (fill != null) {

            // associate the fill with the order
            order.addFills(fill);

            EngineLocator.instance().getBaseEngine().sendEvent(fill);
        }
    }

    public void onMessage(final OrderCancelReject reject, final SessionID sessionID) throws FieldNotFound {

        if (LOGGER.isEnabledFor(Level.ERROR)) {
            StringBuilder buf = new StringBuilder();
            buf.append("Order cancel/replace has been rejected");
            if (reject.isSetField(ClOrdID.FIELD)) {
                String clOrdID = reject.getClOrdID().getValue();
                buf.append(" [order ID: ").append(clOrdID).append("]");
            }
            if (reject.isSetField(OrigClOrdID.FIELD)) {
                String origClOrdID = reject.getOrigClOrdID().getValue();
                buf.append(" [original order ID: ").append(origClOrdID).append("]");
            }
            if (reject.isSetField(Text.FIELD)) {
                String text = reject.getText().getValue();
                buf.append(": ").append(text);
            }
            LOGGER.error(buf.toString());
        }
    }

}
