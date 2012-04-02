package com.algoTrader.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import com.algoTrader.entity.marketData.Tick;
import com.algoTrader.enumeration.Direction;
import com.algoTrader.util.DateUtil;

public class PositionImpl extends Position {

    private static final long serialVersionUID = -2679980079043322328L;

    @Override
    public boolean isOpen() {

        return getQuantity() != 0;
    }

    @Override
    public Direction getDirection() {

        if (getQuantity() < 0) {
            return Direction.SHORT;
        } else if (getQuantity() > 0) {
            return Direction.LONG;
        } else {
            return Direction.FLAT;
        }
    }

    /**
     * always positive
     */
    @Override
    public double getMarketPriceDouble() {

        if (isOpen()) {

            Tick tick = getSecurity().getLastTick();
            if (tick != null) {
                if (getQuantity() < 0) {

                    // short position
                    return tick.getAsk().doubleValue();
                } else {

                    // short position
                    return tick.getBid().doubleValue();
                }
            } else {
                return Double.NaN;
            }
        } else {
            return 0.0;
        }
    }

    @Override
    public double getMarketPriceBaseDouble() {

        return getMarketPriceDouble() * getSecurity().getFXRateBase();
    }

    /**
     * short positions: negative long positions: positive
     */
    @Override
    public double getMarketValueDouble() {

        if (isOpen()) {

            return getQuantity() * getSecurity().getSecurityFamily().getContractSize() * getMarketPriceDouble();
        } else {
            return 0.0;
        }
    }

    @Override
    public double getMarketValueBaseDouble() {

        return getMarketValueDouble() * getSecurity().getFXRateBase();
    }

    /**
     * always positive
     */
    @Override
    public double getAveragePriceDouble() {

        long totalQuantity = 0;
        double totalPrice = 0.0;

        List<QuantityTransaction> quantityTransactions = getFIFIQueue();
        for (QuantityTransaction queueTransaction : quantityTransactions) {

            Transaction transaction = queueTransaction.getTransaction();
            long quantity = queueTransaction.getQuantity();
            double pricePerContract = Math.abs(transaction.getNetValueDouble() / transaction.getQuantity());

            totalQuantity += quantity;
            totalPrice += quantity * pricePerContract;

        }
        return totalPrice / totalQuantity / getSecurity().getSecurityFamily().getContractSize();
    }

    @Override
    public double getAveragePriceBaseDouble() {

        return getAveragePriceDouble() * getSecurity().getFXRateBase();
    }

    /**
     * in days
     */
    @Override
    public double getAverageAge() {

        long totalQuantity = 0;
        long totalAge = 0;

        List<QuantityTransaction> quantityTransactions = getFIFIQueue();
        for (QuantityTransaction queueTransaction : quantityTransactions) {

            Transaction transaction = queueTransaction.getTransaction();
            long quantity = queueTransaction.getQuantity();
            long age = DateUtil.getCurrentEPTime().getTime() - transaction.getDateTime().getTime();

            totalQuantity += quantity;
            totalAge += quantity * age;

        }
        if (totalQuantity != 0) {
            return totalAge / totalQuantity / 86400000.0;
        } else {
            return Double.NaN;
        }
    }

    /**
     * short positions: negative long positions: positive
     */
    @Override
    public double getCostDouble() {

        if (isOpen()) {

            return getQuantity() * getSecurity().getSecurityFamily().getContractSize() * getAveragePriceDouble();
        } else {
            return 0.0;
        }
    }

    @Override
    public double getCostBaseDouble() {

        return getCostDouble() * getSecurity().getFXRateBase();
    }

    @Override
    public double getUnrealizedPLDouble() {
        if (isOpen()) {

            return getMarketValueDouble() - getCostDouble();
        } else {
            return 0.0;
        }
    }

    @Override
    public double getUnrealizedPLBaseDouble() {

        return getUnrealizedPLDouble() * getSecurity().getFXRateBase();
    }

    @Override
    public double getExitValueDouble() {

        if (getExitValue() != null) {
            return getExitValue().doubleValue();
        } else {
            return 0.0;
        }
    }

