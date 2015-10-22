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
package ch.algotrader.util.metric;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.ClassUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.algotrader.config.ConfigLocator;

/**
 * Utility class for recording and logging of performance metrics
 *
 * @author <a href="mailto:aflury@algotrader.ch">Andy Flury</a>
 */
public class MetricsUtil {

    private static final boolean isMetricsEnabled = ConfigLocator.instance().getConfigParams().getBoolean("misc.metricsEnabled", false);
    private static final Logger LOGGER = LogManager.getLogger(MetricsUtil.class);

    private static final Map<String, Metric> metrics = new HashMap<>();
    private static long startMillis = System.nanoTime();

    /**
     * account the given metric by its {@code startMillis} and {@code endMillis}
     */
    public static void account(String metricName, long startMillis, long endMillis) {

        if (isMetricsEnabled) {
            account(metricName, endMillis - startMillis);
        }
    }

    /**
     * account the given metric by its {@code startMillis}. The endTime is taken from the system clock
     */
    public static void accountEnd(String metricName, long startMillis) {

        if (isMetricsEnabled) {
            account(metricName, System.nanoTime() - startMillis);
        }
    }

    /**
     * account the given metric by its {@code startMillis}. The endTime is taken from the system clock.
     * The name of the metric will be a combination of {@code metricName} and {@code clazz}.
     */
    public static void accountEnd(String metricName, Class<?> clazz, long startMillis) {

        if (isMetricsEnabled) {
            account(metricName + "." + ClassUtils.getShortClassName(clazz), System.nanoTime() - startMillis);
        }
    }

    /**
     * account the given metric by its duration in {@code millis}
     */
    public static void account(String metricName, long millis) {

        if (isMetricsEnabled) {
            getMetric(metricName).addTime(millis);
        }
    }

    /**
     * print-out all metric values
     */
    public static void logMetrics() {

        if (isMetricsEnabled && LOGGER.isInfoEnabled()) {

            if (ConfigLocator.instance().getCommonConfig().isSimulation()) {
                LOGGER.info("TotalDuration: {} millis", (System.nanoTime() - startMillis));
            }

            for (Metric metric : metrics.values()) {
                LOGGER.info("{}: {} millis {} executions", metric.getName(), metric.getTime(), metric.getExecutions());
            }
        }
    }

    /**
     * Resets all metrics and also reset the {@code startMillis}
     */
    public static void resetMetrics() {

        metrics.clear();
        startMillis = System.nanoTime();
    }

    private static Metric getMetric(String metricName) {

        Metric metric = metrics.get(metricName);
        if (metric == null) {
            metric = new Metric(metricName);
            metrics.put(metricName, metric);
        }
        return metric;
    }
}
