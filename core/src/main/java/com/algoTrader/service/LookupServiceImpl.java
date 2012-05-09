package com.algoTrader.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.apache.commons.collections15.CollectionUtils;
import org.apache.commons.collections15.Predicate;
import org.springframework.beans.factory.annotation.Value;

import com.algoTrader.entity.Position;
import com.algoTrader.entity.PositionDao;
import com.algoTrader.entity.Strategy;
import com.algoTrader.entity.StrategyImpl;
import com.algoTrader.entity.Subscription;
import com.algoTrader.entity.Transaction;
import com.algoTrader.entity.TransactionDao;
import com.algoTrader.entity.marketData.Bar;
import com.algoTrader.entity.marketData.Tick;
import com.algoTrader.entity.marketData.TickDao;
import com.algoTrader.entity.security.Combination;
import com.algoTrader.entity.security.Component;
import com.algoTrader.entity.security.Future;
import com.algoTrader.entity.security.FutureFamily;
import com.algoTrader.entity.security.Security;
import com.algoTrader.entity.security.SecurityFamily;
import com.algoTrader.entity.security.StockOption;
import com.algoTrader.entity.security.StockOptionFamily;
import com.algoTrader.entity.strategy.CashBalance;
import com.algoTrader.entity.strategy.CashBalanceDao;
import com.algoTrader.entity.strategy.Measurement;
import com.algoTrader.enumeration.Currency;
import com.algoTrader.enumeration.OptionType;
import com.algoTrader.util.DateUtil;
import com.algoTrader.util.HibernateUtil;
import com.algoTrader.util.metric.MetricsUtil;
import com.algoTrader.vo.PortfolioValueVO;
import com.algoTrader.vo.PositionVO;
import com.algoTrader.vo.RawBarVO;
import com.algoTrader.vo.RawTickVO;
import com.algoTrader.vo.TransactionVO;

@SuppressWarnings("unchecked")
public class LookupServiceImpl extends LookupServiceBase {

    private @Value("${simulation}") boolean simulation;
    private @Value("${statement.simulateStockOptions}") boolean simulateStockOptions;
    private @Value("${statement.simulateFuturesByUnderlying}") boolean simulateFuturesByUnderlying;
    private @Value("${statement.simulateFuturesByGenericFutures}") boolean simulateFuturesByGenericFutures;
    private @Value("${misc.transactionDisplayCount}") int transactionDisplayCount;

    @Override
    protected Collection<Security> handleGetAllSecurities() throws Exception {

        return getSecurityDao().loadAll();
    }

    @Override
    protected Security handleGetSecurity(int id) throws java.lang.Exception {

        return getSecurityDao().get(id);
    }

    @Override
    protected Security handleGetSecurityByIsin(String isin) throws Exception {

        return getSecurityDao().findByIsin(isin);
    }

    @Override
    protected Security handleGetSecurityInclFamilyAndUnderlying(int securityId) throws Exception {

        return getSecurityDao().findByIdInclFamilyAndUnderlying(securityId);
    }

    @Override
    protected Security handleGetSecurityInitialized(int id) throws java.lang.Exception {

        Security security = getSecurityDao().get(id);

        // initialize the security
        security.initialize();

        return security;
    }

    @Override
    protected Security handleGetSecurityInclComponentsInitialized(int id) throws java.lang.Exception {

        Security security = getSecurityDao().get(id);

        // initialize the security
        security.initialize();

        // initialize components
        security.getComponentsInitialized();

        return security;
    }

    @Override
    protected List<Security> handleGetSecuritiesByIds(Collection<Integer> ids) throws Exception {

        return getSecurityDao().findByIds(ids);
    }

    @Override
    protected List<Security> handleGetSubscribedSecuritiesByStrategyAndComponent(String strategyName, int securityId) throws Exception {

        return getSecurityDao().findSubscribedByStrategyAndComponent(strategyName, securityId);
    }

    @SuppressWarnings("rawtypes")
    @Override
    protected List<Security> handleGetSubscribedSecuritiesByStrategyAndComponentClass(String strategyName, final Class type) throws Exception {

        int discriminator = HibernateUtil.getDisriminatorValue(getSessionFactory(), type);
        return getSecurityDao().findSubscribedByStrategyAndComponentClass(strategyName, discriminator);
    }

    @Override
    protected List<Security> handleGetSubscribedSecuritiesForAutoActivateStrategiesInclFamily() throws Exception {

        return getSecurityDao().findSubscribedForAutoActivateStrategiesInclFamily();
    }

