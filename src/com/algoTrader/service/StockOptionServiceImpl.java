package com.algoTrader.service;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.apache.commons.math.ConvergenceException;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.util.MathUtils;
import org.apache.log4j.Logger;

import com.algoTrader.criteria.CallOptionCriteria;
import com.algoTrader.criteria.PutOptionCriteria;
import com.algoTrader.entity.Account;
import com.algoTrader.entity.Order;
import com.algoTrader.entity.OrderImpl;
import com.algoTrader.entity.Position;
import com.algoTrader.entity.Security;
import com.algoTrader.entity.StockOption;
import com.algoTrader.entity.StockOptionImpl;
import com.algoTrader.entity.Tick;
import com.algoTrader.entity.Transaction;
import com.algoTrader.enumeration.Currency;
import com.algoTrader.enumeration.Market;
import com.algoTrader.enumeration.OptionType;
import com.algoTrader.enumeration.OrderStatus;
import com.algoTrader.enumeration.RuleName;
import com.algoTrader.enumeration.TransactionType;
import com.algoTrader.stockOption.StockOptionUtil;
import com.algoTrader.util.DateUtil;
import com.algoTrader.util.MyLogger;
import com.algoTrader.util.PropertiesUtil;
import com.algoTrader.util.RoundUtil;

public class StockOptionServiceImpl extends com.algoTrader.service.StockOptionServiceBase {

    private static boolean simulation = PropertiesUtil.getBooleanProperty("simulation");
    private static Market market = Market.fromString(PropertiesUtil.getProperty("simulation.market"));
    private static Currency currency = Currency.fromString(PropertiesUtil.getProperty("simulation.currency"));
    private static int contractSize = PropertiesUtil.getIntProperty("simulation.contractSize");

    private static int minAge = PropertiesUtil.getIntProperty("minAge");
    private static int strikeOffset = PropertiesUtil.getIntProperty("strikeOffset");

    private static Logger logger = MyLogger.getLogger(StockOptionServiceImpl.class.getName());

    private long FORTY_FIVE_DAYS = 3888000000l;

    protected StockOption handleGetStockOption(int underlayingSecurityId, BigDecimal underlayingSpot, OptionType optionType) throws Exception {

        Security underlaying = getSecurityDao().load(underlayingSecurityId);

        Date targetExpirationDate = new Date(DateUtil.getCurrentEPTime().getTime() + minAge);

        StockOption stockOption = findNearestStockOption(underlaying, targetExpirationDate, underlayingSpot, optionType);

        if (simulation) {
            if ((stockOption == null)
                    || (stockOption.getExpiration().getTime() > (targetExpirationDate.getTime() + FORTY_FIVE_DAYS ))
                    || (OptionType.CALL.equals(optionType) && stockOption.getStrike().doubleValue() > underlayingSpot.doubleValue() + (50 + strikeOffset))
                    || (OptionType.PUT.equals(optionType) && stockOption.getStrike().doubleValue() < underlayingSpot.doubleValue() - (50 + strikeOffset))  ) {

                stockOption = createDummyStockOption(underlaying, targetExpirationDate, underlayingSpot, optionType);

                getStockOptionDao().create(stockOption);
            }
        }
        return stockOption;
    }

    private StockOption createDummyStockOption(Security underlaying, Date targetExpirationDate, BigDecimal underlayingSpot, OptionType type) throws Exception {

        // set third Friday of the month
        Date expiration = DateUtil.getNextThirdFriday(targetExpirationDate);


        BigDecimal strike;
        if (OptionType.CALL.equals(type)) {
            // increase by strikeOffset and round to upper 50
            strike = RoundUtil.getBigDecimal(MathUtils.round((underlayingSpot.doubleValue() + strikeOffset)/ 50.0, 0, BigDecimal.ROUND_CEILING) * 50.0);
        } else {
            // reduce by strikeOffset and round to lower 50
            strike = RoundUtil.getBigDecimal(MathUtils.round((underlayingSpot.doubleValue() - strikeOffset)/ 50.0, 0, BigDecimal.ROUND_FLOOR) * 50.0);
        }

        // symbol
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(expiration);
        String symbol = "O" +
        underlaying.getSymbol() + " " +
        new SimpleDateFormat("MMM").format(cal.getTime()).toUpperCase() + "/" +
        (cal.get(Calendar.YEAR) + "-").substring(2) +
        type.toString().substring(0, 1) + " " +
        strike.intValue() + " " +
        contractSize;

        StockOption stockOption = new StockOptionImpl();
        stockOption.setIsin(null); // dummys don't have a isin
        stockOption.setSymbol(symbol);
        stockOption.setMarket(market);
        stockOption.setCurrency(currency);
        stockOption.setOnWatchlist(false);
        stockOption.setStrike(strike);
        stockOption.setExpiration(expiration);
        stockOption.setType(type);
        stockOption.setContractSize(contractSize);
        stockOption.setUnderlaying(underlaying);

        getStockOptionDao().create(stockOption);

        logger.info("created dummy option " + stockOption.getSymbol());

        return stockOption;
    }

