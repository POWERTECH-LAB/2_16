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

import java.util.Date;

import ch.algotrader.entity.marketData.Tick;
import ch.algotrader.future.FutureUtil;
import ch.algotrader.util.DateUtil;

/**
 * @author <a href="mailto:aflury@algotrader.ch">Andy Flury</a>
 *
 * @version $Revision$ $Date$
 */
public class FutureImpl extends Future {

    private static final long serialVersionUID = -7436972192801577685L;

    @Override
    public double getLeverage() {

        return 1.0;
    }

    @Override
    public double getMargin() {

        return FutureUtil.getMaintenanceMargin(this) * getSecurityFamily().getContractSize();
    }

    @Override
    public long getTimeToExpiration() {

        return getExpiration().getTime() - DateUtil.getCurrentEPTime().getTime();
    }

    @Override
    public int getDuration() {

        FutureFamily family = (FutureFamily) this.getSecurityFamilyInitialized();
        Date nextExpDate = DateUtil.getExpirationDate(family.getExpirationType(), DateUtil.getCurrentEPTime());
        return 1 + (int) Math.round(((this.getExpiration().getTime() - nextExpDate.getTime()) / (double)family.getExpirationDistance().value()));
    }

    @Override
    public boolean validateTick(Tick tick) {

        // futures need to have a BID and ASK
        if (tick.getBid() == null) {
            return false;
        } else if (tick.getVolBid() == 0) {
            return false;
        } else if (tick.getAsk() == null) {
            return false;
        }
        if (tick.getVolAsk() == 0) {
            return false;
        }

        return super.validateTick(tick);
    }
}
