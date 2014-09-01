package ch.algotrader.adapter.fix.fix42;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ch.algotrader.entity.Account;
import ch.algotrader.entity.AccountImpl;
import ch.algotrader.entity.security.Forex;
import ch.algotrader.entity.security.ForexImpl;
import ch.algotrader.entity.security.Future;
import ch.algotrader.entity.security.FutureImpl;
import ch.algotrader.entity.security.Option;
import ch.algotrader.entity.security.OptionFamily;
import ch.algotrader.entity.security.OptionFamilyImpl;
import ch.algotrader.entity.security.OptionImpl;
import ch.algotrader.entity.security.SecurityFamily;
import ch.algotrader.entity.security.SecurityFamilyImpl;
import ch.algotrader.entity.security.Stock;
import ch.algotrader.entity.security.StockImpl;
import ch.algotrader.entity.trade.LimitOrder;
import ch.algotrader.entity.trade.LimitOrderImpl;
import ch.algotrader.entity.trade.MarketOrder;
import ch.algotrader.entity.trade.MarketOrderImpl;
import ch.algotrader.entity.trade.OrderPropertyImpl;
import ch.algotrader.entity.trade.StopLimitOrder;
import ch.algotrader.entity.trade.StopLimitOrderImpl;
import ch.algotrader.entity.trade.StopOrder;
import ch.algotrader.entity.trade.StopOrderImpl;
import ch.algotrader.enumeration.Broker;
import ch.algotrader.enumeration.Currency;
import ch.algotrader.enumeration.OptionType;
import ch.algotrader.enumeration.Side;
import ch.algotrader.enumeration.TIF;
import quickfix.field.ClOrdID;
import quickfix.field.ContractMultiplier;
import quickfix.field.ExpireTime;
import quickfix.field.MaturityDay;
import quickfix.field.MaturityMonthYear;
import quickfix.field.OrdType;
import quickfix.field.OrderQty;
import quickfix.field.OrigClOrdID;
import quickfix.field.Price;
import quickfix.field.SecurityType;
import quickfix.field.StopPx;
import quickfix.field.StrikePrice;
import quickfix.field.Symbol;
import quickfix.field.TimeInForce;
import quickfix.fix42.NewOrderSingle;
import quickfix.fix42.OrderCancelReplaceRequest;
import quickfix.fix42.OrderCancelRequest;

public class TestGenericOrderMessageFactory {

    private GenericFix42OrderMessageFactory requestFactory;

    @Before
    public void setup() throws Exception {

        this.requestFactory = new GenericFix42OrderMessageFactory(new GenericFix42SymbologyResolver());
    }

    @Test
    public void testMarketOrderOption() throws Exception {

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

        OptionFamily family = new OptionFamilyImpl();
        family.setCurrency(Currency.BRL);
        family.setSymbolRoot("STUFF");
        family.setContractSize(100.5d);
        family.setWeekly(true);

        Option option = new OptionImpl();
        option.setSymbol("SOME_STUFF");
        option.setType(OptionType.CALL);
        option.setSecurityFamily(family);
        option.setStrike(new BigDecimal("0.5"));
        option.setExpiration(dateFormat.parse("2014-12-31"));

        Account account = new AccountImpl();
        account.setBroker(Broker.RT);

        MarketOrder order = new MarketOrderImpl();
        order.setSecurity(option);
        order.setSide(Side.BUY);
        order.setQuantity(2000);
        order.setAccount(account);


        NewOrderSingle message = this.requestFactory.createNewOrderMessage(order, "test-id");

        Assert.assertNotNull(message);
        Assert.assertEquals(new ClOrdID("test-id"), message.getClOrdID());
        Assert.assertNotNull(message.getTransactTime());
        Assert.assertEquals(new Symbol("STUFF"), message.getSymbol());
        Assert.assertEquals(new quickfix.field.Side(quickfix.field.Side.BUY), message.getSide());
        Assert.assertEquals(new OrderQty(2000), message.getOrderQty());
        Assert.assertEquals(new OrdType(OrdType.MARKET), message.getOrdType());
        Assert.assertEquals(new quickfix.field.Currency("BRL"), message.getCurrency());
        Assert.assertEquals(new SecurityType(SecurityType.OPTION), message.getSecurityType());
        Assert.assertEquals(new StrikePrice(0.5d), message.getStrikePrice());
        Assert.assertEquals(new ContractMultiplier(100.5d), message.getContractMultiplier());
        Assert.assertEquals(new MaturityDay("31"), message.getMaturityDay());
        Assert.assertEquals(new MaturityMonthYear("201412"), message.getMaturityMonthYear());
    }

