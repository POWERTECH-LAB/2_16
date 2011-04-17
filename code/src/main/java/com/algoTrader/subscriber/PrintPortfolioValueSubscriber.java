package com.algoTrader.subscriber;

import org.apache.log4j.Logger;

import com.algoTrader.entity.Transaction;
import com.algoTrader.util.ConfigurationUtil;
import com.algoTrader.util.MyLogger;
import com.algoTrader.util.RoundUtil;
import com.algoTrader.vo.PortfolioValueVO;

public class PrintPortfolioValueSubscriber {

    private static Logger logger = MyLogger.getLogger(PrintPortfolioValueSubscriber.class.getName());

    private static long initialBalance = ConfigurationUtil.getBaseConfig().getLong("simulation.initialBalance");

    private static boolean initialized = false;

    public void update(long timestamp, PortfolioValueVO portfolioValue, Transaction transaction) {

        // dont log anything while initialising macd
        if (portfolioValue.getNetLiqValue() != initialBalance) {
            initialized = true;
        }

        if (initialized) {
            logger.debug(RoundUtil.getBigDecimal(portfolioValue.getCashBalance()) + "," +
                        RoundUtil.getBigDecimal(portfolioValue.getSecuritiesCurrentValue()) + "," +
                        RoundUtil.getBigDecimal(portfolioValue.getMaintenanceMargin()) + "," +
                        RoundUtil.getBigDecimal(portfolioValue.getLeverage())
                        + ((transaction != null) ? ("," + transaction.getValue()) : ""));
        }
    }
}
