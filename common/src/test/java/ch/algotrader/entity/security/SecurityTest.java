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

import org.junit.Assert;
import org.junit.Test;

/**
 * @author <a href="mailto:aflury@algotrader.ch">Andy Flury</a>
 *
 * @version $Revision$ $Date$
 */
public class SecurityTest {

    @Test
    public void testEqualsIsin() {

        Security security1 = new StockImpl();
        Security security2 = new StockImpl();

        Assert.assertEquals(security1, security2);

        security1.setIsin("AAA");

        Assert.assertNotEquals(security1, security2);

        security2.setIsin("BBB");

        Assert.assertNotEquals(security1, security2);

        Security security3 = new StockImpl();

        security3.setIsin("AAA");

        Assert.assertEquals(security1, security3);
    }
}
