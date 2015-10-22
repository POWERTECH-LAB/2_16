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
package ch.algotrader.adapter.cnx;

import org.apache.commons.lang.Validate;

import ch.algotrader.adapter.RequestIdGenerator;
import ch.algotrader.adapter.fix.FixApplicationException;
import ch.algotrader.entity.security.Forex;
import ch.algotrader.entity.security.Security;
import quickfix.field.AggregatedBook;
import quickfix.field.MDEntryType;
import quickfix.field.MDReqID;
import quickfix.field.MDUpdateType;
import quickfix.field.MarketDepth;
import quickfix.field.SubscriptionRequestType;
import quickfix.field.Symbol;
import quickfix.fix44.MarketDataRequest;

/**
 * Currenex market data request factory.
 *
 * @author <a href="mailto:okalnichevski@algotrader.ch">Oleg Kalnichevski</a>
 */
public class CNXFixMarketDataRequestFactory {

    private final RequestIdGenerator<Security> tickerIdGenerator;

    public CNXFixMarketDataRequestFactory(final RequestIdGenerator<Security> tickerIdGenerator) {

        Validate.notNull(tickerIdGenerator, "RequestIdGenerator is null");

        this.tickerIdGenerator = tickerIdGenerator;
    }

    public MarketDataRequest create(final Security security, final char type) {

        if (!(security instanceof Forex)) {
            throw new FixApplicationException("Currenex supports Forex only");
        }
        Forex forex = (Forex) security;

        MarketDataRequest request = new MarketDataRequest();
        request.set(new MDReqID(this.tickerIdGenerator.generateId(forex)));
        request.set(new SubscriptionRequestType(type));
        request.set(new MarketDepth(1)); // top of the book
        request.set(new MDUpdateType(MDUpdateType.INCREMENTAL_REFRESH));
        request.set(new AggregatedBook(true));

        MarketDataRequest.NoMDEntryTypes bid = new MarketDataRequest.NoMDEntryTypes();
        bid.set(new MDEntryType(MDEntryType.BID));
        request.addGroup(bid);

        MarketDataRequest.NoMDEntryTypes offer = new MarketDataRequest.NoMDEntryTypes();
        offer.set(new MDEntryType(MDEntryType.OFFER));
        request.addGroup(offer);

        MarketDataRequest.NoRelatedSym symbol = new MarketDataRequest.NoRelatedSym();
        symbol.set(new Symbol(CNXUtil.getCNXSymbol(forex)));
        request.addGroup(symbol);

        return request;
    }

}