    @Override
    protected StockOption handleGetStockOptionByMinExpirationAndMinStrikeDistance(int underlyingId, Date targetExpirationDate, BigDecimal underlyingSpot,
            OptionType optionType) throws Exception {

        StockOptionFamily family = getStockOptionFamilyDao().findByUnderlying(underlyingId);

        List<StockOption> list = getStockOptionDao().findByMinExpirationAndStrikeLimit(0, 1, underlyingId, targetExpirationDate, underlyingSpot.doubleValue(), optionType);

        StockOption stockOption = null;
        if (!list.isEmpty()) {
            stockOption = list.get(0);
        }

        // if no future was found, create it if simulating options
        if (this.simulation && this.simulateStockOptions) {
            if ((stockOption == null) || Math.abs(stockOption.getStrike().doubleValue() - underlyingSpot.doubleValue()) > family.getStrikeDistance()) {

                stockOption = getStockOptionService().createDummyStockOption(family.getId(), targetExpirationDate, underlyingSpot, optionType);
            }
        }

        if (stockOption == null) {
            throw new LookupServiceException("no stockOption available for expiration " + targetExpirationDate + " strike " + underlyingSpot + " type " + optionType);
        } else {
            return stockOption;
        }
    }

    @Override
    protected StockOption handleGetStockOptionByMinExpirationAndMinStrikeDistanceWithTicks(int underlyingId, Date targetExpirationDate,
            BigDecimal underlyingSpot, OptionType optionType, Date date) throws Exception {

        List<StockOption> list = getStockOptionDao().findByMinExpirationAndMinStrikeDistanceWithTicks(0, 1, underlyingId, targetExpirationDate, underlyingSpot.doubleValue(), optionType, date);

        if (!list.isEmpty()) {
            return list.get(0);
        } else {
            return null;
        }
    }

    @Override
    protected StockOption handleGetStockOptionByMinExpirationAndStrikeLimit(int underlyingId, Date targetExpirationDate, BigDecimal underlyingSpot,
            OptionType optionType)
            throws Exception {

        StockOptionFamily family = getStockOptionFamilyDao().findByUnderlying(underlyingId);

        List<StockOption> list = getStockOptionDao().findByMinExpirationAndStrikeLimit(0, 1, underlyingId, targetExpirationDate, underlyingSpot.doubleValue(), optionType);

        StockOption stockOption = null;
        if (!list.isEmpty()) {
            stockOption = list.get(0);
        }

        // if no future was found, create it if simulating options
        if (this.simulation && this.simulateStockOptions) {
            if ((stockOption == null) || Math.abs(stockOption.getStrike().doubleValue() - underlyingSpot.doubleValue()) > family.getStrikeDistance()) {

                stockOption = getStockOptionService().createDummyStockOption(family.getId(), targetExpirationDate, underlyingSpot, optionType);
            }
        }

        if (stockOption == null) {
            throw new LookupServiceException("no stockOption available for expiration " + targetExpirationDate + " strike " + underlyingSpot + " type "
                    + optionType);
        } else {
            return stockOption;
        }
    }

    @Override
    protected StockOption handleGetStockOptionByMinExpirationAndStrikeLimitWithTicks(int underlyingId, Date targetExpirationDate, BigDecimal underlyingSpot,
            OptionType optionType,
            Date date) throws Exception {

        List<StockOption> list = getStockOptionDao().findByMinExpirationAndStrikeLimitWithTicks(0, 1, underlyingId, targetExpirationDate, underlyingSpot.doubleValue(), optionType, date);

        if (!list.isEmpty()) {
            return list.get(0);
        } else {
            return null;
        }
    }


    @Override
    protected List<StockOption> handleGetSubscribedStockOptions() throws Exception {

        return getStockOptionDao().findSubscribedStockOptions();
    }

    @Override
    protected Future handleGetFutureByMinExpiration(int futureFamilyId, Date expirationDate) throws Exception {

        List<Future> list = getFutureDao().findByMinExpiration(0, 1, futureFamilyId, expirationDate);

        Future future = null;
        if (!list.isEmpty()) {
            future = list.get(0);
        }

        // if no future was found, create the missing part of the future-chain
        if (this.simulation && future == null && (this.simulateFuturesByUnderlying || this.simulateFuturesByGenericFutures)) {

            getFutureService().createDummyFutures(futureFamilyId);

            list = getFutureDao().findByMinExpiration(0, 1, futureFamilyId, expirationDate);

            if (!list.isEmpty()) {
                future = list.get(0);
            }
        }

        if (future == null) {
            throw new LookupServiceException("no future available for expiration " + expirationDate);
        } else {
            return future;
        }

    }

