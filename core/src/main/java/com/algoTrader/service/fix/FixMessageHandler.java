package com.algoTrader.service.fix;

import java.math.BigDecimal;
import java.util.Date;

import org.apache.log4j.Logger;

import quickfix.FieldNotFound;
import quickfix.SessionID;
import quickfix.field.OrdStatus;
import quickfix.fix42.ExecutionReport;
import quickfix.fix42.OrderCancelReject;

import com.algoTrader.entity.StrategyImpl;
import com.algoTrader.entity.trade.Fill;
import com.algoTrader.entity.trade.Order;
import com.algoTrader.entity.trade.OrderStatus;
import com.algoTrader.enumeration.Side;
import com.algoTrader.enumeration.Status;
import com.algoTrader.esper.EsperManager;
import com.algoTrader.util.MyLogger;
import com.algoTrader.util.RoundUtil;

public class FixMessageHandler {

    private static Logger logger = MyLogger.getLogger(FixMessageHandler.class.getName());

    public void onMessage(ExecutionReport executionReport, SessionID sessionID) throws FieldNotFound {

        long number = Long.parseLong(executionReport.getClOrdID().getValue());

        if (executionReport.getOrdStatus().getValue() == OrdStatus.REJECTED) {
            logger.error("order " + number + " has been rejected, reason: " + executionReport.getText().getValue());
        }

        // for orders that have been cancelled by the system get the number from OrigClOrdID
        if (executionReport.getOrdStatus().getValue() == OrdStatus.CANCELED) {
            if (!executionReport.isSetExecRestatementReason()) {
                number = Long.parseLong(executionReport.getOrigClOrdID().getValue());

                // if the field ExecRestatementReason exists, there is something wrong
            } else {
                logger.error("order " + number + " has been canceled, reason: " + executionReport.getText().getValue());
            }
        }

        // get the order from the OpenOrderWindow
        Order order = (Order) EsperManager.executeSingelObjectQuery(StrategyImpl.BASE, "select * from OpenOrderWindow where number = " + number);
        if (order == null) {
            logger.error("order could not be found " + number + " for execution " + executionReport);
            return;
        }

        // get the other fields
        Status status = FixUtil.getStatus(executionReport.getOrdStatus(), executionReport.getCumQty());
        long filledQuantity = (long) executionReport.getCumQty().getValue();
        long remainingQuantity = (long) (executionReport.getOrderQty().getValue() - executionReport.getCumQty().getValue());

        // assemble the orderStatus
        OrderStatus orderStatus = OrderStatus.Factory.newInstance();
        orderStatus.setStatus(status);
        orderStatus.setFilledQuantity(filledQuantity);
        orderStatus.setRemainingQuantity(remainingQuantity);
        orderStatus.setParentOrder(order);

        EsperManager.sendEvent(StrategyImpl.BASE, orderStatus);

        // only create fills if status is PARTIALLY_FILLED or FILLED
        if (executionReport.getOrdStatus().getValue() == OrdStatus.PARTIALLY_FILLED || executionReport.getOrdStatus().getValue() == OrdStatus.FILLED) {

            // get the fields
            Date dateTime = executionReport.getTransactTime().getValue();
            Side side = FixUtil.getSide(executionReport.getSide());
            long quantity = (long) executionReport.getLastShares().getValue();
            BigDecimal price = RoundUtil.getBigDecimal(executionReport.getLastPx().getValue(), order.getSecurity().getSecurityFamily().getScale());
            String extId = executionReport.getExecID().getValue();

            // assemble the fill
            Fill fill = Fill.Factory.newInstance();
            fill.setDateTime(dateTime);
            fill.setSide(side);
            fill.setQuantity(quantity);
            fill.setPrice(price);
            fill.setExtId(extId);
            fill.setParentOrder(order);

            EsperManager.sendEvent(StrategyImpl.BASE, fill);
        }
    }

    public void onMessage(OrderCancelReject orderCancelReject, SessionID sessionID) throws FieldNotFound {

        logger.error("order has been rejected, clOrdID: " + orderCancelReject.getClOrdID().getValue() +
                " origOrdID: " + orderCancelReject.getOrigClOrdID().getValue() +
                " reason: " + orderCancelReject.getText().getValue());
    }
}
