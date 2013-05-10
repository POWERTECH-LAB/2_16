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
package com.algoTrader.entity.marketData;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;

import org.springframework.beans.factory.annotation.Value;

import com.algoTrader.entity.security.SecurityFamily;
import com.algoTrader.enumeration.Direction;
import com.algoTrader.util.RoundUtil;

/**
 * @author <a href="mailto:andyflury@gmail.com">Andy Flury</a>
 *
 * @version $Revision$ $Date$
 */
public class TickImpl extends Tick {

    private static final long serialVersionUID = 7518020445322413106L;

    private static final SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy kk:mm:ss SSS");

    private static @Value("${simulation}") boolean simulation;
    private static @Value("${simulation.simulateBidAsk}") boolean simulateBidAsk;

    @Override
    public BigDecimal getBid() {

        if (simulation && simulateBidAsk && super.getBid().equals(new BigDecimal(0))) {

            // tradeable securities with bid = 0 should return a simulated value
            SecurityFamily family = getSecurity().getSecurityFamily();
            if (family.isTradeable()) {

                if (family.getSpreadSlope() == null || family.getSpreadConstant() == null) {
                    throw new IllegalStateException("SpreadSlope and SpreadConstant have to be defined for dummyBid " + getSecurity());
                }

                // spread depends on the pricePerContract (i.e. spread should be the same
                // for 12.- at contractSize 10 as for 1.20 at contractSize 100)
                double pricePerContract = getLast().doubleValue() * family.getContractSize();
                double spread = pricePerContract * family.getSpreadSlope() + family.getSpreadConstant();
                double dummyBid = (pricePerContract - (spread / 2.0)) / family.getContractSize();
                return RoundUtil.getBigDecimal(dummyBid < 0 ? 0 : dummyBid, family.getScale());
            }
        }

        return super.getBid();
    }

    @Override
    public BigDecimal getAsk() {

        if (simulation && simulateBidAsk && super.getAsk().equals(new BigDecimal(0))) {

            // tradeable securities with ask = 0 should return a simulated value
            SecurityFamily family = getSecurity().getSecurityFamily();
            if (family.isTradeable()) {

                if (family.getSpreadSlope() == null || family.getSpreadConstant() == null) {
                    throw new IllegalStateException("SpreadSlope and SpreadConstant have to be defined for dummyAsk " + getSecurity());
                }

                // spread depends on the pricePerContract (i.e. spread should be the same
                // for 12.- at contractSize 10 as for 1.20 at contractSize 100)
                double pricePerContract = getLast().doubleValue() * family.getContractSize();
                double spread = pricePerContract * family.getSpreadSlope() + family.getSpreadConstant();
                double dummyAsk = (pricePerContract + (spread / 2.0)) / family.getContractSize();
                return RoundUtil.getBigDecimal(dummyAsk, family.getScale());
            }
        }

        return super.getAsk();
    }

    /**
     * Note: ticks that are not valid (i.e. low volume) are not fed into esper, so we don't need to check
     */
    @Override
    public BigDecimal getCurrentValue() {

        int scale = getSecurity().getSecurityFamily().getScale();
        if (simulation) {
            if ((super.getBid().doubleValue() != 0) && (super.getAsk().doubleValue() != 0)) {
                return RoundUtil.getBigDecimal((getAsk().doubleValue() + getBid().doubleValue()) / 2.0, scale);
            } else {
                return getLast();
            }
        } else {
            if (this.getSecurity().getSecurityFamily().isTradeable() || this.getSecurity().getSecurityFamily().isSynthetic()) {
                return RoundUtil.getBigDecimal((getAsk().doubleValue() + getBid().doubleValue()) / 2.0, scale);
            } else {
                return getLast();
            }
        }
    }

    @Override
    public BigDecimal getMarketValue(Direction direction) {

        if (Direction.LONG.equals(direction)) {
            return getBid();
        } else if (Direction.SHORT.equals(direction)) {
            return getAsk();
        } else {
            return new BigDecimal(0);
        }
    }

    @Override
    public BigDecimal getBidAskSpread() {

        return RoundUtil.getBigDecimal(getBidAskSpreadDouble());
    }

    @Override
    public double getBidAskSpreadDouble() {

        return getAsk().doubleValue() - getBid().doubleValue();
    }

    @Override
    public boolean isSpreadValid() {

        SecurityFamily family = getSecurity().getSecurityFamily();

        // only check spread on tradeable ticks
        if (!family.isTradeable()) {
            return true;
        } else {

            if (family.getMaxSpreadSlope() == null || family.getMaxSpreadConstant() == null) {
                throw new IllegalStateException("SpreadSlope and SpreadConstant have to be defined to validate a tradeable security");
            }

            int contractSize = family.getContractSize();
            double maxSpreadSlope = family.getMaxSpreadSlope();
            double maxSpreadConstant = family.getMaxSpreadConstant();

            double mean = contractSize * getCurrentValueDouble();
            double spread = contractSize * getBidAskSpreadDouble();
            double maxSpread = mean * maxSpreadSlope + maxSpreadConstant;

            if (spread <= maxSpread) {
                return true;
            } else {
                return false;
            }
        }
    }

    @Override
    public String toString() {

        StringBuffer buffer = new StringBuffer();

        buffer.append(getSecurity());
        buffer.append(",");
        buffer.append(format.format(getDateTime()));
        buffer.append(",last=");
        buffer.append(getLast());
        buffer.append(",lastDateTime=");
        buffer.append(getLastDateTime());
        buffer.append(",bid=");
        buffer.append(getBid());
        buffer.append(",ask=");
        buffer.append(getAsk());
        buffer.append(",volBid=");
        buffer.append(getVolBid());
        buffer.append(",volAsk=");
        buffer.append(getVolAsk());
        buffer.append(",vol=");
        buffer.append(getVol());
        buffer.append(",openIntrest=");
        buffer.append(getOpenIntrest());
        buffer.append(",settlement=");
        buffer.append(getSettlement());

        return buffer.toString();
    }
}