    @Test
    public void testMarketOrderFuture() throws Exception {

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

        SecurityFamily family = new SecurityFamilyImpl();
        family.setCurrency(Currency.BRL);
        family.setSymbolRoot("STUFF");

        Future future = new FutureImpl();
        future.setSymbol("SOME_STUFF");
        future.setSecurityFamily(family);
        future.setExpiration(dateFormat.parse("2014-12-31"));

        Account account = new AccountImpl();
        account.setBroker(Broker.RT);

        MarketOrder order = new MarketOrderImpl();
        order.setSecurity(future);
        order.setSide(Side.BUY);
        order.setQuantity(2000);
        order.setAccount(account);


        NewOrderSingle message = this.requestFactory.createNewOrderMessage(order, "test-id");

        Assert.assertNotNull(message);
        Assert.assertEquals(new ClOrdID("test-id"), message.getClOrdID());
        Assert.assertNotNull(message.getTransactTime());
        Assert.assertEquals(new Symbol("STUFF"), message.getSymbol());
        Assert.assertEquals(new quickfix.field.Side(quickfix.field.Side.BUY), message.getSide());
        Assert.assertEquals(new OrderQty(2000), message.getOrderQty());
        Assert.assertEquals(new OrdType(OrdType.MARKET), message.getOrdType());
        Assert.assertEquals(new quickfix.field.Currency("BRL"), message.getCurrency());
        Assert.assertEquals(new SecurityType(SecurityType.FUTURE), message.getSecurityType());
        Assert.assertEquals(new MaturityMonthYear("201412"), message.getMaturityMonthYear());
    }

    @Test
    public void testMarketOrderStock() throws Exception {

        SecurityFamily family = new SecurityFamilyImpl();
        family.setCurrency(Currency.USD);

        Stock stock = new StockImpl();
        stock.setSecurityFamily(family);
        stock.setSymbol("APPL");

        Account account = new AccountImpl();
        account.setBroker(Broker.RT);

        MarketOrder order = new MarketOrderImpl();
        order.setSecurity(stock);
        order.setSide(Side.SELL);
        order.setQuantity(10);
        order.setAccount(account);

        NewOrderSingle message = this.requestFactory.createNewOrderMessage(order, "test-id");

        Assert.assertNotNull(message);
        Assert.assertEquals(new ClOrdID("test-id"), message.getClOrdID());
        Assert.assertNotNull(message.getTransactTime());
        Assert.assertEquals(new Symbol("APPL"), message.getSymbol());
        Assert.assertEquals(new quickfix.field.Side(quickfix.field.Side.SELL), message.getSide());
        Assert.assertEquals(new OrderQty(10), message.getOrderQty());
        Assert.assertEquals(new OrdType(OrdType.MARKET), message.getOrdType());
        Assert.assertEquals(new quickfix.field.Currency("USD"), message.getCurrency());
        Assert.assertEquals(new SecurityType(SecurityType.COMMON_STOCK), message.getSecurityType());
    }

    @Test
    public void testMarketOrderForex() throws Exception {

        SecurityFamily family = new SecurityFamilyImpl();
        family.setCurrency(Currency.USD);

        Forex forex = new ForexImpl();
        forex.setSymbol("EUR.USD");
        forex.setLmaxid("4001");
        forex.setBaseCurrency(Currency.EUR);
        forex.setSecurityFamily(family);

        Account account = new AccountImpl();
        account.setBroker(Broker.RT);

        MarketOrder order = new MarketOrderImpl();
        order.setSecurity(forex);
        order.setSide(Side.BUY);
        order.setQuantity(2000);
        order.setAccount(account);

        NewOrderSingle message = this.requestFactory.createNewOrderMessage(order, "test-id");

        Assert.assertNotNull(message);
        Assert.assertEquals(new ClOrdID("test-id"), message.getClOrdID());
        Assert.assertNotNull(message.getTransactTime());
        Assert.assertEquals(new Symbol("EUR"), message.getSymbol());
        Assert.assertEquals(new quickfix.field.Side(quickfix.field.Side.BUY), message.getSide());
        Assert.assertEquals(new OrderQty(2000), message.getOrderQty());
        Assert.assertEquals(new OrdType(OrdType.MARKET), message.getOrdType());
        Assert.assertEquals(new quickfix.field.Currency("USD"), message.getCurrency());
        Assert.assertEquals(new SecurityType(SecurityType.CASH), message.getSecurityType());
    }

