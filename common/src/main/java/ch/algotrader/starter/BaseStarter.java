/***********************************************************************************
 * AlgoTrader Enterprise Trading Framework
 *
 * Copyright (C) 2013 Flury Trading - All rights reserved
 *
 * All information contained herein is, and remains the property of Flury Trading.
 * The intellectual and technical concepts contained herein are proprietary to
 * Flury Trading. Modification, translation, reverse engineering, decompilation,
 * disassembly or reproduction of this material is strictly forbidden unless prior
 * written permission is obtained from Flury Trading
 *
 * Fur detailed terms and conditions consult the file LICENSE.txt or contact
 *
 * Flury Trading
 * Badenerstrasse 16
 * 8004 Zurich
 ***********************************************************************************/
package ch.algotrader.starter;

import ch.algotrader.ServiceLocator;
import ch.algotrader.esper.EngineLocator;
import ch.algotrader.service.InitializingServiceI;
import ch.algotrader.service.MarketDataService;

/**
 * Abstract Base Class for starting the Base in Live Trading Mode
 *
 * @author <a href="mailto:andyflury@gmail.com">Andy Flury</a>
 *
 * @version $Revision$ $Date$
 */
public abstract class BaseStarter {

    public static void startBase() throws Exception {

        // start all BASE rules
        EngineLocator.instance().initBaseEngine();
        EngineLocator.instance().getBaseEngine().setInternalClock(true);
        EngineLocator.instance().getBaseEngine().deployAllModules();

        // initialize services
        for (InitializingServiceI service : ServiceLocator.instance().getServices(InitializingServiceI.class)) {
            service.init();
        }

        // init market data subscriptions (needs to be invoked after all Spring Services have been properly initialized)
        MarketDataService marketDataService = ServiceLocator.instance().getMarketDataService();
        marketDataService.initSubscriptions();
    }
}
