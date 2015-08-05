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
package ch.algotrader.dao.security;

import ch.algotrader.dao.ReadWriteDao;
import ch.algotrader.entity.security.FutureFamily;

/**
 * DAO for {@link ch.algotrader.entity.security.FutureFamily} objects.
 *
 * @see ch.algotrader.entity.security.FutureFamily
 */
public interface FutureFamilyDao extends ReadWriteDao<FutureFamily> {

    /**
     * Finds FutureFamily for the specified underlying
     * @param underlyingId
     * @return FutureFamily
     */
    FutureFamily findByUnderlying(long underlyingId);

    // spring-dao merge-point
}