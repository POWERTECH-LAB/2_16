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
package ch.algorader.esper.callback;

import org.apache.log4j.Logger;

import ch.algorader.esper.EsperManager;
import ch.algorader.util.MyLogger;
import ch.algorader.util.metric.MetricsUtil;

import com.algoTrader.vo.OpenPositionVO;

/**
 * Base Esper Callback Class that will be invoked as soon as a new Position on the given Security passed to {@link EsperManager#addOpenPositionCallback} has been opened.
 *
 * @author <a href="mailto:andyflury@gmail.com">Andy Flury</a>
 *
 * @version $Revision$ $Date$
 */
public abstract class OpenPositionCallback {

    private static Logger logger = MyLogger.getLogger(OpenPositionCallback.class.getName());

    /**
     * Called by the "ON_OPEN_POSITION" statement. Should not be invoked directly.
     */
    public void update(OpenPositionVO positionVO) throws Exception {

        // get the statement alias based on all security ids
        String alias = "ON_OPEN_POSITION_" + positionVO.getSecurityId();

        // undeploy the statement
        EsperManager.undeployStatement(positionVO.getStrategy(), alias);

        long startTime = System.nanoTime();
        logger.debug("onOpenPosition start " + positionVO.getSecurityId());

        // call orderCompleted
        onOpenPosition(positionVO);

        logger.debug("onOpenPosition end " + positionVO.getSecurityId());

        MetricsUtil.accountEnd("OpenPositionCallback." + positionVO.getStrategy(), startTime);
    }

    /**
     * Will be exectued by the Esper Engine as soon as a new Position on the given Security has been opened.
     * Needs to be overwritten by implementing classes.
     */
    public abstract void onOpenPosition(OpenPositionVO positionVO) throws Exception;
}
