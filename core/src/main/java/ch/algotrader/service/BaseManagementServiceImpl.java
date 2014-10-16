/***********************************************************************************
 * AlgoTrader Enterprise Trading Framework
 *
 * Copyright (C) 2014 AlgoTrader GmbH - All rights reserved
 *
 * All information contained herein is, and remains the property of AlgoTrader GmbH.
 * The intellectual and technical concepts contained herein are proprietary to
 * AlgoTrader GmbH. Modification, translation, reverse engineering, decompilation,
 * disassembly or reproduction of this material is strictly forbidden unless prior
 * written permission is obtained from AlgoTrader GmbH
 *
 * Fur detailed terms and conditions consult the file LICENSE.txt or contact
 *
 * AlgoTrader GmbH
 * Badenerstrasse 16
 * 8004 Zurich
 ***********************************************************************************/
package ch.algotrader.service;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.lang.Validate;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedOperationParameter;
import org.springframework.jmx.export.annotation.ManagedOperationParameters;

import ch.algotrader.entity.trade.OrderStatus;
import ch.algotrader.enumeration.Currency;
import ch.algotrader.enumeration.Status;
import ch.algotrader.enumeration.TransactionType;
import ch.algotrader.esper.EngineLocator;
import ch.algotrader.util.RoundUtil;
import ch.algotrader.util.metric.MetricsUtil;
import ch.algotrader.util.spring.HibernateSession;

/**
 * @author <a href="mailto:aflury@algotrader.ch">Andy Flury</a>
 *
 * @version $Revision$ $Date$
 */
@HibernateSession
public class BaseManagementServiceImpl implements BaseManagementService {

    private final PositionService positionService;

    private final ForexService forexService;

    private final CashBalanceService cashBalanceService;

    private final CombinationService combinationService;

    private final TransactionService transactionService;

    private final OptionService optionService;

    private final OrderService orderService;

