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
package ch.algotrader.service.jpm;

import java.util.Date;

import ch.algotrader.entity.trade.SimpleOrder;
import ch.algotrader.enumeration.Broker;
import ch.algotrader.enumeration.OrderServiceType;
import quickfix.field.Account;
import quickfix.field.ExDestination;
import quickfix.field.HandlInst;
import quickfix.field.SecurityExchange;
import quickfix.field.TransactTime;
import quickfix.fix42.NewOrderSingle;
import quickfix.fix42.OrderCancelReplaceRequest;
import quickfix.fix42.OrderCancelRequest;

/**
 * @author <a href="mailto:aflury@algotrader.ch">Andy Flury</a>
 *
 * @version $Revision$ $Date$
 */
public class JPMFixOrderServiceImpl extends JPMFixOrderServiceBase {

    private static final long serialVersionUID = -8881034489922372443L;

    @Override
    protected void handleSendOrder(SimpleOrder order, NewOrderSingle newOrder) {

        newOrder.set(new Account(order.getAccount().getExtAccount()));
        newOrder.set(new HandlInst('1'));
        newOrder.set(new TransactTime(new Date()));

        String exchange = order.getSecurity().getSecurityFamily().getExchangeCode(Broker.JPM);
        newOrder.set(new ExDestination(exchange));
        newOrder.set(new SecurityExchange(exchange));
    }

    @Override
    protected void handleModifyOrder(SimpleOrder order, OrderCancelReplaceRequest replaceRequest) {

        replaceRequest.set(new Account(order.getAccount().getExtAccount()));
        replaceRequest.set(new HandlInst('1'));
        replaceRequest.set(new TransactTime(new Date()));

        String exchange = order.getSecurity().getSecurityFamily().getExchangeCode(Broker.JPM);
        replaceRequest.set(new ExDestination(exchange));
        replaceRequest.set(new SecurityExchange(exchange));
    }

    @Override
    protected void handleCancelOrder(SimpleOrder order, OrderCancelRequest cancelRequest) {

        cancelRequest.set(new Account(order.getAccount().getExtAccount()));
        cancelRequest.set(new TransactTime(new Date()));

        String exchange = order.getSecurity().getSecurityFamily().getExchangeCode(Broker.JPM);
        cancelRequest.set(new SecurityExchange(exchange));
    }

    @Override
    protected OrderServiceType handleGetOrderServiceType() throws Exception {

        return OrderServiceType.JPM_FIX;
    }
}
