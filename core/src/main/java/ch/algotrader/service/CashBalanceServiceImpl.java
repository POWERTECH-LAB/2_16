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

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;

import ch.algotrader.entity.Transaction;
import ch.algotrader.entity.strategy.CashBalance;
import ch.algotrader.entity.strategy.Strategy;
import ch.algotrader.enumeration.Currency;
import ch.algotrader.util.MyLogger;
import ch.algotrader.util.collection.BigDecimalMap;
import ch.algotrader.util.collection.Pair;
import ch.algotrader.vo.CurrencyAmountVO;

/**
 * @author <a href="mailto:aflury@algotrader.ch">Andy Flury</a>
 *
 * @version $Revision$ $Date$
 */
public class CashBalanceServiceImpl extends CashBalanceServiceBase {

    private static Logger logger = MyLogger.getLogger(CashBalanceServiceImpl.class.getName());

    private @Value("#{T(ch.algotrader.enumeration.Currency).fromString('${misc.portfolioBaseCurrency}')}") Currency portfolioBaseCurrency;
    private @Value("${misc.portfolioDigits}") int portfolioDigits;

    @Override
    protected void handleProcessTransaction(Transaction transaction) throws Exception {

        // process all currenyAmounts
        for (CurrencyAmountVO currencyAmount : transaction.getAttributions()) {
            processAmount(transaction.getStrategy().getName(), currencyAmount);
        }
    }

    @Override
    protected void handleProcessAmount(String strategyName, CurrencyAmountVO currencyAmount) throws Exception {

        Strategy strategy = getStrategyDao().findByName(strategyName);
        CashBalance cashBalance = getCashBalanceDao().findByStrategyAndCurrencyLocked(strategy, currencyAmount.getCurrency());

        // create the cashBalance, if it does not exist yet
        if (cashBalance == null) {

            cashBalance = CashBalance.Factory.newInstance();

            // associate currency, amount and strategy
            cashBalance.setCurrency(currencyAmount.getCurrency());
            cashBalance.setAmount(currencyAmount.getAmount());
            cashBalance.setStrategy(strategy);

            getCashBalanceDao().create(cashBalance);

            // reverse-associate strategy (after cashBalance has received an id)
            strategy.getCashBalances().add(cashBalance);

        } else {

            cashBalance.setAmount(cashBalance.getAmount().add(currencyAmount.getAmount()));
        }
    }

    @Override
    protected String handleResetCashBalances() throws Exception {

        // get all existing cashBalances
        Collection<CashBalance> existingCashBalances = getCashBalanceDao().loadAll();

        // sum all transactions
        Collection<Transaction> transactions = getTransactionDao().loadAll();
        BigDecimalMap<Pair<Strategy, Currency>> map = new BigDecimalMap<Pair<Strategy, Currency>>();
        for (Transaction transaction : transactions) {

            // process all currenyAmounts
            for (CurrencyAmountVO currencyAmount : transaction.getAttributions()) {
                map.increment(new Pair<Strategy, Currency>(transaction.getStrategy(), currencyAmount.getCurrency()), currencyAmount.getAmount());
            }
        }

        // create cash balances
        StringBuffer buffer = new StringBuffer();
        for (Map.Entry<Pair<Strategy, Currency>, BigDecimal> entry : map.entrySet()) {

            Strategy strategy = entry.getKey().getFirst();
            Currency currency = entry.getKey().getSecond();
            BigDecimal amount = entry.getValue().setScale(this.portfolioDigits, BigDecimal.ROUND_HALF_UP);

            CashBalance cashBalance = getCashBalanceDao().findByStrategyAndCurrencyLocked(strategy, currency);

            if (cashBalance != null) {

                existingCashBalances.remove(cashBalance);

                BigDecimal oldAmount = cashBalance.getAmount();
                if (oldAmount.doubleValue() != amount.doubleValue()) {

                    cashBalance.setAmount(amount);

                    String info = "adjusted cashBalance " + cashBalance + " from " + oldAmount;
                    logger.info(info);
                    buffer.append(info + "\n");

                }

            } else {

                cashBalance = CashBalance.Factory.newInstance();
                cashBalance.setCurrency(currency);
                cashBalance.setAmount(amount);
                cashBalance.setStrategy(strategy);

                getCashBalanceDao().create(cashBalance);

                // reverse-associate with strategy (after cashBalance has received an id)
                strategy.getCashBalances().add(cashBalance);

                String info = "created cashBalance " + cashBalance;
                logger.info(info);
                buffer.append(info + "\n");

            }
        }

        // remove all obsolete cashBalances
        for (CashBalance cashBalance : existingCashBalances) {

            Strategy strategy = cashBalance.getStrategy();
            strategy.getCashBalances().remove(cashBalance);

            String info = "removed cashBalance " + cashBalance;
            logger.info(info);
            buffer.append(info + "\n");
        }

        getCashBalanceDao().remove(existingCashBalances);

        return buffer.toString();
    }
}
