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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.lang.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.SessionFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import ch.algotrader.config.CommonConfig;
import ch.algotrader.dao.AccountDao;
import ch.algotrader.dao.exchange.ExchangeDao;
import ch.algotrader.dao.security.SecurityDao;
import ch.algotrader.dao.strategy.StrategyDao;
import ch.algotrader.dao.trade.OrderDao;
import ch.algotrader.dao.trade.OrderPreferenceDao;
import ch.algotrader.dao.trade.OrderStatusDao;
import ch.algotrader.entity.Account;
import ch.algotrader.entity.marketData.MarketDataEventVO;
import ch.algotrader.entity.marketData.TickVO;
import ch.algotrader.entity.security.Security;
import ch.algotrader.entity.strategy.Strategy;
import ch.algotrader.entity.trade.AlgoOrder;
import ch.algotrader.entity.trade.Allocation;
import ch.algotrader.entity.trade.ExecutionStatusVO;
import ch.algotrader.entity.trade.Order;
import ch.algotrader.entity.trade.OrderCompletionVO;
import ch.algotrader.entity.trade.OrderDetailsVO;
import ch.algotrader.entity.trade.OrderPreference;
import ch.algotrader.entity.trade.OrderStatus;
import ch.algotrader.entity.trade.OrderValidationException;
import ch.algotrader.entity.trade.SimpleOrder;
import ch.algotrader.entity.trade.SubmittedOrder;
import ch.algotrader.enumeration.InitializingServiceType;
import ch.algotrader.enumeration.OrderServiceType;
import ch.algotrader.enumeration.Status;
import ch.algotrader.enumeration.TIF;
import ch.algotrader.esper.Engine;
import ch.algotrader.esper.EngineManager;
import ch.algotrader.event.dispatch.EventDispatcher;
import ch.algotrader.ordermgmt.OpenOrderRegistry;
import ch.algotrader.util.BeanUtil;

/**
 * @author <a href="mailto:aflury@algotrader.ch">Andy Flury</a>
 *
 * @version $Revision$ $Date$
 */
@Transactional(propagation = Propagation.SUPPORTS)
@InitializationPriority(value = InitializingServiceType.CORE)
public class OrderServiceImpl implements OrderService, InitializingServiceI {

    private static final long serialVersionUID = 3969251081188007542L;

    private static final Logger LOGGER = LogManager.getLogger(OrderServiceImpl.class);

    private final CommonConfig commonConfig;

    private final SessionFactory sessionFactory;

    private final OrderPersistenceService orderPersistService;

    private final LocalLookupService localLookupService;

    private final OrderDao orderDao;

    private final OrderStatusDao orderStatusDao;

    private final StrategyDao strategyDao;

    private final SecurityDao securityDao;

    private final AccountDao accountDao;

    private final ExchangeDao exchangeDao;

    private final OrderPreferenceDao orderPreferenceDao;

    private final OpenOrderRegistry openOrderRegistry;

    private final EventDispatcher eventDispatcher;

    private final EngineManager engineManager;

    private final Engine serverEngine;

    private final Map<OrderServiceType, ExternalOrderService> externalOrderServiceMap;

    public OrderServiceImpl(final CommonConfig commonConfig,
            final SessionFactory sessionFactory,
            final OrderPersistenceService orderPersistService,
            final LocalLookupService localLookupService,
            final OrderDao orderDao,
            final OrderStatusDao orderStatusDao,
            final StrategyDao strategyDao,
            final SecurityDao securityDao,
            final AccountDao accountDao,
            final ExchangeDao exchangeDao,
            final OrderPreferenceDao orderPreferenceDao,
            final OpenOrderRegistry openOrderRegistry,
            final EventDispatcher eventDispatcher,
            final EngineManager engineManager,
            final Engine serverEngine,
            final Map<OrderServiceType, ExternalOrderService> externalOrderServiceMap) {

        Validate.notNull(commonConfig, "CommonConfig is null");
        Validate.notNull(sessionFactory, "SessionFactory is null");
        Validate.notNull(orderPersistService, "OrderPersistStrategy is null");
        Validate.notNull(localLookupService, "LocalLookupService is null");
        Validate.notNull(orderDao, "OrderDao is null");
        Validate.notNull(orderStatusDao, "OrderStatusDao is null");
        Validate.notNull(strategyDao, "StrategyDao is null");
        Validate.notNull(securityDao, "SecurityDao is null");
        Validate.notNull(accountDao, "AccountDao is null");
        Validate.notNull(exchangeDao, "ExchangeDao is null");
        Validate.notNull(orderPreferenceDao, "OrderPreferenceDao is null");
        Validate.notNull(openOrderRegistry, "OpenOrderRegistry is null");
        Validate.notNull(eventDispatcher, "PlatformEventDispatcher is null");
        Validate.notNull(engineManager, "EngineManager is null");
        Validate.notNull(serverEngine, "Engine is null");

        this.commonConfig = commonConfig;
        this.sessionFactory = sessionFactory;
        this.orderPersistService = orderPersistService;
        this.localLookupService = localLookupService;
        this.orderDao = orderDao;
        this.orderStatusDao = orderStatusDao;
        this.strategyDao = strategyDao;
        this.securityDao = securityDao;
        this.accountDao = accountDao;
        this.exchangeDao = exchangeDao;
        this.orderPreferenceDao = orderPreferenceDao;
        this.openOrderRegistry = openOrderRegistry;
        this.eventDispatcher = eventDispatcher;
        this.engineManager = engineManager;
        this.serverEngine = serverEngine;
        this.externalOrderServiceMap = new ConcurrentHashMap<>(externalOrderServiceMap);
    }

