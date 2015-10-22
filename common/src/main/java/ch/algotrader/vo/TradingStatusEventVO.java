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
package ch.algotrader.vo;

import java.io.Serializable;
import java.util.Date;

import ch.algotrader.enumeration.TradingStatus;

public class TradingStatusEventVO implements Serializable  {

    private static final long serialVersionUID = 8956159082770895558L;

    private final TradingStatus status;
    private final long securityId;
    private final String feedType;
    private final Date dateTime;

    public TradingStatusEventVO(final TradingStatus status, final long securityId, final String feedType, final Date dateTime) {
        this.status = status;
        this.securityId = securityId;
        this.feedType = feedType;
        this.dateTime = dateTime;
    }

    public TradingStatus getStatus() {
        return this.status;
    }

    public long getSecurityId() {
        return this.securityId;
    }

    public String getFeedType() {
        return this.feedType;
    }

    public Date getDateTime() {
        return this.dateTime;
    }

    @Override
    public String toString() {
        return "{" +
                "status=" + this.status +
                ", securityId=" + this.securityId +
                ", feedType=" + this.feedType +
                ", dateTime=" + this.dateTime +
                '}';
    }

}