/***********************************************************************************
 * AlgoTrader Enterprise Trading Framework
 *
 * Copyright (C) 2015 AlgoTrader GmbH - All rights reserved
 *
 * All information contained herein is, and remains the property of AlgoTrader GmbH.
 * The intellectual and technical concepts contained herein are proprietary to
 * AlgoTrader GmbH. Modification, translation, reverse engineering, decompilation,
 * disassembly or reproduction of this material is strictly forbidden unless prior
 * written permission is obtained from AlgoTrader GmbH
 *
 * Fur detailed terms and conditions consult the file LICENSE.txt or contact
 *
 * AlgoTrader GmbH
 * Aeschstrasse 6
 * 8834 Schindellegi
 ***********************************************************************************/
package ch.algotrader.entity.trade;

/**
 * A general Exception that is thrown if an Order violates a certain restriction.
 */
public class OrderValidationException extends Exception {

    private static final long serialVersionUID = 4945666166653479421L;

    public OrderValidationException() {
        super();
    }

    public OrderValidationException(Throwable throwable) {
        super(throwable);
    }

    public OrderValidationException(String messageIn) {
        super(messageIn);
    }

    public OrderValidationException(String messageIn, Throwable throwable) {
        super(messageIn, throwable);
    }

}