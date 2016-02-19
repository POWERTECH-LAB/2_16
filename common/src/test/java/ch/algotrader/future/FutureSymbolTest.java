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
package ch.algotrader.future;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ch.algotrader.entity.security.SecurityFamily;
import ch.algotrader.entity.security.SecurityFamilyImpl;
import ch.algotrader.util.DateTimeUtil;

/**
 * @author <a href="mailto:okalnichevski@algotrader.ch">Oleg Kalnichevski</a>
 */
public class FutureSymbolTest {

    private SecurityFamily securityFamily;

    @Before
    public void setup() {

        securityFamily = new SecurityFamilyImpl();
        securityFamily.setSymbolRoot("CL");
    }

    @Test
    public void testGetSymbol() throws Exception {

        Assert.assertEquals("CL JAN/15", FutureSymbol.getSymbol(securityFamily, DateTimeUtil.parseLocalDate("2015-01-01")));
        Assert.assertEquals("CL JUN/15", FutureSymbol.getSymbol(securityFamily, DateTimeUtil.parseLocalDate("2015-06-01")));
    }

    @Test
    public void testGetIsin() throws Exception {

        Assert.assertEquals("0FCLFF00000", FutureSymbol.getIsin(securityFamily, DateTimeUtil.parseLocalDate("2015-01-01")));
        Assert.assertEquals("0FCLMF00000", FutureSymbol.getIsin(securityFamily, DateTimeUtil.parseLocalDate("2015-06-01")));
    }

    @Test
    public void testGetRic() throws Exception {

        Assert.assertEquals("CLF5:VE", FutureSymbol.getRic(securityFamily, DateTimeUtil.parseLocalDate("2015-01-01")));
        Assert.assertEquals("CLM5:VE", FutureSymbol.getRic(securityFamily, DateTimeUtil.parseLocalDate("2015-06-01")));
    }

    @Test
    public void testGetMaturityFromRic() throws Exception {

        Assert.assertEquals(DateTimeUtil.parseLocalDate("2015-01-01"), FutureSymbol.getMaturityFromRic("CLF5:VE"));
        Assert.assertEquals(DateTimeUtil.parseLocalDate("2015-06-01"), FutureSymbol.getMaturityFromRic("CLM5:VE"));
    }
}
