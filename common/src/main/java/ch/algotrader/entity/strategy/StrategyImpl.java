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
package ch.algotrader.entity.strategy;

/**
 * @author <a href="mailto:aflury@algotrader.ch">Andy Flury</a>
 */
public class StrategyImpl extends Strategy {

    public static final String SERVER = "SERVER";

    private static final long serialVersionUID = -2271735085273721632L;

    @Override
    public boolean isServer() {
        return (SERVER.equals(getName()));
    }

    @Override
    public String toString() {

        return getName();
    }

}
