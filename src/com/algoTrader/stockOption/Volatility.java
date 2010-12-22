package com.algoTrader.stockOption;

import org.apache.commons.math.ConvergenceException;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.solvers.UnivariateRealSolver;
import org.apache.commons.math.analysis.solvers.UnivariateRealSolverFactory;

import com.algoTrader.entity.StockOption;
import com.algoTrader.enumeration.OptionType;
import com.algoTrader.sabr.SABRVol;
import com.algoTrader.util.DateUtil;

public class Volatility {

    private static final double MILLISECONDS_PER_DAY = 86400000;

    public static double getIndexVola(double underlayingSpot, double atmVola, double years, double intrest, double dividend, double strikeDistance, double beta, double correlation, double volVol) {

        double accumulation = Math.exp(years * intrest);
        double forward = StockOptionUtil.getForward(underlayingSpot, years, intrest, dividend);
        double atmStrike = Math.round(underlayingSpot / 50.0) * 50.0;

        double factorSum = 0.0;

        // process atm strike
        {
            double sabrVola = SABRVol.volByAtmVol(forward, atmStrike, atmVola, years, beta, correlation, volVol);
            double call = StockOptionUtil.getOptionPriceBS(underlayingSpot, atmStrike, sabrVola, years, intrest, dividend, OptionType.CALL);
            double put = StockOptionUtil.getOptionPriceBS(underlayingSpot, atmStrike, sabrVola, years, intrest, dividend, OptionType.PUT);
            double outOfTheMoneyPrice = (put + call) / 2;
            double factor = getFactor(atmStrike, accumulation, strikeDistance, outOfTheMoneyPrice);
            factorSum += factor;
        }

        // process strikes below atm
        double strike = atmStrike - strikeDistance;
        while (true) {
            double sabrVola = SABRVol.volByAtmVol(forward, strike, atmVola, years, beta, correlation, volVol);
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
            double sabrVola = SABRVol.volByAtmVol(forward, strike, atmVola, years, beta, correlation, volVol);
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

    public static double getAtmVola(final double underlayingSpot, final double indexVola, final double years, final double intrest, final double dividend, final double strikeDistance, final double beta, final double correlation, final double volVol) throws ConvergenceException, FunctionEvaluationException, IllegalArgumentException {

        UnivariateRealFunction function = new UnivariateRealFunction () {
            public double value(double atmVola) throws FunctionEvaluationException {
                double currentIndexVola = getIndexVola(underlayingSpot, atmVola, years, intrest, dividend, strikeDistance, beta, correlation, volVol);
                double difference = currentIndexVola - indexVola;
                return difference;
            }};

        UnivariateRealSolverFactory factory = UnivariateRealSolverFactory.newInstance();
        UnivariateRealSolver solver = factory.newDefaultSolver();
        solver.setAbsoluteAccuracy(0.0001);

        try {
            // normaly atmVola is above 80% of indexVola
            return solver.solve(function, indexVola * 0.8, indexVola, indexVola);
        } catch (Exception e) {
            // in rare cases atmVola is as low as 25% of indexVola
            return solver.solve(function, indexVola * 0.1, indexVola, indexVola);
        }
    }

    private static double getFactor(double strike, double accumulation, double strikeDistance, double outOfTheMoneyPrice) {

        return outOfTheMoneyPrice * accumulation * (strikeDistance / (strike * strike));
    }

    public static double getCorrelation(StockOption stockOption) {
        double days = (stockOption.getExpiration().getTime() - DateUtil.getCurrentEPTime().getTime()) / MILLISECONDS_PER_DAY;
        if (OptionType.CALL.equals(stockOption.getType())) {
            return Math.exp(0.5623 - 0.02989 * Math.log(days) - 0.01095 * days + 0.002167 * Math.log(days) * days) - 2.0;
        } else {
            return Math.exp(1.111 - 0.131 * Math.log(days) - 0.062 * days + 0.0128 * Math.log(days) * days) - 2.0;
        }
    }

    public static double getVolVol(StockOption stockOption) {
        double days = (stockOption.getExpiration().getTime() - DateUtil.getCurrentEPTime().getTime()) / MILLISECONDS_PER_DAY;
        if (OptionType.CALL.equals(stockOption.getType())) {
            return Math.exp(3.332 - 0.65 * Math.log(days) + 0.0325 * days - 0.0048 * Math.log(days) * days) - 2.0;
        } else {
            return Math.exp(3.589 - 0.908 * Math.log(days) + 0.056 * days - 0.00827 * Math.log(days) * days) - 2.0;
        }
    }
}
