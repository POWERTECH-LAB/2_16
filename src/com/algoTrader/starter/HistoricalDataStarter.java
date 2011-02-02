package com.algoTrader.starter;

import com.algoTrader.ServiceLocator;
import com.algoTrader.service.ib.IbHistoricalDataService;

public class HistoricalDataStarter {

    public static void main(String[] args) {

        String startDate = args[0];

        String endDate = args[1];

        String[] whatToShow = args[2].split(":");

        String[] securityIdStrings = args[3].split(":");
        int[] securityIds = new int[securityIdStrings.length];
        for (int i = 0; i < securityIdStrings.length; i++) {
            securityIds[i] = Integer.valueOf(securityIdStrings[i]);
        }

        IbHistoricalDataService service = ServiceLocator.serverInstance().getIbHistoricalDataService();

        service.init();

        service.requestHistoricalData(securityIds, whatToShow, startDate, endDate);

        ServiceLocator.serverInstance().shutdown();
    }
}