    @SuppressWarnings("unchecked")
    private StockOption findNearestStockOption(Security underlaying, Date targetExpirationDate, BigDecimal underlayingSpot,
            OptionType type) throws Exception {

        List<StockOption> list;
        if (OptionType.CALL.equals(type)) {
            BigDecimal targetStrike = RoundUtil.getBigDecimal(underlayingSpot.doubleValue() + strikeOffset);
            CallOptionCriteria criteria = new CallOptionCriteria(underlaying, targetExpirationDate, targetStrike, type);
            criteria.setMaximumResultSize(new Integer(1));
            list = getStockOptionDao().findCallOptionByCriteria(criteria);

        } else {
            BigDecimal targetStrike = RoundUtil.getBigDecimal(underlayingSpot.doubleValue() - strikeOffset);
            PutOptionCriteria criteria = new PutOptionCriteria(underlaying, targetExpirationDate, targetStrike, type);
            criteria.setMaximumResultSize(new Integer(1));
            list = getStockOptionDao().findPutOptionByCriteria(criteria);
        }

        if (list.size() > 0) {
            return list.get(0);
        } else {
            return null;
        }
    }

    protected void handleOpenPosition(int stockOptionId, BigDecimal settlement, BigDecimal currentValue, BigDecimal underlayingSpot, double vola) throws Exception {

        StockOption stockOption = (StockOption)getStockOptionDao().load(stockOptionId);

        Account account = getAccountDao().findByCurrency(stockOption.getCurrency());

        double availableAmount = account.getAvailableAmountDouble();
        int contractSize = stockOption.getContractSize();
        double currentValueDouble = currentValue.doubleValue();
        double underlayingValue = underlayingSpot.doubleValue();

        double margin = StockOptionUtil.getMargin(stockOption, settlement.doubleValue(), underlayingValue);
        double exitValue = StockOptionUtil.getExitValue(stockOption, underlayingValue, vola);

        // we do not want to loose more than the atRiskRatioPerTrade for this new trade
        //         invested capital: margin - currentValue (in a short option deal)
        //         max risk: exitValue - current Value
        //         atRiskRatioPerTrade = max risk / invested capital
        double maxAtRiskRatioPerTrade = PropertiesUtil.getDoubleProperty("maxAtRiskRatioPerTrade");
        double atRiskRatioPerTrade = (exitValue - currentValueDouble) / (margin - currentValueDouble);
        if(atRiskRatioPerTrade > maxAtRiskRatioPerTrade) {
            exitValue = maxAtRiskRatioPerTrade * (margin - currentValueDouble) + currentValueDouble;
        }

        // get numberOfContracts based on margin (how may option can we sell for the available amount of cash
        long numberOfContractsByMargin = (long)((availableAmount / (margin - currentValueDouble)) / contractSize); // i.e. 2 (for 20 stockOptions)

        // get maxNumberOfContracts based on RedemptionValue
        //         available cash after this trade: cashbalance now + quantity * contractSize * currentValue
        //        total redemptionValue = quantity * contractSize * exitValue + RedemptionValue of the other positions
        //        atRiskRatioOfPortfolio = total redemptionValue / available cash after this trade
        //        (we could adjust the exitValue or the quantity, but we trust the exitValue set above and only adjust the quantity)
        double maxAtRiskRatioOfPortfolio = PropertiesUtil.getDoubleProperty("maxAtRiskRatioOfPortfolio");
        long numberOfContractsByRedemptionValue =
            (long)((maxAtRiskRatioOfPortfolio * account.getCashBalanceDouble() - account.getRedemptionValue()) /
            (contractSize *(exitValue - maxAtRiskRatioOfPortfolio * currentValueDouble)));

        // choose which ever is lower
        long numberOfContracts= Math.min(numberOfContractsByMargin, numberOfContractsByRedemptionValue);

        if (numberOfContracts <= 0) {
            if (stockOption.getPosition() == null || !stockOption.getPosition().isOpen()) {
                getWatchlistService().removeFromWatchlist(stockOptionId);
            }
            return; // there is no money available
        }

        // the stockOption might have been removed from the watchlist by another statement (i.e. closePosition)
        if (!stockOption.isOnWatchlist()) {
            getWatchlistService().putOnWatchlist(stockOptionId);
        }

        Order order = new OrderImpl();
        order.setSecurity(stockOption);
        order.setRequestedQuantity(numberOfContracts);
        order.setTransactionType(TransactionType.SELL);

        getDispatcherService().getTransactionService().executeTransaction(order);

        setMargin(order);
        setExitValue(stockOption.getPosition(), exitValue);
    }

