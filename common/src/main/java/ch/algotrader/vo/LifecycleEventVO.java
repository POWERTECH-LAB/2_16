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
package ch.algotrader.vo;

import java.io.Serializable;
import java.util.Date;

import ch.algotrader.enumeration.LifecyclePhase;
import ch.algotrader.enumeration.OperationMode;

/**
 * EVent signalling progression to another strategy lifecycle phase.
 */
public class LifecycleEventVO implements Serializable {

    private static final long serialVersionUID = -3004938183561866728L;

    private final OperationMode operationMode;
    private final LifecyclePhase phase;
    private final Date time;

    public LifecycleEventVO(final OperationMode operationMode, final LifecyclePhase phase, final Date time) {
        this.operationMode = operationMode;
        this.phase = phase;
        this.time = time;
    }

    public OperationMode getOperationMode() {
        return this.operationMode;
    }

    public LifecyclePhase getPhase() {
        return this.phase;
    }

    public Date getTime() {
        return time;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("LifecycleEventVO[");
        sb.append("operationMode=").append(this.operationMode);
        sb.append(", phase=").append(this.phase);
        sb.append(", time=").append(this.time);
        sb.append(']');
        return sb.toString();
    }

}
