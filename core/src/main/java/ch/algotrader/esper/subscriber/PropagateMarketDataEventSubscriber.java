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
package ch.algotrader.esper.subscriber;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.algotrader.ServiceLocator;
import ch.algotrader.entity.marketData.MarketDataEvent;
import ch.algotrader.util.metric.MetricsUtil;

/**
 * Esper event subscriber for {@link ch.algotrader.event.dispatch.EventDispatcher#sendMarketDataEvent(ch.algotrader.entity.marketData.MarketDataEvent)}.
 */
public class PropagateMarketDataEventSubscriber {

    private static final Logger LOGGER = LogManager.getLogger(PropagateMarketDataEventSubscriber.class.getName());

    public void update(final MarketDataEvent marketDataEvent) {

        // security.toString & marketDataEvent.toString is expensive, so only log if debug is enabled
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(marketDataEvent.getSecurity() + " " + marketDataEvent);
        }

        long startTime = System.nanoTime();

        ServiceLocator.instance().getEventDispatcher().sendMarketDataEvent(marketDataEvent);

        MetricsUtil.accountEnd("PropagateMarketDataEventSubscriber.update", startTime);
    }
}
