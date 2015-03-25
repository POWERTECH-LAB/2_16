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
package ch.algotrader.service.fix.fix42;

import ch.algotrader.adapter.fix.FixAdapter;
import ch.algotrader.adapter.fix.FixSessionStateHolder;
import ch.algotrader.entity.security.SecurityDao;
import ch.algotrader.esper.EngineManager;
import ch.algotrader.service.fix.FixMarketDataServiceImpl;

/**
 * Generic FIX 4.2 market data service
 *
 * @author <a href="mailto:okalnichevski@algotrader.ch">Oleg Kalnichevski</a>
 *
 * @version $Revision$ $Date$
 */
public abstract class Fix42MarketDataServiceImpl extends FixMarketDataServiceImpl implements Fix42MarketDataService {

    private static final long serialVersionUID = -660389755007202727L;

    public Fix42MarketDataServiceImpl(
            final FixSessionStateHolder lifeCycle,
            final FixAdapter fixAdapter,
            final EngineManager engineManager,
            final SecurityDao securityDao) {

        super(lifeCycle, fixAdapter, engineManager, securityDao);
    }
}
