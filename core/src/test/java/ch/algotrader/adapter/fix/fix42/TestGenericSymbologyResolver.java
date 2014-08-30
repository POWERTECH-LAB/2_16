package ch.algotrader.adapter.fix.fix42;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ch.algotrader.entity.security.Forex;
import ch.algotrader.entity.security.ForexImpl;
import ch.algotrader.entity.security.Future;
import ch.algotrader.entity.security.FutureImpl;
import ch.algotrader.entity.security.Option;
import ch.algotrader.entity.security.OptionImpl;
import ch.algotrader.entity.security.SecurityFamily;
import ch.algotrader.entity.security.SecurityFamilyImpl;
import ch.algotrader.entity.security.Stock;
import ch.algotrader.entity.security.StockImpl;
import ch.algotrader.enumeration.Broker;
import ch.algotrader.enumeration.Currency;
import ch.algotrader.enumeration.OptionType;
import quickfix.field.ContractMultiplier;
import quickfix.field.MaturityDay;
import quickfix.field.MaturityMonthYear;
import quickfix.field.SecurityType;
import quickfix.field.StrikePrice;
import quickfix.field.Symbol;
import quickfix.fix42.NewOrderSingle;
import quickfix.fix42.OrderCancelReplaceRequest;
import quickfix.fix42.OrderCancelRequest;

public class TestGenericSymbologyResolver {

    private GenericFix42SymbologyResolver symbologyResolver;

    @Before
    public void setup() throws Exception {

        this.symbologyResolver = new GenericFix42SymbologyResolver();
    }

    @Test
    public void testMarketOrderOption() throws Exception {

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

        NewOrderSingle message = new NewOrderSingle();

        this.symbologyResolver.resolve(message, option, Broker.RT);

        Assert.assertEquals(new Symbol("STUFF"), message.getSymbol());
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

        NewOrderSingle message = new NewOrderSingle();

        this.symbologyResolver.resolve(message, future, Broker.RT);

        Assert.assertEquals(new Symbol("STUFF"), message.getSymbol());
        Assert.assertEquals(new quickfix.field.Currency("BRL"), message.getCurrency());
        Assert.assertEquals(new SecurityType(SecurityType.FUTURE), message.getSecurityType());
        Assert.assertEquals(new MaturityDay("31"), message.getMaturityDay());
        Assert.assertEquals(new MaturityMonthYear("201412"), message.getMaturityMonthYear());
    }

    @Test
    public void testMarketOrderStock() throws Exception {

        SecurityFamily family = new SecurityFamilyImpl();
        family.setCurrency(Currency.USD);

        Stock stock = new StockImpl();
        stock.setSecurityFamily(family);
        stock.setSymbol("APPL");

        NewOrderSingle message = new NewOrderSingle();

        this.symbologyResolver.resolve(message, stock, Broker.RT);

        Assert.assertEquals(new Symbol("APPL"), message.getSymbol());
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

        NewOrderSingle message = new NewOrderSingle();

        this.symbologyResolver.resolve(message, forex, Broker.RT);

        Assert.assertEquals(new Symbol("EUR"), message.getSymbol());
        Assert.assertEquals(new quickfix.field.Currency("USD"), message.getCurrency());
        Assert.assertEquals(new SecurityType(SecurityType.CASH), message.getSecurityType());
    }

    @Test
    public void testModifyOrderOption() throws Exception {

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

        OrderCancelReplaceRequest message = new OrderCancelReplaceRequest();

        this.symbologyResolver.resolve(message, option, Broker.RT);

        Assert.assertEquals(new Symbol("STUFF"), message.getSymbol());
        Assert.assertEquals(new StrikePrice(0.5d), message.getStrikePrice());
        Assert.assertEquals(new MaturityMonthYear("201412"), message.getMaturityMonthYear());
    }

