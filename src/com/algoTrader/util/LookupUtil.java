package com.algoTrader.util;

import com.algoTrader.ServiceLocator;
import com.algoTrader.entity.Position;
import com.algoTrader.entity.Security;
import com.algoTrader.entity.StockOption;
import com.algoTrader.entity.Strategy;
import com.algoTrader.vo.PortfolioValueVO;

public class LookupUtil {

    public static Security[] getSecuritiesInPortfolio() {

        return ServiceLocator.commonInstance().getLookupService().getAllSecuritiesInPortfolio();
    }

    public static StockOption[] getStockOptionsOnWatchlist() {

        return ServiceLocator.commonInstance().getLookupService().getStockOptionsOnWatchlist();
    }

    public static Security getSecurityByIsin(String isin) {

        return ServiceLocator.commonInstance().getLookupService().getSecurityByIsin(isin);
    }

    @SuppressWarnings("unchecked")
    public static Position[] getPositions(Security security) {

        return (Position[]) security.getPositions().toArray(new Position[0]);
    }

    public static Position[] getOpenPositions() {

        return ServiceLocator.commonInstance().getLookupService().getOpenPositions();
    }

    public static Position[] getBullishPositionsByStrategy(String strategyName) {

        return ServiceLocator.commonInstance().getLookupService().getBullishPositionsByStrategy(strategyName);
    }

    public static Position[] getBearishPositionsByStrategy(String strategyName) {

        return ServiceLocator.commonInstance().getLookupService().getBearishPositionsByStrategy(strategyName);
    }

    public static Strategy[] getAllStrategies() {

        return ServiceLocator.commonInstance().getLookupService().getAllStrategies();
    }

    public static boolean hasOpenPositions() {

        return (ServiceLocator.commonInstance().getLookupService().getOpenPositions().length != 0);
    }

    public static PortfolioValueVO getPortfolioValue() {

        return ServiceLocator.commonInstance().getLookupService().getPortfolioValue();
    }

    public static boolean hasLastTicks() {

        return (ServiceLocator.commonInstance().getRuleService().getLastEvent(StrategyUtil.getStartedStrategyName(), "GET_LAST_TICK") != null);
    }
}