    @Override
    protected Future handleGetFutureByExpiration(int futureFamilyId, Date expirationDate) throws Exception {

        Future future = getFutureDao().findByExpiration(futureFamilyId, expirationDate);

        // if no future was found, create the missing part of the future-chain
        if (this.simulation && future == null && (this.simulateFuturesByUnderlying || this.simulateFuturesByGenericFutures)) {

            getFutureService().createDummyFutures(futureFamilyId);
            future = getFutureDao().findByExpiration(futureFamilyId, expirationDate);
        }

        if (future == null) {
            throw new LookupServiceException("no future available targetExpiration " + expirationDate);
        } else {
            return future;
        }
    }

    @Override
    protected Future handleGetFutureByDuration(int futureFamilyId, Date targetExpirationDate, int duration) throws Exception {

        FutureFamily futureFamily = getFutureFamilyDao().get(futureFamilyId);

        Date expirationDate = DateUtil.getExpirationDateNMonths(futureFamily.getExpirationType(), targetExpirationDate, duration);
        Future future = getFutureDao().findByExpiration(futureFamilyId, expirationDate);

        // if no future was found, create the missing part of the future-chain
        if (this.simulation && future == null && (this.simulateFuturesByUnderlying || this.simulateFuturesByGenericFutures)) {

            getFutureService().createDummyFutures(futureFamily.getId());
            future = getFutureDao().findByExpiration(futureFamilyId, expirationDate);
        }

        if (future == null) {
            throw new LookupServiceException("no future available targetExpiration " + targetExpirationDate + " and duration " + duration);
        } else {
            return future;
        }
    }

    @Override
    protected List<Future> handleGetFuturesByMinExpiration(int futureFamilyId, Date minExpirationDate) throws Exception {

        return getFutureDao().findFuturesByMinExpiration(futureFamilyId, minExpirationDate);
    }

    @Override
    protected List<Future> handleGetSubscribedFutures() throws Exception {

        return getFutureDao().findSubscribedFutures();
    }

    @Override
    protected Subscription handleGetSubscription(String strategyName, int securityId) throws Exception {

        return getSubscriptionDao().findByStrategyAndSecurity(strategyName, securityId);
    }

    @Override
    protected List<Subscription> handleGetSubscriptionsByStrategyInclComponents(String strategyName) throws Exception {

        List<Subscription> subscriptions = getSubscriptionDao().findByStrategy(strategyName);

        // even when fetching components by the query they are sometimes not initialized, so initializing them manally
        for (Subscription subscription : subscriptions) {
            subscription.getSecurity().getComponentsInitialized();
        }

        return subscriptions;
    }

    @Override
    protected List<Subscription> handleGetSubscriptionsForAutoActivateStrategiesInclComponents() throws Exception {

        List<Subscription> subscriptions = getSubscriptionDao().findForAutoActivateStrategies();

        // even when fetching components by the query they are sometimes not initialized, so initializing them manally
        for (Subscription subscription : subscriptions) {
            subscription.getSecurity().getComponentsInitialized();
        }

        return subscriptions;
    }

    @Override
    protected List<Subscription> handleGetNonPositionSubscriptions(String strategyName) throws Exception {

        return getSubscriptionDao().findNonPositionSubscriptions(strategyName);
    }

    @Override
    protected Collection<Strategy> handleGetAllStrategies() throws Exception {

        return getStrategyDao().loadAll();
    }

    @Override
    protected Strategy handleGetStrategy(int id) throws java.lang.Exception {

        return getStrategyDao().get(id);
    }

    @Override
    protected Strategy handleGetStrategyByName(String name) throws Exception {

        return getStrategyDao().findByName(name);
    }

    @Override
    protected List<Strategy> handleGetAutoActivateStrategies() throws Exception {

        return getStrategyDao().findAutoActivateStrategies();
    }

    @Override
    protected SecurityFamily handleGetSecurityFamily(int id) throws Exception {

        return getSecurityFamilyDao().get(id);
    }

    @Override
    protected StockOptionFamily handleGetStockOptionFamilyByUnderlying(int id) throws Exception {

        return getStockOptionFamilyDao().findByUnderlying(id);
    }

    @Override
    protected FutureFamily handleGetFutureFamilyByUnderlying(int id) throws Exception {

        return getFutureFamilyDao().findByUnderlying(id);
    }

    @Override
    protected Collection<Position> handleGetAllPositions() throws Exception {

        return getPositionDao().loadAll();
    }

    @Override
    protected Position handleGetPosition(int id) throws java.lang.Exception {

        return getPositionDao().get(id);
    }

