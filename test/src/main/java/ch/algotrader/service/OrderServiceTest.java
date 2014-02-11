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
package ch.algotrader.service;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import ch.algotrader.ServiceLocator;
import ch.algotrader.entity.Account;
import ch.algotrader.entity.security.Security;
import ch.algotrader.entity.strategy.Strategy;
import ch.algotrader.entity.trade.LimitOrder;
import ch.algotrader.entity.trade.Order;
import ch.algotrader.enumeration.Side;
import ch.algotrader.util.RoundUtil;

/**
 * @author <a href="mailto:aflury@algotrader.ch">Andy Flury</a>
 *
 * @version $Revision$ $Date$
 */
public class OrderServiceTest extends IBServiceTest {

    @Test
    public void test() throws InterruptedException {

        OrderService orderService = ServiceLocator.instance().getOrderService();
        LookupService lookupService = ServiceLocator.instance().getLookupService();

        Strategy strategy = lookupService.getStrategyByName("BASE");
        Account account = lookupService.getAccountByName("IB_NATIVE_TEST");

        int[] ids = { 8, 9, 10 };

        List<Order> orders = new ArrayList<Order>();
        for (int i : ids) {

            Security security = lookupService.getSecurity(i);

            LimitOrder order = LimitOrder.Factory.newInstance();
            order.setQuantity(20000);
            order.setSide(Side.BUY);
            order.setLimit(RoundUtil.getBigDecimal(1.0));
            order.setSecurity(security);
            order.setStrategy(strategy);
            order.setAccount(account);

            orders.add(order);
        }

        for (Order order : orders) {
            orderService.sendOrder(order);
        }

        for (Order order : orders) {
            orderService.cancelOrder(order);
        }
    }
}
