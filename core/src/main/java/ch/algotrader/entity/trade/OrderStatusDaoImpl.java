/***********************************************************************************
 * AlgoTrader Enterprise Trading Framework
 *
 * Copyright (C) 2013 Flury Trading - All rights reserved
 *
 * All information contained herein is, and remains the property of Flury Trading.
 * The intellectual and technical concepts contained herein are proprietary to
 * Flury Trading. Modification, translation, reverse engineering, decompilation,
 * disassembly or reproduction of this material is strictly forbidden unless prior
 * written permission is obtained from Flury Trading
 *
 * Fur detailed terms and conditions consult the file LICENSE.txt or contact
 *
 * Flury Trading
 * Badenerstrasse 16
 * 8004 Zurich
 ***********************************************************************************/
package ch.algotrader.entity.trade;

import java.util.Collection;
import java.util.Map;

import org.apache.commons.collections15.CollectionUtils;
import org.apache.commons.collections15.Transformer;
import org.apache.commons.lang.ClassUtils;
import org.apache.commons.lang.StringUtils;

import ch.algotrader.entity.strategy.StrategyImpl;
import ch.algotrader.esper.EsperManager;

import ch.algotrader.entity.trade.Order;
import ch.algotrader.entity.trade.OrderStatusDaoBase;
import ch.algotrader.enumeration.Status;
import ch.algotrader.vo.OrderStatusVO;
import com.espertech.esper.collection.Pair;

/**
 * @author <a href="mailto:andyflury@gmail.com">Andy Flury</a>
 *
 * @version $Revision$ $Date$
 */
public class OrderStatusDaoImpl extends OrderStatusDaoBase {

    @SuppressWarnings("unchecked")
    @Override
    protected Collection<OrderStatusVO> handleFindAllOrderStati() throws Exception {

        Collection<Pair<Order, Map<String, ?>>> pairs = EsperManager.executeQuery(StrategyImpl.BASE, "select * from OpenOrderWindow");
        return convertPairCollectionToOrderStatusVOCollection(pairs);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected OrderStatusVO handleFindOrderStatusByIntId(String intId) throws Exception {

        Pair<Order, Map<String, ?>> pair = (Pair<Order, Map<String, ?>>) EsperManager.executeSingelObjectQuery(StrategyImpl.BASE, "select * from OpenOrderWindow where intId = '" + intId + "'");
        return convertPairToOrderStatusVO(pair);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Collection<OrderStatusVO> handleFindOrderStatiByStrategy(String strategyName) throws Exception {

        Collection<Pair<Order, Map<String, ?>>> pairs = EsperManager.executeQuery(StrategyImpl.BASE, "select * from OpenOrderWindow where strategy.name = '" + strategyName + "'");
        return convertPairCollectionToOrderStatusVOCollection(pairs);
    }

    private Collection<OrderStatusVO> convertPairCollectionToOrderStatusVOCollection(Collection<Pair<Order, Map<String, ?>>> pairs) {

        return CollectionUtils.collect(pairs, new Transformer<Pair<Order, Map<String, ?>>, OrderStatusVO>() {
            @Override
            public OrderStatusVO transform(Pair<Order, Map<String, ?>> pair) {
                return convertPairToOrderStatusVO(pair);
            }
        });
    }

    private OrderStatusVO convertPairToOrderStatusVO(Pair<Order, Map<String, ?>> pair) {

        Order order = pair.getFirst();
        Map<String, ?> map = pair.getSecond();

        OrderStatusVO orderStatusVO = new OrderStatusVO();
        orderStatusVO.setSide(order.getSide());
        orderStatusVO.setQuantity(order.getQuantity());
        orderStatusVO.setType(StringUtils.substringBefore(ClassUtils.getShortClassName(order.getClass()), "OrderImpl"));
        orderStatusVO.setName(order.getSecurity().toString());
        orderStatusVO.setStrategy(order.getStrategy().toString());
        orderStatusVO.setAccount(order.getAccount() != null ? order.getAccount().toString() : "");
        orderStatusVO.setIntId(order.getIntId());
        orderStatusVO.setExtId(order.getExtId());
        orderStatusVO.setStatus((Status) map.get("status"));
        orderStatusVO.setFilledQuantity((Long) map.get("filledQuantity"));
        orderStatusVO.setRemainingQuantity((Long) map.get("remainingQuantity"));
        orderStatusVO.setDescription(order.getExtDescription());

        return orderStatusVO;
    }
}