    @Override
    protected List<Position> handleGetPositionsByStrategy(String strategyName) throws Exception {

        return getPositionDao().findByStrategy(strategyName);
    }

    @Override
    protected Position handleGetPositionBySecurityAndStrategy(int securityId, String strategyName) throws Exception {

        return getPositionDao().findBySecurityAndStrategy(securityId, strategyName);
    }

    @Override
    protected List<Position> handleGetOpenPositions() throws Exception {

        return getPositionDao().findOpenPositions();
    }

    @Override
    protected List<Position> handleGetOpenTradeablePositions() throws Exception {

        return getPositionDao().findOpenTradeablePositions();
    }

    @Override
    protected List<Position> handleGetOpenPositionsByStrategy(String strategyName) throws Exception {

        return getPositionDao().findOpenPositionsByStrategy(strategyName);
    }

    @Override
    protected List<Position> handleGetOpenTradeablePositionsByStrategy(String strategyName) throws Exception {

        return getPositionDao().findOpenTradeablePositionsByStrategy(strategyName);
    }

    @Override
    protected List<Position> handleGetOpenPositionsBySecurityId(int securityId) throws Exception {

        return getPositionDao().findOpenPositionsBySecurityId(securityId);
    }

    @SuppressWarnings("rawtypes")
    @Override
    protected List<Position> handleGetOpenPositionsByStrategyAndType(String strategyName, final Class type) throws Exception {

        List<Position> positions = getPositionDao().findOpenPositionsByStrategy(strategyName);

        return new ArrayList(CollectionUtils.select(positions, new Predicate<Position>() {
            @Override
            public boolean evaluate(Position position) {
                return type.isAssignableFrom(position.getSecurity().getClass());
            }
        }));
    }

    @Override
    protected List<Position> handleGetOpenFXPositions() throws Exception {

        return getPositionDao().findOpenFXPositions();
    }

    @Override
    protected List<Position> handleGetOpenFXPositionsByStrategy(String strategyName) throws Exception {

        return getPositionDao().findOpenFXPositionsByStrategy(strategyName);
    }

    @Override
    protected List<PositionVO> handleGetOpenPositionsVO(String strategyName) throws Exception {

        if (strategyName.equals(StrategyImpl.BASE)) {
            return (List<PositionVO>) getPositionDao().findOpenPositions(PositionDao.TRANSFORM_POSITIONVO);
        } else {
            return (List<PositionVO>) getPositionDao().findOpenPositionsByStrategy(PositionDao.TRANSFORM_POSITIONVO, strategyName);
        }
    }

    @Override
    protected Collection<Transaction> handleGetAllTransactions() throws Exception {

        return getTransactionDao().loadAll();
    }

    @Override
    protected List<Transaction> handleGetAllTrades() throws Exception {

        return getTransactionDao().findAllTradesInclSecurity();
    }

    @Override
    protected List<Transaction> handleGetAllCashFlows() throws Exception {

        return getTransactionDao().findAllCashflows();
    }

    @Override
    protected Transaction handleGetTransaction(int id) throws java.lang.Exception {

        return getTransactionDao().get(id);
    }

    @Override
    protected List<TransactionVO> handleGetTransactionsVO(String strategyName) throws Exception {

        if (strategyName.equals(StrategyImpl.BASE)) {
            return (List<TransactionVO>) getTransactionDao().findTransactionsDesc(TransactionDao.TRANSFORM_TRANSACTIONVO, 0, this.transactionDisplayCount);
        } else {
            return (List<TransactionVO>) getTransactionDao().findTransactionsByStrategyDesc(TransactionDao.TRANSFORM_TRANSACTIONVO, 0, this.transactionDisplayCount, strategyName);
        }
    }

    @Override
    protected Tick handleGetLastTick(int securityId) throws Exception {

        List<Tick> list = getTickDao().findTicksForSecurityAndMaxDate(0, 1, securityId, DateUtil.getCurrentEPTime());

        Tick tick = null;
        if (!list.isEmpty()) {
            tick = list.get(0);
            tick.getSecurity().initialize();
        }

        return tick;
    }

    @Override
    protected List<Tick> handleGetDailyTicksBeforeTime(int securityId, Date maxDate, Date time) {

        List<Integer> ids = (List<Integer>) getTickDao().findDailyTickIdsBeforeTime(TickDao.TRANSFORM_NONE, securityId, maxDate, time);
        if (ids.size() > 0) {
            return getTickDao().findByIdsInclSecurityAndUnderlying(ids);
        } else {
            return new ArrayList<Tick>();
        }
    }

