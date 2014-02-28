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
package ch.algotrader.util;

/**
 * Provides utility methods to calculate performance Draw Down Period.
 *
 * Used by module-performance.epl.
 *
 * @author <a href="mailto:aflury@algotrader.ch">Andy Flury</a>
 *
 * @version $Revision$ $Date$
 */
public class DrawDownUtil {

    private static long drawDownPeriod;

    public static long resetDrawDownPeriod() {

        return drawDownPeriod = 0;
    }

    public static long increaseDrawDownPeriod(long milliseconds) {

        drawDownPeriod += milliseconds;

        return drawDownPeriod;
    }
}
