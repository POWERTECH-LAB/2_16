package com.algoTrader.entity.trade;

public class StopOrderImpl extends StopOrder {

    private static final long serialVersionUID = -9213820219309533525L;

    @Override
    public String toString() {

        return super.toString() + " stop: " + getStop();
    }
}
