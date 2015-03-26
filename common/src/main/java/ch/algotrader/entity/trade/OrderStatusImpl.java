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
package ch.algotrader.entity.trade;


/**
 * @author <a href="mailto:aflury@algotrader.ch">Andy Flury</a>
 *
 * @version $Revision$ $Date$
 */
public class OrderStatusImpl extends OrderStatus {

    private static final long serialVersionUID = -423135654204518265L;

    @Override
    public String toString() {

        StringBuilder buffer = new StringBuilder();

        buffer.append(getStatus());

        if (getOrder() != null) {
            buffer.append(",");
            buffer.append(getOrder().getDescription());
        }

        buffer.append(",filledQuantity=");
        buffer.append(getFilledQuantity());
        buffer.append(",remainingQuantity=");
        buffer.append(getRemainingQuantity());

        if (getAvgPrice() != null) {
            buffer.append(",avgPrice=");
            buffer.append(getAvgPrice());
        }

        if (getLastPrice() != null) {
            buffer.append(",lastPrice=");
            buffer.append(getLastPrice());
        }

        if (getReason() != null) {
            buffer.append(",reason=");
            buffer.append(getReason());
        }

        return buffer.toString();
    }
}