    protected void handleClosePosition(int positionId) throws Exception {

        Position position = getPositionDao().load(positionId);

        StockOption stockOption = (StockOption)position.getSecurity();

        long numberOfContracts = Math.abs(position.getQuantity());

        Order order = new OrderImpl();
        order.setSecurity(stockOption);
        order.setRequestedQuantity(numberOfContracts);
        order.setTransactionType(TransactionType.BUY);

        getDispatcherService().getTransactionService().executeTransaction(order);

        // only remove the stockOption from the watchlist, if the transaction did execute fully.
        // otherwise the next tick will execute the reminder of the order
        if (OrderStatus.EXECUTED.equals(order.getStatus()) ||
                OrderStatus.AUTOMATIC.equals(order.getStatus())) {

            getWatchlistService().removeFromWatchlist(stockOption);

            // if there is a and OPEN_POSITION rule acitve for this stockOption deactivate it
            getRuleService().deactivate(RuleName.OPEN_POSITION, stockOption);
        }
    }

    protected void handleExpirePosition(int positionId) throws Exception {

        Position position = getPositionDao().load(positionId);

        if (position.getExitValue() == null || position.getExitValue().doubleValue() == 0d) {
            logger.warn(position.getSecurity().getSymbol() + " expired but did not have a exit value specified");
        }

        StockOption stockOption = (StockOption)position.getSecurity();

        long numberOfContracts = Math.abs(position.getQuantity());

        Order order = new OrderImpl();
        order.setSecurity(stockOption);
        order.setRequestedQuantity(numberOfContracts);
        order.setTransactionType(TransactionType.EXPIRATION);

        getDispatcherService().getTransactionService().executeTransaction(order);

        // only remove the stockOption from the watchlist, if the transaction did execute fully.
        // otherwise the next tick will execute the reminder of the order
        if (OrderStatus.EXECUTED.equals(order.getStatus()) ||
                OrderStatus.AUTOMATIC.equals(order.getStatus())) {

            getWatchlistService().removeFromWatchlist(stockOption);
        }
    }

    @SuppressWarnings("unchecked")
    protected void handleSetMargins() throws Exception {

        List<Position> positions = getPositionDao().findOpenPositions();

        for (Position position : positions) {
            setMargin(position);
        }
    }

    protected void handleSetExitValue(int positionId, double exitValue) throws ConvergenceException, FunctionEvaluationException {

        // we don't want to set the exitValue to Zero
        if (exitValue <= 0.05) {
            return;
        }

        Position position = getPositionDao().load(positionId);

        if (position == null) {
            throw new StockOptionServiceException("position does not exist: " + positionId);
        }


        if (position.getExitValue() == null) {
            throw new StockOptionServiceException("no exitValue was set for position: " + positionId);
        }

        if (exitValue > position.getExitValue().doubleValue()) {
            throw new StockOptionServiceException("exit value " + exitValue + " is higher than existing exit value " + position.getExitValue() + " of position " + positionId);
        }

        setExitValue(position, exitValue);

    }

    @SuppressWarnings("unchecked")
    private void setMargin(Order order) throws Exception {

        Collection<Transaction> transactions = order.getTransactions();

        for (Transaction transaction : transactions) {
            setMargin(transaction.getPosition());
        }
    }

    private void setMargin(Position position) throws Exception {

        StockOption stockOption = (StockOption) position.getSecurity();
        Tick tick = stockOption.getLastTick();
        if (tick != null) {
            double underlayingSpot = stockOption.getUnderlaying().getLastTick().getCurrentValueDouble();

            double marginPerContract = StockOptionUtil.getMargin(stockOption, tick.getSettlementDouble(), underlayingSpot) * stockOption.getContractSize();
            long numberOfContracts = Math.abs(position.getQuantity());
            BigDecimal totalMargin = RoundUtil.getBigDecimal(marginPerContract * numberOfContracts);

            position.setMargin(totalMargin);

            getPositionDao().update(position);

            Account account = position.getAccount();

            int percent = (int)(account.getAvailableAmountDouble() / account.getCashBalanceDouble() * 100.0);
            if (account.getAvailableAmountDouble() >= 0) {
                logger.info("set margin for " + stockOption.getSymbol() + " to " + RoundUtil.getBigDecimal(marginPerContract) + " total margin: " + account.getMargin() + " available amount: " + account.getAvailableAmount() + " (" + percent + "% of balance)");
            } else {
                logger.warn("set margin for " + stockOption.getSymbol() + " to " + RoundUtil.getBigDecimal(marginPerContract) + " total margin: " + account.getMargin() + " available amount: " + account.getAvailableAmount() + " (" + percent + "% of balance)");
            }
        } else {
            logger.warn("no last tick available to set margin on " + stockOption.getSymbol());
        }

    }


    private void setExitValue(Position position, double exitValue) throws ConvergenceException, FunctionEvaluationException {

        double currentValue = position.getSecurity().getLastTick().getCurrentValueDouble();
        if (exitValue < currentValue ) {
            throw new StockOptionServiceException("ExitValue (" + exitValue + ") for position " + position.getId() + " is lower than currentValue: " + currentValue);
        }

        position.setExitValue(RoundUtil.getBigDecimal(exitValue));
        getPositionDao().update(position);

        logger.info("set exit value " + position.getSecurity().getSymbol() + " to " + exitValue);
    }
}
