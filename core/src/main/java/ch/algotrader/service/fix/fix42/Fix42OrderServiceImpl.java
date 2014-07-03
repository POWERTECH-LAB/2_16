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
package ch.algotrader.service.fix.fix42;

import java.text.SimpleDateFormat;
import java.util.Date;

import quickfix.field.ClOrdID;
import quickfix.field.ContractMultiplier;
import quickfix.field.Currency;
import quickfix.field.ExpireTime;
import quickfix.field.MaturityDay;
import quickfix.field.MaturityMonthYear;
import quickfix.field.OrderQty;
import quickfix.field.OrigClOrdID;
import quickfix.field.Price;
import quickfix.field.PutOrCall;
import quickfix.field.SecurityType;
import quickfix.field.StopPx;
import quickfix.field.StrikePrice;
import quickfix.field.TransactTime;
import quickfix.fix42.NewOrderSingle;
import quickfix.fix42.OrderCancelReplaceRequest;
import quickfix.fix42.OrderCancelRequest;
import ch.algotrader.adapter.fix.FixUtil;
import ch.algotrader.entity.security.Forex;
import ch.algotrader.entity.security.Future;
import ch.algotrader.entity.security.Option;
import ch.algotrader.entity.security.Security;
import ch.algotrader.entity.security.Stock;
import ch.algotrader.entity.trade.LimitOrderI;
import ch.algotrader.entity.trade.SimpleOrder;
import ch.algotrader.entity.trade.StopOrderI;
import ch.algotrader.enumeration.Broker;
import ch.algotrader.enumeration.OptionType;
import ch.algotrader.enumeration.TIF;

/**
 * Generic FIX 4.2 order service
 *
 * @author <a href="mailto:aflury@algotrader.ch">Andy Flury</a>
 *
 * @version $Revision$ $Date$
 */
public abstract class Fix42OrderServiceImpl extends Fix42OrderServiceBase {

    private static final long serialVersionUID = -3694423160435186473L;

    private static SimpleDateFormat monthFormat = new SimpleDateFormat("yyyyMM");
    private static SimpleDateFormat dayFormat = new SimpleDateFormat("dd");

    @Override
    protected void handleValidateOrder(SimpleOrder order) throws Exception {
        // to be implememented
    }

    @Override
    protected void handleSendOrder(SimpleOrder order) throws Exception {

        Security security = order.getSecurityInitialized();
        Broker broker = order.getAccount().getBroker();

        // assign a new clOrdID
        String clOrdID = getFixAdapter().getNextOrderId(order.getAccount());
        order.setIntId(clOrdID);

        NewOrderSingle newOrder = new NewOrderSingle();

        // common info
        newOrder.set(new TransactTime(new Date()));
        newOrder.set(new ClOrdID(String.valueOf(clOrdID)));

        newOrder.set(FixUtil.getFixSymbol(security, broker));
        newOrder.set(FixUtil.getFixSide(order.getSide()));
        newOrder.set(new OrderQty(order.getQuantity()));
        newOrder.set(FixUtil.getFixOrderType(order));

        // populate security information
        if (security instanceof Option) {

            Option option = (Option) security;

            newOrder.set(new SecurityType(SecurityType.OPTION));
            newOrder.set(new Currency(option.getSecurityFamily().getCurrency().toString()));
            newOrder.set(new PutOrCall(OptionType.PUT.equals(option.getType()) ? PutOrCall.PUT : PutOrCall.CALL));
            newOrder.set(new StrikePrice(option.getStrike().doubleValue()));
            newOrder.set(new ContractMultiplier(option.getSecurityFamily().getContractSize()));
            newOrder.set(new MaturityMonthYear(monthFormat.format(option.getExpiration())));
            newOrder.set(new MaturityDay(dayFormat.format(option.getExpiration())));

        } else if (security instanceof Future) {

            Future future = (Future) security;

            newOrder.set(new SecurityType(SecurityType.FUTURE));
            newOrder.set(new Currency(future.getSecurityFamily().getCurrency().toString()));
            newOrder.set(new MaturityMonthYear(monthFormat.format(future.getExpiration())));
            newOrder.set(new MaturityDay(dayFormat.format(future.getExpiration())));

        } else if (security instanceof Forex) {

            newOrder.set(new SecurityType(SecurityType.CASH));
            newOrder.set(new Currency(security.getSecurityFamily().getCurrency().getValue()));

        } else if (security instanceof Stock) {

            Stock stock = (Stock) security;

            newOrder.set(new SecurityType(SecurityType.COMMON_STOCK));
            newOrder.set(new Currency(stock.getSecurityFamily().getCurrency().toString()));
        }

        //set the limit price if order is a limit order or stop limit order
        if (order instanceof LimitOrderI) {
            newOrder.set(new Price(((LimitOrderI) order).getLimit().doubleValue()));
        }

        //set the stop price if order is a stop order or stop limit order
        if (order instanceof StopOrderI) {
            newOrder.set(new StopPx(((StopOrderI) order).getStop().doubleValue()));
        }

        // set TIF
        if (order.getTif() != null) {
            newOrder.set(FixUtil.getTimeInForce(order.getTif()));
            if (order.getTif() == TIF.GTD && order.getTifDateTime() != null) {
                newOrder.set(new ExpireTime(order.getTifDateTime()));
            }
        }

        // broker-specific settings
        sendOrder(order, newOrder);

        // send the message
        sendOrder(order, newOrder, true);
    }

