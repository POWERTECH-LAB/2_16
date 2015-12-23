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

import java.util.Map;

import org.apache.commons.lang.Validate;

import ch.algotrader.entity.security.Forex;
import ch.algotrader.entity.security.Future;
import ch.algotrader.entity.security.Option;
import ch.algotrader.entity.security.Security;
import ch.algotrader.entity.security.Stock;
import ch.algotrader.entity.trade.LimitOrder;
import ch.algotrader.entity.trade.MarketOrder;
import ch.algotrader.entity.trade.Order;
import ch.algotrader.entity.trade.OrderProperty;
import ch.algotrader.entity.trade.StopLimitOrder;
import ch.algotrader.entity.trade.StopOrder;
import ch.algotrader.enumeration.OrderPropertyType;
import ch.algotrader.enumeration.Side;
import ch.algotrader.enumeration.TIF;
import quickfix.FieldMap;
import quickfix.field.OrdType;
import quickfix.field.Symbol;
import quickfix.field.TimeInForce;

/**
 * Utility class providing conversion methods for Fix specific types.
 *
 * @author <a href="mailto:aflury@algotrader.ch">Andy Flury</a>
 */
public class FixUtil {

    public static Side getSide(quickfix.field.Side side) {

        if (side.getValue() == quickfix.field.Side.BUY) {
            return Side.BUY;
        } else if (side.getValue() == quickfix.field.Side.SELL) {
            return Side.SELL;
        } else if (side.getValue() == quickfix.field.Side.SELL_SHORT) {
            return Side.SELL_SHORT;
        } else {
            throw new IllegalArgumentException("unknow side " + side);
        }
    }

    public static Symbol getFixSymbol(Security security, String broker) {

        if (security instanceof Option) {
            return new Symbol(security.getSecurityFamily().getSymbolRoot(broker));
        } else if (security instanceof Future) {
            return new Symbol(security.getSecurityFamily().getSymbolRoot(broker));
        } else if (security instanceof Forex) {
            return new Symbol(((Forex) security).getBaseCurrency().name());
        } else if (security instanceof Stock) {
            return new Symbol(security.getSymbol());
        } else {
            throw new UnsupportedOperationException("unsupported security type " + security.getClass());
        }
    }

    public static quickfix.field.Side getFixSide(Side side) {

        if (side.equals(Side.BUY)) {
            return new quickfix.field.Side(quickfix.field.Side.BUY);
        } else if (side.equals(Side.SELL)) {
            return new quickfix.field.Side(quickfix.field.Side.SELL);
        } else if (side.equals(Side.SELL_SHORT)) {
            return new quickfix.field.Side(quickfix.field.Side.SELL_SHORT);
        } else {
            throw new IllegalArgumentException("unknow side " + side);
        }
    }

    public static OrdType getFixOrderType(Order order) {

        if (order instanceof MarketOrder) {
            return new OrdType(OrdType.MARKET);
        } else if (order instanceof LimitOrder) {
            return new OrdType(OrdType.LIMIT);
        } else if (order instanceof StopOrder) {
            return new OrdType(OrdType.STOP);
        } else if (order instanceof StopLimitOrder) {
            return new OrdType(OrdType.STOP_LIMIT);
        } else {
            throw new IllegalArgumentException("unsupported order type " + order.getClass().getName());
        }
    }

    public static TIF getTimeInForce(final TimeInForce tif) {

        switch (tif.getValue()) {
            case TimeInForce.DAY:
                return TIF.DAY;
            case TimeInForce.GOOD_TILL_CANCEL:
                return TIF.GTC;
            case TimeInForce.GOOD_TILL_DATE:
                return TIF.GTD;
            case TimeInForce.IMMEDIATE_OR_CANCEL:
                return TIF.IOC;
            case TimeInForce.FILL_OR_KILL:
                return TIF.FOK;
            case TimeInForce.AT_THE_OPENING:
                return TIF.ATO;
            case TimeInForce.AT_THE_CLOSE:
                return TIF.ATC;
            default:
                throw new IllegalArgumentException("Unsupported Time-in-Force " + tif);
        }
    }

    public static TimeInForce getTimeInForce(final TIF tif) {

        switch (tif) {
            case DAY:
                return new TimeInForce(TimeInForce.DAY);
            case GTC:
                return new TimeInForce(TimeInForce.GOOD_TILL_CANCEL);
            case GTD:
                return new TimeInForce(TimeInForce.GOOD_TILL_DATE);
            case IOC:
                return new TimeInForce(TimeInForce.IMMEDIATE_OR_CANCEL);
            case FOK:
                return new TimeInForce(TimeInForce.FILL_OR_KILL);
            case ATO:
                return new TimeInForce(TimeInForce.AT_THE_OPENING);
            case ATC:
                return new TimeInForce(TimeInForce.AT_THE_CLOSE);
            default:
                throw new IllegalArgumentException("Unsupported Time-in-Force " + tif);
        }
    }

    public static void copyOrderProperties(final FieldMap message, final Map<String, OrderProperty> orderPropertyMap) {

        Validate.notNull(message, "Message is null");
        if (orderPropertyMap == null) {

            return;
        }

        for (Map.Entry<String, OrderProperty> entry: orderPropertyMap.entrySet()) {

            String key = entry.getKey();
            OrderProperty orderProperty = entry.getValue();
            if (OrderPropertyType.FIX.equals(orderProperty.getType())) {

                try {
                    int field = Integer.parseInt(key);
                    message.setString(field, orderProperty.getValue());
                } catch (NumberFormatException ignore) {
                }
            }
        }
    }

}
