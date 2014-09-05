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

import java.io.IOException;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections15.CollectionUtils;
import org.apache.commons.collections15.Predicate;
import org.apache.commons.lang.Validate;
import org.apache.commons.lang.time.DateUtils;
import org.apache.log4j.Logger;
import org.hibernate.SessionFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import ch.algotrader.ServiceLocator;
import ch.algotrader.config.CommonConfig;
import ch.algotrader.config.CoreConfig;
import ch.algotrader.entity.Subscription;
import ch.algotrader.entity.SubscriptionDao;
import ch.algotrader.entity.marketData.MarketDataEvent;
import ch.algotrader.entity.marketData.Tick;
import ch.algotrader.entity.marketData.TickDao;
import ch.algotrader.entity.security.Security;
import ch.algotrader.entity.security.SecurityDao;
import ch.algotrader.entity.strategy.Strategy;
import ch.algotrader.entity.strategy.StrategyDao;
import ch.algotrader.enumeration.FeedType;
import ch.algotrader.esper.EngineLocator;
import ch.algotrader.util.HibernateUtil;
import ch.algotrader.util.MyLogger;
import ch.algotrader.util.io.CsvTickWriter;
import ch.algotrader.util.metric.MetricsUtil;
import ch.algotrader.util.spring.HibernateSession;
import ch.algotrader.vo.GenericEventVO;

import com.espertech.esper.collection.Pair;

/**
 * @author <a href="mailto:aflury@algotrader.ch">Andy Flury</a>
 *
 * @version $Revision$ $Date$
 */
@HibernateSession
public class MarketDataServiceImpl implements MarketDataService, ApplicationContextAware {

    private static Logger logger = MyLogger.getLogger(MarketDataServiceImpl.class.getName());

    private Map<Security, CsvTickWriter> csvWriters = new HashMap<Security, CsvTickWriter>();

    private ApplicationContext applicationContext;

    private final CommonConfig commonConfig;

    private final CoreConfig coreConfig;

    private final SessionFactory sessionFactory;

    private final TickDao tickDao;

    private final SecurityDao securityDao;

    private final StrategyDao strategyDao;

    private final SubscriptionDao subscriptionDao;

