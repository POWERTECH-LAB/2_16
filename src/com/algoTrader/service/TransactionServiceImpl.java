package com.algoTrader.service;

import java.math.BigDecimal;
import java.util.Collection;

import org.apache.log4j.Logger;

import com.algoTrader.ServiceLocator;
import com.algoTrader.entity.Order;
import com.algoTrader.entity.OrderImpl;
import com.algoTrader.entity.Position;
import com.algoTrader.entity.PositionImpl;
import com.algoTrader.entity.Security;
import com.algoTrader.entity.StockOption;
import com.algoTrader.entity.Strategy;
import com.algoTrader.entity.Tick;
import com.algoTrader.entity.Transaction;
import com.algoTrader.entity.TransactionImpl;
import com.algoTrader.enumeration.OrderStatus;
import com.algoTrader.enumeration.TransactionType;
import com.algoTrader.stockOption.StockOptionUtil;
import com.algoTrader.util.ConfigurationUtil;
import com.algoTrader.util.DateUtil;
import com.algoTrader.util.MyLogger;
import com.algoTrader.util.RoundUtil;
import com.algoTrader.vo.OrderVO;

public abstract class TransactionServiceImpl extends TransactionServiceBase {

    private static Logger logger = MyLogger.getLogger(TransactionServiceImpl.class.getName());
    private static Logger mailLogger = MyLogger.getLogger(TransactionServiceImpl.class.getName() + ".TransactionMail");
    private static Logger simulationLogger = MyLogger.getLogger(SimulationServiceImpl.class.getName());

    private static boolean externalTransactionsEnabled = ConfigurationUtil.getBaseConfig().getBoolean("externalTransactionsEnabled");
    private static boolean simulation = ConfigurationUtil.getBaseConfig().getBoolean("simulation");
    private static boolean logTransactions = ConfigurationUtil.getBaseConfig().getBoolean("simulation.logTransactions");
    private static long eventsPerDay = ConfigurationUtil.getBaseConfig().getLong("simulation.eventsPerDay");

    @SuppressWarnings("unchecked")
    protected Order handleExecuteTransaction(String strategyName, OrderVO orderVO) throws Exception {

        Strategy strategy = getStrategyDao().findByName(strategyName);

        // construct a order-entity from the orderVO
        Order order = orderVOToEntity(orderVO);

        Security security = order.getSecurity();
        TransactionType transactionType = order.getTransactionType();
        long requestedQuantity = order.getRequestedQuantity();

        if (requestedQuantity <= 0) {
            throw new IllegalArgumentException("quantity must be greater than 0");
        }

        if (!OrderStatus.PREARRANGED.equals(order.getStatus())) {
            if (!simulation && externalTransactionsEnabled &&
                    (TransactionType.BUY.equals(transactionType) || TransactionType.SELL.equals(transactionType))) {
                executeExternalTransaction(order);
            } else {
                executeInternalTransaction(order);
            }
        }

        Collection<Transaction> transactions = order.getTransactions();
        long totalQuantity = 0;
        double totalPrice = 0.0;
        double totalCommission = 0.0;
        double totalProfit = 0.0;
        double profit = 0.0;
        double profitPct = 0.0;
        double avgAge = 0;

        for (Transaction transaction : transactions) {

            transaction.setType(transactionType);
            transaction.setSecurity(security);
            transaction.setCurrency(security.getSecurityFamily().getCurrency());

            // Strategy
            transaction.setStrategy(strategy);
            strategy.getTransactions().add(transaction);

            // Position
            Position position = getPositionDao().findBySecurityAndStrategy(security.getId(), strategyName);
            if (position == null) {

                position = new PositionImpl();
                position.setQuantity(transaction.getQuantity());

                position.setExitValue(null);
                position.setMaintenanceMargin(null);

                position.setSecurity(security);
                security.getPositions().add(position);

                position.getTransactions().add(transaction);
                transaction.setPosition(position);

                position.setStrategy(strategy);
                strategy.getPositions().add(position);

                getPositionDao().create(position);

            } else {

                // evaluate the profit in closing transactions
                // must get this before attaching the new transaction
                if (Long.signum(position.getQuantity()) * Long.signum(transaction.getQuantity()) == -1) {
                    double cost = position.getCostDouble() * Math.abs((double) transaction.getQuantity() / (double) position.getQuantity());
                    double value = transaction.getValueDouble();
                    profit = value - cost;
                    profitPct = position.isLong() ? ((value - cost) / cost) : ((cost - value) / cost);
                    avgAge = position.getAverageAge();
                }

                position.setQuantity(position.getQuantity() + transaction.getQuantity());

                if (!position.isOpen()) {
                    position.setExitValue(null);
                    position.setMaintenanceMargin(null);
                }

                position.getTransactions().add(transaction);
                transaction.setPosition(position);

                getPositionDao().update(position);
            }

            getTransactionDao().create(transaction);
            getStrategyDao().update(strategy);
            getSecurityDao().update(security);

            totalQuantity += transaction.getQuantity();
            totalPrice += transaction.getPrice().doubleValue() * transaction.getQuantity();
            totalCommission += transaction.getCommission().doubleValue();
            totalProfit += profit;

            String logMessage = "executed transaction type: " + transactionType + " quantity: " + transaction.getQuantity() +
                    " of " + security.getSymbol() + " price: " + transaction.getPrice() + " commission: " + transaction.getCommission() +
                    ((profit != 0.0) ? (" profit: " + RoundUtil.getBigDecimal(profit) + " profitPct: " + RoundUtil.getBigDecimal(profitPct)
                    + " avgAge: " + RoundUtil.getBigDecimal(avgAge)) : "");

            if (simulation && logTransactions) {
                simulationLogger.info(logMessage);
            } else {
                logger.info(logMessage);
            }
        }

        if (order.getTransactions().size() > 0 && !simulation) {
            mailLogger.info("executed transaction type: " + transactionType + " totalQuantity: " + totalQuantity +
                    " of " + security.getSymbol() + " avgPrice: " + RoundUtil.getBigDecimal(totalPrice / totalQuantity) +
                    " commission: " + totalCommission + " netLiqValue: " + strategy.getNetLiqValue() +
                    ((totalProfit != 0) ? (" profit: " + RoundUtil.getBigDecimal(totalProfit)) : ""));

        }
        return order;
    }

