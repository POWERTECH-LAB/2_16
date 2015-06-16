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
package ch.algotrader.wiring.server.external;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import ch.algotrader.adapter.fix.ManagedFixAdapter;
import ch.algotrader.adapter.fix.MarketDataFixSessionStateHolder;
import ch.algotrader.entity.security.SecurityDao;
import ch.algotrader.esper.Engine;
import ch.algotrader.esper.EngineManager;
import ch.algotrader.service.OrderService;
import ch.algotrader.service.ftx.FTXFixMarketDataService;
import ch.algotrader.service.ftx.FTXFixMarketDataServiceImpl;
import ch.algotrader.service.ftx.FTXFixOrderService;
import ch.algotrader.service.ftx.FTXFixOrderServiceImpl;

/**
 * Fortex Fix service configuration.
 */
@Configuration
public class FTXFixServiceWiring {

    @Profile("fTXFix")
    @Bean(name = "fTXFixOrderService")
    public FTXFixOrderService createFTXFixOrderService(
            final ManagedFixAdapter fixAdapter,
            final OrderService orderService,
            final Engine serverEngine) {

        return new FTXFixOrderServiceImpl(fixAdapter, orderService, serverEngine);
    }

    @Profile("fTXMarketData")
    @Bean(name = "fTXFixMarketDataService")
    public FTXFixMarketDataService createFTXFixMarketDataService(
            final MarketDataFixSessionStateHolder fTXMarketDataSessionStateHolder,
            final ManagedFixAdapter fixAdapter,
            final EngineManager engineManager,
            final SecurityDao securityDao) {

        return new FTXFixMarketDataServiceImpl(fTXMarketDataSessionStateHolder, fixAdapter, engineManager, securityDao);
    }

}
