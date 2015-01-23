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
package ch.algotrader.esper.callback;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.collections15.CollectionUtils;
import org.apache.commons.collections15.Transformer;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import ch.algotrader.ServiceLocator;
import ch.algotrader.entity.marketData.Tick;
import ch.algotrader.util.metric.MetricsUtil;

/**
 * Base Esper Callback Class that will be invoked as soon as at least one Tick has arrived for each of the {@code securities}
 * passed to {@link ch.algotrader.esper.Engine#addFirstTickCallback}
 *
 * @author <a href="mailto:aflury@algotrader.ch">Andy Flury</a>
 *
 * @version $Revision$ $Date$
 */
public abstract class TickCallback {

    private static Logger logger = Logger.getLogger(TickCallback.class.getName());

    /**
     * Called by the "ON_FIRST_TICK" statement. Should not be invoked directly.
     */
    public void update(String strategyName, Tick[] ticks) throws Exception {

        List<Tick> tickList = Arrays.asList(ticks);

        // get the securityIds sorted asscending
        Set<Integer> sortedSecurityIds = new TreeSet<Integer>(CollectionUtils.collect(tickList, new Transformer<Tick, Integer>() {
            @Override
            public Integer transform(Tick tick) {
                return tick.getSecurity().getId();
            }
        }));

        // get the statement alias based on all security ids
        String alias = "ON_FIRST_TICK_" + StringUtils.join(sortedSecurityIds, "_");

        // undeploy the statement
        ServiceLocator.instance().getEngineManager().getEngine(strategyName).undeployStatement(alias);

        long startTime = System.nanoTime();
        logger.debug("onFirstTick start " + sortedSecurityIds);

        // call orderCompleted
        onFirstTick(strategyName, tickList);

        logger.debug("onFirstTick end " + sortedSecurityIds);

        MetricsUtil.accountEnd("TickCallback." + strategyName, startTime);
    }

    /**
     * Will be exectued by the Esper Engine as soon as at least one Tick has arrived for each of the {@code securities}.
     * Needs to be overwritten by implementing classes.
     */
    public abstract void onFirstTick(String strategyName, List<Tick> ticks) throws Exception;
}