    @SuppressWarnings("unchecked")
    private void executeInternalTransaction(Order order) {

        Transaction transaction = new TransactionImpl();
        transaction.setDateTime(DateUtil.getCurrentEPTime());

        Security security = order.getSecurity();
        Tick tick = security.getLastTick();
        double currentValue = tick.getCurrentValueDouble();

        // in daily / hourly / 30min / 15min simulation, if exitValue is reached during the day, take the exitValue
        // instead of the currentValue! because we will have passed the exitValue in the meantime
        if (simulation && TransactionType.BUY.equals(order.getTransactionType()) && (eventsPerDay <= 33)) {

            double exitValue = getPositionDao().findBySecurityAndStrategy(security.getId(), order.getStrategy().getName()).getExitValue().doubleValue();
            if (currentValue > exitValue && DateUtil.compareToTime(security.getSecurityFamily().getMarketOpen()) > 0) {

                logger.info("adjusted currentValue (" + currentValue + ") to exitValue (" + exitValue+ ") in closePosition for order on " + order.getSecurity().getSymbol());
                currentValue = exitValue;
            }
        }

        int contractSize = security.getSecurityFamily().getContractSize();

        if (TransactionType.SELL.equals(order.getTransactionType())) {

            double bid = tick.getBid().doubleValue();

            transaction.setPrice(RoundUtil.getBigDecimal(bid));
            transaction.setQuantity(-Math.abs(order.getRequestedQuantity()));

        } else if (TransactionType.BUY.equals(order.getTransactionType())) {

            double ask = tick.getAsk().doubleValue();

            transaction.setPrice(RoundUtil.getBigDecimal(ask));
            transaction.setQuantity(Math.abs(order.getRequestedQuantity()));

        } else if (TransactionType.EXPIRATION.equals(order.getTransactionType())) {

            if (security instanceof StockOption) {

                StockOption stockOption = (StockOption) security;
                double underlayingSpot = security.getUnderlaying().getLastTick().getCurrentValueDouble();
                double intrinsicValue = StockOptionUtil.getIntrinsicValue(stockOption, underlayingSpot);
                BigDecimal price = RoundUtil.getBigDecimal(intrinsicValue * contractSize);
                transaction.setPrice(price);
                transaction.setQuantity(Math.abs(order.getRequestedQuantity()));

            } else {
                throw new IllegalArgumentException("EXPIRATION only allowed for StockOptions");
            }
        }

        if (TransactionType.SELL.equals(order.getTransactionType()) || TransactionType.BUY.equals(order.getTransactionType())) {

            if(security.getSecurityFamily().getCommission() == null) {
                throw new RuntimeException("commission is undefined for " + security.getSymbol());
            }

            double commission = Math.abs(order.getRequestedQuantity() * security.getSecurityFamily().getCommission().doubleValue());
            transaction.setCommission(RoundUtil.getBigDecimal(commission));
        } else {
            transaction.setCommission(new BigDecimal(0));
        }
        transaction.setNumber(null);

        order.setStatus(OrderStatus.AUTOMATIC);
        order.getTransactions().add(transaction);
    }

    protected double getPrice(Order order, double spreadPosition, double bid, double ask) {

        double price = 0.0;
        if (TransactionType.BUY.equals(order.getTransactionType())) {
            price = bid + spreadPosition * (ask - bid);
        } else if (TransactionType.SELL.equals(order.getTransactionType())) {
            price = ask - spreadPosition * (ask - bid);
        }

        return RoundUtil.roundTo10Cent(RoundUtil.getBigDecimal(price)).doubleValue();
    }

    /**
     * implemented here because Order is nonPersistent
     */
    private Order orderVOToEntity(OrderVO orderVO) {

        Order order = new OrderImpl();
        order.setStrategy(getStrategyDao().findByName(orderVO.getStrategyName()));
        order.setSecurity(getSecurityDao().load(orderVO.getSecurityId()));
        order.setRequestedQuantity(orderVO.getRequestedQuantity());
        order.setTransactionType(orderVO.getTransactionType());

        return order;
    }

    public static class RerunOrderSubscriber {

        public void update(String strategyName, OrderVO orderVO) {

            long startTime = System.currentTimeMillis();
            logger.debug("retrieveTicks start");

            ServiceLocator.serverInstance().getDispatcherService().getTransactionService().executeTransaction(strategyName, orderVO);

            logger.debug("retrieveTicks end (" + (System.currentTimeMillis() - startTime) + "ms execution)");
        }
    }
}
