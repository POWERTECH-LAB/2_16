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

import java.util.Date;

import org.apache.commons.math.MathException;

import ch.algotrader.entity.marketData.Tick;
import ch.algotrader.option.OptionUtil;
import ch.algotrader.util.DateUtil;

/**
 * @author <a href="mailto:aflury@algotrader.ch">Andy Flury</a>
 *
 * @version $Revision$ $Date$
 */
public class OptionImpl extends Option {

    private static final long serialVersionUID = -3168298592370987085L;

    @Override
    public double getLeverage(double currentValue, double underlyingCurrentValue) {

        try {
            double delta = OptionUtil.getDelta(this, currentValue, underlyingCurrentValue);
            return underlyingCurrentValue / currentValue * delta;
        } catch (MathException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public double getMargin(double currentValue, double underlyingCurrentValue) {

        try {
            return OptionUtil.getMaintenanceMargin(this, currentValue, underlyingCurrentValue) * getSecurityFamily().getContractSize();
        } catch (MathException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public long getTimeToExpiration(Date dateTime) {

        return getExpiration().getTime() - dateTime.getTime();
    }

    @Override
    public int getDuration(Date dateTime) {

        OptionFamily family = (OptionFamily) this.getSecurityFamily();
        Date nextExpDate = DateUtil.getExpirationDate(family.getExpirationType(), dateTime);
        return 1 + (int) Math.round(((this.getExpiration().getTime() - nextExpDate.getTime()) / (double) family.getExpirationDistance().getValue()));
    }

    /**
     * make sure expiration is a java.util.Date and not a java.sql.TimeStamp
     */
    @Override
    public Date getExpiration() {

        Date expiration = super.getExpiration();
        if (expiration != null && expiration instanceof java.sql.Timestamp) {
            return new Date(expiration.getTime());
        } else {
            return expiration;
        }
    }

    @Override
    public boolean validateTick(Tick tick) {

        // options need to have an ASK (but might not have a BID just before expiration)
        if (tick.getVolAsk() == 0) {
            return false;
        } else if (tick.getAsk() == null) {
            return false;
        }

        return super.validateTick(tick);
    }
}
