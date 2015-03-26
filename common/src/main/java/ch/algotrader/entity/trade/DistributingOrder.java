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

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.algotrader.entity.marketData.Tick;

/**
 * @author <a href="mailto:aflury@algotrader.ch">Andy Flury</a>
 *
 * @version $Revision$ $Date$
 */
public class DistributingOrder extends AlgoOrder {

    private static final Logger logger = LogManager.getLogger(DistributingOrder.class);

    private static final long serialVersionUID = -3256407214793599390L;

    @Override
    public String getExtDescription() {
        return getOrderProperties().toString();
    }

    @Override
    public void validate() throws OrderValidationException {
        // do nothing
    }

    @Override
    public List<SimpleOrder> getInitialOrders(Tick tick) {

        List<SimpleOrder> orders = new ArrayList<>();
        long totalQuantity = 0;
        StringBuilder buffer = new StringBuilder();
        for (Allocation allocation : getAllocations()) {

            // qty proportional to allocation
            long quantity = Math.round(getQuantity() * allocation.getValue());
            totalQuantity += quantity;

            // create the market order
            SimpleOrder order = MarketOrder.Factory.newInstance();
            order.setSecurity(this.getSecurity());
            order.setStrategy(this.getStrategy());
            order.setSide(this.getSide());
            order.setQuantity(quantity);
            order.setAccount(allocation.getAccount());

            // associate the childOrder with the parentOrder(this)
            order.setParentOrder(this);

            orders.add(order);

            buffer.append(order.getAccount() + "(" + order.getQuantity() + ") ");
        }

        // adjust quantity in case of rounding issue
        if (totalQuantity != getQuantity()) {
            logger.info("adjusting totalQuantity of " + this + " from " + getQuantity() + " to " + totalQuantity);
            setQuantity(totalQuantity);
        }

        logger.info("created child orders for " + this + " " + buffer.toString());

        return orders;
    }
}
