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
package ch.algotrader.service;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.collections.map.MultiValueMap;
import org.apache.commons.lang.Validate;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang.time.DateUtils;
import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import org.hibernate.internal.SessionFactoryImpl;
import org.springframework.transaction.annotation.Transactional;

import ch.algotrader.config.CommonConfig;
import ch.algotrader.config.CoreConfig;
import ch.algotrader.dao.AccountDao;
import ch.algotrader.dao.GenericDao;
import ch.algotrader.dao.HibernateInitializer;
import ch.algotrader.dao.NamedParam;
import ch.algotrader.dao.PositionDao;
import ch.algotrader.dao.PositionVOProducer;
import ch.algotrader.dao.SubscriptionDao;
import ch.algotrader.dao.TransactionDao;
import ch.algotrader.dao.TransactionVOProducer;
import ch.algotrader.dao.exchange.ExchangeDao;
import ch.algotrader.dao.marketData.BarDao;
import ch.algotrader.dao.marketData.TickDao;
import ch.algotrader.dao.security.CombinationDao;
import ch.algotrader.dao.security.ComponentDao;
import ch.algotrader.dao.security.EasyToBorrowDao;
import ch.algotrader.dao.security.ForexDao;
import ch.algotrader.dao.security.FutureDao;
import ch.algotrader.dao.security.FutureFamilyDao;
import ch.algotrader.dao.security.IntrestRateDao;
import ch.algotrader.dao.security.OptionDao;
import ch.algotrader.dao.security.OptionFamilyDao;
import ch.algotrader.dao.security.SecurityDao;
import ch.algotrader.dao.security.SecurityFamilyDao;
import ch.algotrader.dao.security.SecurityReferenceDao;
import ch.algotrader.dao.security.StockDao;
import ch.algotrader.dao.strategy.CashBalanceDao;
import ch.algotrader.dao.strategy.MeasurementDao;
import ch.algotrader.dao.strategy.StrategyDao;
import ch.algotrader.dao.trade.OrderDao;
import ch.algotrader.dao.trade.OrderStatusDao;
import ch.algotrader.entity.Account;
import ch.algotrader.entity.Position;
import ch.algotrader.entity.Subscription;
import ch.algotrader.entity.Transaction;
import ch.algotrader.entity.exchange.Exchange;
import ch.algotrader.entity.marketData.Bar;
import ch.algotrader.entity.marketData.Tick;
import ch.algotrader.entity.security.Combination;
import ch.algotrader.entity.security.Component;
import ch.algotrader.entity.security.EasyToBorrow;
import ch.algotrader.entity.security.Forex;
import ch.algotrader.entity.security.Future;
import ch.algotrader.entity.security.FutureFamily;
import ch.algotrader.entity.security.IntrestRate;
import ch.algotrader.entity.security.Option;
import ch.algotrader.entity.security.OptionFamily;
import ch.algotrader.entity.security.Security;
import ch.algotrader.entity.security.SecurityFamily;
import ch.algotrader.entity.security.SecurityReference;
import ch.algotrader.entity.security.Stock;
import ch.algotrader.entity.strategy.CashBalance;
import ch.algotrader.entity.strategy.Measurement;
import ch.algotrader.entity.strategy.Strategy;
import ch.algotrader.entity.strategy.StrategyImpl;
import ch.algotrader.entity.trade.Order;
import ch.algotrader.enumeration.Broker;
import ch.algotrader.enumeration.Currency;
import ch.algotrader.enumeration.Duration;
import ch.algotrader.enumeration.OrderServiceType;
import ch.algotrader.util.HibernateUtil;
import ch.algotrader.util.collection.CollectionUtil;
import ch.algotrader.visitor.InitializationVisitor;
import ch.algotrader.vo.OrderStatusVO;
import ch.algotrader.vo.PositionVO;
import ch.algotrader.vo.TransactionVO;

/**
 * @author <a href="mailto:aflury@algotrader.ch">Andy Flury</a>
 *
 * @version $Revision$ $Date$
 */
@Transactional
public class LookupServiceImpl implements LookupService {

    private final Map<String, Long> securitySymbolMap = new ConcurrentHashMap<>();
    private final Map<String, Long> securityIsinMap = new ConcurrentHashMap<>();
    private final Map<String, Long> securityBbgidMap = new ConcurrentHashMap<>();
    private final Map<String, Long> securityRicMap = new ConcurrentHashMap<>();
    private final Map<String, Long> securityConidMap = new ConcurrentHashMap<>();
    private final Map<String, Long> securityIdMap = new ConcurrentHashMap<>();

    private final CommonConfig commonConfig;

    private final CoreConfig coreConfig;

    private final SessionFactory sessionFactory;

    private final GenericDao genericDao;

    private final FutureFamilyDao futureFamilyDao;

