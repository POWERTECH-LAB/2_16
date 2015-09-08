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
import ch.algotrader.config.CommonConfig;
import ch.algotrader.ordermgmt.OpenOrderRegistry;
import ch.algotrader.service.OrderPersistenceService;
import ch.algotrader.service.jpm.JPMFixOrderServiceImpl;

/**
 * JPM Fix service configuration.
 */
@Configuration
public class JPMFixServiceWiring {

    @Profile("jPMFix")
    @Bean(name = "jPMFixOrderService")
    public JPMFixOrderServiceImpl createJPMFixOrderService(
            final ManagedFixAdapter fixAdapter,
            final OpenOrderRegistry openOrderRegistry,
            final OrderPersistenceService orderPersistenceService,
            final CommonConfig commonConfig) {

        return new JPMFixOrderServiceImpl(fixAdapter, openOrderRegistry, orderPersistenceService, commonConfig);
    }

}
