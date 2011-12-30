package com.algoTrader.service;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;

import com.algoTrader.ServiceLocator;
import com.algoTrader.entity.Position;
import com.algoTrader.entity.PositionImpl;
import com.algoTrader.entity.Strategy;
import com.algoTrader.entity.StrategyImpl;
import com.algoTrader.entity.Transaction;
import com.algoTrader.entity.TransactionImpl;
import com.algoTrader.entity.security.Security;
import com.algoTrader.entity.trade.Fill;
import com.algoTrader.enumeration.Direction;
import com.algoTrader.enumeration.Side;
import com.algoTrader.enumeration.TransactionType;
import com.algoTrader.util.HibernateUtil;
import com.algoTrader.util.MyLogger;
import com.algoTrader.util.RoundUtil;
import com.algoTrader.vo.ClosePositionVO;
import com.algoTrader.vo.TradePerformanceVO;

public abstract class TransactionServiceImpl extends TransactionServiceBase {

    private static Logger logger = MyLogger.getLogger(TransactionServiceImpl.class.getName());
    private static Logger mailLogger = MyLogger.getLogger(TransactionServiceImpl.class.getName() + ".MAIL");
    private static Logger simulationLogger = MyLogger.getLogger(SimulationServiceImpl.class.getName() + ".RESULT");

    private @Value("${simulation}") boolean simulation;
    private @Value("${simulation.logTransactions}") boolean logTransactions;

    @Override
    protected void handleCreateTransaction(Fill fill) throws Exception {

        Strategy strategy = fill.getParentOrder().getStrategy();
        Security security = fill.getParentOrder().getSecurity();

        // initialize the security & strategy
        HibernateUtil.lock(this.getSessionFactory(), security);
        HibernateUtil.lock(this.getSessionFactory(), strategy);

        TransactionType transactionType = Side.BUY.equals(fill.getSide()) ? TransactionType.BUY : TransactionType.SELL;
        long quantity = Side.BUY.equals(fill.getSide()) ? fill.getQuantity() : -fill.getQuantity();
        BigDecimal commission = RoundUtil.getBigDecimal(Math.abs(quantity * security.getSecurityFamily().getCommission().doubleValue()));

        Transaction transaction = new TransactionImpl();
        transaction.setDateTime(fill.getDateTime());
        transaction.setNumber(fill.getNumber());
        transaction.setQuantity(quantity);
        transaction.setPrice(fill.getPrice());
        transaction.setType(transactionType);
        transaction.setSecurity(security);
        transaction.setStrategy(strategy);
        transaction.setCurrency(security.getSecurityFamily().getCurrency());
        transaction.setCommission(commission);

        fill.setTransaction(transaction);

        persistTransaction(transaction);
        propagateTransaction(transaction);
    }

    @Override
    protected void handlePersistTransaction(Transaction transaction) throws Exception {

        Strategy strategy = transaction.getStrategy();
        Security security = transaction.getSecurity();

        strategy.getTransactions().add(transaction);

        double profit = 0.0;
        double profitPct = 0.0;
        double avgAge = 0;

        // create a new position if necessary
        Position position = getPositionDao().findBySecurityAndStrategy(security.getId(), strategy.getName());
        if (position == null) {

            position = new PositionImpl();
            position.setQuantity(transaction.getQuantity());

            position.setExitValue(null);
            position.setMaintenanceMargin(null);
            position.setProfitTarget(null);

            position.setSecurity(security);
            security.getPositions().add(position);

            position.getTransactions().add(transaction);
            transaction.setPosition(position);

            position.setStrategy(strategy);
            strategy.getPositions().add(position);

            getPositionDao().create(position);

        } else {

            // get the closePositionVO
            // must be done before closing the position
            ClosePositionVO closePositionVO = getPositionDao().toClosePositionVO(position);

            // evaluate the profit in closing transactions
            // must be done before attaching the new transaction
            if (Long.signum(position.getQuantity()) * Long.signum(transaction.getQuantity()) == -1) {
                double cost = position.getCostDouble() * Math.abs((double) transaction.getQuantity() / (double) position.getQuantity());
                double value = transaction.getNetValueDouble();
                profit = value - cost;
                profitPct = Direction.LONG.equals(position.getDirection()) ? ((value - cost) / cost) : ((cost - value) / cost);
                avgAge = position.getAverageAge();
            }

            position.setQuantity(position.getQuantity() + transaction.getQuantity());

            // in case a position was closed do the following
            if (!position.isOpen()) {

                // set all values to null
                position.setExitValue(null);
                position.setMaintenanceMargin(null);
                position.setProfitTarget(null);

                // propagate the ClosePosition event
                getRuleService().routeEvent(position.getStrategy().getName(), closePositionVO);
            }

            position.getTransactions().add(transaction);
            transaction.setPosition(position);

            getPositionDao().update(position);
        }

        // add the amount to the corresponding cashBalance
        getCashBalanceService().addAmount(transaction);

        // update all entities
        getTransactionDao().create(transaction);
        getStrategyDao().update(strategy);
        getSecurityDao().update(security);

        // create a TradePerformanceVO and send it into Esper
        if (profit != 0.0) {
            TradePerformanceVO tradePerformance = new TradePerformanceVO();
            tradePerformance.setProfit(profit);
            tradePerformance.setProfitPct(profitPct);
            tradePerformance.setAvgAge(avgAge);
            tradePerformance.setWinning(profit > 0);

            getRuleService().routeEvent(StrategyImpl.BASE, tradePerformance);
        }

        //@formatter:off
        String logMessage = "executed transaction type: " + transaction.getType() +
        " quantity: " + transaction.getQuantity() +
        " of " + security.getSymbol() +
        " price: " + transaction.getPrice() +
        " commission: " + transaction.getCommission() +
        ((profit != 0.0) ? (
            " profit: " + RoundUtil.getBigDecimal(profit) +
            " profitPct: " + RoundUtil.getBigDecimal(profitPct) +
            " avgAge: " + RoundUtil.getBigDecimal(avgAge))
            : "");
        //@formatter:on

        if (this.simulation && this.logTransactions) {
            simulationLogger.info(logMessage);
        } else {
            logger.info(logMessage);
        }

    }

