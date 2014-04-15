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
package ch.algotrader.entity.security;

import java.util.Date;

import org.junit.Before;
import org.junit.Test;

import ch.algotrader.ServiceLocator;
import ch.algotrader.entity.EntityTest;
import ch.algotrader.entity.security.EasyToBorrowDao;
import ch.algotrader.enumeration.Broker;

/**
 * @author <a href="mailto:amasood@algotrader.ch">Ahmad Masood</a>
 *
 * @version $Revision$ $Date$
 */
public class EasyToBorrowTest extends EntityTest {

    private EasyToBorrowDao easyToBorrowDao;

    @Before
    public void before(){
        this.easyToBorrowDao = ServiceLocator.instance().getService("easyToBorrowDao", EasyToBorrowDao.class);
    }

    @Test
    public void testFindByDateAndBroker() {

        this.easyToBorrowDao.findByDateAndBroker(new Date(),Broker.DC);
    }

}
