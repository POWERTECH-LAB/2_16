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
package ch.algotrader.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections.map.MultiValueMap;
import org.apache.commons.lang.Validate;
import org.apache.commons.lang.time.DateUtils;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import ch.algotrader.cache.CacheManager;
import ch.algotrader.dao.GenericDao;
import ch.algotrader.dao.NamedParam;
import ch.algotrader.entity.Account;
import ch.algotrader.entity.AccountImpl;
import ch.algotrader.entity.Position;
import ch.algotrader.entity.PositionImpl;
import ch.algotrader.entity.Subscription;
import ch.algotrader.entity.SubscriptionImpl;
import ch.algotrader.entity.Transaction;
import ch.algotrader.entity.TransactionImpl;
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
import ch.algotrader.entity.security.SecurityFamilyImpl;
import ch.algotrader.entity.security.SecurityImpl;
import ch.algotrader.entity.security.SecurityReference;
import ch.algotrader.entity.security.Stock;
import ch.algotrader.entity.strategy.CashBalance;
import ch.algotrader.entity.strategy.Measurement;
import ch.algotrader.entity.strategy.Strategy;
import ch.algotrader.entity.strategy.StrategyImpl;
import ch.algotrader.entity.trade.Order;
import ch.algotrader.enumeration.Currency;
import ch.algotrader.enumeration.Direction;
import ch.algotrader.enumeration.Duration;
import ch.algotrader.enumeration.QueryType;
import ch.algotrader.util.DateTimeLegacy;
import ch.algotrader.util.collection.CollectionUtil;
import ch.algotrader.util.collection.Pair;
import ch.algotrader.visitor.InitializationVisitor;

/**
 * @author <a href="mailto:aflury@algotrader.ch">Andy Flury</a>
 */
@Transactional(propagation = Propagation.SUPPORTS)
public class LookupServiceImpl implements LookupService {

    private final GenericDao genericDao;

    private final CacheManager cacheManager;

