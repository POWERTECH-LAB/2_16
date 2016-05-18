/***********************************************************************************
 * AlgoTrader Enterprise Trading Framework
 *
 * Copyright (C) 2015 AlgoTrader GmbH - All rights reserved
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
 * Aeschstrasse 6
 * 8834 Schindellegi
 ***********************************************************************************/
package ch.algotrader.esper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.apache.commons.collections15.CollectionUtils;
import org.apache.commons.collections15.Predicate;
import org.apache.commons.lang.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.algotrader.vo.StatementMetricVO;

/**
* {@link ch.algotrader.esper.EngineManager} implementation.
*
* @author <a href="mailto:aflury@algotrader.ch">Andy Flury</a>
*
* @version $Revision$ $Date$
*/
public class EngineManagerImpl implements EngineManager {

    private static final Logger LOGGER = LogManager.getLogger(EngineManagerImpl.class);

    private static final String SERVER_ENGINE = "SERVER";

    private final Map<String, Engine> engineMap;

    public EngineManagerImpl(final Map<String, Engine> engineMap) {
        this.engineMap = new ConcurrentHashMap<>(engineMap != null ? engineMap : Collections.<String, Engine>emptyMap());
    }

    @Override
    public Date getCurrentEPTime() {
        Date newest = null;
        for (final Engine engine : engineMap.values()) {
            if (!engine.isInternalClock()) {
                final Date current = engine.getCurrentTime();
                if (newest == null || newest.compareTo(current) < 0) {
                    newest = current;
                }
            }
        }
        return newest != null ? newest : new Date();
    }

    @Override
    public boolean hasEngine(final String engineName) {

        Validate.notEmpty(engineName, "Engine name is empty");
        return this.engineMap.containsKey(engineName);
    }

    @Override
    public Engine lookup(String engineName) {

        Validate.notEmpty(engineName, "Engine name is empty");
        return this.engineMap.get(engineName);
    }

    @Override
    public Engine getEngine(final String engineName) {

        Engine engine = lookup(engineName);
        if (engine == null) {
            throw new IllegalStateException("Unknown engine: " + engineName);
        }
        return engine;
    }

    @Override
    public Engine getServerEngine() {

        return getEngine(SERVER_ENGINE);
    }

    @Override
    public Collection<Engine> getEngines() {
        List<Engine> engines = new ArrayList<>();
        if (hasEngine(SERVER_ENGINE)) {
            engines.add(getServerEngine());
        }
        engines.addAll(getStrategyEngines());
        return engines;
    }

    @Override
    public Collection<Engine> getStrategyEngines() {
        return this.engineMap.entrySet().stream()
                .filter(entry -> !entry.getKey().equals(SERVER_ENGINE))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
    }

    @Override
    public void destroyEngine(final String engineName) {

        Engine engine = this.engineMap.get(engineName);
        if (engine != null) {
            engine.destroy();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void logStatementMetrics() {

        for (Map.Entry<String, Engine> entry: this.engineMap.entrySet()) {
            Engine engine = entry.getValue();

            if (engine.isDeployed("METRICS")) {

                List<StatementMetricVO> metrics = engine.getAllEvents("METRICS");

                // consolidate ON_TRADE_COMPLETED and ON_FIRST_TICK
                for (final String statementName : new String[] { "ON_TRADE_COMPLETED", "ON_FIRST_TICK" }) {

                    // select metrics where the statementName startsWith
                    Collection<StatementMetricVO> selectedMetrics = CollectionUtils.select(metrics, new Predicate<StatementMetricVO>() {
                        @Override
                        public boolean evaluate(StatementMetricVO metric) {
                            return metric.getStatementName() != null && metric.getStatementName().startsWith(statementName);
                        }
                    });

                    // add cpuTime, wallTime and numInput
                    if (selectedMetrics.size() > 0) {

                        long cpuTime = 0;
                        long wallTime = 0;
                        long numInput = 0;
                        for (StatementMetricVO metric : selectedMetrics) {

                            cpuTime += metric.getCpuTime();
                            wallTime += metric.getWallTime();
                            numInput += metric.getNumInput();

                            // remove the original metric
                            metrics.remove(metric);
                        }

                        // add a consolidated metric
                        metrics.add(new StatementMetricVO(engine.getStrategyName(), statementName, cpuTime, wallTime, numInput));
                    }
                }

                if (LOGGER.isInfoEnabled()) {
                    for (StatementMetricVO metric : metrics) {
                        LOGGER.info("{}.{}: {} millis {} events", metric.getEngineURI(), metric.getStatementName(), metric.getWallTime(), metric.getNumInput());
                    }
                }
            }
        }
    }

    @Override
    public void resetStatementMetrics() {

        for (Map.Entry<String, Engine> entry: this.engineMap.entrySet()) {
            Engine engine = entry.getValue();
            engine.restartStatement("METRICS");
        }
    }

}