    @Override
    protected void handlePropagateTransaction(Transaction transaction) {

        // propagate the transaction to the corresponding strategy
        if (!StrategyImpl.BASE.equals(transaction.getStrategy().getName())) {
            getRuleService().routeEvent(transaction.getStrategy().getName(), transaction);
        }
    }

    @Override
    protected void handlePropagateFill(Fill fill) throws Exception {

        // send the fill to the strategy that placed the corresponding order
        if (!StrategyImpl.BASE.equals(fill.getParentOrder().getStrategy().getName())) {
            getRuleService().routeEvent(fill.getParentOrder().getStrategy().getName(), fill);
        }

        if (!this.simulation) {
            logger.debug("propagated fill: " + fill);
        }
    }

    @Override
    protected void handleLogTransactionSummary(Set<Transaction> transactions) throws Exception {

        if (transactions.size() > 0 && !this.simulation) {

            long totalQuantity = 0;
            double totalPrice = 0.0;
            double totalCommission = 0.0;

            for (Transaction transaction : transactions) {

                totalQuantity += transaction.getQuantity();
                totalPrice += transaction.getPrice().doubleValue() * transaction.getQuantity();
                totalCommission += transaction.getCommission().doubleValue();
            }

            Transaction transaction = transactions.iterator().next();
            Strategy strategy = transaction.getStrategy();
            Security security = transaction.getSecurity();

            // initialize the security & strategy
            HibernateUtil.lock(this.getSessionFactory(), security);
            HibernateUtil.lock(this.getSessionFactory(), strategy);

            //@formatter:off
            mailLogger.info("executed transaction type: " + transaction.getType() +
                    " totalQuantity: " + totalQuantity +
                    " of " + security.getSymbol() +
                    " avgPrice: " + RoundUtil.getBigDecimal(totalPrice / totalQuantity) +
                    " commission: " + totalCommission +
                    " netLiqValue: " + strategy.getNetLiqValue());
            //@formatter:on
        }
    }

    @Override
    protected void handleSetCommission(String transactionNumber, double commissionDouble) throws Exception {

        Transaction transaction = getTransactionDao().findByNumber(transactionNumber);

        BigDecimal commission = RoundUtil.getBigDecimal(commissionDouble);
        transaction.setCommission(commission);

        getTransactionDao().update(transaction);

        logger.debug("set commission of transaction " + transaction.getId() + " to " + commission);
    }

    public static class LogTransactionSummarySubscriber {

        public void update(Map<?, ?>[] insertStream, Map<?, ?>[] removeStream) {

            Set<Transaction> transactions = new HashSet<Transaction>();
            for (Map<?, ?> element : insertStream) {
                Fill fill = (Fill) element.get("fill");
                transactions.add(fill.getTransaction());
            }

            ServiceLocator.instance().getTransactionService().logTransactionSummary(transactions);
        }
    }

    public static class SetCommissionSubscriber {

        public void update(Map<?, ?>[] insertStream, Map<?, ?>[] removeStream) {

            int maxCumQty = 0;
            for (Map<?, ?> element : insertStream) {
                Integer cumQty = (Integer) element.get("cumQty");
                maxCumQty = Math.max(maxCumQty, cumQty);
            }

            for (Map<?, ?> element : insertStream) {

                String transactionNumber = (String) element.get("transactionNumber");
                Integer qty = (Integer) element.get("qty");
                Double commission = (Double) element.get("commission");

                double transactionCommission = commission * qty / maxCumQty;

                ServiceLocator.instance().getTransactionService().setCommission(transactionNumber, transactionCommission);
            }

        }
    }
};
