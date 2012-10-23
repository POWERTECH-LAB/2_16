package com.algoTrader.service.ib;

import quickfix.field.CustomerOrFirm;
import quickfix.field.ExDestination;
import quickfix.field.HandlInst;
import quickfix.fix42.NewOrderSingle;
import quickfix.fix42.OrderCancelReplaceRequest;
import quickfix.fix42.OrderCancelRequest;

import com.algoTrader.entity.trade.SimpleOrder;
import com.algoTrader.enumeration.MarketChannel;
import com.algoTrader.service.InitializingServiceI;

public class IBFixOrderServiceImpl extends IBFixOrderServiceBase implements InitializingServiceI {

    private static final long serialVersionUID = -537844523983750001L;

    @Override
    protected void handleSendOrder(SimpleOrder order, NewOrderSingle newOrder) {

        newOrder.set(new HandlInst('2'));
        newOrder.set(new CustomerOrFirm(0));
        newOrder.set(new ExDestination(order.getSecurity().getSecurityFamily().getMarket().toString()));
    }

    @Override
    protected void handleModifyOrder(SimpleOrder order, OrderCancelReplaceRequest replaceRequest) {

        replaceRequest.set(new HandlInst('2'));
        replaceRequest.set(new CustomerOrFirm(0));
        replaceRequest.set(new ExDestination(order.getSecurity().getSecurityFamily().getMarket().toString()));
    }

    @Override
    protected void handleCancelOrder(SimpleOrder order, OrderCancelRequest cancelRequest) {

        // do nothing
    }

    @Override
    protected MarketChannel handleGetMarketChannel() {

        return MarketChannel.IB_FIX;
    }
}