    @Test
    public void testLimitedOrderStock() throws Exception {

        SecurityFamily family = new SecurityFamilyImpl();
        family.setCurrency(Currency.USD);

        Stock stock = new StockImpl();
        stock.setSecurityFamily(family);
        stock.setSymbol("APPL");

        Account account = new AccountImpl();
        account.setBroker(Broker.RT);

        LimitOrder order = new LimitOrderImpl();
        order.setSecurity(stock);
        order.setSide(Side.BUY);
        order.setQuantity(10);
        order.setAccount(account);
        order.setLimit(new BigDecimal("20.0"));

        NewOrderSingle message = this.requestFactory.createNewOrderMessage(order, "test-id");

        Assert.assertNotNull(message);
        Assert.assertEquals(new ClOrdID("test-id"), message.getClOrdID());
        Assert.assertNotNull(message.getTransactTime());
        Assert.assertEquals(new Symbol("APPL"), message.getSymbol());
        Assert.assertEquals(new quickfix.field.Side(quickfix.field.Side.BUY), message.getSide());
        Assert.assertEquals(new OrderQty(10), message.getOrderQty());
        Assert.assertEquals(new OrdType(OrdType.LIMIT), message.getOrdType());
        Assert.assertEquals(new quickfix.field.Currency("USD"), message.getCurrency());
        Assert.assertEquals(new SecurityType(SecurityType.COMMON_STOCK), message.getSecurityType());
        Assert.assertEquals(new Price(20.0d), message.getPrice());
    }

    @Test
    public void testStopOrderStock() throws Exception {

        SecurityFamily family = new SecurityFamilyImpl();
        family.setCurrency(Currency.USD);

        Stock stock = new StockImpl();
        stock.setSecurityFamily(family);
        stock.setSymbol("APPL");

        Account account = new AccountImpl();
        account.setBroker(Broker.RT);

        StopOrder order = new StopOrderImpl();
        order.setSecurity(stock);
        order.setSide(Side.BUY);
        order.setQuantity(10);
        order.setAccount(account);
        order.setStop(new BigDecimal("20.0"));

        NewOrderSingle message = this.requestFactory.createNewOrderMessage(order, "test-id");

        Assert.assertNotNull(message);
        Assert.assertEquals(new ClOrdID("test-id"), message.getClOrdID());
        Assert.assertNotNull(message.getTransactTime());
        Assert.assertEquals(new Symbol("APPL"), message.getSymbol());
        Assert.assertEquals(new quickfix.field.Side(quickfix.field.Side.BUY), message.getSide());
        Assert.assertEquals(new OrderQty(10), message.getOrderQty());
        Assert.assertEquals(new OrdType(OrdType.STOP), message.getOrdType());
        Assert.assertEquals(new quickfix.field.Currency("USD"), message.getCurrency());
        Assert.assertEquals(new SecurityType(SecurityType.COMMON_STOCK), message.getSecurityType());
        Assert.assertEquals(new StopPx(20.0d), message.getStopPx());
    }

    @Test
    public void testStopLimitOrderStockOneDay() throws Exception {

        SecurityFamily family = new SecurityFamilyImpl();
        family.setCurrency(Currency.USD);

        Stock stock = new StockImpl();
        stock.setSecurityFamily(family);
        stock.setSymbol("APPL");

        Account account = new AccountImpl();
        account.setBroker(Broker.RT);

        StopLimitOrder order = new StopLimitOrderImpl();
        order.setSecurity(stock);
        order.setSide(Side.BUY);
        order.setQuantity(10);
        order.setAccount(account);
        order.setLimit(new BigDecimal("20.0"));
        order.setStop(new BigDecimal("30.0"));
        order.setTif(TIF.DAY);

        NewOrderSingle message = this.requestFactory.createNewOrderMessage(order, "test-id");

        Assert.assertNotNull(message);
        Assert.assertEquals(new ClOrdID("test-id"), message.getClOrdID());
        Assert.assertNotNull(message.getTransactTime());
        Assert.assertEquals(new Symbol("APPL"), message.getSymbol());
        Assert.assertEquals(new quickfix.field.Side(quickfix.field.Side.BUY), message.getSide());
        Assert.assertEquals(new OrderQty(10), message.getOrderQty());
        Assert.assertEquals(new OrdType(OrdType.STOP_LIMIT), message.getOrdType());
        Assert.assertEquals(new quickfix.field.Currency("USD"), message.getCurrency());
        Assert.assertEquals(new SecurityType(SecurityType.COMMON_STOCK), message.getSecurityType());
        Assert.assertEquals(new Price(20.0d), message.getPrice());
        Assert.assertEquals(new StopPx(30.0d), message.getStopPx());
        Assert.assertEquals(new TimeInForce(TimeInForce.DAY), message.getTimeInForce());
    }

