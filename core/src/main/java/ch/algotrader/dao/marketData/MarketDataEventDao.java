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
package ch.algotrader.dao.marketData;

import ch.algotrader.dao.ReadWriteDao;
import ch.algotrader.entity.marketData.MarketDataEvent;

/**
 * DAO for {@link ch.algotrader.entity.marketData.MarketDataEvent} objects.
 *
 * @see ch.algotrader.entity.marketData.MarketDataEvent
 */
public interface MarketDataEventDao extends ReadWriteDao<MarketDataEvent> {

    // spring-dao merge-point
}
