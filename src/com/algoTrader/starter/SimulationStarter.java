package com.algoTrader.starter;

import java.math.BigDecimal;

import com.algoTrader.ServiceLocator;
import com.algoTrader.vo.InterpolationVO;

public class SimulationStarter {

    public static void main(String[] args) {

        start();
    }

    public static void start() {

        ServiceLocator.instance().getSimulationService().init();

        ServiceLocator.instance().getRuleService().activateAll();
        ServiceLocator.instance().getSimulationService().run();

        InterpolationVO interpolation = ServiceLocator.instance().getSimulationService().getInterpolation();

        if (interpolation != null) {
            System.out.print("a=" + interpolation.getA());
            System.out.print(" b=" + interpolation.getB());
            System.out.print(" r=" + interpolation.getR());
        }

        BigDecimal totalValue = ServiceLocator.instance().getManagementService().getAccountTotalValue();
        System.out.println(" totalValue=" + totalValue);
    }
}
