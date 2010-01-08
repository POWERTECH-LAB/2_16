package com.algoTrader.service;

import java.util.List;

import com.algoTrader.entity.Account;
import com.algoTrader.entity.Position;
import com.algoTrader.entity.Rule;
import com.algoTrader.entity.Security;
import com.algoTrader.entity.StockOption;
import com.algoTrader.entity.Transaction;

public class EntityServiceImpl extends com.algoTrader.service.EntityServiceBase {

    protected com.algoTrader.entity.Security handleGetSecurity(int id) throws java.lang.Exception {

        return getSecurityDao().load(id);
    }

    protected Security handleGetSecurityByIsin(String isin) throws Exception {

        return getSecurityByIsin(isin);
    }

    protected com.algoTrader.entity.Account handleGetAccount(int id) throws java.lang.Exception {

        return getAccountDao().load(id);
    }

    protected com.algoTrader.entity.Position handleGetPosition(int id) throws java.lang.Exception {

        return getPositionDao().load(id);
    }

    protected com.algoTrader.entity.Rule handleGetRule(int id) throws java.lang.Exception {

        return getRuleDao().load(id);
    }

    protected com.algoTrader.entity.Transaction handleGetTransaction(int id) throws java.lang.Exception {

        return getTransactionDao().load(id);
    }

    protected Security[] handleGetAllSecurities() throws Exception {

        return (Security[])getSecurityDao().loadAll().toArray(new Security[0]);
    }

    protected Account[] handleGetAllAccounts() throws Exception {

        return (Account[])getAccountDao().loadAll().toArray(new Account[0]);
    }

    protected Position[] handleGetAllPositions() throws Exception {

        return (Position[])getPositionDao().loadAll().toArray(new Position[0]);
    }

    protected Rule[] handleGetAllRules() throws Exception {

        return (Rule[])getRuleDao().loadAll().toArray(new Rule[0]);

    }

    protected Transaction[] handleGetAllTransactions() throws Exception {

        return (Transaction[])getTransactionDao().loadAll().toArray(new Transaction[0]);
    }

    protected Security[] handleGetAllSecuritiesInPortfolio() throws Exception {

        return (Security[])getSecurityDao().findSecuritesInPortfolio().toArray(new Security[0]);
    }

    protected Security[] handleGetDummySecurities() throws Exception {

        return (Security[])getSecurityDao().findDummySecurities().toArray(new Security[0]);
    }
}