    @Test
    public void testStopLimitOrderStockGoodUntil() throws Exception {

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

        SecurityFamily family = new SecurityFamilyImpl();
        family.setCurrency(Currency.USD);

        Stock stock = new StockImpl();
        stock.setSecurityFamily(family);
        stock.setSymbol("APPL");

        Account account = new AccountImpl();
        account.setBroker(Broker.RT);

        StopLimitOrder order = new StopLimitOrderImpl();
        order.setSecurity(stock);
        order.setSide(Side.BUY);
        order.setQuantity(10);
        order.setAccount(account);
        order.setLimit(new BigDecimal("20.0"));
        order.setStop(new BigDecimal("30.0"));
        order.setTif(TIF.GTD);
        order.setTifDateTime(dateFormat.parse("2014-07-01"));

        NewOrderSingle message = this.requestFactory.createNewOrderMessage(order, "test-id");

        Assert.assertNotNull(message);
        Assert.assertEquals(new ClOrdID("test-id"), message.getClOrdID());
        Assert.assertNotNull(message.getTransactTime());
        Assert.assertEquals(new Symbol("APPL"), message.getSymbol());
        Assert.assertEquals(new quickfix.field.Side(quickfix.field.Side.BUY), message.getSide());
        Assert.assertEquals(new OrderQty(10), message.getOrderQty());
        Assert.assertEquals(new OrdType(OrdType.STOP_LIMIT), message.getOrdType());
        Assert.assertEquals(new quickfix.field.Currency("USD"), message.getCurrency());
        Assert.assertEquals(new SecurityType(SecurityType.COMMON_STOCK), message.getSecurityType());
        Assert.assertEquals(new Price(20.0d), message.getPrice());
        Assert.assertEquals(new StopPx(30.0d), message.getStopPx());
        Assert.assertEquals(new TimeInForce(TimeInForce.GOOD_TILL_DATE), message.getTimeInForce());
        Assert.assertEquals(new ExpireTime(dateFormat.parse("2014-07-01")), message.getExpireTime());
    }

    @Test
    public void testModifyMarketOrderOption() throws Exception {

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

        SecurityFamily family = new SecurityFamilyImpl();
        family.setCurrency(Currency.BRL);
        family.setSymbolRoot("STUFF");
        family.setContractSize(100.5d);

        Option option = new OptionImpl();
        option.setSymbol("SOME_STUFF");
        option.setType(OptionType.CALL);
        option.setSecurityFamily(family);
        option.setStrike(new BigDecimal("0.5"));
        option.setExpiration(dateFormat.parse("2014-12-31"));

        Account account = new AccountImpl();
        account.setBroker(Broker.RT);

        MarketOrder order = new MarketOrderImpl();
        order.setIntId("previous-test-id");
        order.setSecurity(option);
        order.setSide(Side.BUY);
        order.setQuantity(2000);
        order.setAccount(account);


        OrderCancelReplaceRequest message = this.requestFactory.createModifyOrderMessage(order, "test-id");

        Assert.assertNotNull(message);
        Assert.assertEquals(new ClOrdID("test-id"), message.getClOrdID());
        Assert.assertEquals(new OrigClOrdID("previous-test-id"), message.getOrigClOrdID());
        Assert.assertEquals(new Symbol("STUFF"), message.getSymbol());
        Assert.assertEquals(new quickfix.field.Side(quickfix.field.Side.BUY), message.getSide());
        Assert.assertEquals(new OrderQty(2000), message.getOrderQty());
        Assert.assertEquals(new OrdType(OrdType.MARKET), message.getOrdType());
        Assert.assertEquals(new StrikePrice(0.5d), message.getStrikePrice());
        Assert.assertEquals(new MaturityMonthYear("201412"), message.getMaturityMonthYear());
    }

