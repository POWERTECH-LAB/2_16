package com.algoTrader.entity;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Value;

import com.algoTrader.entity.security.Security;
import com.algoTrader.vo.TransactionVO;

public class TransactionDaoImpl extends TransactionDaoBase {

    private @Value("${misc.portfolioDigits}") int portfolioDigits;

    @Override
    public void toTransactionVO(Transaction transaction, TransactionVO transactionVO) {

        super.toTransactionVO(transaction, transactionVO);

        completeTransactionVO(transaction, transactionVO);
    }

    @Override
    public TransactionVO toTransactionVO(final Transaction transaction) {

        TransactionVO transactionVO = super.toTransactionVO(transaction);

        completeTransactionVO(transaction, transactionVO);

        return transactionVO;
    }

    private void completeTransactionVO(Transaction transaction, TransactionVO transactionVO) {

        Security security = transaction.getSecurity();
        if (security != null) {
            transactionVO.setSymbol(security.getSymbol());

            int scale = security.getSecurityFamily().getScale();
            transactionVO.setPrice(transaction.getPrice().setScale(scale, BigDecimal.ROUND_HALF_UP));
        } else {
            transactionVO.setPrice(transaction.getPrice().setScale(this.portfolioDigits, BigDecimal.ROUND_HALF_UP));
        }

        transactionVO.setValue(transaction.getNetValue().setScale(this.portfolioDigits, BigDecimal.ROUND_HALF_UP));
        transactionVO.setCommission(transaction.getCommission().setScale(this.portfolioDigits, BigDecimal.ROUND_HALF_UP));
    }

    @Override
    public Transaction transactionVOToEntity(TransactionVO transactionVO) {

        throw new UnsupportedOperationException("transactionVOToEntity not yet implemented.");
    }
}