    private final FutureDao futureDao;

    private final ForexDao forexDao;

    private final SecurityFamilyDao securityFamilyDao;

    private final SecurityReferenceDao securityReferenceDao;

    private final OptionFamilyDao optionFamilyDao;

    private final ExchangeDao exchangeDao;

    private final TickDao tickDao;

    private final OptionDao optionDao;

    private final TransactionDao transactionDao;

    private final PositionDao positionDao;

    private final StrategyDao strategyDao;

    private final SecurityDao securityDao;

    private final CashBalanceDao cashBalanceDao;

    private final SubscriptionDao subscriptionDao;

    private final CombinationDao combinationDao;

    private final ComponentDao componentDao;

    private final MeasurementDao measurementDao;

    private final BarDao barDao;

    private final OrderDao orderDao;

    private final OrderStatusDao orderStatusDao;

    private final AccountDao accountDao;

    private final StockDao stockDao;

    private final IntrestRateDao intrestRateDao;

    private final EasyToBorrowDao easyToBorrowDao;

    public LookupServiceImpl(
            final CommonConfig commonConfig,
            final CoreConfig coreConfig,
            final SessionFactory sessionFactory,
            final GenericDao genericDao,
            final FutureFamilyDao futureFamilyDao,
            final FutureDao futureDao,
            final ForexDao forexDao,
            final SecurityFamilyDao securityFamilyDao,
            final SecurityReferenceDao securityReferenceDao,
            final OptionFamilyDao optionFamilyDao,
            final ExchangeDao exchangeDao,
            final TickDao tickDao,
            final OptionDao optionDao,
            final TransactionDao transactionDao,
            final PositionDao positionDao,
            final StrategyDao strategyDao,
            final SecurityDao securityDao,
            final CashBalanceDao cashBalanceDao,
            final SubscriptionDao subscriptionDao,
            final CombinationDao combinationDao,
            final ComponentDao componentDao,
            final MeasurementDao measurementDao,
            final BarDao barDao,
            final OrderDao orderDao,
            final OrderStatusDao orderStatusDao,
            final AccountDao accountDao,
            final StockDao stockDao,
            final IntrestRateDao intrestRateDao,
            final EasyToBorrowDao easyToBorrowDao) {

        Validate.notNull(commonConfig, "CommonConfig is null");
        Validate.notNull(coreConfig, "CoreConfig is null");
        Validate.notNull(sessionFactory, "SessionFactory is null");
        Validate.notNull(genericDao, "GenericDao is null");
        Validate.notNull(futureFamilyDao, "FutureFamilyDao is null");
        Validate.notNull(futureDao, "FutureDao is null");
        Validate.notNull(forexDao, "ForexDao is null");
        Validate.notNull(securityFamilyDao, "SecurityFamilyDao is null");
        Validate.notNull(securityReferenceDao, "securityReferenceDao is null");
        Validate.notNull(optionFamilyDao, "OptionFamilyDao is null");
        Validate.notNull(exchangeDao, "ExchangeDao is null");
        Validate.notNull(tickDao, "TickDao is null");
        Validate.notNull(optionDao, "OptionDao is null");
        Validate.notNull(transactionDao, "TransactionDao is null");
        Validate.notNull(positionDao, "PositionDao is null");
        Validate.notNull(strategyDao, "StrategyDao is null");
        Validate.notNull(securityDao, "SecurityDao is null");
        Validate.notNull(cashBalanceDao, "CashBalanceDao is null");
        Validate.notNull(subscriptionDao, "SubscriptionDao is null");
        Validate.notNull(combinationDao, "CombinationDao is null");
        Validate.notNull(componentDao, "ComponentDao is null");
        Validate.notNull(measurementDao, "MeasurementDao is null");
        Validate.notNull(barDao, "BarDao is null");
        Validate.notNull(orderDao, "OrderDao is null");
        Validate.notNull(orderStatusDao, "OrderStatusDao is null");
        Validate.notNull(accountDao, "AccountDao is null");
        Validate.notNull(stockDao, "StockDao is null");
        Validate.notNull(intrestRateDao, "IntrestRateDao is null");
        Validate.notNull(easyToBorrowDao, "EasyToBorrowDao is null");

        this.commonConfig = commonConfig;
        this.coreConfig = coreConfig;
        this.sessionFactory = sessionFactory;
        this.genericDao = genericDao;
        this.futureFamilyDao = futureFamilyDao;
        this.futureDao = futureDao;
        this.forexDao = forexDao;
        this.securityFamilyDao = securityFamilyDao;
        this.securityReferenceDao = securityReferenceDao;
        this.optionFamilyDao = optionFamilyDao;
        this.exchangeDao = exchangeDao;
        this.tickDao = tickDao;
        this.optionDao = optionDao;
        this.transactionDao = transactionDao;
        this.positionDao = positionDao;
        this.strategyDao = strategyDao;
        this.securityDao = securityDao;
        this.cashBalanceDao = cashBalanceDao;
        this.subscriptionDao = subscriptionDao;
        this.combinationDao = combinationDao;
        this.componentDao = componentDao;
        this.measurementDao = measurementDao;
        this.barDao = barDao;
        this.orderDao = orderDao;
        this.orderStatusDao = orderStatusDao;
        this.accountDao = accountDao;
        this.stockDao = stockDao;
        this.intrestRateDao = intrestRateDao;
        this.easyToBorrowDao = easyToBorrowDao;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Security getSecurity(final long id) {

        return this.securityDao.get(id);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Security getSecurityByIsin(final String isin) {

        Validate.notEmpty(isin, "isin is empty");

        return this.securityDao.findByIsin(isin);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Security getSecurityBySymbol(final String symbol) {

        Validate.notEmpty(symbol, "Symbol is empty");

        return this.securityDao.findBySymbol(symbol);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Security getSecurityByBbgid(final String bbgid) {

        Validate.notEmpty(bbgid, "bbgid is empty");

        return this.securityDao.findByBbgid(bbgid);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Security getSecurityByRic(final String ric) {

        Validate.notEmpty(ric, "ric is empty");

        return this.securityDao.findByRic(ric);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Security getSecurityByConid(final String conid) {

        Validate.notEmpty(conid, "Con id is empty");

        return this.securityDao.findByConid(conid);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Security getSecurityInclFamilyAndUnderlying(final long id) {

        return this.securityDao.findByIdInclFamilyAndUnderlying(id);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Security getSecurityInitialized(final long id) {

        return this.securityDao.findByIdInitialized(id);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Security> getSecuritiesByIds(final Collection<Long> ids) {

        return this.securityDao.findByIds(ids);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getSecurityIdBySecurityString(final String securityString) {

        Validate.notEmpty(securityString, "Security string is empty");

        // try to find it in the local hashMap cache by symbol, isin, bbgid, ric conid or id
        if (this.securitySymbolMap.containsKey(securityString)) {
            return this.securitySymbolMap.get(securityString);
        }

        if (this.securityIsinMap.containsKey(securityString)) {
            return this.securityIsinMap.get(securityString);
        }

        if (this.securityBbgidMap.containsKey(securityString)) {
            return this.securityBbgidMap.get(securityString);
        }

        if (this.securityRicMap.containsKey(securityString)) {
            return this.securityRicMap.get(securityString);
        }

        if (this.securityConidMap.containsKey(securityString)) {
            return this.securityConidMap.get(securityString);
        }

        if (this.securityIdMap.containsKey(securityString)) {
            return this.securityIdMap.get(securityString);
        }

        // try to find the security by symbol, isin, bbgid, ric conid or id
        Security security = this.securityDao.findBySymbol(securityString);
        if (security != null) {
            this.securitySymbolMap.put(security.getSymbol(), security.getId());
            return security.getId();
        }

        security = this.securityDao.findByIsin(securityString);
        if (security != null) {
            this.securityIsinMap.put(security.getIsin(), security.getId());
            return security.getId();
        }

        security = this.securityDao.findByBbgid(securityString);
        if (security != null) {
            this.securityBbgidMap.put(security.getBbgid(), security.getId());
            return security.getId();
        }

        security = this.securityDao.findByRic(securityString);
        if (security != null) {
            this.securityRicMap.put(security.getRic(), security.getId());
            return security.getId();
        }

        security = this.securityDao.findByConid(securityString);
        if (security != null) {
            this.securityConidMap.put(security.getConid(), security.getId());
            return security.getId();
        }

        if (NumberUtils.isDigits(securityString)) {

            security = this.securityDao.get(Long.parseLong(securityString));
            if (security != null) {
                this.securitySymbolMap.put(Long.toString(security.getId()), security.getId());
                return security.getId();
            }
        }

        throw new ServiceException("Security could not be found: " + securityString);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Security> getAllSecurities() {

        return this.securityDao.loadAll();

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Security> getSubscribedSecuritiesForAutoActivateStrategies() {

        return this.securityDao.findSubscribedForAutoActivateStrategies();

    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public List getSubscribedSecuritiesAndFeedTypeForAutoActivateStrategiesInclComponents() {

        List<Map> subscriptions = this.securityDao.findSubscribedAndFeedTypeForAutoActivateStrategies();

        // initialize components
        for (Map<String, Object> subscription : subscriptions) {

            Security security = (Security) subscription.get("security");
            if (security instanceof Combination) {
                Hibernate.initialize(((Combination) security).getComponents());
            }
        }

        return subscriptions;

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Stock> getStocksBySector(final String code) {

        Validate.notEmpty(code, "Code is empty");

        return this.stockDao.findBySectory(code);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Stock> getStocksByIndustryGroup(final String code) {

        Validate.notEmpty(code, "Code is empty");

        return this.stockDao.findByIndustryGroup(code);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Stock> getStocksByIndustry(final String code) {

        Validate.notEmpty(code, "Code is empty");

        return this.stockDao.findByIndustry(code);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Stock> getStocksBySubIndustry(final String code) {

        Validate.notEmpty(code, "Code is empty");

        return this.stockDao.findBySubIndustry(code);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Option> getSubscribedOptions() {

        return this.optionDao.findSubscribedOptions();

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Future> getSubscribedFutures() {

        return this.futureDao.findSubscribedFutures();

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Combination> getSubscribedCombinationsByStrategy(final String strategyName) {

        Validate.notEmpty(strategyName, "Strategy name is empty");

        return this.combinationDao.findSubscribedByStrategy(strategyName);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Combination> getSubscribedCombinationsByStrategyAndUnderlying(final String strategyName, final long underlyingId) {

        Validate.notEmpty(strategyName, "Strategy name is empty");

        return this.combinationDao.findSubscribedByStrategyAndUnderlying(strategyName, underlyingId);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Combination> getSubscribedCombinationsByStrategyAndComponent(final String strategyName, final long securityId) {

        Validate.notEmpty(strategyName, "Strategy name is empty");

        return this.combinationDao.findSubscribedByStrategyAndComponent(strategyName, securityId);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Combination> getSubscribedCombinationsByStrategyAndComponentClass(final String strategyName, final Class type) {

        Validate.notEmpty(strategyName, "Strategy name is empty");
        Validate.notNull(type, "Type is null");

        int discriminator = HibernateUtil.getDisriminatorValue(this.sessionFactory, type);
        return this.combinationDao.findSubscribedByStrategyAndComponentType(strategyName, discriminator);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Component> getSubscribedComponentsByStrategyInclSecurity(final String strategyName) {

        Validate.notEmpty(strategyName, "Strategy name is empty");

        return this.componentDao.findSubscribedByStrategyInclSecurity(strategyName);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Component> getSubscribedComponentsBySecurityInclSecurity(final long securityId) {

        return this.componentDao.findSubscribedBySecurityInclSecurity(securityId);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Component> getSubscribedComponentsByStrategyAndSecurityInclSecurity(final String strategyName, final long securityId) {

        Validate.notEmpty(strategyName, "Strategy name is empty");

        return this.componentDao.findSubscribedByStrategyAndSecurityInclSecurity(strategyName, securityId);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Subscription getSubscriptionByStrategyAndSecurity(final String strategyName, final long securityId) {

        Validate.notEmpty(strategyName, "Strategy name is empty");

        return this.subscriptionDao.findByStrategyAndSecurity(strategyName, securityId);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Subscription> getSubscriptionsByStrategyInclComponentsAndProps(final String strategyName) {

        Validate.notEmpty(strategyName, "Strategy name is empty");

        List<Subscription> subscriptions = this.subscriptionDao.findByStrategyInclProps(strategyName);

        // initialize components
        for (Subscription subscription : subscriptions) {

            if (subscription.getSecurity() instanceof Combination) {
                Combination combination = (Combination) subscription.getSecurity();
                Hibernate.initialize(combination.getComponents());
            }
        }

        return subscriptions;

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Subscription> getNonPositionSubscriptions(final String strategyName) {

        Validate.notEmpty(strategyName, "Strategy name is empty");

        return this.subscriptionDao.findNonPositionSubscriptions(strategyName);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Strategy> getAllStrategies() {

        return this.strategyDao.loadAll();

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Strategy getStrategy(final long id) {

        return this.strategyDao.get(id);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Strategy getStrategyByName(final String name) {

        Validate.notEmpty(name, "Name is empty");

        return this.strategyDao.findByName(name);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SecurityFamily getSecurityFamily(final long id) {

        return this.securityFamilyDao.get(id);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SecurityFamily getSecurityFamilyByName(final String name) {

        Validate.notEmpty(name, "Name is empty");

        return this.securityFamilyDao.findByName(name);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Security getSecurityReferenceTargetByOwnerAndName(long securityId, String name) {
        Validate.notEmpty(name, "Name is empty");

        final SecurityReference ref = this.securityReferenceDao.findByOwnerAndName(securityId, name);
        return ref == null ? null : ref.getTarget();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OptionFamily getOptionFamilyByUnderlying(final long id) {

        return this.optionFamilyDao.findByUnderlying(id);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FutureFamily getFutureFamilyByUnderlying(final long id) {

        return this.futureFamilyDao.findByUnderlying(id);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Exchange getExchangeByName(String name) {

        return this.exchangeDao.findByName(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Position> getAllPositions() {

        return this.positionDao.loadAll();

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Position getPosition(final long id) {

        return this.positionDao.get(id);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Position getPositionInclSecurityAndSecurityFamily(final long id) {

        return this.positionDao.findByIdInclSecurityAndSecurityFamily(id);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Position> getPositionsByStrategy(final String strategyName) {

        Validate.notEmpty(strategyName, "Strategy name is empty");

        return this.positionDao.findByStrategy(strategyName);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Position getPositionBySecurityAndStrategy(final long securityId, final String strategyName) {

        Validate.notEmpty(strategyName, "Strategy name is empty");

        return this.positionDao.findBySecurityAndStrategy(securityId, strategyName);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<PositionVO> getPositionsVO(final String strategyName, final boolean openPositions) {

        Validate.notEmpty(strategyName, "Strategy name is empty");

        if (strategyName.equals(StrategyImpl.SERVER)) {
            if (openPositions) {
                return this.positionDao.loadAll(PositionVOProducer.INSTANCE);
            } else {
                return this.positionDao.findOpenPositions(PositionVOProducer.INSTANCE);
            }
        } else {
            if (openPositions) {
                return this.positionDao.findByStrategy(strategyName, PositionVOProducer.INSTANCE);
            } else {
                return this.positionDao.findOpenPositionsByStrategy(strategyName, PositionVOProducer.INSTANCE);
            }
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Position> getOpenPositions() {

        return this.positionDao.findOpenPositions();

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Position> getOpenTradeablePositions() {

        return this.positionDao.findOpenTradeablePositions();

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Position> getOpenPositionsByStrategy(final String strategyName) {

        Validate.notEmpty(strategyName, "Strategy name is empty");

        return this.positionDao.findOpenPositionsByStrategy(strategyName);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Position> getOpenTradeablePositionsByStrategy(final String strategyName) {

        Validate.notEmpty(strategyName, "Strategy name is empty");

        return this.positionDao.findOpenTradeablePositionsByStrategy(strategyName);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Position> getOpenPositionsBySecurity(final long securityId) {

        return this.positionDao.findOpenPositionsBySecurity(securityId);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Position> getOpenPositionsByStrategyAndType(final String strategyName, final Class type) {

        Validate.notEmpty(strategyName, "Strategy name is empty");
        Validate.notNull(type, "Type is null");

        int discriminator = HibernateUtil.getDisriminatorValue(this.sessionFactory, type);
        return this.positionDao.findOpenPositionsByStrategyAndType(strategyName, discriminator);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Position> getOpenPositionsByStrategyTypeAndUnderlyingType(final String strategyName, final Class type, final Class underlyingType) {

        Validate.notEmpty(strategyName, "Strategy name is empty");
        Validate.notNull(type, "Type is null");
        Validate.notNull(underlyingType, "Underlying type is null");

        int discriminator = HibernateUtil.getDisriminatorValue(this.sessionFactory, type);
        int underlyingDiscriminator = HibernateUtil.getDisriminatorValue(this.sessionFactory, underlyingType);
        return this.positionDao.findOpenPositionsByStrategyTypeAndUnderlyingType(strategyName, discriminator, underlyingDiscriminator);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Position> getOpenPositionsByStrategyAndSecurityFamily(final String strategyName, final long securityFamilyId) {

        Validate.notEmpty(strategyName, "Strategy name is empty");

        return this.positionDao.findOpenPositionsByStrategyAndSecurityFamily(strategyName, securityFamilyId);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Position> getOpenFXPositions() {

        return this.positionDao.findOpenFXPositions();

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Position> getOpenFXPositionsByStrategy(final String strategyName) {

        Validate.notEmpty(strategyName, "Strategy name is empty");

        return this.positionDao.findOpenFXPositionsByStrategy(strategyName);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Transaction getTransaction(final long id) {

        return this.transactionDao.get(id);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<TransactionVO> getTransactionsVO(final String strategyName) {

        Validate.notEmpty(strategyName, "Strategy name is empty");

        int transactionDisplayCount = this.coreConfig.getTransactionDisplayCount();
        TransactionVOProducer converter = new TransactionVOProducer(this.commonConfig);
        if (strategyName.equals(StrategyImpl.SERVER)) {
            return this.transactionDao.findTransactionsDesc(transactionDisplayCount, converter);
        } else {
            return this.transactionDao.findTransactionsByStrategyDesc(transactionDisplayCount, strategyName, converter);
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Order> getOpenOrdersByStrategy(final String strategyName) {

        Validate.notEmpty(strategyName, "Strategy name is empty");

        return this.orderDao.findOpenOrdersByStrategy(strategyName);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Order> getOpenOrdersByStrategyAndSecurity(final String strategyName, final long securityId) {

        Validate.notEmpty(strategyName, "Strategy name is empty");

        return this.orderDao.findOpenOrdersByStrategyAndSecurity(strategyName, securityId);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Order getOpenOrderByIntId(final String intId) {

        Validate.notEmpty(intId, "Int id is empty");

        return this.orderDao.findOpenOrderByIntId(intId);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Order getOpenOrderByRootIntId(final String intId) {

        Validate.notEmpty(intId, "Int id is empty");

        return this.orderDao.findOpenOrderByRootIntId(intId);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Order getOpenOrderByExtId(final String extId) {

        Validate.notEmpty(extId, "Ext id is empty");

        return this.orderDao.findOpenOrderByExtId(extId);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<OrderStatusVO> getOpenOrdersVOByStrategy(final String strategyName) {

        Validate.notEmpty(strategyName, "Strategy name is empty");

        if (strategyName.equals(StrategyImpl.SERVER)) {
            return this.orderStatusDao.findAllOrderStati();
        } else {
            return this.orderStatusDao.findOrderStatiByStrategy(strategyName);
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Account getAccountByName(final String accountName) {

        Validate.notEmpty(accountName, "Account name is empty");

        return this.accountDao.findByName(accountName);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<String> getActiveSessionsByOrderServiceType(final OrderServiceType orderServiceType) {

        Validate.notNull(orderServiceType, "Order service type is null");

        return this.accountDao.findActiveSessionsByOrderServiceType(orderServiceType);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Tick getLastTick(final long securityId, Date dateTime) {

        Tick tick = CollectionUtil.getSingleElementOrNull(this.tickDao.findTicksBySecurityAndMaxDate(1, securityId, dateTime, this.coreConfig.getIntervalDays()));

        if (tick != null) {
            tick.accept(InitializationVisitor.INSTANCE, HibernateInitializer.INSTANCE);
        }

        return tick;

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Tick> getTicksByMaxDate(final long securityId, final Date maxDate) {

        Validate.notNull(maxDate, "Max date is null");

        return this.tickDao.findTicksBySecurityAndMaxDate(securityId, maxDate, this.coreConfig.getIntervalDays());

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Tick> getTicksByMinDate(final long securityId, final Date minDate) {

        Validate.notNull(minDate, "Min date is null");

        return this.tickDao.findTicksBySecurityAndMinDate(securityId, minDate, this.coreConfig.getIntervalDays());

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Tick> getDailyTicksBeforeTime(final long securityId, final Date time) {

        Validate.notNull(time, "Time is null");

        List<Long> ids = this.tickDao.findDailyTickIdsBeforeTime(securityId, time);
        if (ids.size() > 0) {
            return this.tickDao.findByIdsInclSecurityAndUnderlying(ids);
        } else {
            return new ArrayList<>();
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Tick> getDailyTicksAfterTime(final long securityId, final Date time) {

        Validate.notNull(time, "Time is null");

        List<Long> ids = this.tickDao.findDailyTickIdsAfterTime(securityId, time);
        if (ids.size() > 0) {
            return this.tickDao.findByIdsInclSecurityAndUnderlying(ids);
        } else {
            return new ArrayList<>();
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Tick> getHourlyTicksBeforeMinutesByMinDate(final long securityId, final int minutes, final Date minDate) {

        Validate.notNull(minDate, "Min date is null");

        List<Long> ids = this.tickDao.findHourlyTickIdsBeforeMinutesByMinDate(securityId, minutes, minDate);
        if (ids.size() > 0) {
            return this.tickDao.findByIdsInclSecurityAndUnderlying(ids);
        } else {
            return new ArrayList<>();
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Tick> getHourlyTicksAfterMinutesByMinDate(final long securityId, final int minutes, final Date minDate) {

        Validate.notNull(minDate, "Min date is null");

        List<Long> ids = this.tickDao.findHourlyTickIdsAfterMinutesByMinDate(securityId, minutes, minDate);
        if (ids.size() > 0) {
            return this.tickDao.findByIdsInclSecurityAndUnderlying(ids);
        } else {
            return new ArrayList<>();
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Tick> getSubscribedTicksByTimePeriod(final Date startDate, final Date endDate) {

        Validate.notNull(startDate, "Start date is null");
        Validate.notNull(endDate, "End date is null");

        List<Tick> ticks = this.tickDao.findSubscribedByTimePeriod(startDate, endDate);
        for (Tick tick : ticks) {
            tick.accept(InitializationVisitor.INSTANCE, HibernateInitializer.INSTANCE);
        }
        return ticks;

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Tick getFirstSubscribedTick() {

        return CollectionUtil.getFirstElementOrNull(this.tickDao.findSubscribedByTimePeriod(1, new Date(0), new Date(Long.MAX_VALUE)));

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Tick getTickBySecurityAndMaxDate(final long securityId, final Date date) {

        Validate.notNull(date, "Date is null");

        return this.tickDao.findBySecurityAndMaxDate(securityId, date);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Bar> getDailyBarsFromTicks(final long securityId, final Date fromDate, final Date toDate) {

        Validate.notNull(fromDate, "From date is null");
        Validate.notNull(toDate, "To date is null");

        return this.barDao.findDailyBarsFromTicks(securityId, fromDate, toDate);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Bar> getLastNBarsBySecurityAndBarSize(final int n, final long securityId, final Duration barSize) {

        Validate.notNull(barSize, "Bar size is null");

        return this.barDao.findBarsBySecurityAndBarSize(n, securityId, barSize);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Bar> getBarsBySecurityBarSizeAndMinDate(final long securityId, final Duration barSize, final Date minDate) {

        Validate.notNull(barSize, "Bar size is null");
        Validate.notNull(minDate, "Min date is null");

        return this.barDao.findBarsBySecurityBarSizeAndMinDate(securityId, barSize, minDate);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Bar> getSubscribedBarsByTimePeriodAndBarSize(final Date startDate, final Date endDate, final Duration barSize) {

        Validate.notNull(startDate, "Start date is null");
        Validate.notNull(endDate, "End date is null");
        Validate.notNull(barSize, "Bar size is null");

        List<Bar> bars = this.barDao.findSubscribedByTimePeriodAndBarSize(startDate, endDate, barSize);
        for (Bar bar : bars) {
            bar.accept(InitializationVisitor.INSTANCE, HibernateInitializer.INSTANCE);
        }
        return bars;

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Bar getFirstSubscribedBarByBarSize(final Duration barSize) {

        Validate.notNull(barSize, "Bar size is null");

        return CollectionUtil.getFirstElementOrNull(this.barDao.findSubscribedByTimePeriodAndBarSize(1, new Date(0), new Date(Long.MAX_VALUE), barSize));

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Forex getForex(final Currency baseCurrency, final Currency transactionCurrency) {

        Validate.notNull(baseCurrency, "Base currency is null");
        Validate.notNull(transactionCurrency, "Transaction currency is null");

        return this.forexDao.getForex(baseCurrency, transactionCurrency);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getForexRateByDate(final Currency baseCurrency, final Currency transactionCurrency, final Date date) {

        Validate.notNull(baseCurrency, "Base currency is null");
        Validate.notNull(transactionCurrency, "Transaction currency is null");
        Validate.notNull(date, "Date is null");

        return this.forexDao.getRateDoubleByDate(baseCurrency, transactionCurrency, date);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IntrestRate getInterestRateByCurrencyAndDuration(final Currency currency, final Duration duration) {

        Validate.notNull(currency, "Currency is null");
        Validate.notNull(duration, "Duration is null");

        return this.intrestRateDao.findByCurrencyAndDuration(currency, duration);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getInterestRateByCurrencyDurationAndDate(final Currency currency, final Duration duration, final Date date) {

        Validate.notNull(currency, "Currency is null");
        Validate.notNull(duration, "Duration is null");
        Validate.notNull(date, "Date is null");

        IntrestRate intrestRate = this.intrestRateDao.findByCurrencyAndDuration(currency, duration);

        List<Tick> ticks = this.tickDao.findTicksBySecurityAndMaxDate(1, intrestRate.getId(), date, this.coreConfig.getIntervalDays());
        if (ticks.isEmpty()) {
            throw new ServiceException("Cannot get intrestRate for " + currency + " and duration " + duration + " because no last tick is available for date " + date);
        }

        return CollectionUtil.getFirstElement(ticks).getCurrentValueDouble();

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Currency> getHeldCurrencies() {

        return this.cashBalanceDao.findHeldCurrencies();

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<CashBalance> getCashBalancesByStrategy(final String strategyName) {

        Validate.notNull(strategyName, "Strategy name is null");

        return this.cashBalanceDao.findCashBalancesByStrategy(strategyName);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map getMeasurementsByMaxDate(final String strategyName, final String name, final Date maxDate) {

        Validate.notNull(strategyName, "Strategy name is null");
        Validate.notNull(name, "Name is null");
        Validate.notNull(maxDate, "Max date is null");

        List<Measurement> measurements = this.measurementDao.findMeasurementsByMaxDate(strategyName, name, maxDate);

        return getValuesByDate(measurements);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map getAllMeasurementsByMaxDate(final String strategyName, final Date maxDate) {

        Validate.notNull(strategyName, "Strategy name is null");
        Validate.notNull(maxDate, "Max date is null");

        List<Measurement> measurements = this.measurementDao.findAllMeasurementsByMaxDate(strategyName, maxDate);

        return getNameValuePairsByDate(measurements);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map getMeasurementsByMinDate(final String strategyName, final String name, final Date minDate) {

        Validate.notNull(strategyName, "Strategy name is null");
        Validate.notNull(name, "Name is null");
        Validate.notNull(minDate, "Min date is null");

        List<Measurement> measurements = this.measurementDao.findMeasurementsByMinDate(strategyName, name, minDate);

        return getValuesByDate(measurements);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map getAllMeasurementsByMinDate(final String strategyName, final Date minDate) {

        Validate.notNull(strategyName, "Strategy name is null");
        Validate.notNull(minDate, "Min date is null");

        List<Measurement> measurements = this.measurementDao.findAllMeasurementsByMinDate(strategyName, minDate);

        return getNameValuePairsByDate(measurements);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getMeasurementByMaxDate(final String strategyName, final String name, final Date maxDate) {

        Validate.notNull(strategyName, "Strategy name is null");
        Validate.notNull(name, "Name is null");
        Validate.notNull(maxDate, "Max date is null");

        Measurement measurement = CollectionUtil.getSingleElementOrNull(this.measurementDao.findMeasurementsByMaxDate(1, strategyName, name, maxDate));
        return measurement != null ? measurement.getValue() : null;

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getMeasurementByMinDate(final String strategyName, final String name, final Date minDate) {

        Validate.notNull(strategyName, "Strategy name is null");
        Validate.notNull(name, "Name is null");
        Validate.notNull(minDate, "Min date is null");

        Measurement measurement = CollectionUtil.getSingleElementOrNull(this.measurementDao.findMeasurementsByMinDate(1, strategyName, name, minDate));
        return measurement != null ? measurement.getValue() : null;

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<EasyToBorrow> getEasyToBorrowByDateAndBroker(final Date date, final Broker broker) {

        Validate.notNull(date, "Date is null");
        Validate.notNull(broker, "Broker is null");

        return this.easyToBorrowDao.findByDateAndBroker(DateUtils.truncate(date, Calendar.DATE), broker);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Date getCurrentDBTime() {

        return this.strategyDao.findCurrentDBTime();

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<?> get(final String query, final NamedParam... namedParams) {

        Validate.notEmpty(query, "Query is empty");
        Validate.notNull(namedParams, "Named parameters is null");

        return this.genericDao.find(query, namedParams);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<?> get(final String query, final int maxResults, final NamedParam... namedParams) {

        Validate.notEmpty(query, "Query is empty");
        Validate.notNull(namedParams, "Named parameters is null");

        return this.genericDao.find(query, maxResults, namedParams);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getUnique(final String query, final NamedParam... namedParams) {

        Validate.notEmpty(query, "Query is empty");
        Validate.notNull(namedParams, "Named parameters is null");

        return this.genericDao.findUnique(query, namedParams);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getNamedQuery(String queryName) {
        SessionFactoryImpl sessionFactoryImpl = (SessionFactoryImpl) this.sessionFactory;
        return sessionFactoryImpl.getNamedQuery(queryName).getQueryString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initSecurityStrings() {

        for (Security security : this.securityDao.findSubscribedForAutoActivateStrategies()) {

            this.securityIdMap.put(Long.toString(security.getId()), security.getId());

            if (security.getSymbol() != null) {
                this.securitySymbolMap.put(security.getSymbol(), security.getId());
            }

            if (security.getIsin() != null) {
                this.securityIsinMap.put(security.getIsin(), security.getId());
            }

            if (security.getBbgid() != null) {
                this.securityBbgidMap.put(security.getBbgid(), security.getId());
            }

            if (security.getRic() != null) {
                this.securityRicMap.put(security.getRic(), security.getId());
            }

            if (security.getConid() != null) {
                this.securityConidMap.put(security.getConid(), security.getId());
            }
        }

    }

    private Map<Date, Object> getValuesByDate(List<Measurement> measurements) {

        Map<Date, Object> valuesByDate = new HashMap<>();
        for (Measurement measurement : measurements) {
            valuesByDate.put(measurement.getDateTime(), measurement.getValue());
        }

        return valuesByDate;
    }

    @SuppressWarnings("unchecked")
    private Map<Date, Map<String, Object>> getNameValuePairsByDate(List<Measurement> measurements) {

        // group Measurements by date
        MultiValueMap measurementsByDate = new MultiValueMap();
        for (Measurement measurement : measurements) {
            measurementsByDate.put(measurement.getDateTime(), measurement);
        }

        // create a nameValuePair Map per date
        Map<Date, Map<String, Object>> nameValuePairsByDate = new HashMap<>();
        for (Date dt : (Set<Date>) measurementsByDate.keySet()) {

            Map<String, Object> nameValuePairs = new HashMap<>();
            for (Measurement measurement : (Collection<Measurement>) measurementsByDate.get(dt)) {
                nameValuePairs.put(measurement.getName(), measurement.getValue());
            }
            nameValuePairsByDate.put(dt, nameValuePairs);
        }

        return nameValuePairsByDate;
    }


}
