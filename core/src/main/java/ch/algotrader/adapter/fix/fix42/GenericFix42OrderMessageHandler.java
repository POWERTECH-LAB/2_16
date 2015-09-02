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
package ch.algotrader.adapter.fix.fix42;

import java.math.BigDecimal;
import java.util.Date;

import ch.algotrader.adapter.fix.FixUtil;
import ch.algotrader.entity.trade.Fill;
import ch.algotrader.entity.trade.Order;
import ch.algotrader.entity.trade.OrderStatus;
import ch.algotrader.enumeration.Side;
import ch.algotrader.enumeration.Status;
import ch.algotrader.esper.Engine;
import ch.algotrader.ordermgmt.OpenOrderRegistry;
import ch.algotrader.util.PriceUtil;
import quickfix.FieldNotFound;
import quickfix.field.AvgPx;
import quickfix.field.CumQty;
import quickfix.field.ExecType;
import quickfix.field.LastPx;
import quickfix.field.MsgSeqNum;
import quickfix.field.TransactTime;
import quickfix.fix42.ExecutionReport;

/**
 * Generic Fix42OrderMessageHandler. Needs to be overwritten by specific broker interfaces.
 *
 * @author <a href="mailto:aflury@algotrader.ch">Andy Flury</a>
 *
 * @version $Revision$ $Date$
 */
public class GenericFix42OrderMessageHandler extends AbstractFix42OrderMessageHandler {

    public GenericFix42OrderMessageHandler(final OpenOrderRegistry openOrderRegistry, final Engine serverEngine) {
        super(openOrderRegistry, serverEngine);
    }

    @Override
    protected boolean discardReport(final ExecutionReport executionReport) throws FieldNotFound {
        // ignore PENDING_NEW, PENDING_CANCEL and PENDING_REPLACE
        ExecType execType = executionReport.getExecType();
        if (execType.getValue() == ExecType.PENDING_NEW || execType.getValue() == ExecType.PENDING_REPLACE
                || execType.getValue() == ExecType.PENDING_CANCEL) {

            return true;
        } else {

            return false;
        }
    }

    @Override
    protected boolean isOrderRejected(final ExecutionReport executionReport) throws FieldNotFound {
        ExecType execType = executionReport.getExecType();
        return execType.getValue() == ExecType.REJECTED;
    }

    @Override
    protected boolean isOrderReplaced(final ExecutionReport executionReport) throws FieldNotFound {
        ExecType execType = executionReport.getExecType();
        return execType.getValue() == ExecType.REPLACE;
    }

    @Override
    protected OrderStatus createStatus(final ExecutionReport executionReport, final Order order) throws FieldNotFound {
        // get the other fields
        ExecType execType = executionReport.getExecType();
        Status status = getStatus(execType, executionReport.getCumQty());
        long filledQuantity = (long) executionReport.getCumQty().getValue();
        long remainingQuantity = (long) (executionReport.getOrderQty().getValue() - executionReport.getCumQty().getValue());
        String intId = order.getIntId() != null ? order.getIntId(): executionReport.getClOrdID().getValue();
        String extId = executionReport.getOrderID().getValue();

        // assemble the orderStatus
        OrderStatus orderStatus = OrderStatus.Factory.newInstance();
        orderStatus.setStatus(status);
        orderStatus.setExtId(extId);
        orderStatus.setIntId(intId);
        orderStatus.setSequenceNumber(executionReport.getHeader().getInt(MsgSeqNum.FIELD));
        orderStatus.setFilledQuantity(filledQuantity);
        orderStatus.setRemainingQuantity(remainingQuantity);
        orderStatus.setOrder(order);
        if (executionReport.isSetField(TransactTime.FIELD)) {

            orderStatus.setExtDateTime(executionReport.getTransactTime().getValue());
        }
        if (executionReport.isSetField(LastPx.FIELD)) {

            double lastPrice = executionReport.getLastPx().getValue();
            if (lastPrice != 0.0) {
                orderStatus.setLastPrice(PriceUtil.normalizePrice(order, lastPrice));
            }
        }
        if (executionReport.isSetField(AvgPx.FIELD)) {

            double avgPrice = executionReport.getAvgPx().getValue();
            if (avgPrice != 0.0) {
                orderStatus.setAvgPrice(PriceUtil.normalizePrice(order, avgPrice));
            }
        }

        return orderStatus;
    }

    @Override
    protected Fill createFill(final ExecutionReport executionReport, final Order order) throws FieldNotFound {

        // only create fills if status is PARTIALLY_FILLED or FILLED
        ExecType execType = executionReport.getExecType();
        if (execType.getValue() == ExecType.PARTIAL_FILL || execType.getValue() == ExecType.FILL) {

            // get the fields
            Side side = FixUtil.getSide(executionReport.getSide());
            long quantity = (long) executionReport.getLastShares().getValue();
            BigDecimal price = PriceUtil.normalizePrice(order, executionReport.getLastPx().getValue());
            String extId = executionReport.getExecID().getValue();

            // assemble the fill
            Fill fill = new Fill();
            fill.setDateTime(new Date());
            fill.setExtId(extId);
            fill.setSequenceNumber(executionReport.getHeader().getInt(MsgSeqNum.FIELD));
            fill.setSide(side);
            fill.setQuantity(quantity);
            fill.setPrice(price);
            if (executionReport.isSetField(TransactTime.FIELD)) {
                fill.setExtDateTime(executionReport.getTransactTime().getValue());
            }
            fill.setOrder(order);

            return fill;
        } else {

            return null;
        }
    }

    private Status getStatus(ExecType execType, CumQty cumQty) {

        if (execType.getValue() == ExecType.NEW) {
            return Status.SUBMITTED;
        } else if (execType.getValue() == ExecType.PARTIAL_FILL) {
            return Status.PARTIALLY_EXECUTED;
        } else if (execType.getValue() == ExecType.FILL) {
            return Status.EXECUTED;
        } else if (execType.getValue() == ExecType.CANCELED || execType.getValue() == ExecType.REJECTED
                || execType.getValue() == ExecType.DONE_FOR_DAY || execType.getValue() == ExecType.EXPIRED) {
            return Status.CANCELED;
        } else if (execType.getValue() == ExecType.REPLACE) {
            if (cumQty.getValue() == 0) {
                return Status.SUBMITTED;
            } else {
                return Status.PARTIALLY_EXECUTED;
            }
        } else {
            throw new IllegalArgumentException("unknown execType " + execType.getValue());
        }
    }
}
