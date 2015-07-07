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
package ch.algotrader.dao;

import java.util.Collection;

import ch.algotrader.entity.Account;
import ch.algotrader.enumeration.OrderServiceType;
import ch.algotrader.hibernate.ReadWriteDao;

/**
 * DAO for {@link ch.algotrader.entity.Account} objects.
 *
 * @see ch.algotrader.entity.Account
 */
public interface AccountDao extends ReadWriteDao<Account> {

    /**
     * Finds an Account by the specified name.
     * @param name
     * @return Account
     */
    public Account findByName(String name);

    /**
     * Finds all active Accounts for the specified {@link OrderServiceType}
     * @param orderServiceType
     * @return Collection<String>
     */
    public Collection<String> findActiveSessionsByOrderServiceType(OrderServiceType orderServiceType);

}