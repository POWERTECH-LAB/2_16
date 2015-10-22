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

package ch.algotrader.dao.security;

import java.util.List;

import org.apache.commons.lang.Validate;
import org.hibernate.SessionFactory;
import org.springframework.stereotype.Repository;

import ch.algotrader.dao.AbstractDao;
import ch.algotrader.dao.NamedParam;
import ch.algotrader.entity.security.Component;
import ch.algotrader.entity.security.ComponentImpl;
import ch.algotrader.enumeration.QueryType;

/**
 * @author <a href="mailto:aflury@algotrader.ch">Andy Flury</a>
 */
@Repository // Required for exception translation
public class ComponentDaoImpl extends AbstractDao<Component> implements ComponentDao {

    public ComponentDaoImpl(SessionFactory sessionFactory) {

        super(ComponentImpl.class, sessionFactory);
    }

    @Override
    public List<Component> findSubscribedByStrategyInclSecurity(String strategyName) {

        Validate.notEmpty(strategyName, "Strategy name is empty");

        return findCaching("Component.findSubscribedByStrategyInclSecurity", QueryType.BY_NAME, new NamedParam("strategyName", strategyName));
    }

    @Override
    public List<Component> findSubscribedBySecurityInclSecurity(long securityId) {

        return findCaching("Component.findSubscribedBySecurityInclSecurity", QueryType.BY_NAME, new NamedParam("securityId", securityId));
    }

    @Override
    public List<Component> findSubscribedByStrategyAndSecurityInclSecurity(String strategyName, long securityId) {

        Validate.notEmpty(strategyName, "Strategy name is empty");

        return findCaching("Component.findSubscribedByStrategyAndSecurityInclSecurity", QueryType.BY_NAME, new NamedParam("strategyName", strategyName), new NamedParam("securityId", securityId));
    }

    @Override
    public List<Component> findNonPersistent() {

        return findCaching("Component.findNonPersistent", QueryType.BY_NAME);
    }

}
