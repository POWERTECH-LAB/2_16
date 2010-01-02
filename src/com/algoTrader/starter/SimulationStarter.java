package com.algoTrader.starter;

import com.algoTrader.ServiceLocator;

public class SimulationStarter {

    public static void main(String[] args) {

        long startTime = Long.parseLong(args[0]);

        ServiceLocator.instance().getRuleService().activateAll();
        ServiceLocator.instance().getSimulationService().simulateWatchlist(startTime);
    }
}
