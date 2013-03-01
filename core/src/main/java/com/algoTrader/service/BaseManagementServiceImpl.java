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
package com.algoTrader.service;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.algoTrader.entity.StrategyImpl;
import com.algoTrader.entity.trade.OrderStatus;
import com.algoTrader.enumeration.Currency;
import com.algoTrader.enumeration.Status;
import com.algoTrader.enumeration.TransactionType;
import com.algoTrader.esper.EsperManager;
import com.algoTrader.util.RoundUtil;
import com.algoTrader.util.metric.MetricsUtil;

/**
 * @author <a href="mailto:andyflury@gmail.com">Andy Flury</a>
 *
 * @version $Revision$ $Date$
 */
public class BaseManagementServiceImpl extends BaseManagementServiceBase {

    @Override
    protected void handleCancelAllOrders() throws Exception {

        getOrderService().cancelAllOrders();
    }

    @Override
    protected void handleRecordTransaction(int securityId, String strategyName, String extIdString, String dateTimeString, long quantity, double priceDouble, double executionCommissionDouble,
            double clearingCommissionDouble, String currencyString, String transactionTypeString, String accountName) throws Exception {

        String extId = !"".equals(extIdString) ? extIdString : null;
        Date dateTime = (new SimpleDateFormat("dd.MM.yyyy HH:mm:ss")).parse(dateTimeString);
        BigDecimal price = RoundUtil.getBigDecimal(priceDouble);
        BigDecimal executionCommission = (executionCommissionDouble != 0) ? RoundUtil.getBigDecimal(executionCommissionDouble) : null;
        BigDecimal clearingCommission = (clearingCommissionDouble != 0) ? RoundUtil.getBigDecimal(clearingCommissionDouble) : null;
        Currency currency = !"".equals(currencyString) ? Currency.fromValue(currencyString) : null;
        TransactionType transactionType = TransactionType.fromValue(transactionTypeString);

        getTransactionService().createTransaction(securityId, strategyName, extId, dateTime, quantity, price, executionCommission, clearingCommission, currency, transactionType, accountName);
    }

    @Override
    protected void handleTransferPosition(int positionId, String targetStrategyName) throws Exception {

        getPositionService().transferPosition(positionId, targetStrategyName);
    }

    @Override
    protected void handleSetMargins() throws Exception {

        getPositionService().setMargins();
    }

    @Override
    protected void handleEqualizeForex() throws Exception {

        getForexService().equalizeForex();
    }

    @Override
    protected void handleRebalancePortfolio() throws Exception {

        getPortfolioPersistenceService().rebalancePortfolio();
    }

    @Override
    protected void handleResetPositionsAndCashBalances() throws Exception {

        getPositionService().resetPositions();

        getCashBalanceService().resetCashBalances();
    }

    @Override
    protected void handleResetComponentWindow() throws Exception {

        getCombinationService().resetComponentWindow();
    }

    @Override
    protected void handleEmptyOpenOrderWindow() throws Exception {

        OrderStatus orderStatus = OrderStatus.Factory.newInstance();
        orderStatus.setStatus(Status.CANCELED);

        EsperManager.sendEvent(StrategyImpl.BASE, orderStatus);
    }

    @Override
    protected void handleLogMetrics() throws Exception {

        MetricsUtil.logMetrics();
        EsperManager.logStatementMetrics();
    }

    @Override
    protected void handleResetMetrics() throws Exception {

        MetricsUtil.resetMetrics();
        EsperManager.resetStatementMetrics();
    }
}
