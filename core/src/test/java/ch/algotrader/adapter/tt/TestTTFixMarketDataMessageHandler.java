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
package ch.algotrader.adapter.tt;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import ch.algotrader.adapter.fix.fix42.FixTestUtils;
import ch.algotrader.esper.Engine;
import ch.algotrader.util.DateTimeLegacy;
import ch.algotrader.vo.marketData.AskVO;
import ch.algotrader.vo.marketData.BidVO;
import quickfix.ConfigError;
import quickfix.DataDictionary;
import quickfix.fix42.MarketDataRequestReject;
import quickfix.fix42.MarketDataSnapshotFullRefresh;

/**
 * @author <a href="mailto:okalnichevski@algotrader.ch">Oleg Kalnichevski</a>
 *
 * @version $Revision$ $Date$
 */
public class TestTTFixMarketDataMessageHandler {

    private final static DataDictionary DATA_DICTIONARY;

    static {
        try {
            DATA_DICTIONARY = new DataDictionary("tt/FIX42.xml");
            DATA_DICTIONARY.setCheckUnorderedGroupFields(false);
        } catch (ConfigError configError) {
            throw new Error(configError);
        }
    }

    @Mock
    private Engine engine;

    private TTFixMarketDataMessageHandler impl;

    @Before
    public void setup() throws Exception {

        MockitoAnnotations.initMocks(this);

        this.impl = new TTFixMarketDataMessageHandler(engine);
    }

    @Test
    public void testMarketDataFullRefresh() throws Exception {

        String s = "8=FIX.4.2|9=00202|35=W|49=TTDEV14P|56=RATKODTS2|34=2|52=20150930-12:27:39.456|55=CL|48=00A0KP00CLZ|" +
                "10455=CLX5|167=FUT|207=CME|15=USD|262=1|200=201511|387=4485|268=2|269=0|290=1|270=4520|271=3|269=1|" +
                "290=1|270=4521|271=14|10=233|";
        MarketDataSnapshotFullRefresh fullRefresh = FixTestUtils.parseFix42Message(s, DATA_DICTIONARY, MarketDataSnapshotFullRefresh.class);
        Assert.assertNotNull(fullRefresh);

        this.impl.onMessage(fullRefresh, FixTestUtils.fakeFix42Session());

        ArgumentCaptor<Object> argCaptor1 = ArgumentCaptor.forClass(Object.class);
        Mockito.verify(this.engine, Mockito.atLeast(2)).sendEvent(argCaptor1.capture());

        List<Object> events = argCaptor1.getAllValues();
        Assert.assertNotNull(events);
        Assert.assertEquals(2, events.size());

        Object event1 = events.get(0);
        Assert.assertTrue(event1 instanceof BidVO);
        BidVO bid = (BidVO) event1;
        Assert.assertEquals("1", bid.getTickerId());
        Assert.assertEquals(new BigDecimal("4520.00000"), new BigDecimal(bid.getBid()).setScale(5, RoundingMode.HALF_EVEN));
        Assert.assertEquals(3, bid.getVolBid());
        Assert.assertEquals(DateTimeLegacy.parseAsDateTimeMilliGMT("2015-09-30 12:27:39.456"), bid.getDateTime());

        Object event2 = events.get(1);
        Assert.assertTrue(event2 instanceof AskVO);

        AskVO ask = (AskVO) event2;
        Assert.assertEquals("1", ask.getTickerId());
        Assert.assertEquals(new BigDecimal("4521.00000"), new BigDecimal(ask.getAsk()).setScale(5, RoundingMode.HALF_EVEN));
        Assert.assertEquals(14, ask.getVolAsk());
        Assert.assertEquals(DateTimeLegacy.parseAsDateTimeMilliGMT("2015-09-30 12:27:39.456"), ask.getDateTime());
    }

    @Test
    public void testMarketDataFullRefreshReject() throws Exception {

        String s = "8=FIX.4.2|9=00116|35=Y|49=TTDEV14P|56=RATKODTS2|34=2|52=20150930-12:31:47.465|262=stuff|" +
                "58=Unknown or missing security type: Entry #1|10=123|";
        MarketDataRequestReject reject = FixTestUtils.parseFix42Message(s, DATA_DICTIONARY, MarketDataRequestReject.class);
        Assert.assertNotNull(reject);

        this.impl.onMessage(reject, FixTestUtils.fakeFix42Session());
        Mockito.verify(this.engine, Mockito.never()).sendEvent(Mockito.any());
    }

}
