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
package ch.algotrader.service;

import ch.algotrader.entity.Position;

/**
 * @author <a href="mailto:aflury@algotrader.ch">Andy Flury</a>
 */
public interface PositionService {

    /**
     * Closes all Positions of the specified Strategy and unsubscribes the corresponding Security if
     * {@code unsubscribe} is set to true.
     */
    public void closeAllPositionsByStrategy(String strategyName, boolean unsubscribe);

    /**
     * Closes the specified Position and unsubscribes the corresponding Security if {@code
     * unsubscribe} is set to true.
     */
    public void closePosition(long positionId, boolean unsubscribe);

    /**
     * Creates a Position based on a non-tradeable Security (e.g. {@link
     * ch.algotrader.entity.security.Combination Combination})
     */
    public Position createNonTradeablePosition(String strategyName, long securityId, long quantity);

    /**
     * Modifies a Position that is based on a non-tradeable Security (e.g. {@link
     * ch.algotrader.entity.security.Combination Combination})
     */
    public Position modifyNonTradeablePosition(long positionId, long quantity);

    /**
     * Deletes a Position that is based on a non-tradeable Security (e.g. {@link
     * ch.algotrader.entity.security.Combination Combination})
     */
    public void deleteNonTradeablePosition(long positionId, boolean unsubscribe);

    /**
     * Reduces the specified Position by the specified {@code quantity}
     */
    public void reducePosition(long positionId, long quantity);

    /**
     * Transfers a Position to another Strategy.
     */
    public void transferPosition(long positionId, String targetStrategyName);

    /**
     * Expires all expirable Positions. Only Positions on Securities that have an {@code
     * expirationDate} in the past will be expired.
     */
    public void expirePositions();

    /**
     * Calculates all Position {@code quantities} based on Transactions in the database and makes
     * adjustments if necessary.
     */
    public String resetPositions();

}
