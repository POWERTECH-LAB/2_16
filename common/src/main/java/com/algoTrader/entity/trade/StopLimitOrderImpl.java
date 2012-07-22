package com.algoTrader.entity.trade;

public class StopLimitOrderImpl extends StopLimitOrder {

    private static final long serialVersionUID = -6796363895406178181L;

    @Override
    public String toString() {

        return super.toString() + " stop: " + getStop() + " limit: " + getLimit();
    }
}
