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
package ch.algotrader.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.math.util.MathUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;

import ch.algotrader.entity.strategy.StrategyImpl;
import ch.algotrader.util.DateUtil;
import ch.algotrader.util.MyLogger;
import ch.algotrader.util.RoundUtil;

import ch.algotrader.entity.Position;
import ch.algotrader.entity.Subscription;
import ch.algotrader.entity.security.Forex;
import ch.algotrader.entity.security.ForexFuture;
import ch.algotrader.entity.security.Future;
import ch.algotrader.entity.security.FutureFamily;
import ch.algotrader.entity.strategy.Strategy;
import ch.algotrader.entity.trade.Order;
import ch.algotrader.enumeration.Currency;
import ch.algotrader.enumeration.Side;
import ch.algotrader.service.ForexServiceBase;
import ch.algotrader.vo.BalanceVO;

/**
 * @author <a href="mailto:andyflury@gmail.com">Andy Flury</a>
 *
 * @version $Revision$ $Date$
 */
public class ForexServiceImpl extends ForexServiceBase {

    private static Logger logger = MyLogger.getLogger(ForexServiceImpl.class.getName());
    private static Logger notificationLogger = MyLogger.getLogger("ch.algorader.service.NOTIFICATION");

    private @Value("#{T(ch.algorader.enumeration.Currency).fromString('${misc.portfolioBaseCurrency}')}") Currency portfolioBaseCurrency;
    private @Value("${fx.futureEqualizationEnabled}") boolean fxFutureEqualizationEnabled;
    private @Value("${fx.futureEqualizationMinTimeToExpiration}") int fxFutureEqualizationMinTimeToExpiration;
    private @Value("${fx.equalizationMinAmount}") int fxEqualizationMinAmount;
    private @Value("${fx.equalizationBatchSize}") int fxEqualizationBatchSize;

    @Override
    protected void handleEqualizeForex() throws Exception {

        Strategy base = getStrategyDao().findBase();

        // potentially close a ForexFuture position if it is below the MinTimeToExpiration
        if (this.fxFutureEqualizationEnabled) {

            // get the closing orders
            final List<Order> orders = new ArrayList<Order>();
            for (Position position : getLookupService().getOpenPositionsByStrategyAndType(StrategyImpl.BASE, ForexFuture.class)) {

                // check if expiration is below minimum
                ForexFuture forexFuture = (ForexFuture) position.getSecurityInitialized();

                Forex forex = (Forex) forexFuture.getUnderlyingInitialized();

                Subscription forexSubscription = getSubscriptionDao().findByStrategyAndSecurity(StrategyImpl.BASE, forex.getId());
                if (!forexSubscription.hasProperty("hedgingFamily")) {
                    throw new IllegalStateException("no hedgingFamily defined for forex " + forex);
                }

                FutureFamily futureFamily = getFutureFamilyDao().load(forexSubscription.getIntProperty("hedgingFamily"));
                if (!forexFuture.getSecurityFamily().equals(futureFamily)) {
                    // continue forex is not hedged with this forexFutureFamily
                    continue;
                }

                if (forexFuture.getTimeToExpiration() < this.fxFutureEqualizationMinTimeToExpiration) {

                    Order order = getLookupService().getOrderByStrategyAndSecurityFamily(StrategyImpl.BASE, forexFuture.getSecurityFamily().getId());
                    order.setStrategy(base);
                    order.setSecurity(forexFuture);
                    order.setQuantity(Math.abs(position.getQuantity()));
                    order.setSide(position.getQuantity() > 0 ? Side.SELL : Side.BUY);

                    orders.add(order);
                }
            }

            // setup an TradeCallback so that new hedge positions are only setup when existing positions are closed
            if (orders.size() > 0) {

                notificationLogger.info(orders.size() + " fx hedging position(s) have been closed due to approaching expiration, please run equalizeForex again");

                // send the orders
                for (Order order : orders) {
                    getOrderService().sendOrder(order);
                }

                return; // do not go any furter because closing trades will have to finish first
            }
        }

        // process all non-base currency balances
        Collection<BalanceVO> balances = getPortfolioService().getBalances();
        for (BalanceVO balance : balances) {

            if (balance.getCurrency().equals(this.portfolioBaseCurrency)) {
                continue;
            }

            // get the netLiqValueBase
            double netLiqValue = balance.getNetLiqValue().doubleValue();
            double netLiqValueBase = balance.getExchangeRate() * netLiqValue;

            // check if amount is larger than minimum
            if (Math.abs(netLiqValueBase) >= this.fxEqualizationMinAmount) {

                // get the forex
                Forex forex = getForexDao().getForex(this.portfolioBaseCurrency, balance.getCurrency());

                double tradeValue = forex.getBaseCurrency().equals(this.portfolioBaseCurrency) ? netLiqValueBase : netLiqValue;

                // create the order
                Order order = getLookupService().getOrderByStrategyAndSecurityFamily(StrategyImpl.BASE, forex.getSecurityFamily().getId());
                order.setStrategy(base);

                // if a hedging family is defined for this Forex use it instead of the Forex directly
                int qty;
                if (this.fxFutureEqualizationEnabled) {

                    Subscription forexSubscription = getSubscriptionDao().findByStrategyAndSecurity(StrategyImpl.BASE, forex.getId());
                    if (!forexSubscription.hasProperty("hedgingFamily")) {
                        throw new IllegalStateException("no hedgingFamily defined for forex " + forex);
                    }

                    FutureFamily futureFamily = getFutureFamilyDao().load(forexSubscription.getIntProperty("hedgingFamily"));

                    Date targetDate = DateUtils.addMilliseconds(DateUtil.getCurrentEPTime(), this.fxFutureEqualizationMinTimeToExpiration);
                    Future future = getLookupService().getFutureByMinExpiration(futureFamily.getId(), targetDate);

                    // make sure the future is subscriped
                    getMarketDataService().subscribe(base.getName(), future.getId());

                    order.setSecurity(future);

                    // round to the number of contracts
                    qty = (int) MathUtils.round(tradeValue / futureFamily.getContractSize(), 0);

                } else {

                    order.setSecurity(forex);

                    // round to batchSize
                    qty = (int) RoundUtil.roundToNextN(tradeValue, this.fxEqualizationBatchSize);
                }

                if (forex.getBaseCurrency().equals(this.portfolioBaseCurrency)) {

                    // expected case
                    order.setQuantity(Math.abs(qty));
                    order.setSide(qty > 0 ? Side.BUY : Side.SELL);

                } else {

                    // reverse case
                    order.setQuantity(Math.abs(qty));
                    order.setSide(qty > 0 ? Side.SELL : Side.BUY);
                }

                getOrderService().sendOrder(order);

            } else {

                logger.info("no forex equalization is performed on " + balance.getCurrency() + " because amount "
                        + RoundUtil.getBigDecimal(Math.abs(netLiqValueBase)) + " is less than " + this.fxEqualizationMinAmount);
                continue;
            }
        }
    }
}
