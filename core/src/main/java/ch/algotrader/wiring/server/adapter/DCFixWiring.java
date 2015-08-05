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
package ch.algotrader.wiring.server.adapter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import quickfix.SessionSettings;
import ch.algotrader.adapter.dc.DCFixMarketDataMessageHandler;
import ch.algotrader.adapter.dc.DCFixOrderMessageHandler;
import ch.algotrader.adapter.fix.DefaultFixApplicationFactory;
import ch.algotrader.adapter.fix.DefaultFixSessionStateHolder;
import ch.algotrader.adapter.fix.DefaultLogonMessageHandler;
import ch.algotrader.adapter.fix.FixApplicationFactory;
import ch.algotrader.adapter.fix.FixSessionStateHolder;
import ch.algotrader.adapter.fix.MarketDataFixSessionStateHolder;
import ch.algotrader.enumeration.FeedType;
import ch.algotrader.esper.Engine;
import ch.algotrader.event.dispatch.EventDispatcher;
import ch.algotrader.service.MarketDataService;
import ch.algotrader.service.OrderService;

/**
 * Dukas Copy FIX configuration.
 */
@Configuration
public class DCFixWiring {

    @Profile({"dCMarketData", "dCFix"})
    @Bean(name = "dCLogonMessageHandler")
    public DefaultLogonMessageHandler createDCLogonMessageHandler(final SessionSettings fixSessionSettings) {

        return new DefaultLogonMessageHandler(fixSessionSettings);
    }

    @Profile("dCFix")
    @Bean(name = "dCOrderSessionStateHolder")
    public FixSessionStateHolder createDCOrderSessionStateHolder(final EventDispatcher eventDispatcher) {

        return new DefaultFixSessionStateHolder("DCT", eventDispatcher);
    }

    @Profile("dCFix")
    @Bean(name = "dCOrderApplicationFactory")
    public FixApplicationFactory createDCOrderApplicationFactory(final OrderService orderService,
            final Engine serverEngine,
            final DefaultLogonMessageHandler dCLogonMessageHandler,
            final FixSessionStateHolder dCOrderSessionStateHolder) {

        DCFixOrderMessageHandler dCFixOrderMessageHandler = new DCFixOrderMessageHandler(orderService, serverEngine);
        return new DefaultFixApplicationFactory(dCFixOrderMessageHandler, dCLogonMessageHandler, dCOrderSessionStateHolder);
    }

    @Profile("dCMarketData")
    @Bean(name = "dCMarketDataSessionStateHolder")
    public MarketDataFixSessionStateHolder createDCMarketDataSessionStateHolder(
            final EventDispatcher eventDispatcher,
            final MarketDataService marketDataService) {

        return new MarketDataFixSessionStateHolder("DCMD", eventDispatcher, marketDataService, FeedType.DC);
    }

    @Profile("dCMarketData")
    @Bean(name = "dCMarketDataApplicationFactory")
    public FixApplicationFactory createDCMarketDataApplicationFactory(
            final Engine serverEngine,
            final DefaultLogonMessageHandler dCLogonMessageHandler,
            final MarketDataFixSessionStateHolder dCMarketDataSessionStateHolder) {

        DCFixMarketDataMessageHandler dcFixMarketDataMessageHandler = new DCFixMarketDataMessageHandler(serverEngine);
        return new DefaultFixApplicationFactory(dcFixMarketDataMessageHandler, dCLogonMessageHandler, dCMarketDataSessionStateHolder);
    }

}
