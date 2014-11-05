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

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.Validate;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.SessionFactory;
import org.hibernate.classic.Session;
import org.hibernate.type.IntegerType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import ch.algotrader.entity.trade.LimitOrder;
import ch.algotrader.entity.trade.LimitOrderDao;
import ch.algotrader.entity.trade.MarketOrder;
import ch.algotrader.entity.trade.MarketOrderDao;
import ch.algotrader.entity.trade.Order;
import ch.algotrader.entity.trade.OrderProperty;
import ch.algotrader.entity.trade.OrderPropertyDao;
import ch.algotrader.entity.trade.OrderStatus;
import ch.algotrader.entity.trade.OrderStatusDao;
import ch.algotrader.entity.trade.StopLimitOrder;
import ch.algotrader.entity.trade.StopLimitOrderDao;
import ch.algotrader.entity.trade.StopOrder;
import ch.algotrader.entity.trade.StopOrderDao;
import ch.algotrader.util.MyLogger;
import ch.algotrader.util.spring.HibernateSession;

/**
 * {@link OrderPersistenceService} implementation that directly
 * commits orders and order events to a persistent store.
 *
 * @author <a href="mailto:okalnichevski@algotrader.ch">Oleg Kalnichevski</a>
 *
 * @version $Revision$ $Date$
 */
@HibernateSession
public class OrderPersistenceServiceImpl implements OrderPersistenceService {

    private static final Logger logger = MyLogger.getLogger(OrderPersistenceServiceImpl.class.getName());

    private final SessionFactory sessionFactory;

    private final MarketOrderDao marketOrderDao;

    private final LimitOrderDao limitOrderDao;

    private final StopOrderDao stopOrderDao;

    private final StopLimitOrderDao stopLimitOrderDao;

    private final OrderPropertyDao orderPropertyDao;

    private final OrderStatusDao orderStatusDao;

    public OrderPersistenceServiceImpl(
            final SessionFactory sessionFactory,
            final MarketOrderDao marketOrderDao,
            final LimitOrderDao limitOrderDao,
            final StopOrderDao stopOrderDao,
            final StopLimitOrderDao stopLimitOrderDao,
            final OrderPropertyDao orderPropertyDao,
            final OrderStatusDao orderStatusDao) {

        Validate.notNull(sessionFactory, "SessionFactory is null");
        Validate.notNull(marketOrderDao, "MarketOrderDao is null");
        Validate.notNull(limitOrderDao, "LimitOrderDao is null");
        Validate.notNull(stopOrderDao, "StopOrderDao is null");
        Validate.notNull(stopLimitOrderDao, "StopLimitOrderDao is null");
        Validate.notNull(orderPropertyDao, "OrderPropertyDao is null");
        Validate.notNull(orderStatusDao, "OrderStatusDao is null");

        this.sessionFactory = sessionFactory;
        this.marketOrderDao = marketOrderDao;
        this.limitOrderDao = limitOrderDao;
        this.stopOrderDao = stopOrderDao;
        this.stopLimitOrderDao = stopLimitOrderDao;
        this.orderPropertyDao = orderPropertyDao;
        this.orderStatusDao = orderStatusDao;
    }

    @Override
    @Async("orderPersistExecutor")
    @Transactional(propagation = Propagation.REQUIRED)
    public void persistOrder(final Order order) {

        try {
            if (order instanceof MarketOrder) {
                this.marketOrderDao.create((MarketOrder) order);
            } else if (order instanceof LimitOrder) {
                this.limitOrderDao.create((LimitOrder) order);
            } else if (order instanceof StopOrder) {
                this.stopOrderDao.create((StopOrder) order);
            } else if (order instanceof StopLimitOrder) {
                this.stopLimitOrderDao.create((StopLimitOrder) order);
            } else {
                throw new IllegalStateException("Unexpected order type " + order.getClass());
            }

            // save order properties
            if (order.getOrderProperties() != null && order.getOrderProperties().size() != 0) {
                for (OrderProperty orderProperty : order.getOrderProperties().values()) {
                    this.orderPropertyDao.create(orderProperty);
                }
            }
        } catch (Exception e) {
            logger.error("problem creating order", e);
        }
    }

    @Override
    @Async("orderPersistExecutor")
    @Transactional(propagation = Propagation.REQUIRED)
    public void persistOrderStatus(final OrderStatus orderStatus) {

        try {
            this.orderStatusDao.create(orderStatus);
        } catch (Exception e) {
            logger.error("problem creating orderStatus", e);
        }
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public List<Order> loadPendingOrders() {

        Session currentSession = this.sessionFactory.getCurrentSession();
        SQLQuery sqlQuery = currentSession.createSQLQuery(
                "select os.`order_fk` " +
                        "from `order_status` os" +
                        " left join `order_status` os2" +
                        " on (substring_index(os.`int_id`, '.', 1) = substring_index(os2.`int_id`, '.', 1) " +
                        " and (os.`date_time` < os2.`date_time` " +
                        " or (os.`date_time` = os2.`date_time` and os.`sequence_number` < os2.`sequence_number`)))" +
                        " where os2.`id` is null and " +
                        " (os.`status` = 'OPEN' or os.`status` = 'SUBMITTED' or os.`status` = 'PARTIALLY_EXECUTED')");
        sqlQuery.addScalar("order_fk", new IntegerType());
        @SuppressWarnings("unchecked")
        List<Integer> ids = (List<Integer>) sqlQuery.list();
        if (ids.isEmpty()) {
            return Collections.emptyList();
        }

        Query query = currentSession.createQuery("select o from OrderImpl o " +
                "left outer join fetch o.orderProperties op " +
                "join fetch o.security sec " +
                "join fetch sec.securityFamily secf " +
                "left outer join fetch secf.brokerParameters brpr " +
                "join fetch o.strategy st " +
                "left outer join fetch st.props stpr " +
                "join fetch o.account acc " +
                "where o.id in :ids");
        query.setParameterList("ids", ids);
        @SuppressWarnings("unchecked")
        List<Order> orders = (List<Order>) query.list();
        return orders;
    }

}