    @Override
    public Order createOrderByOrderPreference(final String name) {

        Validate.notEmpty(name, "Name is empty");

        OrderPreference orderPreference = this.orderPreferenceDao.findByName(name);

        Validate.notNull(orderPreference, "unknown OrderPreference");

        Class<?> orderClazz;
        Order order;
        try {

            // create an order instance
            orderClazz = Class.forName(orderPreference.getOrderType().getValue());
            order = (Order) orderClazz.newInstance();

            // populate the order with the properties
            BeanUtil.populate(order, orderPreference.getPropertyNameValueMap());

        } catch (ReflectiveOperationException e) {
            throw new ServiceException(e);
        }

        // set the account if defined
        if (orderPreference.getDefaultAccount() != null) {
            order.setAccount(orderPreference.getDefaultAccount());
        }

        // set allocations if defined
        if (orderPreference.getAllocations().size() > 0) {

            if (!(order instanceof AlgoOrder)) {
                throw new IllegalStateException("allocations cannot be assigned to " + orderClazz + " (only AlgoOrders can have allocations)");
            } else {

                double totalAllocation = 0;
                for (Allocation allocation : orderPreference.getAllocations()) {
                    totalAllocation += allocation.getValue();
                }

                if (totalAllocation != 1.0) {
                    throw new IllegalStateException("sum of allocations are not 1.0 for " + toString());
                }

                AlgoOrder algoOrder = (AlgoOrder) order;
                algoOrder.setAllocations(orderPreference.getAllocations());
            }
        }

        return order;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validateOrder(final Order order) throws OrderValidationException {

        Validate.notNull(order, "Order is null");

        // validate general properties
        Validate.notNull(order.getSide(), "missing side for order " + order);
        Validate.isTrue(order.getQuantity() != 0, "quanity cannot be zero for order " + order);
        Validate.isTrue(order.getQuantity() > 0, "quantity has to be positive for order " + order);

        if (order instanceof SimpleOrder) {
            Validate.notNull(order.getAccount(), "missing account for order " + order);
        }

        // validate order specific properties
        order.validate();

        // check that the security is tradeable
        Security security = order.getSecurity();
        Validate.isTrue(security.getSecurityFamily().isTradeable(), security + " is not tradeable");

        // external validation of the order
        if (order instanceof SimpleOrder) {

            getExternalOrderService(order.getAccount()).validateOrder((SimpleOrder) order);

        } else if (order instanceof AlgoOrder) {

            // check market data for AlgoOrders
            if (security.getSubscriptions().size() == 0) {
                throw new OrderValidationException(security + " is not subscribed for " + order);
            }

            MarketDataEventVO marketDataEvent = this.localLookupService.getCurrentMarketDataEvent(security.getId());
            if (marketDataEvent == null) {
                throw new OrderValidationException("no marketDataEvent available to initialize SlicingOrder");
            } else if (!(marketDataEvent instanceof TickVO)) {
                throw new OrderValidationException("only ticks are supported, " + marketDataEvent.getClass() + " are not supported");
            }
        }

        // TODO add internal validations (i.e. limit, amount, etc.)

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sendOrder(final Order order) {

        Validate.notNull(order, "Order is null");

        // validate strategy and security
        Validate.notNull(order.getStrategy());
        Validate.notNull(order.getSecurity());

        // reload the strategy and security to get potential changes
        order.setStrategy(this.strategyDao.load(order.getStrategy().getId()));
        order.setSecurity(this.securityDao.findByIdInclFamilyUnderlyingExchangeAndBrokerParameters(order.getSecurity().getId()));

        // reload the account if necessary to get potential changes
        if (order.getAccount() != null) {
            order.setAccount(this.accountDao.load(order.getAccount().getId()));
        }

        // reload the exchange if necessary to get potential changes
        if (order.getExchange() != null) {
            order.setExchange(this.exchangeDao.load(order.getExchange().getId()));
        }

        // validate the order before sending it
        try {
            validateOrder(order);
        } catch (OrderValidationException ex) {
            throw new ServiceException(ex);
        }

        // set the dateTime property
        order.setDateTime(this.engineManager.getCurrentEPTime());

        // in case no TIF was specified set DAY
        if (order.getTif() == null) {
            order.setTif(TIF.DAY);
        }

        if (order instanceof AlgoOrder) {
            sendAlgoOrder((AlgoOrder) order);
        } else if (order instanceof SimpleOrder){
            sendSimpleOrder((SimpleOrder) order);
        } else {
            throw new ServiceException("Unexpected order class: " + order.getClass());
        }
    }

    private void sendAlgoOrder(final AlgoOrder order) {

        order.setIntId(AlgoIdGenerator.getInstance().getNextOrderId());

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("send algo order: {}", order);
        }

        this.openOrderRegistry.add(order);

        // progapate the order to all corresponding esper engines
        propagateOrder(order);

        this.serverEngine.sendEvent(order);
    }

    private void sendSimpleOrder(final SimpleOrder order) {

        Account account = order.getAccount();
        if (account == null) {
            throw new ServiceException("Order with missing account");
        }

        ExternalOrderService externalOrderService = getExternalOrderService(account);
        externalOrderService.sendOrder(order);

        if (!this.commonConfig.isSimulation()) {
            this.orderPersistService.persistOrder(order);
        }
        propagateOrder(order);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sendOrders(final Collection<Order> orders) {

        for (Order order : orders) {
            sendOrder(order);
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void cancelOrder(final Order order) {

        Validate.notNull(order, "Order is null");

        if (order instanceof AlgoOrder) {
            cancelAlgoOrder((AlgoOrder) order);
        } else if (order instanceof SimpleOrder){
            cancelSimpleOrder((SimpleOrder) order);
        } else {
            throw new ServiceException("Unexpected order class: " + order.getClass());
        }

    }

    private void cancelAlgoOrder(AlgoOrder order) {

        // cancel existing child orders
        Collection<Order> openOrders = this.openOrderRegistry.findByParentIntId(order.getIntId());
        openOrders.forEach(childOrder -> {
            Account account = order.getAccount();
            Validate.notNull(account, "missing account for order: " + order);
            getExternalOrderService(account).cancelOrder((SimpleOrder) childOrder);
        });

        ExecutionStatusVO executionStatus = this.openOrderRegistry.getStatusByIntId(order.getIntId());

        // assemble a new OrderStatus Entity
        OrderStatus orderStatus = OrderStatus.Factory.newInstance();
        orderStatus.setStatus(Status.CANCELED);
        orderStatus.setFilledQuantity(executionStatus != null ? executionStatus.getFilledQuantity() : 0L);
        orderStatus.setRemainingQuantity(executionStatus != null ? executionStatus.getRemainingQuantity() : 0L);
        orderStatus.setOrder(order);

        // send the orderStatus
        this.serverEngine.sendEvent(orderStatus);

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("cancelled algo order: {}", order);
        }
    }

    private void cancelSimpleOrder(final SimpleOrder order) {

        Account account = order.getAccount();
        if (account == null) {
            throw new ServiceException("Order with missing account");
        }

        ExternalOrderService externalOrderService = getExternalOrderService(account);
        externalOrderService.cancelOrder(order);

        if (!this.commonConfig.isSimulation()) {
            this.orderPersistService.persistOrder(order);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void cancelOrder(final String intId) {

        Validate.notNull(intId, "Int id is null");

        Order order = this.openOrderRegistry.getByIntId(intId);
        if (order == null) {
            throw new ServiceException("Could not find order with IntId " + intId);
        }
        cancelOrder(order);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void cancelAllOrders() {

        for (Order order : this.openOrderRegistry.getAllOrders()) {
            cancelOrder(order);
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void modifyOrder(final Order order) {

        Validate.notNull(order, "Order is null");

        if (order instanceof AlgoOrder) {
            throw new ServiceException("Modification of AlgoOrders is not permitted");
        } else if (order instanceof SimpleOrder){

            SimpleOrder newOrder;
            if (order.getId() != 0) {
                try {
                    newOrder = BeanUtil.clone((SimpleOrder) order);
                    newOrder.setId(0);
                } catch (ReflectiveOperationException ex) {
                    throw new ServiceException(ex);
                }
            } else {
                newOrder = (SimpleOrder) order;
            }

            modifySimpleOrder(newOrder);
        } else {
            throw new ServiceException("Unexpected order class: " + order.getClass());
        }
    }

    private void modifySimpleOrder(final SimpleOrder order) {

        Account account = order.getAccount();
        if (account == null) {
            throw new ServiceException("Order with missing account");
        }

        ExternalOrderService externalOrderService = getExternalOrderService(account);
        externalOrderService.modifyOrder(order);

        if (!this.commonConfig.isSimulation()) {
            this.orderPersistService.persistOrder(order);
        }
        propagateOrder(order);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void modifyOrder(final String intId, final Map<String, String> properties) {

        Validate.notNull(intId, "Int id is null");
        Validate.notNull(properties, "Properties is null");

        Order order = this.openOrderRegistry.getByIntId(intId);
        if (order == null) {
            throw new ServiceException("Could not find order with IntId " + intId);
        }
        Order newOrder;
        try {
            newOrder = BeanUtil.cloneAndPopulate(order, properties);
            newOrder.setId(0);
        } catch (ReflectiveOperationException ex) {
            throw new ServiceException(ex);
        }

        modifyOrder(newOrder);
    }

    private void propagateOrder(final Order order) {

        Validate.notNull(order, "Order is null");

        // send the order into the AlgoTrader Server engine to be correlated with fills
        this.serverEngine.sendEvent(new SubmittedOrder(Status.OPEN, 0, order.getQuantity(), order));

        // also send the order to the strategy that placed the order
        Strategy strategy = order.getStrategy();
        if (!strategy.isServer()) {

            this.eventDispatcher.sendEvent(strategy.getName(), order.convertToVO());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void propagateOrderStatus(final OrderStatus orderStatus) {

        Validate.notNull(orderStatus, "Order status is null");

        String intId = orderStatus.getIntId();
        Order order = this.openOrderRegistry.getByIntId(intId);
        if (order == null) {
            throw new ServiceException("Could not find order with IntId " + intId);
        }
        // send the fill to the strategy that placed the corresponding order
        Strategy strategy = order.getStrategy();
        if (!strategy.isServer()) {

            this.eventDispatcher.sendEvent(strategy.getName(), orderStatus.convertToVO());
        }

        if (!this.commonConfig.isSimulation()) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("propagated orderStatus: {}", orderStatus);
            }

            String extId = orderStatus.getExtId();
            if (extId != null && order.getExtId() == null) {
                order.setExtId(extId);
                this.orderPersistService.persistOrder(order);
            }

            // only store OrderStatus for non AlgoOrders
            // and ignore order status message with synthetic (non-positive) sequence number
            if (orderStatus.getSequenceNumber() > 0 && !(order instanceof AlgoOrder)) {

                this.orderPersistService.persistOrderStatus(orderStatus);
            }
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void propagateOrderCompletion(final OrderCompletionVO orderCompletion) {

        Validate.notNull(orderCompletion, "Order completion is null");

        String intId = orderCompletion.getOrderIntId();
        Order order = this.openOrderRegistry.getByIntId(intId);
        if (order == null) {
            throw new ServiceException("Could not find order with IntId " + intId);
        }
        // send the fill to the strategy that placed the corresponding order
        Strategy strategy = order.getStrategy();
        if (strategy.isServer()) {
            this.eventDispatcher.sendEvent(strategy.getName(), orderCompletion);
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void orderCompleted(final OrderStatus orderStatus) {

        String intId = orderStatus.getIntId();
        this.openOrderRegistry.remove(intId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<OrderDetailsVO> getAllOpenOrders() {

        return this.openOrderRegistry.getAllOrderDetails();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<OrderDetailsVO> getOpenOrdersByStrategy(final String strategyName) {

        Validate.notEmpty(strategyName, "Strategy name is empty");

        return this.openOrderRegistry.getOrderDetailsForStrategy(strategyName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Order getOpenOrderByIntId(final String intId) {

        Validate.notEmpty(intId, "Order IntId is empty");

        return this.openOrderRegistry.getByIntId(intId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExecutionStatusVO getStatusByIntId(final String intId) {

        Validate.notEmpty(intId, "Order IntId is empty");

        return this.openOrderRegistry.getStatusByIntId(intId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OrderDetailsVO getOpenOrderDetailsByIntId(String intId) {

        Validate.notEmpty(intId, "Order IntId is empty");

        return this.openOrderRegistry.getDetailsByIntId(intId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Order getOrderByIntId(String intId) {

        Order order = this.openOrderRegistry.getByIntId(intId);
        if (order != null) {
            return order;
        } else {
            return this.orderDao.findByIntId(intId);
        }
    }

    @Override
    public Map<Order, OrderStatus> loadPendingOrders() {

        List<OrderStatus> pendingOrderStati = this.orderStatusDao.findPending();
        List<Long> unacknowledgedOrderIds = this.orderDao.findUnacknowledgedOrderIds();

        if (pendingOrderStati.isEmpty() && unacknowledgedOrderIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Long> pendingOrderIds = new ArrayList<>(unacknowledgedOrderIds.size() + pendingOrderStati.size());
        Map<Long, OrderStatus> orderStatusMap = new HashMap<>(pendingOrderStati.size());
        for (OrderStatus pendingOrderStatus: pendingOrderStati) {

            long orderId = pendingOrderStatus.getOrder().getId();
            pendingOrderIds.add(orderId);
            orderStatusMap.put(orderId, pendingOrderStatus);
        }
        pendingOrderIds.addAll(unacknowledgedOrderIds);

        // Clear session to evict Order proxies associated with order stati
        this.sessionFactory.getCurrentSession().clear();

        List<Order> orderList = this.orderDao.findByIds(pendingOrderIds);
        Map<Order, OrderStatus> pendingOrderMap = new HashMap<>(pendingOrderIds.size());
        for (Order pendingOrder: orderList) {

            OrderStatus orderStatus = orderStatusMap.get(pendingOrder.getId());
            pendingOrderMap.put(pendingOrder, orderStatus);
        }

        return pendingOrderMap;
    }

    @Override
    public void init() {

        final Map<Order, OrderStatus> pendingOrderMap = loadPendingOrders();
        if (LOGGER.isInfoEnabled() && !pendingOrderMap.isEmpty()) {

            List<Order> orderList  = new ArrayList<>(pendingOrderMap.keySet());

            LOGGER.info("{} order(s) are pending", orderList.size());
            for (int i = 0; i < orderList.size(); i++) {
                Order order = orderList.get(i);
                LOGGER.info("{}: {}", (i + 1), order);
            }
        }

        for (Map.Entry<Order, OrderStatus> entry: pendingOrderMap.entrySet()) {

            Order order = entry.getKey();
            this.openOrderRegistry.add(order);

            OrderStatus orderStatus = entry.getValue();
            if (orderStatus != null) {
                this.openOrderRegistry.updateExecutionStatus(order.getIntId(), orderStatus.getStatus(), orderStatus.getFilledQuantity(), orderStatus.getRemainingQuantity());
                this.serverEngine.sendEvent(new SubmittedOrder(orderStatus.getStatus(), orderStatus.getFilledQuantity(), orderStatus.getRemainingQuantity(), order));
            } else {
                this.serverEngine.sendEvent(new SubmittedOrder(Status.OPEN, 0, order.getQuantity(), order));
            }
        }
    }

    private ExternalOrderService getExternalOrderService(Account account) {

        Validate.notNull(account, "Account is null");

        OrderServiceType orderServiceType;

        if (this.commonConfig.isSimulation()) {
            orderServiceType = OrderServiceType.SIMULATION;
        } else {
            orderServiceType = account.getOrderServiceType();
        }

        ExternalOrderService externalOrderService = this.externalOrderServiceMap.get(orderServiceType);
        if (externalOrderService == null) {
            throw new ServiceException("No ExternalOrderService found for service type " + orderServiceType);
        }
        return externalOrderService;
    }

    private static final class AlgoIdGenerator {

        private static AlgoIdGenerator instance;

        private final AtomicLong orderId = new AtomicLong(0);

        public static synchronized AlgoIdGenerator getInstance() {

            if (instance == null) {
                instance = new AlgoIdGenerator();
            }
            return instance;
        }

        public String getNextOrderId() {
            return "a" + Long.toString(this.orderId.incrementAndGet());
        }
    }

}
