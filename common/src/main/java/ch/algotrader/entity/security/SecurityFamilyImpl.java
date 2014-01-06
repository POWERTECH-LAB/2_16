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
package ch.algotrader.entity.security;

import java.math.BigDecimal;
import java.text.ChoiceFormat;

import ch.algotrader.enumeration.Broker;
import ch.algotrader.util.ObjectUtil;
import ch.algotrader.util.RoundUtil;

/**
 * @author <a href="mailto:aflury@algotrader.ch">Andy Flury</a>
 *
 * @version $Revision$ $Date$
 */
public class SecurityFamilyImpl extends SecurityFamily {

    private static final long serialVersionUID = -2318908709333325986L;

    @Override
    public String toString() {

        return getName();
    }

    @Override
    public BigDecimal getTickSize(BigDecimal price, boolean upwards) {

        return RoundUtil.getBigDecimal(getTickSize(price.doubleValue(), upwards), getScale());
    }

    @Override
    public double getTickSize(double price, boolean upwards) {

        // add or subtract a very small amount to the price to get the tickSize just above or below the trigger
        double adjustedPrice = upwards ? price * 1.00000000001 : price / 1.00000000001;
        return Double.valueOf(new ChoiceFormat(getTickSizePattern()).format(adjustedPrice));
    }


    @Override
    public BigDecimal getTotalCommission() {

        if (getExecutionCommission() != null && getClearingCommission() != null) {
            return getExecutionCommission().add(getClearingCommission());
        } else if (getExecutionCommission() == null) {
            return getClearingCommission();
        } else if (getClearingCommission() == null) {
            return getExecutionCommission();
        } else {
            return null;
        }
    }

    @Override
    public String getBaseSymbol(Broker broker) {

        BrokerParameters brokerParams = getBrokerParameters().get(broker.toString());
        if (brokerParams != null && brokerParams.getBaseSymbol() != null) {
            return brokerParams.getBaseSymbol();
        } else {
            return getBaseSymbol();
        }
    }

    @Override
    public String getMarket(Broker broker) {

        BrokerParameters brokerParams = getBrokerParameters().get(broker.toString());
        if (brokerParams != null && brokerParams.getMarket() != null) {
            return brokerParams.getMarket();
        } else {
            return getMarket();
        }
    }

    @Override
    public BigDecimal getExecutionCommission(Broker broker) {

        BrokerParameters brokerParams = getBrokerParameters().get(broker.toString());
        if (brokerParams != null && brokerParams.getExecutionCommission() != null) {
            return brokerParams.getExecutionCommission();
        } else {
            return getExecutionCommission();
        }
    }

    @Override
    public BigDecimal getClearingCommission(Broker broker) {

        BrokerParameters brokerParams = getBrokerParameters().get(broker.toString());
        if (brokerParams != null && brokerParams.getClearingCommission() != null) {
            return brokerParams.getClearingCommission();
        } else {
            return getClearingCommission();
        }
    }

    @Override
    public BigDecimal getTotalCommission(Broker broker) {

        if (getExecutionCommission(broker) != null && getClearingCommission(broker) != null) {
            return getExecutionCommission(broker).add(getClearingCommission(broker));
        } else if (getExecutionCommission(broker) == null) {
            return getClearingCommission(broker);
        } else if (getClearingCommission(broker) == null) {
            return getExecutionCommission(broker);
        } else {
            return null;
        }
    }

    @Override
    public int getSpreadTicks(BigDecimal bid, BigDecimal ask) {

        int ticks = 0;
        BigDecimal price = bid;
        if (bid.compareTo(ask) <= 0) {
            while (price.compareTo(ask) < 0) {
                ticks++;
                price = adjustPrice(price, 1);
            }
        } else {
            while (price.compareTo(ask) > 0) {
                ticks--;
                price = adjustPrice(price, -1);
            }
        }
        return ticks;
    }

    @Override
    public BigDecimal adjustPrice(BigDecimal price, int ticks) {

        if (ticks > 0) {
            for (int i = 0; i < ticks; i++) {
                price = price.add(getTickSize(price, true));
            }
        } else if (ticks < 0) {
            for (int i = 0; i > ticks; i--) {
                price = price.subtract(getTickSize(price, false));
            }
        }
        return price;
    }

    @Override
    public boolean equals(Object obj) {

        if (this == obj) {
            return true;
        }
        if (obj instanceof SecurityFamily) {
            SecurityFamily that = (SecurityFamily) obj;
            return ObjectUtil.equalsNonNull(this.getName(), that.getName());

        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {

        int hash = 17;
        hash = hash * 37 + ObjectUtil.hashCode(getName());
        return hash;
    }
}
