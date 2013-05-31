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
package ch.algorader.service;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.algoTrader.service.ChartProvidingServiceBase;
import com.algoTrader.vo.AnnotationVO;
import com.algoTrader.vo.BarVO;
import com.algoTrader.vo.ChartDefinitionVO;
import com.algoTrader.vo.IndicatorVO;
import com.algoTrader.vo.MarkerVO;

/**
 * @author <a href="mailto:andyflury@gmail.com">Andy Flury</a>
 *
 * @version $Revision$ $Date$
 */
public class ChartProvidingServiceImpl extends ChartProvidingServiceBase {

    private ChartDefinitionVO diagramDefinition;

    @Override
    protected void handleSetChartDefinition(ChartDefinitionVO diagramDefinition) throws Exception {

        this.diagramDefinition = diagramDefinition;
    }

    @Override
    protected ChartDefinitionVO handleGetChartDefinition() throws Exception {

        return this.diagramDefinition;
    }

    @Override
    protected Set<IndicatorVO> handleGetIndicators(long startDateTime) throws Exception {

        return new HashSet<IndicatorVO>();
    }

    @Override
    protected Set<BarVO> handleGetBars(long startDateTime) throws Exception {

        return new HashSet<BarVO>();
    }

    @Override
    protected Set<MarkerVO> handleGetMarkers() throws Exception {

        return new HashSet<MarkerVO>();
    }

    @Override
    protected Collection<AnnotationVO> handleGetAnnotations(long startDateTime) throws Exception {

        return new HashSet<AnnotationVO>();
    }

    @Override
    protected String handleGetDescription() throws Exception {

        return null;
    }
}
