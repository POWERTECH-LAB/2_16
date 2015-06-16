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
package ch.algotrader.service.ftx;

import org.apache.commons.lang.Validate;

import ch.algotrader.adapter.cnx.CNXUtil;
import ch.algotrader.adapter.fix.FixAdapter;
import ch.algotrader.adapter.fix.FixApplicationException;
import ch.algotrader.adapter.fix.FixSessionLifecycle;
import ch.algotrader.adapter.ftx.FTXFixMarketDataRequestFactory;
import ch.algotrader.config.CommonConfig;
import ch.algotrader.entity.security.Forex;
import ch.algotrader.entity.security.Security;
import ch.algotrader.entity.security.SecurityDao;
import ch.algotrader.enumeration.FeedType;
import ch.algotrader.service.fix.fix44.Fix44MarketDataServiceImpl;
import quickfix.fix44.QuoteRequest;

/**
 * @author <a href="mailto:okalnichevski@algotrader.ch">Oleg Kalnichevski</a>
 */
public class FTXFixMarketDataServiceImpl extends Fix44MarketDataServiceImpl implements FTXFixMarketDataService {

    private static final long serialVersionUID = 2946126163433296876L;

    private final FTXFixMarketDataRequestFactory requestFactory;

    public FTXFixMarketDataServiceImpl(
            final CommonConfig commonConfig,
            final FixSessionLifecycle lifeCycle,
            final FixAdapter fixAdapter,
            final SecurityDao securityDao) {

        super(commonConfig, lifeCycle, fixAdapter, securityDao);

        this.requestFactory = new FTXFixMarketDataRequestFactory();
    }

    @Override
    public void sendSubscribeRequest(Security security) {

        Validate.notNull(security, "Security is null");

        QuoteRequest request = this.requestFactory.create(security, 1);

        getFixAdapter().sendMessage(request, getSessionQualifier());
    }

    @Override
    public String getTickerId(Security security) {

        Validate.notNull(security, "Security is null");

        if (!(security instanceof Forex)) {
            throw new FixApplicationException("Fortex supports forex orders only");
        }
        Forex forex = (Forex) security;
        return CNXUtil.getCNXSymbol(forex);
    }

    @Override
    public void sendUnsubscribeRequest(Security security) {

        Validate.notNull(security, "Security is null");

        QuoteRequest request = this.requestFactory.create(security, 2);

        getFixAdapter().sendMessage(request, getSessionQualifier());
    }

    @Override
    public String getSessionQualifier() {

        return "FTXMD";
    }

    @Override
    public FeedType getFeedType() {

        return FeedType.FTX;
    }
}
