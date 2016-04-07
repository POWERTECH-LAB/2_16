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
package ch.algotrader.adapter.lmax;

import java.util.Date;

import quickfix.field.ClOrdID;
import quickfix.field.OrdType;
import quickfix.field.OrderQty;
import quickfix.field.OrigClOrdID;
import quickfix.field.Price;
import quickfix.field.SecurityID;
import quickfix.field.SecurityIDSource;
import quickfix.field.StopPx;
import quickfix.field.TimeInForce;
import quickfix.field.TransactTime;
import quickfix.fix44.NewOrderSingle;
import quickfix.fix44.OrderCancelReplaceRequest;
import quickfix.fix44.OrderCancelRequest;
import ch.algotrader.adapter.BrokerAdapterException;
import ch.algotrader.adapter.fix.FixUtil;
import ch.algotrader.adapter.fix.fix44.Fix44OrderMessageFactory;
import ch.algotrader.entity.security.Forex;
import ch.algotrader.entity.security.Security;
import ch.algotrader.entity.trade.LimitOrder;
import ch.algotrader.entity.trade.MarketOrder;
import ch.algotrader.entity.trade.SimpleOrder;
import ch.algotrader.entity.trade.StopOrder;
import ch.algotrader.enumeration.TIF;
import ch.algotrader.util.PriceUtil;

/**
 *  LMAX order message factory.
 *
 * @author <a href="mailto:okalnichevski@algotrader.ch">Oleg Kalnichevski</a>
 */
public class LMAXFixOrderMessageFactory implements Fix44OrderMessageFactory {

    protected TimeInForce resolveTimeInForce(final SimpleOrder order) throws BrokerAdapterException {

        TIF tif = order.getTif();
        if (tif == null) {

            return null;
        }
        if (order instanceof MarketOrder) {

            if (tif != TIF.IOC && tif != TIF.FOK) {

                throw new BrokerAdapterException("Time in force '" + tif + "' is not supported by LMAX for market orders");
            }
        } else if (order instanceof LimitOrder) {

            if (tif != TIF.DAY && tif != TIF.GTC && tif != TIF.IOC && tif != TIF.FOK) {

                throw new BrokerAdapterException("Time in force '" + tif + "' is not supported by LMAX for limit orders");
            }
        } else if (order instanceof StopOrder) {

            if (tif != TIF.DAY && tif != TIF.GTC) {

                throw new BrokerAdapterException("Time in force '" + tif + "' is not supported by LMAX for stop orders");
            }
        }

        return FixUtil.getTimeInForce(tif);
    }

    protected SecurityID resolveSecurityID(final Security security) throws BrokerAdapterException {

        if (!(security instanceof Forex)) {

            throw new BrokerAdapterException("LMAX interface currently only supports FX orders");
        }
        String lmaxid = security.getLmaxid();
        if (lmaxid == null) {
            throw new BrokerAdapterException(security + " is not supported by LMAX");
        }
        return new SecurityID(lmaxid);
    }

    protected OrderQty resolveOrderQty(final SimpleOrder order) {

        long quantity = order.getQuantity();
        if ((quantity % 1000) != 0) {
            throw new BrokerAdapterException("FX orders on LMAX need to be multiples of 1000");
        } else {
            return new OrderQty((double) quantity / LMAXConsts.FOREX_CONTRACT_MULTIPLIER);
        }
    }

    @Override
    public NewOrderSingle createNewOrderMessage(final SimpleOrder order, final String clOrdID) throws BrokerAdapterException {

        NewOrderSingle message = new NewOrderSingle();
        message.set(new ClOrdID(clOrdID));
        message.set(new TransactTime(new Date()));

        message.set(resolveSecurityID(order.getSecurity()));
        message.set(new SecurityIDSource("8"));

        message.set(FixUtil.getFixSide(order.getSide()));
        message.set(resolveOrderQty(order));

        if (order instanceof MarketOrder) {

            message.set(new OrdType(OrdType.MARKET));

        } else if (order instanceof LimitOrder) {

            message.set(new OrdType(OrdType.LIMIT));
            LimitOrder limitOrder = (LimitOrder) order;
            message.set(new Price(PriceUtil.denormalizePrice(order, limitOrder.getLimit())));

        } else if (order instanceof StopOrder) {

            message.set(new OrdType(OrdType.STOP));
            StopOrder stopOrder = (StopOrder) order;
            message.set(new StopPx(PriceUtil.denormalizePrice(order, stopOrder.getStop())));

        } else {

            throw new BrokerAdapterException("Order type " + order.getClass().getName() + " is not supported by LMAX");
        }

        if (order.getTif() != null) {

            message.set(resolveTimeInForce(order));
        }

        return message;
    }

    @Override
    public OrderCancelReplaceRequest createModifyOrderMessage(final SimpleOrder order, final String clOrdID) throws BrokerAdapterException {

        if (!(order instanceof LimitOrder)) {

            throw new BrokerAdapterException("Order modification of type " + order.getClass().getName() + " is not supported by LMAX");
        }

        LimitOrder limitOrder = (LimitOrder) order;

        // get origClOrdID and assign a new clOrdID
        String origClOrdID = order.getIntId();

        OrderCancelReplaceRequest message = new OrderCancelReplaceRequest();

        message.set(new ClOrdID(clOrdID));
        message.set(new OrigClOrdID(origClOrdID));
        message.set(new TransactTime(new Date()));

        message.set(resolveSecurityID(order.getSecurity()));
        message.set(new SecurityIDSource("8"));

        message.set(FixUtil.getFixSide(order.getSide()));
        message.set(resolveOrderQty(order));

        message.set(new OrdType(OrdType.LIMIT));
        message.set(new Price(PriceUtil.denormalizePrice(order, limitOrder.getLimit())));

        if (order.getTif() != null) {

            message.set(resolveTimeInForce(order));
        }

        return message;
    }

    @Override
    public OrderCancelRequest createOrderCancelMessage(final SimpleOrder order, final String clOrdID) throws BrokerAdapterException {

        // get origClOrdID and assign a new clOrdID
        String origClOrdID = order.getIntId();

        OrderCancelRequest message = new OrderCancelRequest();

        message.set(new ClOrdID(clOrdID));
        message.set(new OrigClOrdID(origClOrdID));
        message.set(new TransactTime(new Date()));

        message.set(resolveSecurityID(order.getSecurity()));
        message.set(new SecurityIDSource("8"));

        message.set(FixUtil.getFixSide(order.getSide()));
        message.set(resolveOrderQty(order));

        return message;
    }

}