    public BaseManagementServiceImpl(
            final PositionService positionService,
            final ForexService forexService,
            final CashBalanceService cashBalanceService,
            final CombinationService combinationService,
            final TransactionService transactionService,
            final OptionService optionService,
            final OrderService orderService) {

        Validate.notNull(positionService, "PositionService is null");
        Validate.notNull(forexService, "ForexService is null");
        Validate.notNull(cashBalanceService, "CashBalanceService is null");
        Validate.notNull(combinationService, "CombinationService is null");
        Validate.notNull(transactionService, "TransactionService is null");
        Validate.notNull(optionService, "OptionService is null");
        Validate.notNull(orderService, "OrderService is null");

        this.positionService = positionService;
        this.forexService = forexService;
        this.cashBalanceService = cashBalanceService;
        this.combinationService = combinationService;
        this.transactionService = transactionService;
        this.optionService = optionService;
        this.orderService = orderService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @ManagedOperation(description = "Cancels all orders currently outstanding.")
    @ManagedOperationParameters({})
    public void cancelAllOrders() {

        this.orderService.cancelAllOrders();

    }

    /**
     * {@inheritDoc}
     */
    @Override
    @ManagedOperation(description = "Manually record a Transaction")
    @ManagedOperationParameters({
            @ManagedOperationParameter(name = "securityId", description = "SecurityId (for CREDIT / DEBIT / INTREST_PAID / INTREST_RECEIVED / DIVIDEND / FEES / REFUND enter 0)"),
            @ManagedOperationParameter(name = "strategyName", description = "Name of the Strategy"),
            @ManagedOperationParameter(name = "extId", description = "External transaction id (e.g. 0001f4e6.4fe7e2cb.01.01)"),
            @ManagedOperationParameter(name = "dateTime", description = "DateTime of the Transaction. Format: dd.mm.yyyy hh:mm:ss"),
            @ManagedOperationParameter(name = "quantity", description = "Requested quantity: <ul> <li> BUY: pos </li> <li> SELL: neg </li> <li> EXPIRATION: pos/neg </li> <li> TRANSFER : pos/neg </li> <li> CREDIT: 1 </li> <li> INTREST_RECEIVED: 1 </li> <li> REFUND : 1 </li> <li> DIVIDEND : 1 </li> <li> DEBIT: -1 </li> <li> INTREST_PAID: -1 </li> <li> FEES: -1 </li> </ul>"),
            @ManagedOperationParameter(name = "price", description = "Price"),
            @ManagedOperationParameter(name = "executionCommission", description = "Execution Commission. 0 if not applicable"),
            @ManagedOperationParameter(name = "clearingCommission", description = "Clearing Commission. 0 if not applicable"),
            @ManagedOperationParameter(name = "fee", description = "fee"),
            @ManagedOperationParameter(name = "currency", description = "Currency"),
            @ManagedOperationParameter(name = "transactionType", description = "Transaction type: <ul> <li> B (BUY) </li> <li> S (SELL) </li> <li> E (EXPIRATION) </li> <li> T (TRANSFER) </li> <li> C (CREDIT) </li> <li> D (DEBIT) </li> <li> IP (INTREST_PAID) </li> <li> IR (INTREST_RECEIVED) </li> <li> DI (DIVIDEND) </li> <li> F (FEES) </li> <li> RF (REFUND) </li> </ul>"),
            @ManagedOperationParameter(name = "accountName", description = "Account Name") })
    public void recordTransaction(final int securityId, final String strategyName, final String extId, final String dateTime, final long quantity, final double price,
            final double executionCommission, final double clearingCommission, final double fee, final String currency, final String transactionType, final String accountName) {

        Validate.notEmpty(strategyName, "Strategy name is empty");
        Validate.notEmpty(dateTime, "Date time is empty");
        Validate.notEmpty(transactionType, "Transaction type is empty");

        Date dateTimeObject;
        try {
            dateTimeObject = (new SimpleDateFormat("dd.MM.yyyy HH:mm:ss")).parse(dateTime);
        } catch (ParseException ex) {
            throw new BaseManagementServiceException(ex);
        }
        String extIdString = !"".equals(extId) ? extId : null;
        BigDecimal priceDecimal = RoundUtil.getBigDecimal(price);
        BigDecimal executionCommissionDecimal = (executionCommission != 0) ? RoundUtil.getBigDecimal(executionCommission) : null;
        BigDecimal clearingCommissionDecimal = (clearingCommission != 0) ? RoundUtil.getBigDecimal(clearingCommission) : null;
        BigDecimal feeDecimal = (fee != 0) ? RoundUtil.getBigDecimal(fee) : null;
        Currency currencyObject = !"".equals(currency) ? Currency.fromValue(currency) : null;
        TransactionType transactionTypeObject = TransactionType.fromValue(transactionType);

        this.transactionService.createTransaction(securityId, strategyName, extIdString, dateTimeObject, quantity, priceDecimal, executionCommissionDecimal, clearingCommissionDecimal, feeDecimal,
                currencyObject, transactionTypeObject, accountName, null);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    @ManagedOperation(description = "Transfers a Position to another Strategy.")
    @ManagedOperationParameters({ @ManagedOperationParameter(name = "positionId", description = "Id of the Position"),
            @ManagedOperationParameter(name = "targetStrategyName", description = "Strategy where the Position should be moved to") })
    public void transferPosition(final int positionId, final String targetStrategyName) {

        Validate.notEmpty(targetStrategyName, "Target strategy name is empty");

        this.positionService.transferPosition(positionId, targetStrategyName);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    @ManagedOperation(description = "Calculates margins for all open positions")
    @ManagedOperationParameters({})
    public void setMargins() {

        this.positionService.setMargins();

    }

    /**
     * {@inheritDoc}
     */
    @Override
    @ManagedOperation(description = "Hedges all non-base currency exposures with a corresponding FX / FX Future Position")
    @ManagedOperationParameters({})
    public void hedgeForex() {

        this.forexService.hedgeForex();

    }

    /**
     * {@inheritDoc}
     */
    @Override
    @ManagedOperation(description = "performs a Delta Hedge of all Securities of the specified underlyingId")
    @ManagedOperationParameters({ @ManagedOperationParameter(name = "underlyingId", description = "underlyingId") })
    public void hedgeDelta(final int underlyingId) {

        this.optionService.hedgeDelta(underlyingId);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    @ManagedOperation(description = "Creates Rebalance Transactions so that Net-Liquidation-Values of all strategies are in line with the defined Strategy-Allocation.")
    @ManagedOperationParameters({})
    public void rebalancePortfolio() {

        this.transactionService.rebalancePortfolio();

    }

    /**
     * {@inheritDoc}
     */
    @Override
    @ManagedOperation(description = "Calculates all Cash Balances and Position quantities based on Transactions in the database and makes adjustments if necessary")
    @ManagedOperationParameters({})
    public String resetPositionsAndCashBalances() {

        return this.positionService.resetPositions() + this.cashBalanceService.resetCashBalances();

    }

    /**
     * {@inheritDoc}
     */
    @Override
    @ManagedOperation(description = "Updates the Component Window. This method should only be called after manually manipulating components in the DB.")
    @ManagedOperationParameters({})
    public void resetComponentWindow() {

        this.combinationService.resetComponentWindow();

    }

    /**
     * {@inheritDoc}
     */
    @Override
    @ManagedOperation(description = "Clears the Open Order Window. Should only be called if there are no open orders outstanding with the external Broker")
    @ManagedOperationParameters({})
    public void emptyOpenOrderWindow() {

        OrderStatus orderStatus = OrderStatus.Factory.newInstance();
        orderStatus.setStatus(Status.CANCELED);

        EngineLocator.instance().getBaseEngine().sendEvent(orderStatus);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void logMetrics() {

        MetricsUtil.logMetrics();
        EngineLocator.instance().logStatementMetrics();

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void resetMetrics() {

        MetricsUtil.resetMetrics();
        EngineLocator.instance().resetStatementMetrics();

    }

}
