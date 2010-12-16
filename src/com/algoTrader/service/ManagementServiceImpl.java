package com.algoTrader.service;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import com.algoTrader.util.StrategyUtil;
import com.algoTrader.vo.PositionVO;
import com.algoTrader.vo.TickVO;
import com.algoTrader.vo.TransactionVO;

public class ManagementServiceImpl extends ManagementServiceBase {

    private static final SimpleDateFormat format = new SimpleDateFormat("dd-MM-yy kk:mm");

    protected String handleGetCurrentTime() throws Exception {

        return format.format(new Date(getRuleService().getCurrentTime(StrategyUtil.getStartedStrategyName())));
    }

    protected String handleGetStrategyName() throws Exception {

        return StrategyUtil.getStartedStrategyName();
    }

    protected BigDecimal handleGetStrategyCashBalance() throws Exception {

        return getReportingService().getStrategyCashBalance(StrategyUtil.getStartedStrategyName());
    }

    protected BigDecimal handleGetStrategySecuritiesCurrentValue() throws Exception {

        return getReportingService().getStrategySecuritiesCurrentValue(StrategyUtil.getStartedStrategyName());
    }

    protected BigDecimal handleGetStrategyMaintenanceMargin() throws Exception {

        return getReportingService().getStrategyMaintenanceMargin(StrategyUtil.getStartedStrategyName());
    }

    protected BigDecimal handleGetStrategyNetLiqValue() throws Exception {

        return getReportingService().getStrategyNetLiqValue(StrategyUtil.getStartedStrategyName());
    }

    protected BigDecimal handleGetStrategyAvailableFunds() throws Exception {

        return getReportingService().getStrategyAvailableFunds(StrategyUtil.getStartedStrategyName());
    }

    protected double handleGetStrategyLeverage() throws Exception {

        return getReportingService().getStrategyLeverage(StrategyUtil.getStartedStrategyName());
    }

    protected BigDecimal handleGetStrategyUnderlaying() throws Exception {

        return getReportingService().getStrategyUnderlaying(StrategyUtil.getStartedStrategyName());
    }

    protected BigDecimal handleGetStrategyVolatility() throws Exception {

        return getReportingService().getStrategyVolatility(StrategyUtil.getStartedStrategyName());
    }

    @SuppressWarnings("unchecked")
    protected List<TickVO> handleGetDataLastTicks() {

        return getRuleService().getAllEventsProperty(StrategyUtil.getStartedStrategyName(), "GET_LAST_TICK", "tick");
    }

    @SuppressWarnings("unchecked")
    protected List<PositionVO> handleGetDataOpenPositions() throws Exception {

        return getReportingService().getDataOpenPositions(StrategyUtil.getStartedStrategyName());
    }

    @SuppressWarnings("unchecked")
    protected List<TransactionVO> handleGetDataTransactions() throws Exception {

        return getReportingService().getDataTransactions(StrategyUtil.getStartedStrategyName());
    }

    protected void handleActivate(String ruleName) throws Exception {

        getRuleService().activate(StrategyUtil.getStartedStrategyName(), ruleName);
    }

    protected void handleDeactivate(String ruleName) throws Exception {

        getRuleService().deactivate(StrategyUtil.getStartedStrategyName(), ruleName);
    }
}
