package com.algoTrader.entity;

import java.math.BigDecimal;

import com.algoTrader.enumeration.TransactionType;
import com.algoTrader.util.RoundUtil;

public class TransactionImpl extends Transaction {

    private static final long serialVersionUID = -1528408715199422753L;

    private Double value = null; // cache getValueDouble because getValue get's called very often

    public BigDecimal getValue() {

        return RoundUtil.getBigDecimal(getValueDouble());
    }

    public double getValueDouble() {

        if (value == null) {
            if (getType().equals(TransactionType.BUY) ||
                    getType().equals(TransactionType.SELL) ||
                    getType().equals(TransactionType.EXPIRATION)) {
                value = -getPrice().doubleValue() * (double) getQuantity() - getCommission().doubleValue();
            } else if (getType().equals(TransactionType.CREDIT) ||
                    getType().equals(TransactionType.INTREST)) {
                value = getPrice().doubleValue();
            } else if (getType().equals(TransactionType.DEBIT) ||
                    getType().equals(TransactionType.FEES)) {
                value = -getPrice().doubleValue();
            } else {
                throw new IllegalArgumentException("unsupported transactionType: " + getType());
            }
        }
        return value;
    }
}
