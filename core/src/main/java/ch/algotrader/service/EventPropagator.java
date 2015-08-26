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
package ch.algotrader.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.algotrader.entity.marketData.MarketDataEventVO;
import ch.algotrader.event.dispatch.EventDispatcher;
import ch.algotrader.util.metric.MetricsUtil;
import ch.algotrader.vo.GenericEventVO;
import ch.algotrader.vo.marketData.TradingStatusEventVO;

/**
 * A utility class that propagates generic and market data events through {@link EventDispatcher}.
 *
 * @author <a href="mailto:okalnichevski@algotrader.ch">Oleg Kalnichevski</a>
 */
public class EventPropagator {

    private static final Logger LOGGER = LogManager.getLogger(EventPropagator.class);

    private final EventDispatcher eventDispatcher;

    public EventPropagator(final EventDispatcher eventDispatcher) {
        this.eventDispatcher = eventDispatcher;
    }

    public void propagateMarketData(final MarketDataEventVO marketDataEvent) {

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(marketDataEvent);
        }

        long startTime = System.nanoTime();
        this.eventDispatcher.sendMarketDataEvent(marketDataEvent);
        MetricsUtil.accountEnd("PropagateMarketDataEventSubscriber.update", startTime);
    }

    public void propagateGenericEvent(final GenericEventVO genericEvent) {

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(genericEvent);
        }
        this.eventDispatcher.broadcastAllStrategies(genericEvent);
    }

    public void propagateTradingStatusEvent(final TradingStatusEventVO tradingStatusEvent) {

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(tradingStatusEvent);
        }
        this.eventDispatcher.broadcastAllStrategies(tradingStatusEvent);
    }

}