    @Test
    public void testModifyMarketOrderFuture() throws Exception {

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

        SecurityFamily family = new SecurityFamilyImpl();
        family.setCurrency(Currency.BRL);
        family.setSymbolRoot("STUFF");

        Future future = new FutureImpl();
        future.setSymbol("SOME_STUFF");
        future.setSecurityFamily(family);
        future.setExpiration(dateFormat.parse("2014-12-31"));

        Account account = new AccountImpl();
        account.setBroker(Broker.RT);

        MarketOrder order = new MarketOrderImpl();
        order.setIntId("previous-test-id");
        order.setSecurity(future);
        order.setSide(Side.BUY);
        order.setQuantity(2000);
        order.setAccount(account);


        OrderCancelReplaceRequest message = this.requestFactory.createModifyOrderMessage(order, "test-id");

        Assert.assertNotNull(message);
        Assert.assertEquals(new ClOrdID("test-id"), message.getClOrdID());
        Assert.assertEquals(new OrigClOrdID("previous-test-id"), message.getOrigClOrdID());
        Assert.assertEquals(new Symbol("STUFF"), message.getSymbol());
        Assert.assertEquals(new quickfix.field.Side(quickfix.field.Side.BUY), message.getSide());
        Assert.assertEquals(new OrderQty(2000), message.getOrderQty());
        Assert.assertEquals(new OrdType(OrdType.MARKET), message.getOrdType());
        Assert.assertEquals(new MaturityMonthYear("201412"), message.getMaturityMonthYear());
    }

    @Test
    public void testModifyMarketOrderStock() throws Exception {

        SecurityFamily family = new SecurityFamilyImpl();
        family.setCurrency(Currency.USD);

        Stock stock = new StockImpl();
        stock.setSecurityFamily(family);
        stock.setSymbol("APPL");

        Account account = new AccountImpl();
        account.setBroker(Broker.RT);

        MarketOrder order = new MarketOrderImpl();
        order.setIntId("previous-test-id");
        order.setSecurity(stock);
        order.setSide(Side.SELL);
        order.setQuantity(10);
        order.setAccount(account);

        OrderCancelReplaceRequest message = this.requestFactory.createModifyOrderMessage(order, "test-id");

        Assert.assertNotNull(message);
        Assert.assertEquals(new ClOrdID("test-id"), message.getClOrdID());
        Assert.assertEquals(new OrigClOrdID("previous-test-id"), message.getOrigClOrdID());
        Assert.assertEquals(new Symbol("APPL"), message.getSymbol());
        Assert.assertEquals(new quickfix.field.Side(quickfix.field.Side.SELL), message.getSide());
        Assert.assertEquals(new OrderQty(10), message.getOrderQty());
        Assert.assertEquals(new OrdType(OrdType.MARKET), message.getOrdType());
    }

    @Test
    public void testModifyMarketOrderForex() throws Exception {

        SecurityFamily family = new SecurityFamilyImpl();
        family.setCurrency(Currency.USD);

        Forex forex = new ForexImpl();
        forex.setSymbol("EUR.USD");
        forex.setLmaxid("4001");
        forex.setBaseCurrency(Currency.EUR);
        forex.setSecurityFamily(family);

        Account account = new AccountImpl();
        account.setBroker(Broker.RT);

        MarketOrder order = new MarketOrderImpl();
        order.setIntId("previous-test-id");
        order.setSecurity(forex);
        order.setSide(Side.BUY);
        order.setQuantity(2000);
        order.setAccount(account);

        OrderCancelReplaceRequest message = this.requestFactory.createModifyOrderMessage(order, "test-id");

        Assert.assertNotNull(message);
        Assert.assertEquals(new ClOrdID("test-id"), message.getClOrdID());
        Assert.assertEquals(new OrigClOrdID("previous-test-id"), message.getOrigClOrdID());
        Assert.assertEquals(new Symbol("EUR"), message.getSymbol());
        Assert.assertEquals(new quickfix.field.Side(quickfix.field.Side.BUY), message.getSide());
        Assert.assertEquals(new OrderQty(2000), message.getOrderQty());
        Assert.assertEquals(new OrdType(OrdType.MARKET), message.getOrdType());
    }

    @Test
    public void testModifyLimitedOrderStock() throws Exception {

        SecurityFamily family = new SecurityFamilyImpl();
        family.setCurrency(Currency.USD);

        Stock stock = new StockImpl();
        stock.setSecurityFamily(family);
        stock.setSymbol("APPL");

        Account account = new AccountImpl();
        account.setBroker(Broker.RT);

        LimitOrder order = new LimitOrderImpl();
        order.setIntId("previous-test-id");
        order.setSecurity(stock);
        order.setSide(Side.BUY);
        order.setQuantity(10);
        order.setAccount(account);
        order.setLimit(new BigDecimal("20.0"));

        OrderCancelReplaceRequest message = this.requestFactory.createModifyOrderMessage(order, "test-id");

        Assert.assertNotNull(message);
        Assert.assertEquals(new ClOrdID("test-id"), message.getClOrdID());
        Assert.assertEquals(new OrigClOrdID("previous-test-id"), message.getOrigClOrdID());
        Assert.assertEquals(new Symbol("APPL"), message.getSymbol());
        Assert.assertEquals(new quickfix.field.Side(quickfix.field.Side.BUY), message.getSide());
        Assert.assertEquals(new OrderQty(10), message.getOrderQty());
        Assert.assertEquals(new OrdType(OrdType.LIMIT), message.getOrdType());
        Assert.assertEquals(new SecurityType(SecurityType.COMMON_STOCK), message.getSecurityType());
        Assert.assertEquals(new Price(20.0d), message.getPrice());
    }

