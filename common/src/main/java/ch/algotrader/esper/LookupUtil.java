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
package ch.algotrader.esper;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections15.map.SingletonMap;

import ch.algotrader.ServiceLocator;
import ch.algotrader.cache.CacheManager;
import ch.algotrader.entity.Position;
import ch.algotrader.entity.Subscription;
import ch.algotrader.entity.marketData.Tick;
import ch.algotrader.entity.security.Future;
import ch.algotrader.entity.security.Option;
import ch.algotrader.entity.security.Security;
import ch.algotrader.entity.security.SecurityImpl;
import ch.algotrader.util.collection.CollectionUtil;

/**
 * Provides static Lookup methods based mainly on the {@link ch.algotrader.service.LookupService}
 *
 * @author <a href="mailto:aflury@algotrader.ch">Andy Flury</a>
 *
 * @version $Revision$ $Date$
 */
public class LookupUtil {

    private static CacheManager getCacheManager() {
        return ServiceLocator.instance().containsService("cacheManager") ? ServiceLocator.instance().getService("cacheManager", CacheManager.class) : null;
    }

    /**
     * Gets a Security by its {@code id} and initializes {@link Subscription Subscriptions}, {@link
     * Position Positions}, Underlying {@link Security} and {@link ch.algotrader.entity.security.SecurityFamily} to make sure that
     * they are available when the Hibernate Session is closed and this Security is in a detached
     * state.
     */
    public static Security getSecurityInitialized(int securityId) {

        CacheManager cacheManager = getCacheManager();
        if (cacheManager != null) {
            return cacheManager.get(SecurityImpl.class, securityId);
        } else {
            return ServiceLocator.instance().getLookupService().getSecurityInitialized(securityId);
        }
    }

    /**
     * Gets a Security by its {@code isin}.
     */
    public static Security getSecurityByIsin(String isin) {

        CacheManager cacheManager = getCacheManager();
        if (cacheManager != null) {

            String queryString = "from SecurityImpl as s where s.isin = :isin";

            Map<String, Object> namedParameters = new SingletonMap<String, Object>("isin", isin);

            return (Security) CollectionUtil.getSingleElementOrNull(cacheManager.query(queryString, namedParameters));
        } else {
            return ServiceLocator.instance().getLookupService().getSecurityByIsin(isin);
        }
    }

    /**
     * Gets a {@link ch.algotrader.entity.security.SecurityFamily} id by the {@code securityId} of one of its Securities
     */
    public static int getSecurityFamilyIdBySecurity(int securityId) {

        Security security = getSecurityInitialized(securityId);
        return security != null ? security.getSecurityFamily().getId() : 0;
    }

    /**
     * Gets a Subscriptions by the defined {@code strategyName} and {@code securityId}.
     */
    public static Subscription getSubscription(String strategyName, int securityId) {

        CacheManager cacheManager = getCacheManager();
        if (cacheManager != null) {

            String queryString = "from SubscriptionImpl where strategy.name = :strategyName and security.id = :securityId";

            Map<String, Object> namedParameters = new HashMap<String, Object>();
            namedParameters.put("strategyName", strategyName);
            namedParameters.put("securityId", securityId);

            return (Subscription) CollectionUtil.getSingleElementOrNull(cacheManager.query(queryString, namedParameters));
        } else {
            return ServiceLocator.instance().getLookupService().getSubscriptionByStrategyAndSecurity(strategyName, securityId);
        }
    }

    /**
     * Gets all Options that are subscribed by at least one Strategy.
     */
    public static Option[] getSubscribedOptions() {

        CacheManager cacheManager = getCacheManager();
        if (cacheManager != null) {

            String queryString = "select distinct s from OptionImpl as s join s.subscriptions as s2 where s2 != null order by s.id";

            return cacheManager.query(queryString).toArray(new Option[] {});
        } else {
            return ServiceLocator.instance().getLookupService().getSubscribedOptions().toArray(new Option[] {});
        }
    }

