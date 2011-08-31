package com.algoTrader.entity;

import java.util.Collection;

import com.algoTrader.enumeration.Status;

public class OrderImpl extends Order {

    private static final long serialVersionUID = -1384387263238787678L;

    public OrderImpl() {
        super();
        setStatus(Status.OPEN);
    }

    @Override
    public long getPartialOrderExecutedQuantity() {

        long executedQuantity = 0;
        Collection<PartialOrder> partialOrders = getPartialOrders();
        for (PartialOrder partialOrder : partialOrders) {
            executedQuantity += partialOrder.getExecutedQuantity();
        }
        return executedQuantity;
    }

    @Override
    public PartialOrder createPartialOrder() {

        PartialOrder partialOrder = new PartialOrderImpl();

        partialOrder.setStatus(Status.OPEN);

        // part of the order might already have gone through, so reduce the
        // requested numberOfContracts by this number
        partialOrder.setRequestedQuantity(getRequestedQuantity() - getPartialOrderExecutedQuantity());

        partialOrder.setParentOrder(this);
        getPartialOrders().add(partialOrder);
        setCurrentPartialOrder(partialOrder);

        return partialOrder;
    }

    @Override
    public String toString() {
        return String.valueOf(getNumber());
    }
}
