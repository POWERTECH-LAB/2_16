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
package ch.algotrader.entity;

import org.junit.Assert;
import org.junit.Test;

import ch.algotrader.entity.security.OptionImpl;
import ch.algotrader.entity.security.Security;
import ch.algotrader.entity.security.StockImpl;
import ch.algotrader.entity.strategy.Strategy;
import ch.algotrader.entity.strategy.StrategyImpl;

/**
 * @author <a href="mailto:aflury@algotrader.ch">Andy Flury</a>
 *
 * @version $Revision$ $Date$
 */
public class SubscriptionTest {

    @Test
    public void testEquals() {

        Subscription subscription1 = new SubscriptionImpl();
        Subscription subscription2 = new SubscriptionImpl();

        Assert.assertNotEquals(subscription1, subscription2);

        Strategy strategy = new StrategyImpl();
        strategy.setId(1);

        subscription1.setStrategy(strategy);

        Assert.assertNotEquals(subscription1, subscription2);

        Security security1 = new StockImpl();
        security1.setId(2);

        subscription1.setSecurity(security1);

        Assert.assertNotEquals(subscription1, subscription2);

        subscription2.setStrategy(strategy);
        subscription2.setSecurity(security1);

        Assert.assertEquals(subscription1, subscription2);

        Security security2 = new OptionImpl();
        security2.setId(2);

        subscription2.setSecurity(security2);

        Assert.assertNotEquals(subscription1, subscription2);
    }
}