    /**
     * Gets all Futures that are subscribed by at least one Strategy.
     */
    public static Future[] getSubscribedFutures() {

        CacheManager cacheManager = getCacheManager();
        if (cacheManager != null) {

            String queryString = "select distinct f from FutureImpl as f join f.subscriptions as s where s != null order by f.id";

            return cacheManager.query(queryString).toArray(new Future[] {});
        } else {
            return ServiceLocator.instance().getLookupService().getSubscribedFutures().toArray(new Future[] {});
        }
    }

    /**
     * Gets a Position by Security and Strategy.
     */
    public static Position getPositionBySecurityAndStrategy(int securityId, String strategyName) {

        CacheManager cacheManager = getCacheManager();
        if (cacheManager != null) {

            String queryString = "select p from PositionImpl as p join p.strategy as s where p.security.id = :securityId and s.name = :strategyName";

            Map<String, Object> namedParameters = new HashMap<String, Object>();
            namedParameters.put("strategyName", strategyName);
            namedParameters.put("securityId", securityId);

            return (Position) CollectionUtil.getSingleElementOrNull(cacheManager.query(queryString, namedParameters));
        } else {
            return ServiceLocator.instance().getLookupService().getPositionBySecurityAndStrategy(securityId, strategyName);
        }
    }

    /**
     * Gets all open Position (with a quantity != 0).
     */
    public static Position[] getOpenPositions() {

        CacheManager cacheManager = getCacheManager();
        if (cacheManager != null) {

            String queryString = "from PositionImpl as p where p.quantity != 0 order by p.security.id";

            return cacheManager.query(queryString).toArray(new Position[] {});
        } else {
            return ServiceLocator.instance().getLookupService().getOpenPositions().toArray(new Position[] {});
        }
    }

    /**
     * Gets open Positions for the specified Strategy.
     */
    public static Position[] getOpenPositionsByStrategy(String strategyName) {

        CacheManager cacheManager = getCacheManager();
        if (cacheManager != null) {

            String queryString = "from PositionImpl as p where p.strategy.name = :strategyName and p.quantity != 0 order by p.security.id";

            Map<String, Object> namedParameters = new SingletonMap<String, Object>("strategyName", strategyName);

            return cacheManager.query(queryString, namedParameters).toArray(new Position[] {});
        } else {
            return ServiceLocator.instance().getLookupService().getOpenPositionsByStrategy(strategyName).toArray(new Position[] {});
        }
    }

    /**
     * Gets open Positions for the specified Security
     */
    public static Position[] getOpenPositionsBySecurity(int securityId) {

        CacheManager cacheManager = getCacheManager();
        if (cacheManager != null) {

            String queryString = "from PositionImpl as p where p.security.id = :securityId and p.quantity != 0 order by p.id";

            Map<String, Object> namedParameters = new SingletonMap<String, Object>("securityId", securityId);

            return cacheManager.query(queryString, namedParameters).toArray(new Position[] {});
        } else {
            return ServiceLocator.instance().getLookupService().getOpenPositionsBySecurity(securityId).toArray(new Position[] {});
        }
    }

    /**
     * Gets open Positions for the specified Strategy and SecurityFamily.
     */
    public static Position[] getOpenPositionsByStrategyAndSecurityFamily(String strategyName, int securityFamilyId) {

        CacheManager cacheManager = getCacheManager();
        if (cacheManager != null) {

            String queryString = "from PositionImpl as p where p.strategy.name = :strategyName and p.quantity != 0 and p.security.securityFamily.id = :securityFamilyId order by p.security.id";

            Map<String, Object> namedParameters = new HashMap<String, Object>();
            namedParameters.put("strategyName", strategyName);
            namedParameters.put("securityFamilyId", securityFamilyId);

            return cacheManager.query(queryString, namedParameters).toArray(new Position[] {});
        } else {
            return ServiceLocator.instance().getLookupService().getOpenPositionsByStrategyAndSecurityFamily(strategyName, securityFamilyId).toArray(new Position[] {});
        }
    }

    /**
     * Gets the first Tick of the defined Security that is before the maxDate (but not earlier than
     * one minute before that the maxDate).
     */
    public static Tick getTickByDateAndSecurity(int securityId, Date date) {

        return ServiceLocator.instance().getLookupService().getTickBySecurityAndMaxDate(securityId, date);
    }

}