    @Override
    public double getExitValueDoubleBase() {

        return getExitValueDouble() * getSecurity().getFXRateBase();
    }

    @Override
    public double getMaintenanceMarginDouble() {

        if (isOpen() && getMaintenanceMargin() != null) {
            return getMaintenanceMargin().doubleValue();
        } else {
            return 0.0;
        }
    }

    @Override
    public double getMaintenanceMarginBaseDouble() {

        return getMaintenanceMarginDouble() * getSecurity().getFXRateBase();
    }

    @Override
    public double getRedemptionValueDouble() {

        if (isOpen() && getExitValue() != null) {

            return -getQuantity() * getSecurity().getSecurityFamily().getContractSize() * getExitValueDouble();
        } else {
            return 0.0;
        }
    }

    @Override
    public double getRedemptionValueBaseDouble() {

        return getRedemptionValueDouble() * getSecurity().getFXRateBase();
    }

    @Override
    public double getMaxLossDouble() {

        if (isOpen() && getExitValue() != null) {

            double maxLossPerItem;
            if (Direction.LONG.equals(getDirection())) {
                maxLossPerItem = getMarketPriceDouble() - getExitValueDouble();
            } else {
                maxLossPerItem = getExitValueDouble() - getMarketPriceDouble();
            }
            return -getQuantity() * getSecurity().getSecurityFamily().getContractSize() * maxLossPerItem;
        } else {
            return 0.0;
        }
    }

    @Override
    public double getMaxLossBaseDouble() {

        return getMaxLossDouble() * getSecurity().getFXRateBase();
    }

    @Override
    public double getExposure() {

        return getMarketValueDouble() * getSecurity().getLeverage();
    }

    @Override
    public String toString() {

        return getQuantity() + " " + getSecurity();
    }

    private List<QuantityTransaction> getFIFIQueue() {

        List<Transaction> transactions = new ArrayList<Transaction>(getTransactions());

        // sort by date ascending
        Collections.sort(transactions, new Comparator<Transaction>() {
            @Override
            public int compare(Transaction t1, Transaction t2) {
                return (t1.getDateTime().compareTo(t2.getDateTime()));
            }
        });

        List<QuantityTransaction> queue = new ArrayList<QuantityTransaction>();
        long totalQuantity = 0;
        for (Transaction transaction : transactions) {

            // if queue is empty or transaction increases existing position -> add transaction to queue
            if (queue.size() == 0 || Long.signum(totalQuantity) == Long.signum(transaction.getQuantity())) {
                queue.add(new QuantityTransaction(transaction.getQuantity(), transaction));

                // if transaction is reducing quantity -> go through the queue and remove as many items as necessary
            } else {
                long runningQuantity = transaction.getQuantity();
                for (Iterator<QuantityTransaction> it = queue.iterator(); it.hasNext();) {

                    QuantityTransaction queueTransaction = it.next();

                    // transaction will be completely removed
                    if (Math.abs(queueTransaction.getQuantity()) <= Math.abs(runningQuantity)) {
                        runningQuantity += queueTransaction.getQuantity();
                        it.remove();

                        // transaction will be partly removed
                    } else {
                        queueTransaction.setQuantity(queueTransaction.getQuantity() + runningQuantity);
                        runningQuantity = 0;
                        break;
                    }
                }

                // if not the entire runningQuantity could be eliminated,
                // create a new Quantity Transaction with the reminder
                if (runningQuantity != 0) {
                    queue.add(new QuantityTransaction(runningQuantity, transaction));
                }
            }
            totalQuantity += transaction.getQuantity();
        }

        return queue;
    }

    private static class QuantityTransaction {

        private long quantity;
        private Transaction transaction;

        public QuantityTransaction(long quantity, Transaction transaction) {
            super();
            this.quantity = quantity;
            this.transaction = transaction;
        }

        public long getQuantity() {
            return this.quantity;
        }

        public void setQuantity(long quantity) {
            this.quantity = quantity;
        }

        public Transaction getTransaction() {
            return this.transaction;
        }
    }
}
