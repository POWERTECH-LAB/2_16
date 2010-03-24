package com.algoTrader.subscriber;

import java.math.BigDecimal;

import com.algoTrader.ServiceLocator;

public class OpenPositionSubscriber {

    public void update(int securityId, BigDecimal settlement, BigDecimal currentValue, BigDecimal underlayingSpot) {

        ServiceLocator.instance().getActionService().openPosition(securityId, settlement, currentValue, underlayingSpot);
    }
}
