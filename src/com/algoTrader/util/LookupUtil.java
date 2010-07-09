package com.algoTrader.util;

import com.algoTrader.ServiceLocator;
import com.algoTrader.entity.Security;
import com.algoTrader.entity.StockOption;

public class LookupUtil {

    public static Security[] getSecuritiesInPortfolio() {

        ServiceLocator serviceLocator = ServiceLocator.instance();
        return serviceLocator.getLookupService().getAllSecuritiesInPortfolio();
    }

    public static Security[] getDummySecuritiesOnWatchlist() {

        ServiceLocator serviceLocator = ServiceLocator.instance();
        return serviceLocator.getLookupService().getDummySecuritiesOnWatchlist();
    }

    public static StockOption[] getStockOptionsOnWatchlist() {

        ServiceLocator serviceLocator = ServiceLocator.instance();
        return serviceLocator.getLookupService().getStockOptionsOnWatchlist();
    }

    public static boolean hasStockOptionsOnWatchlist() {

        ServiceLocator serviceLocator = ServiceLocator.instance();
        return (serviceLocator.getLookupService().getStockOptionsOnWatchlist().length != 0);
    }

    public static boolean hasOpenPositions() {

        ServiceLocator serviceLocator = ServiceLocator.instance();
        return (serviceLocator.getLookupService().getOpenPositions().length != 0);
    }

    public static double getPortfolioValueAllAccounts() {

        ServiceLocator serviceLocator = ServiceLocator.instance();
        return serviceLocator.getLookupService().getPortfolioValueAllAccounts().doubleValue();
    }
}
