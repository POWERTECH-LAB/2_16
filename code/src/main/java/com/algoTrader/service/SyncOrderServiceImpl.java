package com.algoTrader.service;

import java.math.BigDecimal;

import org.apache.log4j.Logger;

import com.algoTrader.entity.Order;
import com.algoTrader.entity.OrderImpl;
import com.algoTrader.entity.Position;
import com.algoTrader.entity.Transaction;
import com.algoTrader.entity.TransactionImpl;
import com.algoTrader.entity.marketData.Tick;
import com.algoTrader.entity.security.Future;
import com.algoTrader.entity.security.Security;
import com.algoTrader.entity.security.StockOption;
import com.algoTrader.enumeration.Status;
import com.algoTrader.enumeration.TransactionType;
import com.algoTrader.stockOption.StockOptionUtil;
import com.algoTrader.util.ConfigurationUtil;
import com.algoTrader.util.DateUtil;
import com.algoTrader.util.RoundUtil;
import com.algoTrader.vo.OrderVO;

public abstract class SyncOrderServiceImpl extends SyncOrderServiceBase {

    private static boolean externalTransactionsEnabled = ConfigurationUtil.getBaseConfig().getBoolean("externalTransactionsEnabled");
    private static boolean simulation = ConfigurationUtil.getBaseConfig().getBoolean("simulation");

    //private static long eventsPerDay = ConfigurationUtil.getBaseConfig().getLong("simulation.eventsPerDay");

    @Override
    protected Order handleSendOrder(OrderVO orderVO) throws Exception {

        // construct a order-entity from the orderVO
        Order order = orderVOToEntity(orderVO);

        return sendOrder(order);
    }

    @Override
    protected Order handleSendOrder(Order order) throws Exception {

        if (order.getRequestedQuantity() <= 0) {
            throw new IllegalArgumentException("quantity must be greater than 0");
        }

        if (!Status.PREARRANGED.equals(order.getStatus())) {
            if (!simulation && externalTransactionsEnabled
                    && (TransactionType.BUY.equals(order.getTransactionType()) || TransactionType.SELL.equals(order.getTransactionType()))) {
                sendExternalOrder(order);
            } else {
                executeInternalTransaction(order);
            }
        }


        for (Transaction transaction : order.getTransactions()) {

            transaction.setStrategy(order.getStrategy());
            transaction.setSecurity(order.getSecurity());
            transaction.setType(order.getTransactionType());
            transaction.setCurrency(order.getSecurity().getSecurityFamily().getCurrency());

            getTransactionService().persistTransaction(transaction);
        }

        return order;
    }

    private void executeInternalTransaction(Order order) {

        Transaction transaction = new TransactionImpl();
        transaction.setDateTime(DateUtil.getCurrentEPTime());

        Security security = order.getSecurity();
        Position position = getPositionDao().findBySecurityAndStrategy(security.getId(), order.getStrategy().getName());
        Tick tick = security.getLastTick();

        if (tick == null) {
            throw new SyncOrderServiceException("no last tick available for security " + security);
        }

        // double currentValue = tick.getCurrentValueDouble();
        //
        // in daily / hourly / 30min / 15min simulation, if exitValue is reached during the day, take the exitValue (only valid for Theta)
        // instead of the currentValue! because we will have passed the exitValue in the meantime
        //        if (simulation && TransactionType.BUY.equals(order.getTransactionType()) && (eventsPerDay <= 33)) {
        //
        //            double exitValue = position.getExitValueDouble();
        //            if (currentValue > exitValue && DateUtil.compareToTime(security.getSecurityFamily().getMarketOpen()) > 0) {
        //
        //                logger.info("adjusted currentValue (" + currentValue + ") to exitValue (" + exitValue+ ") in closePosition for order on " + order.getSecurity().getSymbol());
        //                currentValue = exitValue;
        //            }
        //        }

        int contractSize = security.getSecurityFamily().getContractSize();
        int scale = security.getSecurityFamily().getScale();

        if (TransactionType.SELL.equals(order.getTransactionType())) {

            double bid = tick.getBid().doubleValue();

            transaction.setPrice(RoundUtil.getBigDecimal(bid, scale));
            transaction.setQuantity(-Math.abs(order.getRequestedQuantity()));

        } else if (TransactionType.BUY.equals(order.getTransactionType())) {

            double ask = tick.getAsk().doubleValue();

            transaction.setPrice(RoundUtil.getBigDecimal(ask, scale));
            transaction.setQuantity(Math.abs(order.getRequestedQuantity()));

        } else if (TransactionType.EXPIRATION.equals(order.getTransactionType())) {

            long quantity = -(int) Math.signum(position.getQuantity()) * Math.abs(order.getRequestedQuantity());
            transaction.setQuantity(quantity);

            if (security instanceof StockOption) {

                StockOption stockOption = (StockOption) security;
                double underlayingSpot = security.getUnderlaying().getLastTick().getCurrentValueDouble();
                double intrinsicValue = StockOptionUtil.getIntrinsicValue(stockOption, underlayingSpot);
                BigDecimal price = RoundUtil.getBigDecimal(intrinsicValue * contractSize, scale);
                transaction.setPrice(price);

            } else if (security instanceof Future) {

                BigDecimal price = security.getUnderlaying().getLastTick().getCurrentValue();
                transaction.setPrice(price);

            } else {
                throw new IllegalArgumentException("EXPIRATION only allowed for " + security.getClass().getName());
            }
        }

        if (TransactionType.SELL.equals(order.getTransactionType()) || TransactionType.BUY.equals(order.getTransactionType())) {

            if (security.getSecurityFamily().getCommission() == null) {
                throw new RuntimeException("commission is undefined for " + security.getSymbol());
            }

            double commission = Math.abs(order.getRequestedQuantity() * security.getSecurityFamily().getCommission().doubleValue());
            transaction.setCommission(RoundUtil.getBigDecimal(commission));
        } else {
            transaction.setCommission(new BigDecimal(0));
        }
        transaction.setNumber(null);

        order.setStatus(Status.AUTOMATIC);
        order.getTransactions().add(transaction);
    }

    protected double getPrice(Order order, double spreadPosition, double bid, double ask) {

        double price = 0.0;
        if (TransactionType.BUY.equals(order.getTransactionType())) {
            price = bid + spreadPosition * (ask - bid);
        } else if (TransactionType.SELL.equals(order.getTransactionType())) {
            price = ask - spreadPosition * (ask - bid);
        }

        double tickSize = order.getSecurity().getSecurityFamily().getTickSize();

        return RoundUtil.roundToNextN(price, tickSize);
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
}