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
package ch.algotrader.entity;

import java.util.Date;
import java.util.List;

import org.apache.commons.lang.Validate;
import org.hibernate.LockOptions;
import org.hibernate.SessionFactory;
import org.springframework.stereotype.Repository;

import ch.algotrader.enumeration.QueryType;
import ch.algotrader.hibernate.AbstractDao;
import ch.algotrader.hibernate.EntityConverter;
import ch.algotrader.hibernate.NamedParam;

/**
 * @author <a href="mailto:aflury@algotrader.ch">Andy Flury</a>
 *
 * @version $Revision$ $Date$
 */
@Repository // Required for exception translation
public class PositionDaoImpl extends AbstractDao<Position> implements PositionDao {

    public PositionDaoImpl(final SessionFactory sessionFactory) {

        super(PositionImpl.class, sessionFactory);
    }

    @Override
    public Position findByIdInclSecurityAndSecurityFamily(long id) {

        return findUnique("Position.findByIdInclSecurityAndSecurityFamily", QueryType.BY_NAME, new NamedParam("id", id));
    }

    @Override
    public List<Position> findByStrategy(String strategyName) {

        Validate.notEmpty(strategyName, "Strategy name is empty");

        return find("Position.findByStrategy", QueryType.BY_NAME, new NamedParam("strategyName", strategyName));
    }

    @Override
    public <V >List<V> findByStrategy(String strategyName, EntityConverter<Position, V> converter) {

        Validate.notEmpty(strategyName, "Strategy name is empty");

        return find(converter, "Position.findByStrategy", QueryType.BY_NAME, new NamedParam("strategyName", strategyName));
    }

    @Override
    public Position findBySecurityAndStrategy(long securityId, String strategyName) {

        Validate.notEmpty(strategyName, "Strategy name is empty");

        return findUnique("Position.findBySecurityAndStrategy", QueryType.BY_NAME, new NamedParam("securityId", securityId), new NamedParam("strategyName", strategyName));
    }

    @Override
    public Position findBySecurityAndStrategyIdLocked(long securityId, long strategyId) {

        return findUnique(LockOptions.UPGRADE, "Position.findBySecurityAndStrategyIdLocked", QueryType.BY_NAME, new NamedParam("securityId", securityId), new NamedParam("strategyId", strategyId));

    }

    @Override
    public List<Position> findOpenPositions() {

        return find("Position.findOpenPositions", QueryType.BY_NAME);
    }

    @Override
    public <V> List<V> findOpenPositions(EntityConverter<Position, V> converter) {

        return find(converter, "Position.findOpenPositions", QueryType.BY_NAME);
    }

    @Override
    public List<Position> findOpenPositionsByMaxDateAggregated(Date maxDate) {

        Validate.notNull(maxDate, "maxDate is null");

        return find("Position.findOpenPositionsByMaxDateAggregated", QueryType.BY_NAME, new NamedParam("maxDate", maxDate));
    }

    @Override
    public List<Position> findOpenPositionsByStrategy(String strategyName) {

        Validate.notEmpty(strategyName, "Strategy name is empty");

        return find("Position.findOpenPositionsByStrategy", QueryType.BY_NAME, new NamedParam("strategyName", strategyName));
    }

    @Override
    public <V> List<V> findOpenPositionsByStrategy(String strategyName, EntityConverter<Position, V> converter) {

        Validate.notEmpty(strategyName, "Strategy name is empty");

        return find(converter, "Position.findOpenPositionsByStrategy", QueryType.BY_NAME, new NamedParam("strategyName", strategyName));
    }

    @Override
    public List<Position> findOpenPositionsByStrategyAndMaxDate(String strategyName, Date maxDate) {

        Validate.notEmpty(strategyName, "Strategy name is empty");
        Validate.notNull(maxDate, "maxDate is null");

        return find("Position.findOpenPositionsByStrategyAndMaxDate", QueryType.BY_NAME, new NamedParam("strategyName", strategyName), new NamedParam("maxDate", maxDate));
    }

    @Override
    public List<Position> findOpenPositionsByStrategyAndType(String strategyName, int type) {

        Validate.notEmpty(strategyName, "Strategy name is empty");

        return find("Position.findOpenPositionsByStrategyAndType", QueryType.BY_NAME, new NamedParam("strategyName", strategyName), new NamedParam("type", type));
    }

    @Override
    public List<Position> findOpenPositionsByStrategyTypeAndUnderlyingType(String strategyName, int type, int underlyingType) {

        Validate.notEmpty(strategyName, "Strategy name is empty");

        return find("Position.findOpenPositionsByStrategyTypeAndUnderlyingType", QueryType.BY_NAME, new NamedParam("strategyName", strategyName), new NamedParam("type", type), new NamedParam(
                "underlyingType", underlyingType));
    }

    @Override
    public List<Position> findOpenPositionsByStrategyAndSecurityFamily(String strategyName, long securityFamilyId) {

        Validate.notEmpty(strategyName, "Strategy name is empty");

        return find("Position.findOpenPositionsByStrategyAndSecurityFamily", QueryType.BY_NAME, new NamedParam("strategyName", strategyName), new NamedParam("securityFamilyId", securityFamilyId));
    }

    @Override
    public List<Position> findOpenPositionsByUnderlying(long underlyingId) {

        return find("Position.findOpenPositionsByUnderlying", QueryType.BY_NAME, new NamedParam("underlyingId", underlyingId));
    }

    @Override
    public List<Position> findOpenPositionsBySecurity(long securityId) {

        return find("Position.findOpenPositionsBySecurity", QueryType.BY_NAME, new NamedParam("securityId", securityId));
    }

    @Override
    public List<Position> findOpenTradeablePositions() {

        return find("Position.findOpenTradeablePositions", QueryType.BY_NAME);
    }

    @Override
    public List<Position> findOpenTradeablePositionsAggregated() {

        return find("Position.findOpenTradeablePositionsAggregated", QueryType.BY_NAME);
    }

    @Override
    public List<Position> findOpenTradeablePositionsByStrategy(String strategyName) {

        Validate.notEmpty(strategyName, "Strategy name is empty");

        return find("Position.findOpenTradeablePositionsByStrategy", QueryType.BY_NAME, new NamedParam("strategyName", strategyName));
    }

    @Override
    public List<Position> findOpenFXPositions() {

        return find("Position.findOpenFXPositions", QueryType.BY_NAME);
    }

    @Override
    public List<Position> findOpenFXPositionsAggregated() {

        return find("Position.findOpenFXPositionsAggregated", QueryType.BY_NAME);
    }

    @Override
    public List<Position> findOpenFXPositionsByStrategy(String strategyName) {

        Validate.notEmpty(strategyName, "Strategy name is empty");

        return find("Position.findOpenFXPositionsByStrategy", QueryType.BY_NAME, new NamedParam("strategyName", strategyName));
    }

    @Override
    public List<Position> findExpirablePositions(Date currentTime) {

        Validate.notNull(currentTime, "currentTime is null");

        return find("Position.findExpirablePositions", QueryType.BY_NAME, new NamedParam("currentTime", currentTime));
    }

    @Override
    public List<Position> findPersistent() {

        return find("Position.findPersistent", QueryType.BY_NAME);
    }

    @Override
    public List<Position> findNonPersistent() {

        return find("Position.findNonPersistent", QueryType.BY_NAME);
    }

    @Override
    public List<Position> findOpenPositionsAggregated() {

        return find("Position.findOpenPositionsAggregated", QueryType.BY_NAME);
    }

}
