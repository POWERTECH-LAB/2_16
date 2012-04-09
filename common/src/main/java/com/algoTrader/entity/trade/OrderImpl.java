package com.algoTrader.entity.trade;

import org.apache.commons.lang.ClassUtils;

public abstract class OrderImpl extends Order {

    private static final long serialVersionUID = -6501807818853981164L;

    @Override
    public String toString() {

        //@formatter:off
        return getSide() + " " +
                getQuantity() + " " +
                ClassUtils.getShortClassName(this.getClass()) + " " +
                getSecurity();
        //@formatter:on
    }

    @Override
    public void setQuantity(long quantityIn) {

        // always set a positive quantity
        super.setQuantity(Math.abs(quantityIn));
    }
}
