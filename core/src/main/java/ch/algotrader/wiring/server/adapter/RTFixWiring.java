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

import ch.algotrader.adapter.fix.DefaultFixApplicationFactory;
import ch.algotrader.adapter.fix.DefaultFixSessionStateHolder;
import ch.algotrader.adapter.fix.FixApplicationFactory;
import ch.algotrader.adapter.ExternalSessionStateHolder;
import ch.algotrader.adapter.rt.RTFixOrderMessageHandler;
import ch.algotrader.esper.Engine;
import ch.algotrader.event.dispatch.EventDispatcher;
import ch.algotrader.ordermgmt.OpenOrderRegistry;

/**
 * RealTick Fix configuration.
 */
@Configuration
@Profile("rTFix")
public class RTFixWiring {

    @Bean(name = "rTOrderSessionStateHolder")
    public ExternalSessionStateHolder createRTOrderSessionStateHolder(final EventDispatcher eventDispatcher) {

        return new DefaultFixSessionStateHolder("RTT", eventDispatcher);
    }

    @Bean(name = "rTOrderApplicationFactory")
    public FixApplicationFactory createRTOrderApplicationFactory(
            final OpenOrderRegistry openOrderRegistry,
            final Engine serverEngine,
            final ExternalSessionStateHolder rTOrderSessionStateHolder) {

        RTFixOrderMessageHandler rtFixOrderMessageHandler = new RTFixOrderMessageHandler(openOrderRegistry, serverEngine);
        return new DefaultFixApplicationFactory(rtFixOrderMessageHandler, rTOrderSessionStateHolder);
    }

}