    public MarketDataServiceImpl(final CommonConfig commonConfig,
            final CoreConfig coreConfig,
            final SessionFactory sessionFactory,
            final TickDao tickDao,
            final SecurityDao securityDao,
            final StrategyDao strategyDao,
            final SubscriptionDao subscriptionDao) {

        Validate.notNull(commonConfig, "CommonConfig is null");
        Validate.notNull(coreConfig, "CoreConfig is null");
        Validate.notNull(sessionFactory, "SessionFactory is null");
        Validate.notNull(tickDao, "TickDao is null");
        Validate.notNull(securityDao, "SecurityDao is null");
        Validate.notNull(strategyDao, "StrategyDao is null");
        Validate.notNull(subscriptionDao, "SubscriptionDao is null");

        this.commonConfig = commonConfig;
        this.coreConfig = coreConfig;
        this.sessionFactory = sessionFactory;
        this.tickDao = tickDao;
        this.securityDao = securityDao;
        this.strategyDao = strategyDao;
        this.subscriptionDao = subscriptionDao;

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void persistTick(final Tick tick) {

        Validate.notNull(tick, "Tick is null");

        try {
            // get the current Date rounded to MINUTES
            Date date = DateUtils.round(new Date(), Calendar.MINUTE);
            tick.setDateTime(date);

            saveCvs(tick);

            // write the tick to the DB (even if not valid)
            this.tickDao.create(tick);
        } catch (Exception ex) {
            throw new MarketDataServiceException(ex.getMessage(), ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initSubscriptions(final FeedType feedType) {

        Validate.notNull(feedType, "Feed type is null");

        try {
            getExternalMarketDataService(feedType).initSubscriptions();
        } catch (Exception ex) {
            throw new MarketDataServiceException(ex.getMessage(), ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void subscribe(final String strategyName, final int securityId) {

        Validate.notEmpty(strategyName, "Strategy name is empty");

        try {
            subscribe(strategyName, securityId, this.coreConfig.getDefaultFeedType());
        } catch (Exception ex) {
            throw new MarketDataServiceException(ex.getMessage(), ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void subscribe(final String strategyName, final int securityId, final FeedType feedType) {

        Validate.notEmpty(strategyName, "Strategy name is empty");
        Validate.notNull(feedType, "Feed type is null");

        try {
            if (this.subscriptionDao.findByStrategySecurityAndFeedType(strategyName, securityId, feedType) == null) {

                Strategy strategy = this.strategyDao.findByName(strategyName);
                Security security = this.securityDao.findByIdInclFamilyAndUnderlying(securityId);

                // only external subscribe if nobody was watching the specified security with the specified feedType so far
                if (!this.commonConfig.isSimulation()) {
                    List<Subscription> subscriptions = this.subscriptionDao.findBySecurityAndFeedTypeForAutoActivateStrategies(securityId, feedType);
                    if (subscriptions.size() == 0) {
                        if (!security.getSecurityFamily().isSynthetic()) {
                            getExternalMarketDataService(feedType).subscribe(security);
                        }
                    }
                }

                // update links
                Subscription subscription = Subscription.Factory.newInstance(feedType, false, strategy, security);

                this.subscriptionDao.create(subscription);

                // reverse-associate security (after subscription has received an id)
                security.getSubscriptions().add(subscription);

                logger.info("subscribed security " + security + " with " + feedType);
            }
        } catch (Exception ex) {
            throw new MarketDataServiceException(ex.getMessage(), ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void unsubscribe(final String strategyName, final int securityId) {

        Validate.notEmpty(strategyName, "Strategy name is empty");

        try {
            unsubscribe(strategyName, securityId, this.coreConfig.getDefaultFeedType());
        } catch (Exception ex) {
            throw new MarketDataServiceException(ex.getMessage(), ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void unsubscribe(final String strategyName, final int securityId, final FeedType feedType) {

        Validate.notEmpty(strategyName, "Strategy name is empty");
        Validate.notNull(feedType, "Feed type is null");

        try {
            Subscription subscription = this.subscriptionDao.findByStrategySecurityAndFeedType(strategyName, securityId, feedType);
            if (subscription != null && !subscription.isPersistent()) {

                Security security = this.securityDao.get(securityId);

                // update links
                security.getSubscriptions().remove(subscription);

                this.subscriptionDao.remove(subscription);

                // only external unsubscribe if nobody is watching this security anymore
                if (!this.commonConfig.isSimulation()) {
                    if (security.getSubscriptions().size() == 0) {
                        if (!security.getSecurityFamily().isSynthetic()) {
                            getExternalMarketDataService(feedType).unsubscribe(security);
                        }
                    }
                }

                logger.info("unsubscribed security " + security + " with " + feedType);
            }
        } catch (Exception ex) {
            throw new MarketDataServiceException(ex.getMessage(), ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void removeNonPositionSubscriptions(final String strategyName) {

        Validate.notEmpty(strategyName, "Strategy name is empty");

        try {
            Collection<Subscription> subscriptions = this.subscriptionDao.findNonPositionSubscriptions(strategyName);

            for (Subscription subscription : subscriptions) {
                unsubscribe(subscription.getStrategy().getName(), subscription.getSecurity().getId());
            }
        } catch (Exception ex) {
            throw new MarketDataServiceException(ex.getMessage(), ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void removeNonPositionSubscriptionsByType(final String strategyName, final Class type) {

        Validate.notEmpty(strategyName, "Strategy name is empty");
        Validate.notNull(type, "Type is null");

        try {
            int discriminator = HibernateUtil.getDisriminatorValue(this.sessionFactory, type);
            Collection<Subscription> subscriptions = this.subscriptionDao.findNonPositionSubscriptionsByType(strategyName, discriminator);

            for (Subscription subscription : subscriptions) {
                unsubscribe(subscription.getStrategy().getName(), subscription.getSecurity().getId());
            }
        } catch (Exception ex) {
            throw new MarketDataServiceException(ex.getMessage(), ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void requestCurrentTicks(final String strategyName) {

        Validate.notEmpty(strategyName, "Strategy name is empty");

        try {
            Collection<Tick> ticks = this.tickDao.findCurrentTicksByStrategy(strategyName);

            for (Tick tick : ticks) {
                EngineLocator.instance().sendEvent(strategyName, tick);
            }
        } catch (Exception ex) {
            throw new MarketDataServiceException(ex.getMessage(), ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void logTickGap(final int securityId) {

        try {
            Security security = this.securityDao.get(securityId);

            logger.error(security + " has not received any ticks for " + security.getSecurityFamily().getMaxGap() + " minutes");
        } catch (Exception ex) {
            throw new MarketDataServiceException(ex.getMessage(), ex);
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {

        this.applicationContext = applicationContext;
    }

    private void saveCvs(Tick tick) throws IOException {

        Security security = tick.getSecurity();

        CsvTickWriter csvWriter;
        synchronized (this.csvWriters) {
            csvWriter = this.csvWriters.get(security);
            if (csvWriter == null) {
                String fileName = security.getIsin() != null ? security.getIsin() : security.getSymbol() != null ? security.getSymbol() : String.valueOf(security.getId());
                csvWriter = new CsvTickWriter(fileName);
                this.csvWriters.put(security, csvWriter);
            }
        }

        synchronized (csvWriter) {
            csvWriter.write(tick);
        }
    }

    /**
     * get the externalMarketDataService defined by MarketDataServiceType
     */
    @SuppressWarnings({ "unchecked" })
    private ExternalMarketDataService getExternalMarketDataService(final FeedType feedType) throws Exception {

        Validate.notNull(feedType, "feedType must not be null");

        Class<ExternalMarketDataService> marketDataServiceClass = (Class<ExternalMarketDataService>) Class.forName(feedType.getValue());

        Map<String, ExternalMarketDataService> externalMarketDataServices = this.applicationContext.getBeansOfType(marketDataServiceClass);

        // select the proxy
        String name = CollectionUtils.find(externalMarketDataServices.keySet(), new Predicate<String>() {
            @Override
            public boolean evaluate(String name) {
                return !name.startsWith("ch.algotrader.service");
            }
        });

        ExternalMarketDataService externalMarketDataService = externalMarketDataServices.get(name);

        Validate.notNull(externalMarketDataService, "externalMarketDataService was not found: " + feedType);

        return externalMarketDataService;
    }

    public static class PropagateMarketDataEventSubscriber {

        public void update(final MarketDataEvent marketDataEvent) {

            // security.toString & marketDataEvent.toString is expensive, so only log if debug is enabled
            if (logger.isTraceEnabled()) {
                logger.trace(marketDataEvent.getSecurityInitialized() + " " + marketDataEvent);
            }

            long startTime = System.nanoTime();

            EngineLocator.instance().sendMarketDataEvent(marketDataEvent);

            MetricsUtil.accountEnd("PropagateMarketDataEventSubscriber.update", startTime);
        }
    }

    public static class PropagateGenericEventSubscriber {

        public void update(final GenericEventVO genericEvent) {

            // security.toString & marketDataEvent.toString is expensive, so only log if debug is enabled
            if (logger.isTraceEnabled()) {
                logger.trace(genericEvent);
            }

            EngineLocator.instance().sendGenericEvent(genericEvent);
        }
    }

    public static class PersistTickSubscriber {

        @SuppressWarnings("rawtypes")
        public void update(Pair<Tick, Object> insertStream, Map removeStream) {

            Tick tick = insertStream.getFirst();

            try {
                ServiceLocator.instance().getMarketDataService().persistTick(tick);

                // catch duplicate entry errors and log them as warn
            } catch (DataIntegrityViolationException e) {
                logger.warn(e.getRootCause().getMessage());
            }
        }
    }
}
