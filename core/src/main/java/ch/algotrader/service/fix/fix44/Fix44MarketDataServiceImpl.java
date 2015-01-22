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
package ch.algotrader.service.fix.fix44;

import ch.algotrader.adapter.fix.FixAdapter;
import ch.algotrader.adapter.fix.FixSessionLifecycle;
import ch.algotrader.config.CommonConfig;
import ch.algotrader.entity.security.SecurityDao;
import ch.algotrader.esper.Engine;
import ch.algotrader.service.fix.FixMarketDataServiceImpl;

/**
 * Generic FIX 4.4 market data service
 *
 * @author <a href="mailto:okalnichevski@algotrader.ch">Oleg Kalnichevski</a>
 *
 * @version $Revision$ $Date$
 */
public abstract class Fix44MarketDataServiceImpl extends FixMarketDataServiceImpl implements Fix44MarketDataService {

    private static final long serialVersionUID = 3043686661971232423L;

    public Fix44MarketDataServiceImpl(
            final CommonConfig commonConfig,
            final FixSessionLifecycle lifeCycle,
            final FixAdapter fixAdapter,
            final Engine serverEngine,
            final SecurityDao securityDao) {

        super(commonConfig, lifeCycle, fixAdapter, serverEngine, securityDao);
    }
}
