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
package ch.algotrader.vo;

import ch.algotrader.enumeration.Direction;

/**
 * A ValueObject representing an opening of a {@link ch.algotrader.entity.Position Position}
 */
public class OpenPositionVO extends PositionMutationVO {

    private static final long serialVersionUID = -1764567855247856700L;

    public OpenPositionVO(final long id, final long securityId, final String strategy, final long quantity, final Direction direction) {

        super(id, securityId, strategy, quantity, direction);
    }

    /**
     * Copies constructor from other OpenPositionVO
     *
     * @param otherBean Cannot be <code>null</code>
     * @throws NullPointerException if the argument is <code>null</code>
     */
    public OpenPositionVO(final OpenPositionVO otherBean) {

        super(otherBean);
    }

    @Override
    public String toString() {

        return super.toString();
    }

}
