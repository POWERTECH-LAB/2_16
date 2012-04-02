package com.algoTrader.entity.security;

import java.util.Date;

import org.apache.commons.math.MathException;
import org.apache.log4j.Logger;

import com.algoTrader.entity.marketData.Tick;
import com.algoTrader.stockOption.StockOptionUtil;
import com.algoTrader.util.DateUtil;
import com.algoTrader.util.MyLogger;

public class StockOptionImpl extends StockOption {

    private static final long serialVersionUID = -3168298592370987085L;
    private static Logger logger = MyLogger.getLogger(StockOptionImpl.class.getName());

    @Override
    public double getLeverage() {

        try {
            double underlyingSpot = getUnderlying().getLastTick().getCurrentValueDouble();
            double currentValue = getLastTick().getCurrentValueDouble();
            double delta = StockOptionUtil.getDelta(this, currentValue, underlyingSpot);

            return underlyingSpot / currentValue * delta;

        } catch (Exception e) {

            return Double.NaN;
        }
    }

    @Override
    public double getMargin() {

        Tick stockOptionTick = getLastTick();
        Tick underlyingTick = getUnderlying().getLastTick();

        double marginPerContract = 0;
        if (stockOptionTick != null && underlyingTick != null && stockOptionTick.getCurrentValueDouble() > 0.0) {

            double stockOptionSettlement = stockOptionTick.getSettlement().doubleValue();
            double underlyingSettlement = underlyingTick.getSettlement().doubleValue();
            int contractSize = getSecurityFamily().getContractSize();
            try {
                marginPerContract = StockOptionUtil.getMaintenanceMargin(this, stockOptionSettlement, underlyingSettlement) * contractSize;
            } catch (MathException e) {
                logger.warn("could not calculate margin for " + getSymbol(), e);
            }
        } else {
            logger.warn("no last tick available or currentValue to low to set margin on " + getSymbol());
        }
        return marginPerContract;
    }

    @Override
    public long getTimeToExpiration() {

        return getExpiration().getTime() - DateUtil.getCurrentEPTime().getTime();
    }

    @Override
    public int getDuration() {

        StockOptionFamily family = (StockOptionFamily) this.getSecurityFamily();
        Date nextExpDate = DateUtil.getExpirationDate(family.getExpirationType(), DateUtil.getCurrentEPTime());
        return 1 + (int) Math.round(((this.getExpiration().getTime() - nextExpDate.getTime()) / 2592000000d));
    }

    /**
     * make sure expiration is a java.util.Date and not a java.sql.TimeStamp
     */
    @Override
    public Date getExpiration() {
        return new Date(super.getExpiration().getTime());
    }

    @Override
    public boolean validateTick(Tick tick) {

        // stockOptions need to have a bis/ask volume / openIntrest
        // but might not have a last/lastDateTime yet on the current day
        if (tick.getVolBid() == 0) {
            return false;
        } else if (tick.getVolAsk() == 0) {
            return false;
        } else if (tick.getOpenIntrest() == 0) {
            return false;
        } else if (tick.getBid() != null && tick.getBid().doubleValue() <= 0) {
            return false;
        } else if (tick.getAsk() != null && tick.getAsk().doubleValue() <= 0) {
            return false;
        }

        return super.validateTick(tick);
    }
}