    public LookupServiceImpl(
            final GenericDao genericDao,
            final CacheManager cacheManager) {

        Validate.notNull(genericDao, "GenericDao is null");
        Validate.notNull(cacheManager, "CacheManager is null");

        this.genericDao = genericDao;
        this.cacheManager = cacheManager;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Security getSecurity(final long id) {

        return this.cacheManager.get(SecurityImpl.class, id);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Security getSecurityByIsin(final String isin) {

        Validate.notEmpty(isin, "isin is empty");

        return  this.cacheManager.findUnique(Security.class, "Security.findByIsin", QueryType.BY_NAME, new NamedParam("isin", isin));

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Security getSecurityBySymbol(final String symbol) {

        return this.cacheManager.findUnique(Security.class, "Security.findBySymbol", QueryType.BY_NAME, new NamedParam("symbol", symbol));

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Security getSecurityByBbgid(final String bbgid) {

        Validate.notEmpty(bbgid, "bbgid is empty");

        return this.cacheManager.findUnique(Security.class, "Security.findByBbgid", QueryType.BY_NAME, new NamedParam("bbgid", bbgid));

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Security getSecurityByRic(final String ric) {

        Validate.notEmpty(ric, "ric is empty");

        return this.cacheManager.findUnique(Security.class, "Security.findByRic", QueryType.BY_NAME, new NamedParam("ric", ric));

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Security getSecurityByConid(final String conid) {

        Validate.notEmpty(conid, "Con id is empty");

        return this.cacheManager.findUnique(Security.class, "Security.findByConid", QueryType.BY_NAME, new NamedParam("conid", conid));

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Security getSecurityInclUnderlyingFamilyAndExchange(final long id) {

        return this.cacheManager.findUnique(Security.class, "Security.findByIdInclUnderlyingFamilyAndExchange", QueryType.BY_NAME, new NamedParam("id", id));

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Security> getSecuritiesByIds(final Collection<Long> ids) {

        return this.cacheManager.find(Security.class, "Security.findByIds", QueryType.BY_NAME, new NamedParam("ids", ids));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends SecurityFamily> Collection<T> getAllSecurityFamilies(Class<T> familyClass) {

        return this.cacheManager.getAll(familyClass);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Security> getAllSecurities() {

        return this.cacheManager.getAll(Security.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SecurityFamily getSecurityFamilyBySecurity(long securityId) {

        Security security = getSecurityInclUnderlyingFamilyAndExchange(securityId);
        return security != null ? security.getSecurityFamily() : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Exchange getExchangeBySecurity(long securityId) {

        Security security = getSecurityInclUnderlyingFamilyAndExchange(securityId);
        return security != null ? security.getSecurityFamily().getExchange() : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Security> getSubscribedSecuritiesForAutoActivateStrategies() {

        return this.cacheManager.find(Security.class, "Security.findSubscribedForAutoActivateStrategies", QueryType.BY_NAME);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Pair<Security, String>> getSubscribedSecuritiesAndFeedTypeForAutoActivateStrategiesInclComponents() {

        List<SubscriptionImpl> subscriptions = this.cacheManager.find(SubscriptionImpl.class, "Subscription.findSubscribedAndFeedTypeForAutoActivateStrategies", QueryType.BY_NAME);
        List<Pair<Security, String>> list = subscriptions.stream().map(s -> new Pair<>(s.getSecurity(), s.getFeedType())).collect(Collectors.toList());

        // initialize components
        for (Pair<Security, String> pair : list) {
            if (pair.getFirst() instanceof Combination) {
                pair.getFirst().accept(InitializationVisitor.INSTANCE, this.cacheManager);
            }
        }

        return list;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Stock> getStocksBySector(final String code) {

        Validate.notEmpty(code, "Code is empty");

        return this.cacheManager.find(Stock.class, "Stock.findBySectory", QueryType.BY_NAME, new NamedParam("code", code));

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Stock> getStocksByIndustryGroup(final String code) {

        Validate.notEmpty(code, "Code is empty");

        return this.cacheManager.find(Stock.class, "Stock.findByIndustryGroup", QueryType.BY_NAME, new NamedParam("code", code));

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Stock> getStocksByIndustry(final String code) {

        Validate.notEmpty(code, "Code is empty");

        return this.cacheManager.find(Stock.class, "Stock.findByIndustry", QueryType.BY_NAME, new NamedParam("code", code));

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Stock> getStocksBySubIndustry(final String code) {

        Validate.notEmpty(code, "Code is empty");

        return this.cacheManager.find(Stock.class, "Stock.findBySubIndustry", QueryType.BY_NAME, new NamedParam("code", code));

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Option> getSubscribedOptions() {

        return this.cacheManager.find(Option.class, "Option.findSubscribedOptions", QueryType.BY_NAME);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Future> getSubscribedFutures() {

        return this.cacheManager.find(Future.class, "Future.findSubscribedFutures", QueryType.BY_NAME);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Combination> getSubscribedCombinationsByStrategy(final String strategyName) {

        Validate.notEmpty(strategyName, "Strategy name is empty");

        return this.cacheManager.find(Combination.class, "Combination.findSubscribedByStrategy", QueryType.BY_NAME, new NamedParam("strategyName", strategyName));

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Combination> getSubscribedCombinationsByStrategyAndUnderlying(final String strategyName, final long underlyingId) {

        Validate.notEmpty(strategyName, "Strategy name is empty");

        return this.cacheManager.find(Combination.class, "Combination.findSubscribedByStrategyAndUnderlying", QueryType.BY_NAME, new NamedParam("strategyName", strategyName),new NamedParam("underlyingId", underlyingId));

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Combination> getSubscribedCombinationsByStrategyAndComponent(final String strategyName, final long securityId) {

        Validate.notEmpty(strategyName, "Strategy name is empty");

        return this.cacheManager.find(Combination.class, "Combination.findSubscribedByStrategyAndComponent", QueryType.BY_NAME, new NamedParam("strategyName", strategyName), new NamedParam("securityId", securityId));

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Combination> getSubscribedCombinationsByStrategyAndComponentClass(final String strategyName, final Class<? extends Security> type) {

        Validate.notEmpty(strategyName, "Strategy name is empty");
        Validate.notNull(type, "Type is null");

        return this.cacheManager.find(Combination.class, "Combination.findSubscribedByStrategyAndComponentType", QueryType.BY_NAME, new NamedParam("strategyName", strategyName),
                new NamedParam("type", type.getSimpleName()));

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Component> getSubscribedComponentsByStrategyInclSecurity(final String strategyName) {

        Validate.notEmpty(strategyName, "Strategy name is empty");

        return this.cacheManager.find(Component.class, "Component.findSubscribedByStrategyInclSecurity", QueryType.BY_NAME, new NamedParam("strategyName", strategyName));

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Component> getSubscribedComponentsBySecurityInclSecurity(final long securityId) {

        return this.cacheManager.find(Component.class, "Component.findSubscribedBySecurityInclSecurity", QueryType.BY_NAME, new NamedParam("securityId", securityId));

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Component> getSubscribedComponentsByStrategyAndSecurityInclSecurity(final String strategyName, final long securityId) {

        Validate.notEmpty(strategyName, "Strategy name is empty");

        return this.cacheManager.find(Component.class, "Component.findSubscribedByStrategyAndSecurityInclSecurity", QueryType.BY_NAME, new NamedParam("strategyName", strategyName), new NamedParam("securityId", securityId));

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Subscription getSubscriptionByStrategyAndSecurity(final String strategyName, final long securityId) {

        Validate.notEmpty(strategyName, "Strategy name is empty");

        return this.cacheManager.findUnique(Subscription.class, "Subscription.findByStrategyAndSecurity", QueryType.BY_NAME, new NamedParam("strategyName", strategyName), new NamedParam("securityId", securityId));

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Subscription> getSubscriptionsByStrategy(final String strategyName) {

        Validate.notEmpty(strategyName, "Strategy name is empty");

        List<Subscription> subscriptions = this.cacheManager.find(Subscription.class, "Subscription.findByStrategy", QueryType.BY_NAME, new NamedParam("strategyName", strategyName));

        // initialize components
        for (Subscription subscription : subscriptions) {
            if (subscription.getSecurity() instanceof Combination) {
                subscription.getSecurity().accept(InitializationVisitor.INSTANCE, this.cacheManager);
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

        return this.cacheManager.find(Subscription.class, "Subscription.findNonPositionSubscriptions", QueryType.BY_NAME, new NamedParam("strategyName", strategyName));

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Strategy> getAllStrategies() {

        return this.cacheManager.getAll(Strategy.class);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Strategy getStrategy(final long id) {

        return this.cacheManager.get(StrategyImpl.class, id);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Strategy getStrategyByName(final String name) {

        Validate.notEmpty(name, "Name is empty");

        return this.cacheManager.findUnique(Strategy.class, "Strategy.findByName", QueryType.BY_NAME, new NamedParam("name", name));

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SecurityFamily getSecurityFamily(final long id) {

        return this.cacheManager.get(SecurityFamilyImpl.class, id);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SecurityFamily getSecurityFamilyByName(final String name) {

        Validate.notEmpty(name, "Name is empty");

        return this.cacheManager.findUnique(SecurityFamily.class, "SecurityFamily.findByName", QueryType.BY_NAME, new NamedParam("name", name));

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Security getSecurityReferenceTargetByOwnerAndName(long securityId, String name) {

        Validate.notEmpty(name, "Name is empty");

        final SecurityReference ref = this.cacheManager.findUnique(SecurityReference.class, "SecurityReference.findByOwnerAndName", QueryType.BY_NAME, new NamedParam("ownerSecurityId", securityId), new NamedParam("name", name));
        return ref == null ? null : ref.getTarget();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OptionFamily getOptionFamilyByUnderlying(final long underlyingId) {

        return this.cacheManager.findUnique(OptionFamily.class, "OptionFamily.findByUnderlying", QueryType.BY_NAME, new NamedParam("underlyingId", underlyingId));

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FutureFamily getFutureFamilyByUnderlying(final long underlyingId) {

        return this.cacheManager.findUnique(FutureFamily.class, "FutureFamily.findByUnderlying", QueryType.BY_NAME, new NamedParam("underlyingId", underlyingId));

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Exchange> getAllExchanges() {

        return this.cacheManager.getAll(Exchange.class);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Exchange getExchangeByName(String name) {

        return this.cacheManager.findUnique(Exchange.class, "Exchange.findByName", QueryType.BY_NAME, new NamedParam("name", name));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Position> getAllPositions() {

        return this.cacheManager.getAll(Position.class);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Position getPosition(final long id) {

        return this.cacheManager.get(PositionImpl.class, id);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Position getPositionInclSecurityAndSecurityFamily(final long id) {

        return this.cacheManager.findUnique(Position.class, "Position.findByIdInclSecurityAndSecurityFamily", QueryType.BY_NAME, new NamedParam("id", id));

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Position> getPositionsByStrategy(final String strategyName) {

        Validate.notEmpty(strategyName, "Strategy name is empty");

        return this.cacheManager.find(Position.class, "Position.findByStrategy", QueryType.BY_NAME, new NamedParam("strategyName", strategyName));

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Position getPositionBySecurityAndStrategy(final long securityId, final String strategyName) {

        Validate.notEmpty(strategyName, "Strategy name is empty");

        return this.cacheManager.findUnique(Position.class, "Position.findBySecurityAndStrategy", QueryType.BY_NAME, new NamedParam("securityId", securityId), new NamedParam("strategyName", strategyName));

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Position> getOpenPositions() {

        return this.cacheManager.find(Position.class, "Position.findOpenPositions", QueryType.BY_NAME);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Position> getOpenTradeablePositions() {

        return this.cacheManager.find(Position.class, "Position.findOpenTradeablePositions", QueryType.BY_NAME);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Position> getOpenPositionsByStrategy(final String strategyName) {

        Validate.notEmpty(strategyName, "Strategy name is empty");

        return this.cacheManager.find(Position.class, "Position.findOpenPositionsByStrategy", QueryType.BY_NAME, new NamedParam("strategyName", strategyName));

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Position> getOpenTradeablePositionsByStrategy(final String strategyName) {

        Validate.notEmpty(strategyName, "Strategy name is empty");

        return this.cacheManager.find(Position.class, "Position.findOpenTradeablePositionsByStrategy", QueryType.BY_NAME, new NamedParam("strategyName", strategyName));

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Position> getOpenPositionsBySecurity(final long securityId) {

        return this.cacheManager.find(Position.class, "Position.findOpenPositionsBySecurity", QueryType.BY_NAME, new NamedParam("securityId", securityId));

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Position> getOpenPositionsByStrategyAndType(final String strategyName, final Class<? extends Security> type) {

        Validate.notEmpty(strategyName, "Strategy name is empty");
        Validate.notNull(type, "Type is null");

        return this.cacheManager.find(Position.class, "Position.findOpenPositionsByStrategyAndType", QueryType.BY_NAME, new NamedParam("strategyName", strategyName),
                new NamedParam("type", type.getSimpleName()));

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Position> getOpenPositionsByStrategyTypeAndUnderlyingType(final String strategyName, final Class<? extends Security> type, final Class<? extends Security> underlyingType) {

        Validate.notEmpty(strategyName, "Strategy name is empty");
        Validate.notNull(type, "Type is null");
        Validate.notNull(underlyingType, "Underlying type is null");

        return this.cacheManager.find(Position.class, "Position.findOpenPositionsByStrategyTypeAndUnderlyingType", QueryType.BY_NAME,
                new NamedParam("strategyName", strategyName),
                new NamedParam("type", type.getSimpleName()),
                new NamedParam("underlyingType", underlyingType.getSimpleName()));

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Position> getOpenPositionsByStrategyAndSecurityFamily(final String strategyName, final long securityFamilyId) {

        Validate.notEmpty(strategyName, "Strategy name is empty");

        return this.cacheManager.find(Position.class, "Position.findOpenPositionsByStrategyAndSecurityFamily", QueryType.BY_NAME, new NamedParam("strategyName", strategyName), new NamedParam(
                "securityFamilyId", securityFamilyId));

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Position> getOpenFXPositions() {

        return this.cacheManager.find(Position.class, "Position.findOpenFXPositions", QueryType.BY_NAME);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Position> getOpenFXPositionsByStrategy(final String strategyName) {

        Validate.notEmpty(strategyName, "Strategy name is empty");

        return this.cacheManager.find(Position.class, "Position.findOpenFXPositionsByStrategy", QueryType.BY_NAME, new NamedParam("strategyName", strategyName));

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasOpenPosition(long securityId, String strategyName) {

        Position position = getPositionBySecurityAndStrategy(securityId, strategyName);
        return position != null && position.getDirection() != Direction.FLAT;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Transaction getTransaction(final long id) {

        return this.cacheManager.get(TransactionImpl.class, id);
    }

    @Override
    public Transaction getTransactionByExtId(final String extId) {

        Validate.notEmpty(extId, "ExtId is empty");

        return this.cacheManager.findUnique(TransactionImpl.class, "Transaction.findByExtId", QueryType.BY_NAME,
                new NamedParam("extId", extId));
    }

    @Override
    public List<Transaction> getDailyTransactions(int limit) {

        LocalDate today = LocalDate.now();
        return this.cacheManager.find(Transaction.class, "Transaction.findDailyTransactions", limit, QueryType.BY_NAME,
                new NamedParam("curdate", DateTimeLegacy.toLocalDate(today)));
    }

    @Override
    public List<Transaction> getDailyTransactionsByStrategy(String strategyName, int limit) {

        Validate.notEmpty(strategyName, "Strategy name is empty");

        LocalDate today = LocalDate.now();
        return this.cacheManager.find(Transaction.class, "Transaction.findDailyTransactionsByStrategy", limit, QueryType.BY_NAME,
                new NamedParam("strategyName", strategyName), new NamedParam("curdate", DateTimeLegacy.toLocalDate(today)));
    }

    @Override
    public List<Transaction> getTradesByMinDateAndMaxDate(final Date minDate, final Date maxDate) {

        Validate.notNull(minDate, "minDate is null");
        Validate.notNull(maxDate, "maxDate is null");

        return this.cacheManager.find(Transaction.class, "Transaction.findTradesByMinDateAndMaxDate", QueryType.BY_NAME,
                new NamedParam("minDate", minDate), new NamedParam("maxDate", maxDate));
    }

    @Override
    public List<Order> getDailyOrders() {

        LocalDate today = LocalDate.now();
        return this.cacheManager.find(Order.class, "Order.findDailyOrders", QueryType.BY_NAME,
                new NamedParam("curdate", DateTimeLegacy.toLocalDate(today)));
    }

    @Override
    public List<Order> getDailyOrdersByStrategy(String strategyName) {

        Validate.notEmpty(strategyName, "Strategy name is empty");

        LocalDate today = LocalDate.now();
        return this.cacheManager.find(Order.class, "Order.findDailyOrdersByStrategy", QueryType.BY_NAME,
                new NamedParam("strategyName", strategyName), new NamedParam("curdate", DateTimeLegacy.toLocalDate(today)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Account> getAllAccounts() {

        return this.cacheManager.getAll(Account.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Account getAccount(long accountId) {

        return this.cacheManager.get(AccountImpl.class, accountId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Account getAccountByName(final String accountName) {

        Validate.notEmpty(accountName, "Account name is empty");

        return this.cacheManager.findUnique(Account.class, "Account.findByName", QueryType.BY_NAME, new NamedParam("name", accountName));

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<String> getActiveSessionsByOrderServiceType(final String orderServiceType) {

        Validate.notNull(orderServiceType, "Order service type is null");

        return this.genericDao.find(String.class, "Account.findActiveSessionsByOrderServiceType", QueryType.BY_NAME, new NamedParam("orderServiceType", orderServiceType));

    }

    private List<Tick> getTicksByMaxDate(final int limit, final long securityId, final Date maxDate, int intervalDays) {

        Validate.notNull(maxDate, "Max date is null");

        LocalDateTime maxLocalDateTime = DateTimeLegacy.toLocalDateTime(maxDate);
        LocalDateTime minLocalDateTime = maxLocalDateTime.minusDays(intervalDays);
        return this.genericDao.find(Tick.class, "Tick.findTicksBySecurityAndMaxDate", limit, QueryType.BY_NAME,
                new NamedParam("securityId", securityId),
                new NamedParam("minDate", DateTimeLegacy.toLocalDateTime(minLocalDateTime)),
                new NamedParam("maxDate", DateTimeLegacy.toLocalDateTime(maxLocalDateTime)));
    }

    private List<Tick> getTicksByMinDate(final int limit, final long securityId, final Date minDate, int intervalDays) {

        Validate.notNull(minDate, "Min date is null");

        LocalDateTime minLocalDateTime = DateTimeLegacy.toLocalDateTime(minDate);
        LocalDateTime maxLocalDateTime = minLocalDateTime.plusDays(intervalDays);
        return this.genericDao.find(Tick.class, "Tick.findTicksBySecurityAndMinDate", limit, QueryType.BY_NAME,
                new NamedParam("securityId", securityId),
                new NamedParam("minDate", DateTimeLegacy.toLocalDateTime(minLocalDateTime)),
                new NamedParam("maxDate", DateTimeLegacy.toLocalDateTime(maxLocalDateTime)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Tick getLastTick(final long securityId, Date dateTime, int intervalDays) {

        return CollectionUtil.getSingleElementOrNull(getTicksByMaxDate(1, securityId, dateTime, intervalDays));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Tick> getTicksByMaxDate(final long securityId, final Date maxDate, int intervalDays) {

        return getTicksByMaxDate(-1, securityId, maxDate, intervalDays);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Tick> getTicksByMinDate(final long securityId, final Date minDate, int intervalDays) {

        return getTicksByMinDate(-1, securityId, minDate, intervalDays);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Tick> getDailyTicksBeforeTime(final long securityId, final Date time) {

        Validate.notNull(time, "Time is null");

        List<Long> ids = this.genericDao.find(Long.class, "Tick.findDailyTickIdsBeforeTime", QueryType.BY_NAME, new NamedParam("securityId", securityId), new NamedParam("time", time));

        if (ids.size() > 0) {
            return this.cacheManager.find(Tick.class, "Tick.findByIdsInclSecurityAndUnderlying", QueryType.BY_NAME, new NamedParam("ids", ids));
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

        List<Long> ids = this.genericDao.find(Long.class, "Tick.findDailyTickIdsAfterTime", QueryType.BY_NAME, new NamedParam("securityId", securityId), new NamedParam("time", time));

        if (ids.size() > 0) {
            return this.cacheManager.find(Tick.class, "Tick.findByIdsInclSecurityAndUnderlying", QueryType.BY_NAME, new NamedParam("ids", ids));
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

        List<Long> ids = this.genericDao.find(Long.class, "Tick.findHourlyTickIdsBeforeMinutesByMinDate", QueryType.BY_NAME,
                new NamedParam("securityId", securityId),
                new NamedParam("minutes", minutes),
                new NamedParam("minDate", minDate));

        if (ids.size() > 0) {
            return this.cacheManager.find(Tick.class, "Tick.findByIdsInclSecurityAndUnderlying", QueryType.BY_NAME, new NamedParam("ids", ids));
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

        List<Long> ids = this.genericDao.find(Long.class, "Tick.findHourlyTickIdsAfterMinutesByMinDate", QueryType.BY_NAME,
                new NamedParam("securityId", securityId),
                new NamedParam("minutes", minutes),
                new NamedParam("minDate", minDate));

        if (ids.size() > 0) {
            return this.cacheManager.find(Tick.class, "Tick.findByIdsInclSecurityAndUnderlying", QueryType.BY_NAME, new NamedParam("ids", ids));
        } else {
            return new ArrayList<>();
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Tick getTickBySecurityAndMaxDate(final long securityId, final Date maxDate) {

        Validate.notNull(maxDate, "Date is null");

        return this.genericDao.findUnique(Tick.class, "Tick.findBySecurityAndMaxDate", QueryType.BY_NAME, new NamedParam("securityId", securityId), new NamedParam("maxDate", maxDate));

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Bar> getDailyBarsFromTicks(final long securityId, final Date fromDate, final Date toDate) {

        Validate.notNull(fromDate, "From date is null");
        Validate.notNull(toDate, "To date is null");

        return this.genericDao.find(Bar.class, "Bar.findDailyBarsFromTicks", QueryType.BY_NAME, new NamedParam("securityId", securityId), new NamedParam("minDate", fromDate), new NamedParam(
                "maxDate", toDate));

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Bar> getLastNBarsBySecurityAndBarSize(final int n, final long securityId, final Duration barSize) {

        Validate.notNull(barSize, "Bar size is null");

        return this.genericDao.find(Bar.class, "Bar.findBarsBySecurityAndBarSize", n, QueryType.BY_NAME, new NamedParam("securityId", securityId), new NamedParam("barSize", barSize));

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Bar> getBarsBySecurityBarSizeAndMinDate(final long securityId, final Duration barSize, final Date minDate) {

        Validate.notNull(barSize, "Bar size is null");
        Validate.notNull(minDate, "Min date is null");

        return this.genericDao.find(Bar.class, "Bar.findBarsBySecurityBarSizeAndMinDate", QueryType.BY_NAME, new NamedParam("securityId", securityId), new NamedParam("barSize", barSize), new NamedParam("minDate", minDate));

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Forex getForex(final Currency baseCurrency, final Currency transactionCurrency) {

        Validate.notNull(baseCurrency, "Base currency is null");
        Validate.notNull(transactionCurrency, "Transaction currency is null");

        Forex forex = this.cacheManager.findUnique(Forex.class, "Forex.findByBaseAndTransactionCurrency", QueryType.BY_NAME, new NamedParam("baseCurrency", baseCurrency), new NamedParam(
                "transactionCurrency", transactionCurrency));

        if (forex == null) {

            // reverse lookup
            forex = this.cacheManager.findUnique(Forex.class, "Forex.findByBaseAndTransactionCurrency", QueryType.BY_NAME, new NamedParam("baseCurrency", transactionCurrency), new NamedParam(
                    "transactionCurrency", baseCurrency));
        }

        return forex;

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getForexRateByDate(final Currency baseCurrency, final Currency transactionCurrency, final Date date) {

        Validate.notNull(baseCurrency, "Base currency is null");
        Validate.notNull(transactionCurrency, "Transaction currency is null");
        Validate.notNull(date, "Date is null");

        if (baseCurrency.equals(transactionCurrency)) {
            return 1.0;
        }

        Forex forex = getForex(baseCurrency, transactionCurrency);
        if (forex == null) {
            throw new ForexAvailabilityException("Forex does not exist: " + baseCurrency + "." + transactionCurrency);
        }

        List<Tick> ticks = getTicksByMaxDate(forex.getId(), date, 1);
        if (ticks.isEmpty()) {
            throw new ForexAvailabilityException("No exchange rate available for " + baseCurrency + "." + transactionCurrency + " for " + date);
        }

        Tick tick = ticks.get(0);
        if (forex.getBaseCurrency().equals(baseCurrency)) {
            // expected case
            return tick.getCurrentValueDouble();
        } else {
            // reverse case
            return 1.0 / tick.getCurrentValueDouble();
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IntrestRate getInterestRateByCurrencyAndDuration(final Currency currency, final Duration duration) {

        Validate.notNull(currency, "Currency is null");
        Validate.notNull(duration, "Duration is null");

        return this.cacheManager.findUnique(IntrestRate.class, "IntrestRate.findByCurrencyAndDuration", QueryType.BY_NAME, new NamedParam("currency", currency), new NamedParam("duration", duration));

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getInterestRateByCurrencyDurationAndDate(final Currency currency, final Duration duration, final Date date) {

        Validate.notNull(currency, "Currency is null");
        Validate.notNull(duration, "Duration is null");
        Validate.notNull(date, "Date is null");

        IntrestRate intrestRate = getInterestRateByCurrencyAndDuration(currency, duration);

        List<Tick> ticks = getTicksByMaxDate(intrestRate.getId(), date, 1);
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

        return this.genericDao.find(Currency.class, "CashBalance.findHeldCurrencies", QueryType.BY_NAME);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<CashBalance> getCashBalancesByStrategy(final String strategyName) {

        Validate.notNull(strategyName, "Strategy name is null");

        return this.cacheManager.find(CashBalance.class, "CashBalance.findCashBalancesByStrategy", QueryType.BY_NAME, new NamedParam("strategyName", strategyName));

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<Date, Object> getMeasurementsByMaxDate(final String strategyName, final String name, final Date maxDate) {

        Validate.notNull(strategyName, "Strategy name is null");
        Validate.notNull(name, "Name is null");
        Validate.notNull(maxDate, "Max date is null");

        List<Measurement> measurements = this.genericDao.find(Measurement.class, "Measurement.findMeasurementsByMaxDate", QueryType.BY_NAME,
                new NamedParam("strategyName", strategyName),
                new NamedParam("name", name),
                new NamedParam("maxDateTime", maxDate));

        return getValuesByDate(measurements);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<Date, Map<String, Object>> getAllMeasurementsByMaxDate(final String strategyName, final Date maxDate) {

        Validate.notNull(strategyName, "Strategy name is null");
        Validate.notNull(maxDate, "Max date is null");

        List<Measurement> measurements = this.genericDao.find(Measurement.class, "Measurement.findAllMeasurementsByMaxDate", QueryType.BY_NAME,
                new NamedParam("strategyName", strategyName),
                new NamedParam("maxDateTime", maxDate));

        return getNameValuePairsByDate(measurements);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<Date, Object> getMeasurementsByMinDate(final String strategyName, final String name, final Date minDate) {

        Validate.notNull(strategyName, "Strategy name is null");
        Validate.notNull(name, "Name is null");
        Validate.notNull(minDate, "Min date is null");

        List<Measurement> measurements = this.genericDao.find(Measurement.class, "Measurement.findMeasurementsByMinDate", QueryType.BY_NAME,
                new NamedParam("strategyName", strategyName),
                new NamedParam("name", name),
                new NamedParam("minDateTime", minDate));

        return getValuesByDate(measurements);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<Date, Map<String, Object>> getAllMeasurementsByMinDate(final String strategyName, final Date minDate) {

        Validate.notNull(strategyName, "Strategy name is null");
        Validate.notNull(minDate, "Min date is null");

        List<Measurement> measurements = this.genericDao.find(Measurement.class, "Measurement.findAllMeasurementsByMinDate", QueryType.BY_NAME,
                new NamedParam("strategyName", strategyName),
                new NamedParam("minDateTime", minDate));

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

        Measurement measurement = CollectionUtil.getSingleElementOrNull(this.genericDao.find(Measurement.class, "Measurement.findMeasurementsByMaxDate", 1, QueryType.BY_NAME, new NamedParam("strategyName", strategyName),
                new NamedParam("name", name),
                new NamedParam("maxDateTime",maxDate)));

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

        Measurement measurement = CollectionUtil.getSingleElementOrNull(this.genericDao.find(Measurement.class, "Measurement.findMeasurementsByMinDate", 1, QueryType.BY_NAME, new NamedParam("strategyName", strategyName),
                new NamedParam("name", name),
                new NamedParam("minDateTime", minDate)));

        return measurement != null ? measurement.getValue() : null;

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<EasyToBorrow> getEasyToBorrowByDateAndBroker(final Date date, final String broker) {

        Validate.notNull(date, "Date is null");
        Validate.notNull(broker, "Broker is null");

        Date truncatedDate = DateUtils.truncate(date, Calendar.DATE);
        return this.cacheManager.find(EasyToBorrow.class, "EasyToBorrow.findByDateAndBroker", QueryType.BY_NAME, new NamedParam("date", truncatedDate), new NamedParam("broker", broker));

    }

    @Override
    public Date getCurrentDBTime() {

        return this.genericDao.findUnique(Date.class, "Strategy.findCurrentDBTime", QueryType.BY_NAME);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> List<T> find(final Class<T> clazz, final String query, QueryType type, boolean useCache, final NamedParam... namedParams) {

        Validate.notEmpty(query, "Query is empty");
        Validate.notNull(namedParams, "Named parameters is null");

        if (useCache) {
            return this.cacheManager.find(clazz, query, type, namedParams);
        } else {
            return this.genericDao.find(clazz, query, type, namedParams);
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> List<T> find(final Class<T> clazz, final String query, final int maxResults, QueryType type, boolean useCache, final NamedParam... namedParams) {

        Validate.notEmpty(query, "Query is empty");
        Validate.notNull(namedParams, "Named parameters is null");

        if (useCache) {
            return this.cacheManager.find(clazz, query, maxResults, type, namedParams);
        } else {
            return this.genericDao.find(clazz, query, maxResults, type, namedParams);
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T findUnique(final Class<T> clazz, final String query, QueryType type, boolean useCache, final NamedParam... namedParams) {

        Validate.notEmpty(query, "Query is empty");
        Validate.notNull(namedParams, "Named parameters is null");

        if (useCache) {
            return this.cacheManager.findUnique(clazz, query, type, namedParams);
        } else {
            return this.genericDao.findUnique(clazz, query, type, namedParams);
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
