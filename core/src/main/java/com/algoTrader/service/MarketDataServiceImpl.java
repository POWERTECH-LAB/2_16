package com.algoTrader.service;

import java.io.IOException;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.time.DateUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;

import com.algoTrader.ServiceLocator;
import com.algoTrader.entity.Strategy;
import com.algoTrader.entity.Subscription;
import com.algoTrader.entity.SubscriptionImpl;
import com.algoTrader.entity.marketData.MarketDataEvent;
import com.algoTrader.entity.marketData.Tick;
import com.algoTrader.entity.security.Security;
import com.algoTrader.esper.EsperManager;
import com.algoTrader.util.DateUtil;
import com.algoTrader.util.HibernateUtil;
import com.algoTrader.util.MyLogger;
import com.algoTrader.util.io.CsvTickWriter;
import com.algoTrader.util.metric.MetricsUtil;
import com.algoTrader.vo.GenericEventVO;
import com.espertech.esper.collection.Pair;

public abstract class MarketDataServiceImpl extends MarketDataServiceBase {

    private static Logger logger = MyLogger.getLogger(MarketDataServiceImpl.class.getName());

    private @Value("${simulation}") boolean simulation;

    private Map<Security, CsvTickWriter> csvWriters = new HashMap<Security, CsvTickWriter>();

    @Override
    protected void handlePersistTick(Tick tick) throws IOException {

        // get the current Date rounded to MINUTES
        Date date = DateUtils.round(DateUtil.getCurrentEPTime(), Calendar.MINUTE);
        tick.setDateTime(date);

        // write the tick to file
        Security security = tick.getSecurity();
        CsvTickWriter csvWriter = this.csvWriters.get(security);
        if (csvWriter == null) {
            String fileName = security.getIsin() != null ? security.getIsin() : String.valueOf(security.getId());
            csvWriter = new CsvTickWriter(fileName);
            this.csvWriters.put(security, csvWriter);
        }
        csvWriter.write(tick);

        // write the tick to the DB (even if not valid)
        getTickDao().create(tick);
    }

    @Override
    protected void handleInitSubscriptions() {

        if (!this.simulation) {

            List<Security> securities = getSecurityDao().findSubscribedForAutoActivateStrategiesInclFamily();

            for (Security security : securities) {
                if (!security.getSecurityFamily().isSynthetic()) {
                    externalSubscribe(security);
                }
            }
        }
    }

    @Override
    protected void handleSubscribe(String strategyName, int securityId) throws Exception {

        Strategy strategy = getStrategyDao().findByName(strategyName);
        Security security = getSecurityDao().get(securityId);

        if (getSubscriptionDao().findByStrategyAndSecurity(strategyName, securityId) == null) {

            // only external subscribe if nobody was watching this security so far
            List<Subscription> subscriptions = getSubscriptionDao().findBySecurityForAutoActivateStrategies(security.getId());
            if (subscriptions.size() == 0) {
                if (!this.simulation && !security.getSecurityFamily().isSynthetic()) {
                    externalSubscribe(security);
                }
            }

            // update links
            Subscription subscription = new SubscriptionImpl();
            subscription.setPersistent(false);

            // associate the security
            security.addSubscriptions(subscription);

            // associate the strategy
            subscription.setStrategy(strategy);

            getSubscriptionDao().create(subscription);

            logger.info("subscribed security " + security);
        }
    }

    protected void handleRequestCurrentTicks(String strategyName) throws Exception {

        Collection<Tick> ticks = getTickDao().findCurrentTicksByStrategy(strategyName);

        for (Tick tick : ticks) {
            EsperManager.sendEvent(strategyName, tick);
        }
    }

    @Override
    protected void handleUnsubscribe(String strategyName, int securityId) throws Exception {

        Security security = getSecurityDao().get(securityId);

        Subscription subscription = getSubscriptionDao().findByStrategyAndSecurity(strategyName, securityId);

        if (subscription != null && !subscription.isPersistent()) {

            // update links
            security.getSubscriptions().remove(subscription);

            getSubscriptionDao().remove(subscription);

            // only external unsubscribe if nobody is watching this security anymore
            if (security.getSubscriptions().size() == 0) {
                if (!this.simulation && !security.getSecurityFamily().isSynthetic()) {
                    externalUnsubscribe(security);
                }
            }

            logger.info("unsubscribed security " + security);
        }
    }

    @Override
    protected void handleRemoveNonPositionSubscriptions(String strategyName) throws Exception {

        Collection<Subscription> subscriptions = getSubscriptionDao().findNonPositionSubscriptions(strategyName);

        for (Subscription subscription : subscriptions) {
            unsubscribe(subscription.getStrategy().getName(), subscription.getSecurity().getId());
        }
    }

    @SuppressWarnings("rawtypes")
    @Override
    protected void handleRemoveNonPositionSubscriptionsByType(String strategyName, Class type) throws Exception {

        int discriminator = HibernateUtil.getDisriminatorValue(getSessionFactory(), type);
        Collection<Subscription> subscriptions = getSubscriptionDao().findNonPositionSubscriptionsByType(strategyName, discriminator);

        for (Subscription subscription : subscriptions) {
            unsubscribe(subscription.getStrategy().getName(), subscription.getSecurity().getId());
        }
    }

    public static class PropagateMarketDataEventSubscriber {

        public void update(final MarketDataEvent marketDataEvent) {

            // security.toString & marketDataEvent.toString is expensive, so only log if debug is anabled
            if (!logger.getParent().getLevel().isGreaterOrEqual(Level.DEBUG)) {
                logger.trace(marketDataEvent.getSecurityInitialized() + " " + marketDataEvent);
            }

            long startTime = System.nanoTime();

            EsperManager.sendMarketDataEvent(marketDataEvent);

            MetricsUtil.accountEnd("PropagateMarketDataEventSubscriber.update", startTime);
        }
    }

    public static class PropagateGenericEventSubscriber {

        public void update(final GenericEventVO genericEvent) {

            // security.toString & marketDataEvent.toString is expensive, so only log if debug is anabled
            if (!logger.getParent().getLevel().isGreaterOrEqual(Level.DEBUG)) {
                logger.trace(genericEvent);
            }

            EsperManager.sendGenericEvent(genericEvent);
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
