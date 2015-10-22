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
package ch.algotrader.entity.trade;

/**
 * @author <a href="mailto:aflury@algotrader.ch">Andy Flury</a>
 */
public class AllocationImpl extends Allocation {

    private static final long serialVersionUID = 1184764642126535447L;

    @Override
    public String toString() {

        StringBuilder buffer = new StringBuilder();
        buffer.append(getAccount());
        buffer.append(",");
        buffer.append(getValue());
        return buffer.toString();
    }

}
