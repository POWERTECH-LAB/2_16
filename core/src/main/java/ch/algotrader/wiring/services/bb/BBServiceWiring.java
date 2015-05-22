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
package ch.algotrader.wiring.services.bb;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import ch.algotrader.adapter.bb.BBAdapter;
import ch.algotrader.entity.marketData.BarDao;
import ch.algotrader.entity.marketData.TickDao;
import ch.algotrader.entity.security.FutureDao;
import ch.algotrader.entity.security.OptionDao;
import ch.algotrader.entity.security.SecurityDao;
import ch.algotrader.entity.security.SecurityFamilyDao;
import ch.algotrader.esper.EngineManager;
import ch.algotrader.service.bb.BBHistoricalDataService;
import ch.algotrader.service.bb.BBHistoricalDataServiceImpl;
import ch.algotrader.service.bb.BBMarketDataService;
import ch.algotrader.service.bb.BBMarketDataServiceImpl;
import ch.algotrader.service.bb.BBReferenceDataService;
import ch.algotrader.service.bb.BBReferenceDataServiceImpl;

/**
 * Bloomberg service configuration.
 */
@Configuration
public class BBServiceWiring {

    @Profile("bBHistoricalData")
    @Bean(name = {"bBHistoricalDataService", "historicalDataService"})
    public BBHistoricalDataService createBBHistoricalDataService(
            final BBAdapter bBAdapter,
            final SecurityDao securityDao,
            final BarDao barDao) {

        return new BBHistoricalDataServiceImpl(bBAdapter, securityDao, barDao);
    }

    @Profile("bBMarketData")
    @Bean(name = "bBMarketDataService")
    public BBMarketDataService createBBMarketDataService(
            final BBAdapter bBAdapter,
            final EngineManager engineManager,
            final TickDao tickDao,
            final SecurityDao securityDao) {

        return new BBMarketDataServiceImpl(bBAdapter, engineManager, tickDao, securityDao);
    }

    @Profile("bBReferenceData")
    @Bean(name = {"bBReferenceDataService", "referenceDataService"})
    public BBReferenceDataService createBBReferenceDataService(
            final BBAdapter bBAdapter,
            final SecurityFamilyDao securityFamilyDao,
            final OptionDao optionDao,
            final FutureDao futureDao) {

        return new BBReferenceDataServiceImpl(bBAdapter, securityFamilyDao, optionDao, futureDao);
    }

}
