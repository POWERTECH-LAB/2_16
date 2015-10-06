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
package ch.algotrader.adapter.fix.fix44;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import ch.algotrader.adapter.fix.FixUtil;
import ch.algotrader.entity.trade.Fill;
import ch.algotrader.entity.trade.Order;
import ch.algotrader.entity.trade.OrderStatus;
import ch.algotrader.enumeration.Side;
import ch.algotrader.enumeration.Status;
import ch.algotrader.util.RoundUtil;
import quickfix.FieldNotFound;
import quickfix.field.AvgPx;
import quickfix.field.ExecType;
import quickfix.field.LastPx;
import quickfix.field.MsgSeqNum;
import quickfix.field.TransactTime;
import quickfix.fix44.ExecutionReport;

/**
 * Generic Fix44OrderMessageHandler. Can still be overwritten by specific broker interfaces.
 *
 * @author <a href="mailto:okalnichevski@algotrader.ch">Oleg Kalnichevski</a>
 *
 * @version $Revision$ $Date$
 */
public class GenericFix44OrderMessageHandler extends AbstractFix44OrderMessageHandler {

    private final Map<Integer, Double> priceMultiplierMap = new HashMap<Integer, Double>();

    public GenericFix44OrderMessageHandler() {

        String priceMultipliers = System.getProperty("misc.priceMultipliers");
        if (priceMultipliers != null && !"".equals(priceMultipliers)) {
            for (String priceMultiplier : priceMultipliers.split(",")) {
                int securityFamilyId = Integer.parseInt(priceMultiplier.split(":")[0]);
                double multiplier = Double.parseDouble(priceMultiplier.split(":")[1]);
                this.priceMultiplierMap.put(securityFamilyId, multiplier);
            }
        }
    }

    @Override
    protected boolean discardReport(final ExecutionReport executionReport) throws FieldNotFound {

        // ignore PENDING_NEW, PENDING_CANCEL and PENDING_REPLACE
        ExecType execType = executionReport.getExecType();

        if (execType.getValue() == ExecType.PENDING_NEW
                || execType.getValue() == ExecType.PENDING_REPLACE
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
    protected OrderStatus createStatus(final ExecutionReport executionReport, final Order order) throws FieldNotFound {

        ExecType execType = executionReport.getExecType();
        long orderQty;
        if (executionReport.isSetOrderQty()) {
            orderQty = (long) executionReport.getOrderQty().getValue();
        } else {
            orderQty = order.getQuantity();
        }
        long cumQty = (long) executionReport.getCumQty().getValue();

        Status status = getStatus(execType, orderQty, cumQty);
        long remainingQuantity = orderQty - cumQty;
        String extId = executionReport.getOrderID().getValue();
        String intId = executionReport.getClOrdID().getValue();

        // assemble the orderStatus
        OrderStatus orderStatus = OrderStatus.Factory.newInstance();
        orderStatus.setStatus(status);
        orderStatus.setExtId(extId);
        orderStatus.setIntId(intId);
        orderStatus.setSequenceNumber(executionReport.getHeader().getInt(MsgSeqNum.FIELD));
        orderStatus.setFilledQuantity(cumQty);
        orderStatus.setRemainingQuantity(remainingQuantity);
        orderStatus.setOrder(order);
        if (executionReport.isSetField(TransactTime.FIELD)) {

            orderStatus.setExtDateTime(executionReport.getTransactTime().getValue());
        }
        if (executionReport.isSetField(LastPx.FIELD)) {

            double d = executionReport.getLastPx().getValue();
            if (d != 0.0) {
                double multiplier = getPriceMultiplier(order.getSecurity().getSecurityFamily().getId());
                orderStatus.setLastPrice(RoundUtil.getBigDecimal(d / multiplier, order.getSecurity().getSecurityFamily().getScale()));
            }
        }
        if (executionReport.isSetField(AvgPx.FIELD)) {

            double d = executionReport.getAvgPx().getValue();
            if (d != 0.0) {
                orderStatus.setAvgPrice(RoundUtil.getBigDecimal(d, order.getSecurity().getSecurityFamily().getScale()));
            }
        }

        return orderStatus;
    }

    @Override
    protected Fill createFill(ExecutionReport executionReport, Order order) throws FieldNotFound {

        ExecType execType = executionReport.getExecType();
        // only create fills if status is TRADE
        if (execType.getValue() == ExecType.TRADE) {

            // get the fields
            Side side = FixUtil.getSide(executionReport.getSide());
            long quantity = (long) executionReport.getLastQty().getValue();
            double multiplier = getPriceMultiplier(order.getSecurity().getSecurityFamily().getId());
            double price = executionReport.getLastPx().getValue() / multiplier;
            String extId = executionReport.getExecID().getValue();

            // assemble the fill
            Fill fill = Fill.Factory.newInstance();
            fill.setExtId(extId);
            fill.setSequenceNumber(executionReport.getHeader().getInt(MsgSeqNum.FIELD));
            fill.setDateTime(new Date());
            fill.setSide(side);
            fill.setQuantity(quantity);
            fill.setPrice(RoundUtil.getBigDecimal(price, order.getSecurityInitialized().getSecurityFamilyInitialized().getScale()));
            if (executionReport.isSetField(TransactTime.FIELD)) {
                fill.setExtDateTime(executionReport.getTransactTime().getValue());
            }

            return fill;
        } else {

            return null;
        }
    }

    private static Status getStatus(ExecType execType, long orderQty, long cumQty) {

        if (execType.getValue() == ExecType.NEW) {
            return Status.SUBMITTED;
        } else if (execType.getValue() == ExecType.TRADE) {
            if (cumQty == orderQty) {
                return Status.EXECUTED;
            } else {
                return Status.PARTIALLY_EXECUTED;
            }
        } else if (execType.getValue() == ExecType.CANCELED || execType.getValue() == ExecType.DONE_FOR_DAY
                || execType.getValue() == ExecType.EXPIRED) {
            return Status.CANCELED;
        } else if (execType.getValue() == ExecType.REJECTED) {
            return Status.REJECTED;
        } else if (execType.getValue() == ExecType.REPLACE) {
            if (cumQty == 0) {
                return Status.SUBMITTED;
            } else {
                return Status.PARTIALLY_EXECUTED;
            }
        } else {
            throw new IllegalArgumentException("unknown execType " + execType.getValue());
        }
    }

    private double getPriceMultiplier(int securityFamilyId) {

        Double multiplier = this.priceMultiplierMap.get(securityFamilyId);
        if (multiplier != null) {
            return multiplier;
        } else {
            return 1.0;
        }
    }

}