    @Override
    protected void handleModifyOrder(SimpleOrder order) throws Exception {

        Security security = order.getSecurityInitialized();
        Broker broker = order.getAccount().getBroker();

        // get origClOrdID and assign a new clOrdID
        String origClOrdID = order.getIntId();
        String clOrdID = getFixAdapter().getNextOrderIdVersion(order);

        OrderCancelReplaceRequest replaceRequest = new OrderCancelReplaceRequest();

        // common info
        replaceRequest.set(new ClOrdID(clOrdID));
        replaceRequest.set(new OrigClOrdID(origClOrdID));

        replaceRequest.set(FixUtil.getFixSymbol(security, broker));
        replaceRequest.set(FixUtil.getFixSide(order.getSide()));
        replaceRequest.set(new OrderQty(order.getQuantity()));
        replaceRequest.set(FixUtil.getFixOrderType(order));

        // populate security information
        if (security instanceof Option) {

            Option option = (Option) security;

            replaceRequest.set(new SecurityType(SecurityType.OPTION));
            replaceRequest.set(new PutOrCall(OptionType.PUT.equals(option.getType()) ? PutOrCall.PUT : PutOrCall.CALL));
            replaceRequest.set(new StrikePrice(option.getStrike().doubleValue()));
            replaceRequest.set(new MaturityMonthYear(monthFormat.format(option.getExpiration())));

        } else if (security instanceof Future) {

            Future future = (Future) security;

            replaceRequest.set(new SecurityType(SecurityType.FUTURE));
            replaceRequest.set(new MaturityMonthYear(monthFormat.format(future.getExpiration())));

        } else if (security instanceof Forex) {

            replaceRequest.set(new SecurityType(SecurityType.CASH));

        } else if (security instanceof Stock) {
            replaceRequest.set(new SecurityType(SecurityType.COMMON_STOCK));
        }

        //set the limit price if order is a limit order or stop limit order
        if (order instanceof LimitOrderI) {
            replaceRequest.set(new Price(((LimitOrderI) order).getLimit().doubleValue()));
        }

        //set the stop price if order is a stop order or stop limit order
        if (order instanceof StopOrderI) {
            replaceRequest.set(new StopPx(((StopOrderI) order).getStop().doubleValue()));
        }

        // set TIF
        if (order.getTif() != null) {
            replaceRequest.set(FixUtil.getTimeInForce(order.getTif()));
            if (order.getTif() == TIF.GTD && order.getTifDateTime() != null) {
                replaceRequest.set(new ExpireTime(order.getTifDateTime()));
            }
        }

        // broker-specific settings
        modifyOrder(order, replaceRequest);

        // send the message
        sendOrder(order, replaceRequest, true);
    }

    @Override
    protected void handleCancelOrder(SimpleOrder order) throws Exception {

        Security security = order.getSecurityInitialized();
        Broker broker = order.getAccount().getBroker();

        // get origClOrdID and assign a new clOrdID
        String origClOrdID = order.getIntId();
        String clOrdID = getFixAdapter().getNextOrderIdVersion(order);

        OrderCancelRequest cancelRequest = new OrderCancelRequest();

        // common info
        cancelRequest.set(new ClOrdID(clOrdID));
        cancelRequest.set(new OrigClOrdID(origClOrdID));

        cancelRequest.set(FixUtil.getFixSymbol(security, broker));
        cancelRequest.set(FixUtil.getFixSide(order.getSide()));
        cancelRequest.set(new OrderQty(order.getQuantity()));

        // populate security information
        if (security instanceof Option) {

            Option option = (Option) security;

            cancelRequest.set(new SecurityType(SecurityType.OPTION));
            cancelRequest.set(new PutOrCall(OptionType.PUT.equals(option.getType()) ? PutOrCall.PUT : PutOrCall.CALL));
            cancelRequest.set(new StrikePrice(option.getStrike().doubleValue()));
            cancelRequest.set(new MaturityMonthYear(monthFormat.format(option.getExpiration())));

        } else if (security instanceof Future) {

            Future future = (Future) security;

            cancelRequest.set(new SecurityType(SecurityType.FUTURE));
            cancelRequest.set(new MaturityMonthYear(monthFormat.format(future.getExpiration())));

        } else if (security instanceof Forex) {

            cancelRequest.set(new SecurityType(SecurityType.CASH));

        } else if (security instanceof Stock) {
            cancelRequest.set(new SecurityType(SecurityType.COMMON_STOCK));
        }

        // broker-specific settings
        cancelOrder(order, cancelRequest);

        // send the message
        sendOrder(order, cancelRequest, false);
    }
}
