package com.algoTrader.service;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.collections15.CollectionUtils;
import org.apache.commons.collections15.Predicate;

import com.algoTrader.ServiceLocator;
import com.algoTrader.entity.Property;
import com.algoTrader.entity.Strategy;
import com.algoTrader.entity.StrategyImpl;
import com.algoTrader.entity.Subscription;
import com.algoTrader.entity.marketData.Tick;
import com.algoTrader.entity.security.Security;
import com.algoTrader.entity.trade.InitializingOrderI;
import com.algoTrader.entity.trade.LimitOrder;
import com.algoTrader.entity.trade.LimitOrderI;
import com.algoTrader.entity.trade.MarketOrder;
import com.algoTrader.entity.trade.Order;
import com.algoTrader.entity.trade.StopLimitOrder;
import com.algoTrader.entity.trade.StopOrder;
import com.algoTrader.entity.trade.StopOrderI;
import com.algoTrader.entity.trade.TickwiseIncrementalLimitOrder;
import com.algoTrader.entity.trade.VariableIncrementalLimitOrder;
import com.algoTrader.enumeration.Side;
import com.algoTrader.esper.EsperManager;
import com.algoTrader.util.StrategyUtil;
import com.algoTrader.vo.BalanceVO;
import com.algoTrader.vo.PositionVO;
import com.algoTrader.vo.TickVO;
import com.algoTrader.vo.TransactionVO;

public class ManagementServiceImpl extends ManagementServiceBase {

    private static final SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy kk:mm:ss");

    @Override
    protected String handleGetCurrentTime() throws Exception {

        return format.format(new Date(EsperManager.getCurrentTime(StrategyUtil.getStartedStrategyName())));
    }

    @Override
    protected String handleGetStrategyName() throws Exception {

        return StrategyUtil.getStartedStrategyName();
    }

    @Override
    protected BigDecimal handleGetStrategyCashBalance() throws Exception {

        String strategyName = StrategyUtil.getStartedStrategyName();
        if (strategyName.equals(StrategyImpl.BASE)) {
            return getPortfolioService().getCashBalance();
        } else {
            return getPortfolioService().getCashBalance(StrategyUtil.getStartedStrategyName());
        }
    }

    @Override
    protected BigDecimal handleGetStrategySecuritiesCurrentValue() throws Exception {

        String strategyName = StrategyUtil.getStartedStrategyName();
        if (strategyName.equals(StrategyImpl.BASE)) {
            return getPortfolioService().getSecuritiesCurrentValue();
        } else {
            return getPortfolioService().getSecuritiesCurrentValue(strategyName);
        }
    }

    @Override
    protected BigDecimal handleGetStrategyMaintenanceMargin() throws Exception {

        String strategyName = StrategyUtil.getStartedStrategyName();
        if (strategyName.equals(StrategyImpl.BASE)) {
            return getPortfolioService().getMaintenanceMargin();
        } else {
            return getPortfolioService().getMaintenanceMargin(strategyName);
        }
    }

    @Override
    protected BigDecimal handleGetStrategyNetLiqValue() throws Exception {

        String strategyName = StrategyUtil.getStartedStrategyName();
        if (strategyName.equals(StrategyImpl.BASE)) {
            return getPortfolioService().getNetLiqValue();
        } else {
            return getPortfolioService().getNetLiqValue(strategyName);
        }
    }

    @Override
    protected BigDecimal handleGetStrategyAvailableFunds() throws Exception {

        String strategyName = StrategyUtil.getStartedStrategyName();
        if (strategyName.equals(StrategyImpl.BASE)) {
            return getPortfolioService().getAvailableFunds();
        } else {
            return getPortfolioService().getAvailableFunds(strategyName);
        }
    }

    @Override
    protected double handleGetStrategyAllocation() throws Exception {

        String strategyName = StrategyUtil.getStartedStrategyName();
        return getLookupService().getStrategyByName(strategyName).getAllocation();
    }

    @Override
    protected double handleGetStrategyLeverage() throws Exception {

        String strategyName = StrategyUtil.getStartedStrategyName();
        if (strategyName.equals(StrategyImpl.BASE)) {
            return getPortfolioService().getLeverage();
        } else {
            return getPortfolioService().getLeverage(strategyName);
        }
    }

    @Override
    protected BigDecimal handleGetStrategyBenchmark() throws Exception {

        String strategyName = StrategyUtil.getStartedStrategyName();
        return getLookupService().getStrategyByName(strategyName).getBenchmark();
    }

