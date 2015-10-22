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
package ch.algotrader.adapter.fix;

import ch.algotrader.entity.Account;
import ch.algotrader.entity.trade.Order;
import quickfix.Message;

/**
 * Main entry point to Fix sessions.
 *
 * @author <a href="mailto:aflury@algotrader.ch">Andy Flury</a>
 */
public interface FixAdapter {

    /**
     * creates an individual session
     */
    void createSessionForService(String orderServiceType) throws FixApplicationException;

    /**
     * Makes sure there is an existing session with the given qualifier. New session will be created
     * if there is no session with the given qualifier.
     */
    void openSessionForService(String orderServiceType) throws FixApplicationException;

    /**
     * creates an individual session with the given qualifier
     */
    void createSession(String sessionQualifier) throws FixApplicationException;

    /**
     * Makes sure there is an existing session with the given qualifier. New session will be created
     * if there is no session with the given qualifier.
     */
    void openSession(String sessionQualifier) throws FixApplicationException;

    /**
     * sends a message to the designated session for the given account
     */
    void sendMessage(Message message, Account account) throws FixApplicationException;

    /**
     * sends a message to the designated session with the given qualifier.
     */
    void sendMessage(Message message, String sessionQualifier) throws FixApplicationException;

    /**
     * Gets the next {@code orderId} for the specified {@code account}
     */
    String getNextOrderId(Account account);

    /**
     * Gets the next {@code orderIdVersion} based on the specified {@code order}
     */
    String getNextOrderIdVersion(Order order);

    /**
     * Sets the current order count for the given session qualifier.
     */
    void setOrderId(String sessionQualifier, int orderId);

}
