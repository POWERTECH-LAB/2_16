package com.algoTrader.entity.marketData;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.collections15.CollectionUtils;
import org.apache.commons.collections15.Transformer;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.algoTrader.esper.EsperManager;
import com.algoTrader.util.MyLogger;
import com.algoTrader.util.metric.MetricsUtil;

public abstract class TickCallback {

    private static Logger logger = MyLogger.getLogger(TickCallback.class.getName());

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
        EsperManager.undeployStatement(strategyName, alias);

        long startTime = System.nanoTime();
        logger.debug("onFirstTick start " + sortedSecurityIds);

        // call orderCompleted
        onFirstTick(strategyName, tickList);

        logger.debug("onFirstTick end " + sortedSecurityIds);

        MetricsUtil.accountEnd("TickCallback." + strategyName, startTime);
    }

    public abstract void onFirstTick(String strategyName, List<Tick> ticks) throws Exception;
}
