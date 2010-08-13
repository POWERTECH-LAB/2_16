package com.algoTrader.stockOption;

import org.apache.commons.math.ConvergenceException;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.solvers.UnivariateRealSolver;
import org.apache.commons.math.analysis.solvers.UnivariateRealSolverFactory;

import com.algoTrader.enumeration.OptionType;
import com.algoTrader.util.PropertiesUtil;

public class Volatility {

    private static int strikeDistance = PropertiesUtil.getIntProperty("strategie.strikeDistance");
    private static double intrest = PropertiesUtil.getDoubleProperty("strategie.intrest");
    private static double dividend = PropertiesUtil.getDoubleProperty("strategie.dividend");

    public static double getIndexVola(double underlayingSpot, double atmVola, double years) {

        double accumulation = Math.exp(years * intrest);
        double forward = underlayingSpot * (1 - years * dividend) * Math.exp(years * intrest);
        double atmStrike = Math.round(underlayingSpot / 50.0) * 50.0;

        double factorSum = 0.0;

        // process atm strike
        {
            double sabrVola = Sabr.getSabrVolatility(atmStrike, forward, years, atmVola);
            double call = StockOptionUtil.getOptionPriceBS(underlayingSpot, atmStrike, sabrVola, years, intrest, dividend, OptionType.CALL);
            double put = StockOptionUtil.getOptionPriceBS(underlayingSpot, atmStrike, sabrVola, years, intrest, dividend, OptionType.PUT);
            double outOfTheMoneyPrice = (put + call) / 2;
            double factor = getFactor(atmStrike, accumulation, strikeDistance, outOfTheMoneyPrice);
            factorSum += factor;
        }

        // process strikes below atm
        double strike = atmStrike - strikeDistance;
        while (true) {
            double sabrVola = Sabr.getSabrVolatility(strike, forward, years, atmVola);
            double put = StockOptionUtil.getOptionPriceBS(underlayingSpot, strike, sabrVola, years, intrest, dividend, OptionType.PUT);
            if (put < 0.5)
                break;
            double factor = getFactor(strike, accumulation, strikeDistance, put);
            if ((factor / factorSum) < 0.0001)
                break;
            factorSum += factor;
            strike -= strikeDistance;
        }

        // process strikes above atm
        strike = atmStrike + strikeDistance;
        while (true) {
            double sabrVola = Sabr.getSabrVolatility(strike, forward, years, atmVola);
            double call = StockOptionUtil.getOptionPriceBS(underlayingSpot, strike, sabrVola, years, intrest, dividend, OptionType.CALL);
            if (call < 0.5)
                break;
            double factor = getFactor(strike, accumulation, strikeDistance, call);
            if ((factor / factorSum) < 0.0001)
                break;
            factorSum += factor;
            strike += strikeDistance;
        }

        return Math.sqrt((factorSum * 2 - Math.pow(forward / atmStrike-1 , 2)) / years);
    }

    public static double getAtmVola(final double underlayingSpot, final double indexVola, final double years) throws ConvergenceException, FunctionEvaluationException, IllegalArgumentException {

        UnivariateRealFunction function = new UnivariateRealFunction () {
            public double value(double atmVola) throws FunctionEvaluationException {
                double currentIndexVola = getIndexVola(underlayingSpot, atmVola, years);
                double difference = currentIndexVola - indexVola;
                return difference;
            }};

        UnivariateRealSolverFactory factory = UnivariateRealSolverFactory.newInstance();
        UnivariateRealSolver solver = factory.newDefaultSolver();
        solver.setAbsoluteAccuracy(0.0001);

        return solver.solve(function, indexVola * 0.7 , indexVola * 1.1, indexVola);
    }

    private static double getFactor(double strike, double accumulation, int strikeDistance, double outOfTheMoneyPrice) {

        return outOfTheMoneyPrice * accumulation * (strikeDistance / (strike * strike));
    }
}
