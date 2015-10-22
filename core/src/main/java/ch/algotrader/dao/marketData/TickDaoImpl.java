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

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.Validate;
import org.hibernate.SessionFactory;
import org.springframework.stereotype.Repository;

import ch.algotrader.dao.AbstractDao;
import ch.algotrader.dao.NamedParam;
import ch.algotrader.entity.marketData.Tick;
import ch.algotrader.entity.marketData.TickImpl;
import ch.algotrader.enumeration.QueryType;
import ch.algotrader.util.DateTimeLegacy;

/**
 * @author <a href="mailto:aflury@algotrader.ch">Andy Flury</a>
 */
@Repository // Required for exception translation
public class TickDaoImpl extends AbstractDao<Tick> implements TickDao {

    public TickDaoImpl(final SessionFactory sessionFactory) {
        super(TickImpl.class, sessionFactory);
    }

    @Override
    public List<Tick> findBySecurity(long securityId) {

        return findCaching("Tick.findBySecurity", QueryType.BY_NAME, new NamedParam("securityId", securityId));
    }

    @Override
    public List<Tick> findTicksBySecurityAndMinDate(long securityId, Date minDate, int intervalDays) {

        return findTicksBySecurityAndMinDate(-1, securityId, minDate, intervalDays);
    }

    @Override
    public List<Tick> findTicksBySecurityAndMinDate(int limit, long securityId, Date minDate, int intervalDays) {

        Validate.notNull(minDate, "minDate is null");

        LocalDateTime minLocalDateTime = DateTimeLegacy.toLocalDateTime(minDate);
        LocalDateTime maxLocalDateTime = minLocalDateTime.plusDays(intervalDays);
        return find("Tick.findTicksBySecurityAndMinDate", limit, QueryType.BY_NAME,
                new NamedParam("securityId", securityId),
                new NamedParam("minDate", DateTimeLegacy.toLocalDateTime(minLocalDateTime)),
                new NamedParam("maxDate", DateTimeLegacy.toLocalDateTime(maxLocalDateTime)));
    }

    @Override
    public List<Tick> findTicksBySecurityAndMaxDate(long securityId, Date maxDate, int intervalDays) {

        Validate.notNull(maxDate, "maxDate is null");

        return findTicksBySecurityAndMaxDate(-1, securityId, maxDate, intervalDays);
    }

    @Override
    public List<Tick> findTicksBySecurityAndMaxDate(int limit, long securityId, Date maxDate, int intervalDays) {

        Validate.notNull(maxDate, "maxDate is null");

        LocalDateTime maxLocalDateTime = DateTimeLegacy.toLocalDateTime(maxDate);
        LocalDateTime minLocalDateTime = maxLocalDateTime.minusDays(intervalDays);
        return find("Tick.findTicksBySecurityAndMaxDate", limit, QueryType.BY_NAME,
                new NamedParam("securityId", securityId),
                new NamedParam("minDate", DateTimeLegacy.toLocalDateTime(minLocalDateTime)),
                new NamedParam("maxDate", DateTimeLegacy.toLocalDateTime(maxLocalDateTime)));
    }

    @Override
    public List<Long> findDailyTickIdsBeforeTime(long securityId, Date time) {

        Validate.notNull(time, "Time is null");

        return convertIds(findObjects(null, "Tick.findDailyTickIdsBeforeTime", QueryType.BY_NAME, new NamedParam("securityId", securityId), new NamedParam("time", time)));
    }

    @Override
    public List<Long> findDailyTickIdsAfterTime(long securityId, Date time) {

        Validate.notNull(time, "Time is null");

        return convertIds(findObjects(null, "Tick.findDailyTickIdsAfterTime", QueryType.BY_NAME, new NamedParam("securityId", securityId), new NamedParam("time", time)));
    }

    @Override
    public List<Long> findHourlyTickIdsBeforeMinutesByMinDate(long securityId, int minutes, Date minDate) {

        Validate.notNull(minDate, "minDate is null");

        return convertIds((findObjects(null, "Tick.findHourlyTickIdsBeforeMinutesByMinDate", QueryType.BY_NAME, new NamedParam("securityId", securityId), new NamedParam("minutes", minutes),
                new NamedParam("minDate", minDate))));
    }

    @Override
    public List<Long> findHourlyTickIdsAfterMinutesByMinDate(long securityId, int minutes, Date minDate) {

        Validate.notNull(minDate, "minDate is null");

        return convertIds(findObjects(null, "Tick.findHourlyTickIdsAfterMinutesByMinDate", QueryType.BY_NAME, new NamedParam("securityId", securityId), new NamedParam("minutes", minutes),
                new NamedParam("minDate", minDate)));
    }

    @Override
    public List<Tick> findByIdsInclSecurityAndUnderlying(List<Long> ids) {

        Validate.notEmpty(ids, "Ids are empty");

        return find("Tick.findByIdsInclSecurityAndUnderlying", QueryType.BY_NAME, new NamedParam("ids", ids));
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

}