    @Test
    public void testModifyStopOrderStock() throws Exception {

        SecurityFamily family = new SecurityFamilyImpl();
        family.setCurrency(Currency.USD);

        Stock stock = new StockImpl();
        stock.setSecurityFamily(family);
        stock.setSymbol("APPL");

        Account account = new AccountImpl();
        account.setBroker(Broker.RT);

        StopOrder order = new StopOrderImpl();
        order.setIntId("previous-test-id");
        order.setSecurity(stock);
        order.setSide(Side.BUY);
        order.setQuantity(10);
        order.setAccount(account);
        order.setStop(new BigDecimal("20.0"));

        OrderCancelReplaceRequest message = this.requestFactory.createModifyOrderMessage(order, "test-id");

        Assert.assertNotNull(message);
        Assert.assertEquals(new ClOrdID("test-id"), message.getClOrdID());
        Assert.assertEquals(new OrigClOrdID("previous-test-id"), message.getOrigClOrdID());
        Assert.assertEquals(new Symbol("APPL"), message.getSymbol());
        Assert.assertEquals(new quickfix.field.Side(quickfix.field.Side.BUY), message.getSide());
        Assert.assertEquals(new OrderQty(10), message.getOrderQty());
        Assert.assertEquals(new OrdType(OrdType.STOP), message.getOrdType());
        Assert.assertEquals(new StopPx(20.0d), message.getStopPx());
    }

    @Test
    public void testModifyStopLimitOrderStockOneDay() throws Exception {

        SecurityFamily family = new SecurityFamilyImpl();
        family.setCurrency(Currency.USD);

        Stock stock = new StockImpl();
        stock.setSecurityFamily(family);
        stock.setSymbol("APPL");

        Account account = new AccountImpl();
        account.setBroker(Broker.RT);

        StopLimitOrder order = new StopLimitOrderImpl();
        order.setIntId("previous-test-id");
        order.setSecurity(stock);
        order.setSide(Side.BUY);
        order.setQuantity(10);
        order.setAccount(account);
        order.setLimit(new BigDecimal("20.0"));
        order.setStop(new BigDecimal("30.0"));
        order.setTif(TIF.DAY);

        OrderCancelReplaceRequest message = this.requestFactory.createModifyOrderMessage(order, "test-id");

        Assert.assertNotNull(message);
        Assert.assertEquals(new ClOrdID("test-id"), message.getClOrdID());
        Assert.assertEquals(new OrigClOrdID("previous-test-id"), message.getOrigClOrdID());
        Assert.assertEquals(new Symbol("APPL"), message.getSymbol());
        Assert.assertEquals(new quickfix.field.Side(quickfix.field.Side.BUY), message.getSide());
        Assert.assertEquals(new OrderQty(10), message.getOrderQty());
        Assert.assertEquals(new OrdType(OrdType.STOP_LIMIT), message.getOrdType());
        Assert.assertEquals(new Price(20.0d), message.getPrice());
        Assert.assertEquals(new StopPx(30.0d), message.getStopPx());
        Assert.assertEquals(new TimeInForce(TimeInForce.DAY), message.getTimeInForce());
    }

