package com.algoTrader.starter;

import java.math.BigDecimal;
import java.util.List;

import com.algoTrader.ServiceLocator;
import com.algoTrader.entity.MonthlyPerformance;
import com.algoTrader.enumeration.RuleName;
import com.algoTrader.service.RuleService;
import com.algoTrader.util.RoundUtil;
import com.algoTrader.vo.InterpolationVO;
import com.algoTrader.vo.MaxDrawDownVO;
import com.algoTrader.vo.PerformanceKeysVO;

public class SimulationStarter {

    public static void main(String[] args) {

        if (args[0].equals("simulateByUnderlayings")) {
            simulateByUnderlayings();
        } else if (args[0].equals("simulateByActualOrders")) {
            simulateByActualOrders();
        } else {
            System.out.println("please specify simulateByUnderlayings or simulateByActualOrders on the commandline");
        }
    }

    public static void simulateByUnderlayings() {

        ServiceLocator.instance().getSimulationService().init();

        ServiceLocator.instance().getRuleService().activateAll();
        ServiceLocator.instance().getSimulationService().simulateByUnderlayings();

        printStatistics();
    }

    public static void simulateByActualOrders() {

        ServiceLocator.instance().getSimulationService().init();

        RuleService ruleService = ServiceLocator.instance().getRuleService();

        ruleService.activate(RuleName.CREATE_PORTFOLIO_VALUE);
        ruleService.activate(RuleName.CREATE_MONTHLY_PERFORMANCE);
        ruleService.activate(RuleName.GET_LAST_TICK);
        ruleService.activate(RuleName.PRINT_TICK);
        ruleService.activate(RuleName.CREATE_INTERPOLATION);
        ruleService.activate(RuleName.CREATE_PERFORMANCE_KEYS);
        ruleService.activate(RuleName.KEEP_MONTHLY_PERFORMANCE);
        ruleService.activate(RuleName.CREATE_DRAW_DOWN);
        ruleService.activate(RuleName.CREATE_MAX_DRAW_DOWN);
        ruleService.activate(RuleName.RERUN_ORDERS);

        ServiceLocator.instance().getSimulationService().simulateByActualTransactions();

        printStatistics();
    }

    @SuppressWarnings("unchecked")
    private static void printStatistics() {

        BigDecimal totalValue = ServiceLocator.instance().getManagementService().getAccountTotalValue();
        System.out.println("totalValue=" + totalValue);

        InterpolationVO interpolation = ServiceLocator.instance().getSimulationService().getInterpolation();

        if (interpolation != null) {
            System.out.print("a=" + RoundUtil.getBigDecimal(interpolation.getA()));
            System.out.print(" b=" + RoundUtil.getBigDecimal(interpolation.getB()));
            System.out.println(" r=" + RoundUtil.getBigDecimal(interpolation.getR()));
        }

        List<MonthlyPerformance> monthlyPerformances = ServiceLocator.instance().getSimulationService().getMonthlyPerformances();

        System.out.print("monthlyPerformance: ");
        double maxDrawDownM = 0d;
        double bestMonthlyPerformance = 0d;
        for (MonthlyPerformance monthlyPerformance : monthlyPerformances) {
            maxDrawDownM = Math.min(maxDrawDownM, monthlyPerformance.getValue());
            bestMonthlyPerformance = Math.max(bestMonthlyPerformance, monthlyPerformance.getValue());
            System.out.print(RoundUtil.getBigDecimal(monthlyPerformance.getValue() * 100) + "% " );
        }
        System.out.println();

        PerformanceKeysVO performanceKeys = ServiceLocator.instance().getSimulationService().getPerformanceKeys();
        MaxDrawDownVO maxDrawDownVO = ServiceLocator.instance().getSimulationService().getMaxDrawDown();

        if (performanceKeys != null && maxDrawDownVO != null) {
            System.out.print("n=" + RoundUtil.getBigDecimal(performanceKeys.getN()));
            System.out.print(" avgM=" + RoundUtil.getBigDecimal(performanceKeys.getAvgM() * 100) + "%");
            System.out.print(" stdM=" + RoundUtil.getBigDecimal(performanceKeys.getStdM() * 100) + "%");
            System.out.print(" avgY=" + RoundUtil.getBigDecimal(performanceKeys.getAvgY() * 100) + "%");
            System.out.print(" stdY=" + RoundUtil.getBigDecimal(performanceKeys.getStdY() * 100) + "%");
            System.out.println(" sharpRatio=" + RoundUtil.getBigDecimal(performanceKeys.getSharpRatio()));

            System.out.print("maxDrawDownM: " + RoundUtil.getBigDecimal(-maxDrawDownM * 100) + "%");
            System.out.print(" bestMonthlyPerformance: " + RoundUtil.getBigDecimal(bestMonthlyPerformance * 100) + "%");
            System.out.print(" maxDrawDown: " + RoundUtil.getBigDecimal(maxDrawDownVO.getAmount() * 100) + "%");
            System.out.print(" maxDrawDownPeriod: " + RoundUtil.getBigDecimal(maxDrawDownVO.getPeriod() / 86400000) + "days");
            System.out.println(" colmarRatio: " + RoundUtil.getBigDecimal(performanceKeys.getAvgY() / maxDrawDownVO.getAmount()));
        }
    }
}
