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

import java.util.Collections;
import java.util.Map;

import ch.algotrader.config.CommonConfig;
import ch.algotrader.entity.PositionVO;
import ch.algotrader.entity.TransactionVO;
import ch.algotrader.entity.marketData.BarVO;
import ch.algotrader.entity.marketData.TickVO;
import ch.algotrader.entity.strategy.Strategy;
import ch.algotrader.entity.trade.FillVO;
import ch.algotrader.entity.trade.OrderCompletionVO;
import ch.algotrader.entity.trade.OrderStatusVO;
import ch.algotrader.entity.trade.OrderVO;
import ch.algotrader.esper.Engine;
import ch.algotrader.event.listener.BarEventListener;
import ch.algotrader.event.listener.FillEventListener;
import ch.algotrader.event.listener.LifecycleEventListener;
import ch.algotrader.event.listener.OrderCompletionEventListener;
import ch.algotrader.event.listener.OrderEventListener;
import ch.algotrader.event.listener.OrderStatusEventListener;
import ch.algotrader.event.listener.PositionEventListener;
import ch.algotrader.event.listener.SessionEventListener;
import ch.algotrader.event.listener.TickEventListener;
import ch.algotrader.event.listener.TransactionEventListener;
import ch.algotrader.simulation.SimulationResultsProducer;
import ch.algotrader.vo.LifecycleEventVO;
import ch.algotrader.vo.SessionEventVO;

/**
 * Base strategy that implements all event listener interfaces. Events are
 * propagated to the listener methods. Alternatively strategies can implement
 * listener interfaces selectively without needing to extend this class.
 * <p>
 * The framework is made aware of event listener via Spring. As a consequence,
 * all implementors of these interfaces should be managed by the Spring
 * container.
 *
 * @author <a href="mailto:aflury@algotrader.ch">Andy Flury</a>
 */
//@formatter:off
public class StrategyService implements
        LifecycleEventListener, BarEventListener, TickEventListener, OrderEventListener, OrderStatusEventListener, OrderCompletionEventListener,
        FillEventListener, TransactionEventListener, PositionEventListener, SessionEventListener, SimulationResultsProducer {
//@formatter:on

    private CommonConfig commonConfig;
    private CalendarService calendarService;
    private CombinationService combinationService;
    private FutureService futureService;
    private HistoricalDataService historicalDataService;
    private LookupService lookupService;
    private MarketDataCacheService marketDataCacheService;
    private MarketDataService marketDataService;
    private MeasurementService measurementService;
    private OptionService optionService;
    private OrderService orderService;
    private PortfolioService portfolioService;
    private PositionService positionService;
    private PropertyService propertyService;
    private ReferenceDataService referenceDataService;
    private SubscriptionService subscriptionService;
    private String strategyName;
    private double weight;
    private Engine engine;

    public CommonConfig getCommonConfig() {
        return this.commonConfig;
    }

    public void setCommonConfig(CommonConfig commonConfig) {
        this.commonConfig = commonConfig;
    }

    public CalendarService getCalendarService() {
        return this.calendarService;
    }

    public void setCalendarService(CalendarService calendarService) {
        this.calendarService = calendarService;
    }

    public CombinationService getCombinationService() {
        return this.combinationService;
    }

    public void setCombinationService(CombinationService combinationService) {
        this.combinationService = combinationService;
    }

    public FutureService getFutureService() {
        return this.futureService;
    }

    public void setFutureService(FutureService futureService) {
        this.futureService = futureService;
    }

    public HistoricalDataService getHistoricalDataService() {
        return this.historicalDataService;
    }

    public void setHistoricalDataService(HistoricalDataService historicalDataService) {
        this.historicalDataService = historicalDataService;
    }

    public LookupService getLookupService() {
        return this.lookupService;
    }

    public void setLookupService(LookupService lookupService) {
        this.lookupService = lookupService;
    }

    public MarketDataCacheService getMarketDataCacheService() {
        return this.marketDataCacheService;
    }

    public void setMarketDataCacheService(MarketDataCacheService marketDataCacheService) {
        this.marketDataCacheService = marketDataCacheService;
    }

    public MarketDataService getMarketDataService() {
        return this.marketDataService;
    }

    public void setMarketDataService(MarketDataService marketDataService) {
        this.marketDataService = marketDataService;
    }

    public MeasurementService getMeasurementService() {
        return this.measurementService;
    }

    public void setMeasurementService(MeasurementService measurementService) {
        this.measurementService = measurementService;
    }

    public OptionService getOptionService() {
        return this.optionService;
    }

    public void setOptionService(OptionService optionService) {
        this.optionService = optionService;
    }

    public OrderService getOrderService() {
        return this.orderService;
    }

    public void setOrderService(OrderService orderService) {
        this.orderService = orderService;
    }

    public PortfolioService getPortfolioService() {
        return this.portfolioService;
    }

    public void setPortfolioService(PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
    }

    public PositionService getPositionService() {
        return this.positionService;
    }

    public void setPositionService(PositionService positionService) {
        this.positionService = positionService;
    }

    public PropertyService getPropertyService() {
        return this.propertyService;
    }

    public void setPropertyService(PropertyService propertyService) {
        this.propertyService = propertyService;
    }

    public ReferenceDataService getReferenceDataService() {
        return this.referenceDataService;
    }

    public void setReferenceDataService(ReferenceDataService referenceDataService) {
        this.referenceDataService = referenceDataService;
    }

    public SubscriptionService getSubscriptionService() {
        return this.subscriptionService;
    }

    public void setSubscriptionService(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    public void setStrategyName(final String strategyName) {
        this.strategyName = strategyName;
    }

    public String getStrategyName() {
        return this.strategyName;
    }

    public void setEngine(final Engine engine) {
        this.engine = engine;
    }

    public Engine getEngine() {
        return this.engine;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public double getWeight() {
        return this.weight;
    }

    public Strategy getStrategy() {
        return getLookupService().getStrategyByName(this.strategyName);
    }

    @Override
    public Map<String, Object> getSimulationResults() {

        return Collections.emptyMap();
    }

    @Override
    public void onChange(final LifecycleEventVO event) {
        switch (event.getPhase()) {
            case INIT:
                onInit(event);
                break;
            case PREFEED:
                onPrefeed(event);
                break;
            case START:
                onStart(event);
                break;
            case EXIT:
                onExit(event);
                break;
            default:
                break;
        }
    }

    protected void onInit(final LifecycleEventVO event) {
    }

    protected void onPrefeed(final LifecycleEventVO event) {
    }

    protected void onStart(final LifecycleEventVO event) {
    }

    protected void onExit(final LifecycleEventVO event) {
    }

    @Override
    public void onTick(final TickVO bar) {
    }

    @Override
    public void onBar(final BarVO bar) {
    }

    @Override
    public void onOrder(final OrderVO order) {
    }

    @Override
    public void onOrderStatus(final OrderStatusVO orderStatus) {
    }

    @Override
    public void onOrderCompletion(final OrderCompletionVO orderCompletion) {
    }

    @Override
    public void onFill(final FillVO fill) {
    }

    @Override
    public void onTransaction(final TransactionVO transaction) {
    }

    @Override
    public void onPositionChange(final PositionVO openPosition) {
    }

    @Override
    public void onChange(final SessionEventVO event) {
    }

}
