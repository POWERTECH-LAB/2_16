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

package ch.algotrader.entity.marketData;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.Validate;
import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.hibernate.type.IntegerType;
import org.springframework.stereotype.Repository;

import ch.algotrader.ServiceLocator;
import ch.algotrader.entity.Subscription;
import ch.algotrader.entity.SubscriptionDao;
import ch.algotrader.entity.security.Security;
import ch.algotrader.enumeration.Duration;
import ch.algotrader.enumeration.OptionType;
import ch.algotrader.enumeration.QueryType;
import ch.algotrader.esper.EngineLocator;
import ch.algotrader.hibernate.AbstractDao;
import ch.algotrader.hibernate.NamedParam;
import ch.algotrader.util.collection.Pair;

/**
 * @author <a href="mailto:aflury@algotrader.ch">Andy Flury</a>
 *
 * @version $Revision$ $Date$
 */
@Repository // Required for exception translation
public class TickDaoImpl extends AbstractDao<Tick> implements TickDao {

    private final SubscriptionDao subscriptionDao;

    public TickDaoImpl(final SessionFactory sessionFactory, final SubscriptionDao subscriptionDao) {

        super(TickImpl.class, sessionFactory);

        Validate.notNull(subscriptionDao);

        this.subscriptionDao = subscriptionDao;
    }

    @Override
    public List<Tick> findBySecurity(int securityId) {

        return find("Tick.findBySecurity", QueryType.BY_NAME, new NamedParam("securityId", securityId));
    }

    @Override
    public Tick findBySecurityAndMaxDate(int securityId, Date maxDate) {

        Validate.notNull(maxDate, "maxDate is null");

        return findUnique("Tick.findBySecurityAndMaxDate", QueryType.BY_NAME, new NamedParam("securityId", securityId), new NamedParam("maxDate", maxDate));
    }

    @Override
    public List<Tick> findTicksBySecurityAndMinDate(int securityId, Date minDate, int intervalDays) {

        Validate.notNull(minDate, "minDate is null");

        return find("Tick.findTicksBySecurityAndMinDate", QueryType.BY_NAME, new NamedParam("securityId", securityId), new NamedParam("minDate", minDate), new NamedParam("intervalDays", intervalDays));
    }

    @Override
    public List<Tick> findTicksBySecurityAndMinDate(int limit, int securityId, Date minDate, int intervalDays) {

        Validate.notNull(minDate, "minDate is null");

        return find("Tick.findTicksBySecurityAndMinDate", limit, QueryType.BY_NAME, new NamedParam("securityId", securityId), new NamedParam("minDate", minDate), new NamedParam("intervalDays",
                intervalDays));
    }

    @Override
    public List<Tick> findTicksBySecurityAndMaxDate(int securityId, Date maxDate, int intervalDays) {

        Validate.notNull(maxDate, "maxDate is null");

        return find("Tick.findTicksBySecurityAndMaxDate", QueryType.BY_NAME, new NamedParam("securityId", securityId), new NamedParam("maxDate", maxDate), new NamedParam("intervalDays", intervalDays));
    }

