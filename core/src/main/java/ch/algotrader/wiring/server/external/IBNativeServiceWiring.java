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

import java.util.Set;
import java.util.concurrent.LinkedBlockingDeque;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import ch.algotrader.adapter.ib.AccountUpdate;
import ch.algotrader.adapter.ib.IBIdGenerator;
import ch.algotrader.adapter.ib.IBOrderMessageFactory;
import ch.algotrader.adapter.ib.IBPendingRequests;
import ch.algotrader.adapter.ib.IBSession;
import ch.algotrader.adapter.ib.IBSessionStateHolder;
import ch.algotrader.config.IBConfig;
import ch.algotrader.dao.marketData.BarDao;
import ch.algotrader.dao.marketData.TickDao;
import ch.algotrader.dao.security.FutureDao;
import ch.algotrader.dao.security.OptionDao;
import ch.algotrader.dao.security.SecurityDao;
import ch.algotrader.dao.security.SecurityFamilyDao;
import ch.algotrader.dao.security.StockDao;
import ch.algotrader.esper.Engine;
import ch.algotrader.service.OrderService;
import ch.algotrader.service.ib.IBNativeAccountService;
import ch.algotrader.service.ib.IBNativeAccountServiceImpl;
import ch.algotrader.service.ib.IBNativeHistoricalDataService;
import ch.algotrader.service.ib.IBNativeHistoricalDataServiceImpl;
import ch.algotrader.service.ib.IBNativeMarketDataService;
import ch.algotrader.service.ib.IBNativeMarketDataServiceImpl;
import ch.algotrader.service.ib.IBNativeOrderService;
import ch.algotrader.service.ib.IBNativeOrderServiceImpl;
import ch.algotrader.service.ib.IBNativeReferenceDataService;
import ch.algotrader.service.ib.IBNativeReferenceDataServiceImpl;

/**
 * IB native service configuration.
 */
@Configuration
public class IBNativeServiceWiring {

    @Profile("iBNative")
    @Bean(name = {"iBNativeAccountService", "accountService"})
    public IBNativeAccountService createIBNativeAccountService(
            final LinkedBlockingDeque<AccountUpdate> iBAccountUpdateQueue,
            final LinkedBlockingDeque<Set<String>> accountsQueue,
            final LinkedBlockingDeque<ch.algotrader.adapter.ib.Profile> profilesQueue,
            final IBSession iBSession) {

        return new IBNativeAccountServiceImpl(iBAccountUpdateQueue, accountsQueue, profilesQueue, iBSession);
    }

    @Profile("iBNative")
    @Bean(name = "iBNativeOrderService")
    public IBNativeOrderService createIBNativeOrderService(
            final IBSession iBSession,
            final IBIdGenerator iBIdGenerator,
            final IBOrderMessageFactory iBOrderMessageFactory,
            final Engine serverEngine,
            final OrderService orderService) {

        return new IBNativeOrderServiceImpl(iBSession, iBIdGenerator, iBOrderMessageFactory, serverEngine, orderService);
    }

    @Profile("iBHistoricalData")
    @Bean(name = {"iBNativeHistoricalDataService", "historicalDataService"})
    public IBNativeHistoricalDataService createIBNativeHistoricalDataService(
            final IBSession iBSession,
            final IBPendingRequests iBPendingRequests,
            final IBIdGenerator iBIdGenerator,
            final SecurityDao securityDao,
            final BarDao barDao) {

        return new IBNativeHistoricalDataServiceImpl(iBSession, iBPendingRequests, iBIdGenerator, securityDao, barDao);
    }

    @Profile("iBMarketData")
    @Bean(name = "iBNativeMarketDataService")
    public IBNativeMarketDataService createIBNativeMarketDataService(
            final IBSession iBSession,
            final IBSessionStateHolder iBSessionStateHolder,
            final IBIdGenerator iBIdGenerator,
            final IBConfig iBConfig,
            final Engine serverEngine,
            final TickDao tickDao) {

        return new IBNativeMarketDataServiceImpl(iBSession, iBSessionStateHolder, iBIdGenerator, iBConfig, serverEngine, tickDao);
    }

    @Profile("iBReferenceData")
    @Bean(name = { "iBNativeReferenceDataService", "referenceDataService" })
    public IBNativeReferenceDataService createIBNativeReferenceDataService(
            final IBSession iBSession,
            final IBPendingRequests iBPendingRequests,
            final IBIdGenerator iBIdGenerator,
            final OptionDao optionDao,
            final FutureDao futureDao,
            final SecurityFamilyDao securityFamilyDao,
            final StockDao stockDao) {

        return new IBNativeReferenceDataServiceImpl(iBSession, iBPendingRequests, iBIdGenerator, optionDao, futureDao, securityFamilyDao, stockDao);
    }

}