    @Override
    protected double handleGetStrategyPerformance() throws Exception {

        String strategyName = StrategyUtil.getStartedStrategyName();
        if (strategyName.equals(StrategyImpl.BASE)) {
            return getPortfolioService().getPerformance();
        } else {
            return getPortfolioService().getPerformance(strategyName);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    protected List<TickVO> handleGetDataTicks() {

        String strategyName = StrategyUtil.getStartedStrategyName();
        List<Tick> ticks = EsperManager.getAllEventsProperty(strategyName, "GET_LAST_TICK", "tick");

        List<TickVO> tickVOs = getTickVOs(ticks);

        // get all subscribed securities

        if (StrategyUtil.isStartedStrategyBASE()) {

            // for base iterate over all subscribed securities
            Map<Integer, TickVO> processedTickVOs = new TreeMap<Integer, TickVO>();
            for (Subscription subscription : getLookupService().getSubscriptionsForAutoActivateStrategiesInclComponents()) {

                Security security = subscription.getSecurity();

                // try to get the processedTick
                TickVO tickVO = processedTickVOs.get(security.getId());
                if (tickVO == null) {
                    tickVO = getTickVO(tickVOs, security);
                    processedTickVOs.put(security.getId(), tickVO);
                }

                // add all properties from this subscription
                Map<String, Property> properties = subscription.getPropertiesInitialized();
                if (!properties.isEmpty()) {
                    if (tickVO.getProperties() != null) {
                        tickVO.getProperties().putAll(properties);
                    } else {
                        tickVO.setProperties(properties);
                    }
                }
            }
            return new ArrayList<TickVO>(processedTickVOs.values());

        } else {

            // for strategies iterate over all subscriptions
            List<TickVO> processedTickVOs = new ArrayList<TickVO>();
            for (Subscription subscription : getLookupService().getSubscriptionsByStrategyInclComponents(strategyName)) {

                TickVO tickVO = getTickVO(tickVOs, subscription.getSecurity());

                // add properties from this strategies subscription
                Map<String, Property> properties = subscription.getPropertiesInitialized();
                if (!properties.isEmpty()) {
                    tickVO.setProperties(properties);
                }

                processedTickVOs.add(tickVO);
            }
            return processedTickVOs;
        }

    }

    @Override
    protected List<PositionVO> handleGetDataPositions() throws Exception {

        return getLookupService().getOpenPositionsVO(StrategyUtil.getStartedStrategyName());
    }

    @Override
    protected Collection<BalanceVO> handleGetDataBalances() throws Exception {

        String strategyName = StrategyUtil.getStartedStrategyName();
        if (strategyName.equals(StrategyImpl.BASE)) {
            return getPortfolioService().getBalances();
        } else {
            return new ArrayList<BalanceVO>();
        }
    }

    @Override
    protected List<TransactionVO> handleGetDataTransactions() throws Exception {

        return getLookupService().getTransactionsVO(StrategyUtil.getStartedStrategyName());
    }

    @Override
    protected Map<Object, Object> handleGetProperties() throws Exception {

        return new TreeMap<Object, Object>(getConfiguration().getProperties());
    }

    @Override
    protected void handleDeployStatement(String moduleName, String statementName) throws Exception {

        EsperManager.deployStatement(StrategyUtil.getStartedStrategyName(), moduleName, statementName);
    }

    @Override
    protected void handleDeployModule(String moduleName) throws Exception {

        EsperManager.deployModule(StrategyUtil.getStartedStrategyName(), moduleName);
    }

    @Override
    protected void handleSendOrder(int securityId, long quantity, String sideString, String type, Double limit, Double stop) throws Exception {

        Side side = Side.fromValue(sideString);
        String strategyName = StrategyUtil.getStartedStrategyName();

        Strategy strategy = getLookupService().getStrategyByName(strategyName);
        final Security security = getLookupService().getSecurity(securityId);

        // instantiate the order
        Order order;
        if ("M".equals(type)) {
            order = MarketOrder.Factory.newInstance();
        } else if ("L".equals(type)) {
            order = LimitOrder.Factory.newInstance();
        } else if ("S".equals(type)) {
            order = StopOrder.Factory.newInstance();
        } else if ("SL".equals(type)) {
            order = StopLimitOrder.Factory.newInstance();
        } else if ("TIL".equals(type)) {
            order = TickwiseIncrementalLimitOrder.Factory.newInstance();
        } else if ("VIL".equals(type)) {
            order = VariableIncrementalLimitOrder.Factory.newInstance();
        } else {
            throw new IllegalArgumentException("unkown order type " + type);
        }

        // set common values
        order.setStrategy(strategy);
        order.setSecurity(security);
        order.setQuantity(Math.abs(quantity));
        order.setSide(side);

        // set the limit if applicable
        if (order instanceof LimitOrderI && limit != null) {
            ((LimitOrderI) order).setLimit(new BigDecimal(limit));
        }

        // set the stop if applicable
        if (order instanceof StopOrderI && stop != null) {
            ((StopOrderI) order).setStop(new BigDecimal(stop));
        }

        // init the order if applicable
        if (order instanceof InitializingOrderI) {
            ((InitializingOrderI) order).init(null);
        }

        // send orders
        getOrderService().sendOrder(order);
    }

    @Override
    protected void handleSetVariableValue(String variableName, String value) {

        EsperManager.setVariableValue(StrategyUtil.getStartedStrategyName(), variableName, value);
    }

    @Override
    protected void handleAddProperty(int propertyHolderId, String name, String value, String type) throws Exception {

        Object obj;
        if ("INT".equals(type)) {
            obj = Integer.parseInt(value);
        } else if ("DOUBLE".equals(type)) {
            obj = Double.parseDouble(value);
        } else if ("MONEY".equals(type)) {
            obj = new BigDecimal(value);
        } else if ("TEXT".equals(type)) {
            obj = value;
        } else if ("DATE".equals(type)) {
            obj = (new SimpleDateFormat("dd.MM.yyyy HH:mm:ss")).parse(value);
        } else if ("BOOLEAN".equals(type)) {
            obj = Boolean.parseBoolean(value);
        } else {
            throw new IllegalArgumentException("unknown type " + type);
        }

        getPropertyService().addProperty(propertyHolderId, name, obj, false);
    }

    @Override
    protected void handleRemoveProperty(int propertyHolderId, String name) throws Exception {

        getPropertyService().removeProperty(propertyHolderId, name);
    }

    @Override
    protected void handleShutdown() throws Exception {

        // cancel all orders if we called from base
        if (StrategyUtil.isStartedStrategyBASE()) {
            ServiceLocator.instance().getOrderService().cancelAllOrders();
        }

        ServiceLocator.instance().shutdown();

        // need to force exit because grafefull shutdown of esper-service (and esper-jmx) does not work
        System.exit(0);
    }

    @Override
    protected void handleSubscribe(int securityid) throws Exception {

        getSubscriptionService().subscribeMarketDataEvent(StrategyUtil.getStartedStrategyName(), securityid);
    }

    @Override
    protected void handleUnsubscribe(int securityid) throws Exception {

        getSubscriptionService().unsubscribeMarketDataEvent(StrategyUtil.getStartedStrategyName(), securityid);
    }

    private List<TickVO> getTickVOs(List<Tick> ticks) {

        // create TickVOs based on the ticks (have to do this manually since we have no access to the Dao)
        List<TickVO> tickVOs = new ArrayList<TickVO>();
        for (Tick tick : ticks) {

            TickVO tickVO = new TickVO();
            tickVO.setDateTime(tick.getDateTime());
            tickVO.setLast(tick.getLast());
            tickVO.setLastDateTime(tick.getLastDateTime());
            tickVO.setVol(tick.getVol());
            tickVO.setVolBid(tick.getVolBid());
            tickVO.setVolAsk(tick.getVolAsk());
            tickVO.setBid(tick.getBid());
            tickVO.setAsk(tick.getAsk());
            tickVO.setOpenIntrest(tick.getOpenIntrest());
            tickVO.setSettlement(tick.getSettlement());
            tickVO.setSecurityId(tick.getSecurity().getId());
            tickVO.setCurrentValue(tick.getCurrentValue());

            tickVOs.add(tickVO);
        }
        return tickVOs;
    }

    private TickVO getTickVO(List<TickVO> tickVOs, final Security security) {

        // get the tickVO matching the securityId
        TickVO tickVO = CollectionUtils.find(tickVOs, new Predicate<TickVO>() {
            @Override
            public boolean evaluate(TickVO TickVO) {
                return TickVO.getSecurityId() == security.getId();
            }
        });

        // create an empty TickVO if non exists
        if (tickVO == null) {
            tickVO = new TickVO();
        }

        // set db data
        tickVO.setSecurityId(security.getId());
        tickVO.setName(security.toString());
        return tickVO;
    }
}