    @Override
    protected List<Tick> handleGetDailyTicksAfterTime(int securityId, Date maxDate, Date time) {

        List<Integer> ids = (List<Integer>) getTickDao().findDailyTickIdsAfterTime(TickDao.TRANSFORM_NONE, securityId, maxDate, time);
        if (ids.size() > 0) {
            return getTickDao().findByIdsInclSecurityAndUnderlying(ids);
        } else {
            return new ArrayList<Tick>();
        }
    }

    @Override
    protected List<Tick> handleGetSubscribedTicksByTimePeriod(Date startDate, Date endDate) throws Exception {

        return getTickDao().findSubscribedByTimePeriod(startDate, endDate);
    }

    @Override
    protected Tick handleGetTickByDateAndSecurityInclSecurityInitialized(Date date, int securityId) {

        Tick tick = getTickDao().findByDateAndSecurity(date, securityId);

        // initialize the security
        tick.getSecurity().initialize();

        return tick;
    }

    @Override
    protected PortfolioValueVO handleGetPortfolioValue() throws Exception {

        double cashBalance = getPortfolioService().getCashBalanceDouble();
        double securitiesCurrentValue = getPortfolioService().getSecuritiesCurrentValueDouble();
        double maintenanceMargin = getPortfolioService().getMaintenanceMarginDouble();
        double leverage = getPortfolioService().getLeverage();

        PortfolioValueVO portfolioValueVO = new PortfolioValueVO();
        portfolioValueVO.setCashBalance(cashBalance);
        portfolioValueVO.setSecuritiesCurrentValue(securitiesCurrentValue);
        portfolioValueVO.setMaintenanceMargin(maintenanceMargin);
        portfolioValueVO.setNetLiqValue(cashBalance + securitiesCurrentValue);
        portfolioValueVO.setLeverage(leverage);

        return portfolioValueVO;
    }

    @Override
    protected double handleGetForexRateDouble(Currency baseCurrency, Currency transactionCurrency) throws Exception {

        return getForexDao().getRateDouble(baseCurrency, transactionCurrency);
    }

    @Override
    protected List<Currency> handleGetHeldCurrencies() throws Exception {

        return (List<Currency>) getCashBalanceDao().findHeldCurrencies(CashBalanceDao.TRANSFORM_NONE);
    }

    @Override
    protected List<CashBalance> handleGetCashBalancesByStrategy(String strategyName) throws Exception {

        return getCashBalanceDao().findCashBalancesByStrategy(strategyName);
    }

    @Override
    protected Collection<Combination> handleGetAllCombinations() throws Exception {

        return getCombinationDao().loadAll();
    }

    @Override
    protected Collection<Combination> handleGetCombinationByStrategyAndUnderlying(String strategyName, int underlyingId) throws Exception {

        return getCombinationDao().findSubscribedByStrategyAndUnderlying(strategyName, underlyingId);
    }

    @Override
    protected List<Component> handleGetSubscribedComponentsByStrategy(String strategyName) throws Exception {

        return getComponentDao().findSubscribedByStrategyInclSecurity(strategyName);
    }

    @Override
    protected Collection<Component> handleGetSubscribedComponentsBySecurity(int securityId) throws Exception {

        return getComponentDao().findSubscribedBySecurityInclSecurity(securityId);
    }

    @Override
    protected List<Component> handleGetSubscribedComponentsByStrategyAndSecurity(String strategyName, int securityId) throws Exception {

        return getComponentDao().findSubscribedByStrategyAndSecurityInclSecurity(strategyName, securityId);
    }

    @Override
    protected List<Measurement> handleGetMeasurementsAfterDate(String strategyName, String type, Date date) throws Exception {

        return getMeasurementDao().findMeasurementsAfterDate(strategyName, type, date);
    }

    @Override
    protected Measurement handleGetMeasurementForMaxDate(String strategyName, String type, Date maxDate) throws Exception {

        List<Measurement> list = getMeasurementDao().findMeasurementsBeforeDate(0, 1, strategyName, type, maxDate);

        if (!list.isEmpty()) {
            return list.get(0);
        } else {
            return null;
        }
    }

    @Override
    protected Tick handleGetTickFromRawTick(RawTickVO rawTick) {

        long beforeCompleteRawTick = System.nanoTime();
        Tick tick = getTickDao().rawTickVOToEntity(rawTick);
        long afterCompleteRawTick = System.nanoTime();

        MetricsUtil.account("LookupService.getTickFromRawTick", (afterCompleteRawTick - beforeCompleteRawTick));

        return tick;
    }

    @Override
    protected Bar handleGetBarFromRawBar(RawBarVO barVO) {

        return getBarDao().rawBarVOToEntity(barVO);
    }
}
