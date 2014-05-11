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
package ch.algotrader.adapter.fix;

import ch.algotrader.entity.security.Security;
import ch.algotrader.enumeration.FeedType;
import ch.algotrader.service.fix.fix44.Fix44MarketDataServiceBase;
import quickfix.field.MDReqID;
import quickfix.field.SubscriptionRequestType;
import quickfix.fix44.MarketDataRequest;

/**
 * Mock FIX 4.4 market data service
 *
 * @author <a href="mailto:okalnichevski@algotrader.ch">Oleg Kalnichevski</a>
 */
class FakeFix44MarketDataService extends Fix44MarketDataServiceBase {


    @Override
    protected void handleSendSubscribeRequest(final Security security) throws Exception {

        String symbol = security.getSymbol();
        MarketDataRequest request = new MarketDataRequest();
        request.set(new MDReqID(symbol));
        request.set(new SubscriptionRequestType(SubscriptionRequestType.SNAPSHOT_PLUS_UPDATES));
        getFixAdapter().sendMessage(request, "FAKE");
    }

    @Override
    protected void handleSendUnsubscribeRequest(final Security security) throws Exception {

        String symbol = security.getSymbol();
        MarketDataRequest request = new MarketDataRequest();
        request.set(new MDReqID(symbol));
        request.set(new SubscriptionRequestType(SubscriptionRequestType.DISABLE_PREVIOUS_SNAPSHOT_PLUS_UPDATE_REQUEST));
        getFixAdapter().sendMessage(request, "FAKE");
    }

    @Override
    protected String handleGetSessionQualifier() throws Exception {

        return "FAKE";
    }

    @Override
    protected String handleGetTickerId(final Security security) throws Exception {

        return security.getSymbol();
    }

    @Override
    protected FeedType handleGetFeedType() throws Exception {

        return FeedType.SIM;
    }

}