    @Override
    public List<Tick> findTicksBySecurityAndMaxDate(int limit, int securityId, Date maxDate, int intervalDays) {

        Validate.notNull(maxDate, "maxDate is null");

        return find("Tick.findTicksBySecurityAndMaxDate", limit, QueryType.BY_NAME, new NamedParam("securityId", securityId), new NamedParam("maxDate", maxDate), new NamedParam("intervalDays",
                intervalDays));
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Integer> findDailyTickIdsBeforeTime(int securityId, Date time) {

        Validate.notNull(time, "Time is null");

        return (List<Integer>) findObjects(null, "Tick.findDailyTickIdsBeforeTime", QueryType.BY_NAME, new NamedParam("securityId", securityId), new NamedParam("time", time));
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Integer> findDailyTickIdsAfterTime(int securityId, Date time) {

        Validate.notNull(time, "Time is null");

        return (List<Integer>) findObjects(null, "Tick.findDailyTickIdsAfterTime", QueryType.BY_NAME, new NamedParam("securityId", securityId), new NamedParam("time", time));
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Integer> findHourlyTickIdsBeforeMinutesByMinDate(int securityId, int minutes, Date minDate) {

        Validate.notNull(minDate, "minDate is null");

        return (List<Integer>) findObjects(null, "Tick.findHourlyTickIdsBeforeMinutesByMinDate", QueryType.BY_NAME, new NamedParam("securityId", securityId), new NamedParam("minutes", minutes),
                new NamedParam("minDate", minDate));
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Integer> findHourlyTickIdsAfterMinutesByMinDate(int securityId, int minutes, Date minDate) {

        Validate.notNull(minDate, "minDate is null");

        return (List<Integer>) findObjects(null, "Tick.findHourlyTickIdsAfterMinutesByMinDate", QueryType.BY_NAME, new NamedParam("securityId", securityId), new NamedParam("minutes", minutes),
                new NamedParam("minDate", minDate));
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Tick> findByIdsInclSecurityAndUnderlying(List<Integer> ids) {

        Validate.notEmpty(ids, "Ids are empty");

        Query query = this.prepareQuery(null, "Tick.findByIdsInclSecurityAndUnderlying", QueryType.BY_NAME);
        query.setParameterList("ids", ids, IntegerType.INSTANCE);

        return query.list();
    }

    @Override
    public List<Tick> findSubscribedByTimePeriod(Date minDate, Date maxDate) {

        Validate.notNull(minDate, "minDate is null");
        Validate.notNull(maxDate, "maxDate is null");

        return find("Tick.findSubscribedByTimePeriod", QueryType.BY_NAME, new NamedParam("minDate", minDate), new NamedParam("maxDate", maxDate));
    }

    @Override
    public List<Tick> findSubscribedByTimePeriod(int limit, Date minDate, Date maxDate) {

        Validate.notNull(minDate, "minDate is null");
        Validate.notNull(maxDate, "maxDate is null");

        return find("Tick.findSubscribedByTimePeriod", limit, QueryType.BY_NAME, new NamedParam("minDate", minDate), new NamedParam("maxDate", maxDate));
    }

    @Override
    public List<Tick> findOptionTicksBySecurityDateTypeAndExpirationInclSecurity(int underlyingId, Date date, OptionType type, Date expiration) {

        Validate.notNull(date, "Date is null");
        Validate.notNull(type, "Type is null");
        Validate.notNull(expiration, "expiration is null");

        return find("Tick.findOptionTicksBySecurityDateTypeAndExpirationInclSecurity", QueryType.BY_NAME, new NamedParam("underlyingId", underlyingId), new NamedParam("date", date), new NamedParam(
                "type", type), new NamedParam("expiration", expiration));
    }

    @Override
    public List<Tick> findImpliedVolatilityTicksBySecurityAndDate(int underlyingId, Date date) {

        Validate.notNull(date, "Date is null");

        return find("Tick.findImpliedVolatilityTicksBySecurityAndDate", QueryType.BY_NAME, new NamedParam("underlyingId", underlyingId), new NamedParam("date", date));
    }

    @Override
    public List<Tick> findImpliedVolatilityTicksBySecurityDateAndDuration(int underlyingId, Date date, Duration duration) {

        Validate.notNull(date, "Date is null");
        Validate.notNull(duration, "Duration is null");

        return find("Tick.findImpliedVolatilityTicksBySecurityDateAndDuration", QueryType.BY_NAME, new NamedParam("underlyingId", underlyingId), new NamedParam("date", date), new NamedParam(
                "duration", duration));
    }

    @SuppressWarnings("unchecked")
    @Override
    public String findTickerIdBySecurity(int securityId) {

        // sometimes Esper returns a Map instead of scalar
        String query = "select tickerId from TickWindow where security.id = " + securityId;
        Object obj = EngineLocator.instance().getServerEngine().executeSingelObjectQuery(query);
        if (obj instanceof Map) {
            return ((Map<String, String>) obj).get("tickerId");
        } else {
            return (String) obj;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Tick> findCurrentTicksByStrategy(String strategyName) {

        Validate.notNull(strategyName, "Strategy name is null");

        List<Subscription> subscriptions = this.subscriptionDao.findByStrategy(strategyName);

        List<Tick> ticks = new ArrayList<Tick>();
        for (Subscription subscription : subscriptions) {
            String query = "select * from TickWindow where security.id = " + subscription.getSecurity().getId();
            Pair<Tick, Object> pair = (Pair<Tick, Object>) EngineLocator.instance().getServerEngine().executeSingelObjectQuery(query);
            if (pair != null) {

                Tick tick = pair.getFirst();
                tick.setDateTime(new Date());

                // refresh the security (associated entities might have been modified
                Security security = ServiceLocator.instance().getLookupService().getSecurityInitialized(tick.getSecurity().getId());
                tick.setSecurity(security);

                if (security.validateTick(tick)) {
                    ticks.add(tick);
                }
            }
        }

        return ticks;
    }

}
