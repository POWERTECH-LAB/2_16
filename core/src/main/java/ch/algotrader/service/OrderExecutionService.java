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
package ch.algotrader.service;

import ch.algotrader.entity.trade.ExecutionStatusVO;
import ch.algotrader.entity.trade.ExternalFill;
import ch.algotrader.entity.trade.Fill;
import ch.algotrader.entity.trade.Order;
import ch.algotrader.entity.trade.OrderCompletionVO;
import ch.algotrader.entity.trade.OrderDetailsVO;
import ch.algotrader.entity.trade.OrderStatus;

/**
 * @author <a href="mailto:aflury@algotrader.ch">Andy Flury</a>
 */
public interface OrderExecutionService {

    /**
     * Handles incoming {@link OrderStatus}.
     */
    void handleOrderStatus(OrderStatus orderStatus);

    /**
     * Handles incoming {@link Fill}.
     */
    void handleFill(Fill fill);

    /**
     * Handles incoming {@link ExternalFill}.
     */
    void handleFill(ExternalFill fill);

    /**
     * Handles generated {@link OrderCompletionVO}.
     */
    void handleOrderCompletion(final OrderCompletionVO orderCompletion);

    /**
     * Looks up  {@code IntId} by {@code ExtId}.
     */
    String lookupIntId(String extId);

    /**
     * Gets an open order details by its {@code intId}.
     */
    OrderDetailsVO getOpenOrderDetailsByIntId(String intId);

    /**
     * Returns execution status of the order with the given {@code IntId} or {@code null}
     * if an order with this {@code IntId} has been fully executed.
     */
    ExecutionStatusVO getStatusByIntId(String intId);

    /**
     * Gets an open order by its {@code intId}.
     */
    Order getOpenOrderByIntId(String intId);

}
