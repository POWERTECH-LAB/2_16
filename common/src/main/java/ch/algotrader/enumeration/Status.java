/***********************************************************************************
 * AlgoTrader Enterprise Trading Framework
 *
 * Copyright (C) 2014 AlgoTrader GmbH - All rights reserved
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
 * Badenerstrasse 16
 * 8004 Zurich
 ***********************************************************************************/
package ch.algotrader.enumeration;

/**
 * Status of an Order
 */
public enum Status {

    /**
     * Order is not submitted yet
     */
    OPEN,

    /**
     * Order is submitted to and received by the external Broker.
     */
    SUBMITTED,

    /**
     * Partially executed Order.
     */
    PARTIALLY_EXECUTED,

    /**
     * Fully Executed Order.
     */
    EXECUTED,

    /**
     * Canceled Order.
     */
    CANCELED,

    /**
     * Rejected Order.
     */
    REJECTED;

    private static final long serialVersionUID = 6359963093170544955L;

}
