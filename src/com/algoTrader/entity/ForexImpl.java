package com.algoTrader.entity;

import com.algoTrader.enumeration.Currency;

public class ForexImpl extends Forex {

    private static final long serialVersionUID = -6204294412084812111L;

    public Currency getTransactionCurrency() {

        return getSecurityFamily().getCurrency();
    }
}
