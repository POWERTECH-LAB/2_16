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
package ch.algotrader.wiring.client.service;

import java.util.Collection;
import java.util.Iterator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import ch.algotrader.config.CommonConfig;
import ch.algotrader.config.ConfigParams;
import ch.algotrader.esper.Engine;
import ch.algotrader.esper.EngineManager;
import ch.algotrader.service.CombinationService;
import ch.algotrader.service.LocalLookupService;
import ch.algotrader.service.LookupService;
import ch.algotrader.service.ManagementService;
import ch.algotrader.service.ManagementServiceImpl;
import ch.algotrader.service.MarketDataService;
import ch.algotrader.service.OrderService;
import ch.algotrader.service.PortfolioChartService;
import ch.algotrader.service.PortfolioChartServiceImpl;
import ch.algotrader.service.PortfolioService;
import ch.algotrader.service.PositionService;
import ch.algotrader.service.PropertyService;
import ch.algotrader.service.SubscriptionService;
import ch.algotrader.vo.ChartDefinitionVO;

/**
 * Management Service configuration.
 *
 * @version $Revision$ $Date$
 */
@Profile(value = "live")
@Configuration
public class ClientManagementServiceWiring {

    private static final Logger LOGGER = LogManager.getLogger(ClientManagementServiceWiring.class);

    private Engine getMainEngine(final EngineManager engineManager) {
        Engine engine;
        Collection<Engine> strategyEngines = engineManager.getStrategyEngines();
        if (strategyEngines.isEmpty()) {
            throw new IllegalStateException("No strategy engine found");
        } else {
            Iterator<Engine> it = strategyEngines.iterator();
            engine = it.next();
            if (it.hasNext()) {
                if (LOGGER.isWarnEnabled()) {
                    LOGGER.warn("Management services do not support multiple strategies. Using strategy {}", engine.getStrategyName());
                }
            }
        }
        return engine;
    }

    @Bean(name = "managementService")
    public ManagementService createManagementService(
            final CommonConfig commonConfig,
            final EngineManager engineManager,
            final SubscriptionService subscriptionService,
            final LookupService lookupService,
            final LocalLookupService localLookupService,
            final PortfolioService portfolioService,
            final OrderService orderService,
            final PositionService positionService,
            final CombinationService combinationService,
            final PropertyService propertyService,
            final MarketDataService marketDataService,
            final ConfigParams configParams) {

        return new ManagementServiceImpl(commonConfig, getMainEngine(engineManager), subscriptionService, lookupService, localLookupService, portfolioService, orderService, positionService,
                combinationService, propertyService, marketDataService, configParams);
    }

    @Bean(name = "portfolioChartService")
    public PortfolioChartService createPortfolioChartService(
            final ChartDefinitionVO portfolioChartDefinition,
            final EngineManager engineManager,
            final PortfolioService portfolioService) {

        return new PortfolioChartServiceImpl(portfolioChartDefinition, getMainEngine(engineManager).getStrategyName(), portfolioService);
    }

}
