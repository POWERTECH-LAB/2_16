/***********************************************************************************
 * AlgoTrader Enterprise Trading Framework
 *
 * Copyright (C) 2013 Flury Trading - All rights reserved
 *
 * All information contained herein is, and remains the property of Flury Trading.
 * The intellectual and technical concepts contained herein are proprietary to
 * Flury Trading. Modification, translation, reverse engineering, decompilation,
 * disassembly or reproduction of this material is strictly forbidden unless prior
 * written permission is obtained from Flury Trading
 *
 * Fur detailed terms and conditions consult the file LICENSE.txt or contact
 *
 * Flury Trading
 * Badenerstrasse 16
 * 8004 Zurich
 ***********************************************************************************/
package com.algoTrader.entity.strategy;

import com.algoTrader.entity.strategy.Strategy;


/**
 * @author <a href="mailto:andyflury@gmail.com">Andy Flury</a>
 *
 * @version $Revision$ $Date$
 */
public class StrategyImpl extends Strategy {

    public static final String BASE = "BASE";

    private static final long serialVersionUID = -2271735085273721632L;

    @Override
    public boolean isBase() {
        return (BASE.equals(getName()));
    }

    @Override
    public String toString() {

        return getName();
    }
}