    @Test
    public void testModifyStopLimitOrderStockGoodUntil() throws Exception {

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

        SecurityFamily family = new SecurityFamilyImpl();
        family.setCurrency(Currency.USD);

        Stock stock = new StockImpl();
        stock.setSecurityFamily(family);
        stock.setSymbol("APPL");

        Account account = new AccountImpl();
        account.setBroker(Broker.RT);

        StopLimitOrder order = new StopLimitOrderImpl();
        order.setIntId("previous-test-id");
        order.setSecurity(stock);
        order.setSide(Side.BUY);
        order.setQuantity(10);
        order.setAccount(account);
        order.setLimit(new BigDecimal("20.0"));
        order.setStop(new BigDecimal("30.0"));
        order.setTif(TIF.GTD);
        order.setTifDateTime(dateFormat.parse("2014-07-01"));

        OrderCancelReplaceRequest message = this.requestFactory.createModifyOrderMessage(order, "test-id");

        Assert.assertNotNull(message);
        Assert.assertEquals(new ClOrdID("test-id"), message.getClOrdID());
        Assert.assertEquals(new OrigClOrdID("previous-test-id"), message.getOrigClOrdID());
        Assert.assertEquals(new Symbol("APPL"), message.getSymbol());
        Assert.assertEquals(new quickfix.field.Side(quickfix.field.Side.BUY), message.getSide());
        Assert.assertEquals(new OrderQty(10), message.getOrderQty());
        Assert.assertEquals(new OrdType(OrdType.STOP_LIMIT), message.getOrdType());
        Assert.assertEquals(new Price(20.0d), message.getPrice());
        Assert.assertEquals(new StopPx(30.0d), message.getStopPx());
        Assert.assertEquals(new TimeInForce(TimeInForce.GOOD_TILL_DATE), message.getTimeInForce());
        Assert.assertEquals(new ExpireTime(dateFormat.parse("2014-07-01")), message.getExpireTime());
    }

    @Test
    public void testCancelMarketOrderOption() throws Exception {

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

        SecurityFamily family = new SecurityFamilyImpl();
        family.setCurrency(Currency.BRL);
        family.setSymbolRoot("STUFF");
        family.setContractSize(100.5d);

        Option option = new OptionImpl();
        option.setSymbol("SOME_STUFF");
        option.setType(OptionType.CALL);
        option.setSecurityFamily(family);
        option.setStrike(new BigDecimal("0.5"));
        option.setExpiration(dateFormat.parse("2014-12-31"));

        Account account = new AccountImpl();
        account.setBroker(Broker.RT);

        MarketOrder order = new MarketOrderImpl();
        order.setIntId("previous-test-id");
        order.setSecurity(option);
        order.setSide(Side.BUY);
        order.setQuantity(2000);
        order.setAccount(account);

        OrderCancelRequest message = this.requestFactory.createOrderCancelMessage(order, "test-id");

        Assert.assertNotNull(message);
        Assert.assertEquals(new ClOrdID("test-id"), message.getClOrdID());
        Assert.assertEquals(new Symbol("STUFF"), message.getSymbol());
        Assert.assertEquals(new quickfix.field.Side(quickfix.field.Side.BUY), message.getSide());
        Assert.assertEquals(new OrderQty(2000), message.getOrderQty());
        Assert.assertEquals(new SecurityType(SecurityType.OPTION), message.getSecurityType());
        Assert.assertEquals(new StrikePrice(0.5d), message.getStrikePrice());
        Assert.assertEquals(new MaturityMonthYear("201412"), message.getMaturityMonthYear());
    }

    @Test
    public void testCancelMarketOrderFuture() throws Exception {

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

        SecurityFamily family = new SecurityFamilyImpl();
        family.setCurrency(Currency.BRL);
        family.setSymbolRoot("STUFF");

        Future future = new FutureImpl();
        future.setSymbol("SOME_STUFF");
        future.setSecurityFamily(family);
        future.setExpiration(dateFormat.parse("2014-12-31"));

        Account account = new AccountImpl();
        account.setBroker(Broker.RT);

        MarketOrder order = new MarketOrderImpl();
        order.setIntId("previous-test-id");
        order.setSecurity(future);
        order.setSide(Side.BUY);
        order.setQuantity(2000);
        order.setAccount(account);


        OrderCancelRequest message = this.requestFactory.createOrderCancelMessage(order, "test-id");

        Assert.assertNotNull(message);
        Assert.assertEquals(new ClOrdID("test-id"), message.getClOrdID());
        Assert.assertEquals(new Symbol("STUFF"), message.getSymbol());
        Assert.assertEquals(new quickfix.field.Side(quickfix.field.Side.BUY), message.getSide());
        Assert.assertEquals(new OrderQty(2000), message.getOrderQty());
        Assert.assertEquals(new SecurityType(SecurityType.FUTURE), message.getSecurityType());
        Assert.assertEquals(new MaturityMonthYear("201412"), message.getMaturityMonthYear());
    }