    @Test
    public void testModifyOrderFuture() throws Exception {

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

        SecurityFamily family = new SecurityFamilyImpl();
        family.setCurrency(Currency.BRL);
        family.setSymbolRoot("STUFF");

        Future future = new FutureImpl();
        future.setSymbol("SOME_STUFF");
        future.setSecurityFamily(family);
        future.setExpiration(dateFormat.parse("2014-12-31"));

        OrderCancelReplaceRequest message = new OrderCancelReplaceRequest();

        this.symbologyResolver.resolve(message, future, Broker.RT);

        Assert.assertEquals(new Symbol("STUFF"), message.getSymbol());
        Assert.assertEquals(new MaturityMonthYear("201412"), message.getMaturityMonthYear());
    }

    @Test
    public void testModifyOrderStock() throws Exception {

        SecurityFamily family = new SecurityFamilyImpl();
        family.setCurrency(Currency.USD);

        Stock stock = new StockImpl();
        stock.setSecurityFamily(family);
        stock.setSymbol("APPL");

        OrderCancelReplaceRequest message = new OrderCancelReplaceRequest();

        this.symbologyResolver.resolve(message, stock, Broker.RT);

        Assert.assertEquals(new Symbol("APPL"), message.getSymbol());
    }

    @Test
    public void testModifyOrderForex() throws Exception {

        SecurityFamily family = new SecurityFamilyImpl();
        family.setCurrency(Currency.USD);

        Forex forex = new ForexImpl();
        forex.setSymbol("EUR.USD");
        forex.setLmaxid("4001");
        forex.setBaseCurrency(Currency.EUR);
        forex.setSecurityFamily(family);

        OrderCancelReplaceRequest message = new OrderCancelReplaceRequest();

        this.symbologyResolver.resolve(message, forex, Broker.RT);

        Assert.assertEquals(new Symbol("EUR"), message.getSymbol());
    }

    @Test
    public void testCanceOrderOption() throws Exception {

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

        OrderCancelRequest message = new OrderCancelRequest();

        this.symbologyResolver.resolve(message, option, Broker.RT);

        Assert.assertEquals(new Symbol("STUFF"), message.getSymbol());
        Assert.assertEquals(new SecurityType(SecurityType.OPTION), message.getSecurityType());
        Assert.assertEquals(new StrikePrice(0.5d), message.getStrikePrice());
        Assert.assertEquals(new MaturityMonthYear("201412"), message.getMaturityMonthYear());
    }

    @Test
    public void testCanceOrderFuture() throws Exception {

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

        SecurityFamily family = new SecurityFamilyImpl();
        family.setCurrency(Currency.BRL);
        family.setSymbolRoot("STUFF");

        Future future = new FutureImpl();
        future.setSymbol("SOME_STUFF");
        future.setSecurityFamily(family);
        future.setExpiration(dateFormat.parse("2014-12-31"));

        OrderCancelRequest message = new OrderCancelRequest();

        this.symbologyResolver.resolve(message, future, Broker.RT);

        Assert.assertEquals(new Symbol("STUFF"), message.getSymbol());
        Assert.assertEquals(new SecurityType(SecurityType.FUTURE), message.getSecurityType());
        Assert.assertEquals(new MaturityMonthYear("201412"), message.getMaturityMonthYear());
    }

    @Test
    public void testCanceOrderStock() throws Exception {

        SecurityFamily family = new SecurityFamilyImpl();
        family.setCurrency(Currency.USD);

        Stock stock = new StockImpl();
        stock.setSecurityFamily(family);
        stock.setSymbol("APPL");

        OrderCancelRequest message = new OrderCancelRequest();

        this.symbologyResolver.resolve(message, stock, Broker.RT);

        Assert.assertEquals(new Symbol("APPL"), message.getSymbol());
    }

    @Test
    public void testCanceOrderForex() throws Exception {

        SecurityFamily family = new SecurityFamilyImpl();
        family.setCurrency(Currency.USD);

        Forex forex = new ForexImpl();
        forex.setSymbol("EUR.USD");
        forex.setLmaxid("4001");
        forex.setBaseCurrency(Currency.EUR);
        forex.setSecurityFamily(family);

        OrderCancelRequest message = new OrderCancelRequest();

        this.symbologyResolver.resolve(message, forex, Broker.RT);

        Assert.assertEquals(new Symbol("EUR"), message.getSymbol());
    }

}
