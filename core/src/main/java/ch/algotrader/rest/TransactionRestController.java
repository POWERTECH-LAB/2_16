/***********************************************************************************
 * AlgoTrader Enterprise Trading Framework
 *
 * Copyright (C) 2015 AlgoTrader GmbH - All rights reserved
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
 * Aeschstrasse 6
 * 8834 Schindellegi
 ***********************************************************************************/

package ch.algotrader.rest;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import ch.algotrader.entity.Account;
import ch.algotrader.entity.TransactionVO;
import ch.algotrader.entity.strategy.Strategy;
import ch.algotrader.service.LookupService;
import ch.algotrader.service.TransactionService;

@RestController
@RequestMapping(path = "/rest")
public class TransactionRestController extends RestControllerBase {

    private final TransactionService transactionService;
    private final LookupService lookupService;

    public TransactionRestController(final TransactionService transactionService, final LookupService lookupService) {
        this.transactionService = transactionService;
        this.lookupService = lookupService;
    }

    @CrossOrigin
    @RequestMapping(path = "/transaction/{id}", method = RequestMethod.POST)
    public void createTransaction(@PathVariable final long id, @RequestBody TransactionVO transaction) {

        Strategy strategy = this.lookupService.getStrategy(transaction.getStrategyId());
        if (strategy == null) {
            throw new EntityNotFoundException("Strategy not found: " + transaction.getStrategyId());
        }
        Account account = this.lookupService.getAccount(transaction.getStrategyId());
        if (account == null) {
            throw new EntityNotFoundException("Accounr not found: " + transaction.getAccountId());
        }
        this.transactionService.createTransaction(transaction.getSecurityId(), strategy.getName(), transaction.getExtId(), transaction.getDateTime(),
                transaction.getQuantity(), transaction.getPrice(), transaction.getExecutionCommission(), transaction.getClearingCommission(), transaction.getFee(), transaction.getCurrency(),
                transaction.getType(), account.getName(), transaction.getDescription());
    }

    @CrossOrigin
    @RequestMapping(path = "/transaction/rebalance-portfolio", method = RequestMethod.POST)
    public void createTransaction() {

        this.transactionService.rebalancePortfolio();
    }

    @CrossOrigin
    @RequestMapping(path = "/transaction/reset-cache-balances", method = RequestMethod.POST)
    public void resetCashBalances() {

        this.transactionService.resetCashBalances();
    }

}