    @Test
    public void testCancelMarketOrderStock() throws Exception {

        SecurityFamily family = new SecurityFamilyImpl();
        family.setCurrency(Currency.USD);

        Stock stock = new StockImpl();
        stock.setSecurityFamily(family);
        stock.setSymbol("APPL");

        Account account = new AccountImpl();
        account.setBroker(Broker.RT);

        MarketOrder order = new MarketOrderImpl();
        order.setIntId("previous-test-id");
        order.setSecurity(stock);
        order.setSide(Side.SELL);
        order.setQuantity(10);
        order.setAccount(account);

        OrderCancelRequest message = this.requestFactory.createOrderCancelMessage(order, "test-id");

        Assert.assertNotNull(message);
        Assert.assertEquals(new ClOrdID("test-id"), message.getClOrdID());
        Assert.assertEquals(new Symbol("APPL"), message.getSymbol());
        Assert.assertEquals(new quickfix.field.Side(quickfix.field.Side.SELL), message.getSide());
        Assert.assertEquals(new OrderQty(10), message.getOrderQty());
        Assert.assertEquals(new SecurityType(SecurityType.COMMON_STOCK), message.getSecurityType());
    }

    @Test
    public void testCancelMarketOrderForex() throws Exception {

        SecurityFamily family = new SecurityFamilyImpl();
        family.setCurrency(Currency.USD);

        Forex forex = new ForexImpl();
        forex.setSymbol("EUR.USD");
        forex.setLmaxid("4001");
        forex.setBaseCurrency(Currency.EUR);
        forex.setSecurityFamily(family);

        Account account = new AccountImpl();
        account.setBroker(Broker.RT);

        MarketOrder order = new MarketOrderImpl();
        order.setIntId("previous-test-id");
        order.setSecurity(forex);
        order.setSide(Side.BUY);
        order.setQuantity(2000);
        order.setAccount(account);

        OrderCancelRequest message = this.requestFactory.createOrderCancelMessage(order, "test-id");

        Assert.assertNotNull(message);
        Assert.assertEquals(new ClOrdID("test-id"), message.getClOrdID());
        Assert.assertEquals(new Symbol("EUR"), message.getSymbol());
        Assert.assertEquals(new quickfix.field.Side(quickfix.field.Side.BUY), message.getSide());
        Assert.assertEquals(new OrderQty(2000), message.getOrderQty());
        Assert.assertEquals(new SecurityType(SecurityType.CASH), message.getSecurityType());
    }

    @Test
    public void testMarketOrderWithProperties() throws Exception {

        SecurityFamily family = new SecurityFamilyImpl();
        family.setCurrency(Currency.USD);

        Forex forex = new ForexImpl();
        forex.setSymbol("EUR.USD");
        forex.setBaseCurrency(Currency.EUR);
        forex.setSecurityFamily(family);

        Account account = new AccountImpl();
        account.setBroker(Broker.RT);

        MarketOrder order = new MarketOrderImpl();
        order.setSecurity(forex);
        order.setSide(Side.BUY);
        order.setQuantity(2000);
        order.setAccount(account);
        OrderPropertyImpl orderProperty1 = new OrderPropertyImpl();
        orderProperty1.setFix(true);
        orderProperty1.setValue("this stuff");
        OrderPropertyImpl orderProperty2 = new OrderPropertyImpl();
        orderProperty2.setFix(true);
        orderProperty2.setValue("that stuff");
        OrderPropertyImpl orderProperty3 = new OrderPropertyImpl();
        orderProperty3.setFix(false);
        orderProperty3.setValue("other stuff");
        order.getOrderProperties().put("1000", orderProperty1);
        order.getOrderProperties().put("1001", orderProperty2);
        order.getOrderProperties().put("1002", orderProperty3);

        NewOrderSingle message = this.requestFactory.createNewOrderMessage(order, "test-id");

        Assert.assertNotNull(message);
        Assert.assertEquals(new ClOrdID("test-id"), message.getClOrdID());
        Assert.assertNotNull(message.getTransactTime());
        Assert.assertEquals(new Symbol("EUR"), message.getSymbol());
        Assert.assertEquals(new quickfix.field.Side(quickfix.field.Side.BUY), message.getSide());
        Assert.assertEquals(new OrderQty(2000), message.getOrderQty());
        Assert.assertEquals(new OrdType(OrdType.MARKET), message.getOrdType());
        Assert.assertEquals(new quickfix.field.Currency("USD"), message.getCurrency());
        Assert.assertEquals(new SecurityType(SecurityType.CASH), message.getSecurityType());

        Assert.assertEquals("this stuff", message.getString(1000));
        Assert.assertEquals("that stuff", message.getString(1001));
        Assert.assertFalse(message.isSetField(1002));
    }
}
