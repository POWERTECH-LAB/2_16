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

import java.net.URL;
import java.util.Collection;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.ConfigurationEngineDefaults;
import com.espertech.esper.client.time.CurrentTimeEvent;

import ch.algotrader.config.ConfigParams;

/**
 * {@link Engine} factory class.
 *
 * @author <a href="mailto:aflury@algotrader.ch">Andy Flury</a>
 */
public class EngineFactory {

    private final SubscriberResolver subscriberResolver;
    private final ConfigParams configParams;

    public EngineFactory(final SubscriberResolver subscriberResolver, final ConfigParams configParams) {
        this.subscriberResolver = subscriberResolver;
        this.configParams = configParams;
    }

    public Engine createServer(final Collection<URL> configResources, final String[] initModules, final String[] runModules) {
        Configuration configuration = new Configuration();
        for (URL configResource: configResources) {
            configuration.configure(configResource);
        }
        boolean simulation = this.configParams.getBoolean("simulation");
        if (!simulation) {

            ConfigurationEngineDefaults.Threading threading = configuration.getEngineDefaults().getThreading();
            threading.setThreadPoolOutbound(true);
            threading.setThreadPoolOutboundNumThreads(this.configParams.getInteger("misc.outboundThreads"));
        }
        String strategyName = "SERVER";
        Engine engine = new EngineImpl(strategyName, this.subscriberResolver, configuration, initModules, runModules, this.configParams);

        // must send time event before first schedule pattern
        engine.sendEvent(new CurrentTimeEvent(0));
        return engine;
    }

    public Engine createStrategy(
            final String strategyName,
            final Collection<URL> configResources, final String[] initModules, final String[] runModules) {
        Configuration configuration = new Configuration();
        for (URL configResource: configResources) {
            configuration.configure(configResource);
        }
        Engine engine = new EngineImpl(strategyName, this.subscriberResolver, configuration, initModules, runModules, this.configParams);

        // must send time event before first schedule pattern
        engine.sendEvent(new CurrentTimeEvent(0));
        return engine;
    }

}
