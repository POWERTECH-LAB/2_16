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
package ch.algotrader.entity.security;

import java.math.BigDecimal;
import java.text.ChoiceFormat;
import org.apache.commons.lang.Validate;

import ch.algotrader.enumeration.Broker;
import ch.algotrader.util.RoundUtil;

/**
 * @author <a href="mailto:aflury@algotrader.ch">Andy Flury</a>
 *
 * @version $Revision$ $Date$
 */
public class SecurityFamilyImpl extends SecurityFamily {

    private static final long serialVersionUID = -2318908709333325986L;

    @Override
    public String getSymbolRoot(Broker broker) {

        if (broker != null) {
            BrokerParameters brokerParams = getBrokerParameters().get(broker.toString());
            if (brokerParams != null && brokerParams.getSymbolRoot() != null) {
                return brokerParams.getSymbolRoot();
            }
        }

        return getSymbolRoot();
    }

    @Override
    public double getContractSize(Broker broker) {

        if (broker != null) {
            BrokerParameters brokerParams = getBrokerParameters().get(broker.toString());
            if (brokerParams != null && brokerParams.getContractSize() != null) {
                return brokerParams.getContractSize();
            }
        }

        return getContractSize();
    }

    @Override
    public int getScale(Broker broker) {

        if (broker != null) {
            BrokerParameters brokerParams = getBrokerParameters().get(broker.toString());
            if (brokerParams != null && brokerParams.getScale() != null) {
                return brokerParams.getScale();
            }
        }

        return getScale();
    }

    @Override
    public String getTickSizePattern(Broker broker) {

        if (broker != null) {
            BrokerParameters brokerParams = getBrokerParameters().get(broker.toString());
            if (brokerParams != null && brokerParams.getTickSizePattern() != null) {
                return brokerParams.getTickSizePattern();
            }
        }

        return getTickSizePattern();
    }

    @Override
    public BigDecimal getExecutionCommission(Broker broker) {

        if (broker != null) {
            BrokerParameters brokerParams = getBrokerParameters().get(broker.toString());
            if (brokerParams != null && brokerParams.getExecutionCommission() != null) {
                return brokerParams.getExecutionCommission();
            }
        }

        return getExecutionCommission();
    }

    @Override
    public BigDecimal getClearingCommission(Broker broker) {

        if (broker != null) {
            BrokerParameters brokerParams = getBrokerParameters().get(broker.toString());
            if (brokerParams != null && brokerParams.getClearingCommission() != null) {
                return brokerParams.getClearingCommission();
            }
        }

        return getClearingCommission();
    }

    @Override
    public BigDecimal getFee(Broker broker) {

        if (broker != null) {
            BrokerParameters brokerParams = getBrokerParameters().get(broker.toString());
            if (brokerParams != null && brokerParams.getFee() != null) {
                return brokerParams.getFee();
            }
        }

        return getFee();
    }

    @Override
    public BigDecimal getTotalCharges(Broker broker) {

        BigDecimal totalCharges = new BigDecimal(0.0);

        BigDecimal executionCommission = getExecutionCommission(broker);
        if (executionCommission != null) {
            totalCharges.add(executionCommission);
        }

        BigDecimal clearingCommission = getClearingCommission(broker);
        if (clearingCommission != null) {
            totalCharges.add(executionCommission);
        }

        BigDecimal fee = getFee(broker);
        if (fee != null) {
            totalCharges.add(fee);
        }

        return totalCharges;
    }

    @Override
    public double getPriceMultiplier(Broker broker) {

        Validate.notNull(broker, "Broker cannot be null");

        BrokerParameters brokerParams = getBrokerParameters().get(broker.toString());
        if (brokerParams != null && brokerParams.getPriceMultiplier() != null) {
            return brokerParams.getPriceMultiplier();
        } else {
            return 1.0;
        }

    }

    @Override
    public int getSpreadTicks(Broker broker, BigDecimal bid, BigDecimal ask) {

        Validate.notNull(bid, "Bid cannot be null");
        Validate.notNull(ask, "Ask cannot be null");

        int ticks = 0;
        BigDecimal price = bid;
        if (bid.compareTo(ask) <= 0) {
            while (price.compareTo(ask) < 0) {
                ticks++;
                price = adjustPrice(broker, price, 1);
            }
        } else {
            while (price.compareTo(ask) > 0) {
                ticks--;
                price = adjustPrice(broker, price, -1);
            }
        }
        return ticks;
    }

    @Override
    public BigDecimal getTickSize(Broker broker, BigDecimal price, boolean upwards) {

        Validate.notNull(price, "Price cannot be null");

        return RoundUtil.getBigDecimal(getTickSize(broker, price.doubleValue(), upwards), getScale(broker));
    }

    @Override
    public double getTickSize(Broker broker, double price, boolean upwards) {

        // add or subtract a very small amount to the price to get the tickSize just above or below the trigger
        double adjustedPrice = upwards ? price * 1.00000000001 : price / 1.00000000001;
        return Double.valueOf(new ChoiceFormat(getTickSizePattern(broker)).format(adjustedPrice));
    }

    @Override
    public BigDecimal adjustPrice(Broker broker, BigDecimal price, int ticks) {

        Validate.notNull(price, "Price cannot be null");

        if (ticks > 0) {
            for (int i = 0; i < ticks; i++) {
                price = price.add(getTickSize(broker, price, true));
            }
        } else if (ticks < 0) {
            for (int i = 0; i > ticks; i--) {
                price = price.subtract(getTickSize(broker, price, false));
            }
        }
        return price;
    }

    @Override
    public double adjustPrice(Broker broker, double price, int ticks) {

        if (ticks > 0) {
            for (int i = 0; i < ticks; i++) {
                price = price + getTickSize(broker, price, true);
            }
        } else if (ticks < 0) {
            for (int i = 0; i > ticks; i--) {
                price = price - getTickSize(broker, price, false);
            }
        }
        return price;
    }

    @Override
    public BigDecimal roundUp(Broker broker, BigDecimal price) {

        Validate.notNull(price, "Price cannot be null");

        return RoundUtil.roundToNextN(price, getTickSize(broker, price, true), BigDecimal.ROUND_UP);
    }

    @Override
    public double roundUp(Broker broker, double price) {
        return RoundUtil.roundToNextN(price, getTickSize(broker, price, true), BigDecimal.ROUND_UP);
    }

    @Override
    public BigDecimal roundDown(Broker broker, BigDecimal price) {

        Validate.notNull(price, "Price cannot be null");

        return RoundUtil.roundToNextN(price, getTickSize(broker, price, false), BigDecimal.ROUND_DOWN);
    }

    @Override
    public double roundDown(Broker broker, double price) {
        return RoundUtil.roundToNextN(price, getTickSize(broker, price, false), BigDecimal.ROUND_DOWN);
    }

    @Override
    public String toString() {
        return getName();
    }

}
