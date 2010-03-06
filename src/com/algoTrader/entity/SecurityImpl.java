package com.algoTrader.entity;

import java.math.BigDecimal;
import java.util.Iterator;

import com.algoTrader.enumeration.RuleName;
import com.algoTrader.enumeration.TransactionType;
import com.algoTrader.util.EsperService;
import com.algoTrader.util.RoundUtil;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;

public class SecurityImpl extends com.algoTrader.entity.Security {

    private static final long serialVersionUID = -6631052475125813394L;

    public Tick getLastTick() {
        EPStatement statement = EsperService.getEPServiceInstance().getEPAdministrator().getStatement(RuleName.GET_LAST_TICK.getValue());

        if (statement != null) {
            Iterator it = statement.iterator();
            while (it.hasNext()) {
                EventBean bean = (EventBean) it.next();
                Integer securityId = (Integer) bean.get("securityId");
                if (securityId.equals(getId())) {
                    return (Tick)bean.get("tick");
                }
            }
        }
        return null;
    }

    public boolean hasOpenPositions() {
        return getPosition().getQuantity() > 0;
    }

    public BigDecimal getCommission(int quantity, TransactionType transactionType) {

        if (this instanceof StockOption &&
                (TransactionType.SELL.equals(transactionType) || TransactionType.BUY.equals(transactionType))) {
            if (quantity < 4) {
                return RoundUtil.getBigDecimal(quantity * 1.5 + 5);
            } else {
                return RoundUtil.getBigDecimal(quantity * 3);
            }
        } else {
            return new BigDecimal(0);
        }
    }
}
