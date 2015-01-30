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
package ch.algotrader.vo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Map;

import ch.algotrader.enumeration.Currency;

/**
 * A ValueObject representing a {@link ch.algotrader.entity.Position Position}. Used for Client display.
 */
public class PositionVO implements Serializable {

    private static final long serialVersionUID = 9152785353277989943L;

    /**
     * The Id of the Position.
     */
    private int id;

    /**
     * boolean setter for primitive attribute, so we can tell if it's initialized
     */
    private boolean setId = false;

    /**
     * The Id of the Security.
     */
    private int securityId;

    /**
     * boolean setter for primitive attribute, so we can tell if it's initialized
     */
    private boolean setSecurityId = false;

    /**
     * The current quantity of this Position.
     */
    private long quantity;

    /**
     * boolean setter for primitive attribute, so we can tell if it's initialized
     */
    private boolean setQuantity = false;

    /**
     * The Symbol of the associated Security
     */
    private String name;

    /**
     * The name of the Strategy
     */
    private String strategy;

    /**
     * The Currency of the associated Security
     */
    private Currency currency;

    /**
     * Either {@code bid} or {@code ask} depending on the direction of the position.
     */
    private BigDecimal marketPrice;

    /**
     * The value of the position based on either {@code bid} or {@code ask} depending on the
     * direction of the position.
     */
    private BigDecimal marketValue;

    /**
     * The average price of the position based on all relevant opening transactions.
     */
    private BigDecimal averagePrice;

    /**
     * The total cost of the position based on all relevant opening transactions.
     */
    private BigDecimal cost;

    /**
     * The unrealized Profit-and-Loss for this Position.
     */
    private BigDecimal unrealizedPL;

    /**
     * The realized Profit-and-Loss for this Position.
     */
    private BigDecimal realizedPL;

    /**
     * The price at which the Position will be closed.
     */
    private BigDecimal exitValue;

    /**
     * The potential loss that would incur, if the price reached the {@code exitValue}
     */
    private BigDecimal maxLoss;

    /**
     * The current margin needed by this Position.
     */
    private BigDecimal margin;

    /**
     * Any {@link ch.algotrader.entity.property.Property Property Properties} associated with the
     * Position
     */
    private Map properties;

    /**
     * Default Constructor
     */
    public PositionVO() {

        // Documented empty block - avoid compiler warning - no super constructor
    }

    /**
     * Constructor with all properties
     * @param idIn int
     * @param securityIdIn int
     * @param quantityIn long
     * @param nameIn String
     * @param strategyIn String
     * @param currencyIn Currency
     * @param marketPriceIn BigDecimal
     * @param marketValueIn BigDecimal
     * @param averagePriceIn BigDecimal
     * @param costIn BigDecimal
     * @param unrealizedPLIn BigDecimal
     * @param realizedPLIn BigDecimal
     * @param exitValueIn BigDecimal
     * @param maxLossIn BigDecimal
     * @param marginIn BigDecimal
     * @param propertiesIn Map
     */
    public PositionVO(final int idIn, final int securityIdIn, final long quantityIn, final String nameIn, final String strategyIn, final Currency currencyIn, final BigDecimal marketPriceIn,
            final BigDecimal marketValueIn, final BigDecimal averagePriceIn, final BigDecimal costIn, final BigDecimal unrealizedPLIn, final BigDecimal realizedPLIn, final BigDecimal exitValueIn,
            final BigDecimal maxLossIn, final BigDecimal marginIn, final Map propertiesIn) {

        this.id = idIn;
        this.setId = true;
        this.securityId = securityIdIn;
        this.setSecurityId = true;
        this.quantity = quantityIn;
        this.setQuantity = true;
        this.name = nameIn;
        this.strategy = strategyIn;
        this.currency = currencyIn;
        this.marketPrice = marketPriceIn;
        this.marketValue = marketValueIn;
        this.averagePrice = averagePriceIn;
        this.cost = costIn;
        this.unrealizedPL = unrealizedPLIn;
        this.realizedPL = realizedPLIn;
        this.exitValue = exitValueIn;
        this.maxLoss = maxLossIn;
        this.margin = marginIn;
        this.properties = propertiesIn;
    }

    /**
     * Copies constructor from other PositionVO
     *
     * @param otherBean Cannot be <code>null</code>
     * @throws NullPointerException if the argument is <code>null</code>
     */
    public PositionVO(final PositionVO otherBean) {

        this.id = otherBean.getId();
        this.setId = true;
        this.securityId = otherBean.getSecurityId();
        this.setSecurityId = true;
        this.quantity = otherBean.getQuantity();
        this.setQuantity = true;
        this.name = otherBean.getName();
        this.strategy = otherBean.getStrategy();
        this.currency = otherBean.getCurrency();
        this.marketPrice = otherBean.getMarketPrice();
        this.marketValue = otherBean.getMarketValue();
        this.averagePrice = otherBean.getAveragePrice();
        this.cost = otherBean.getCost();
        this.unrealizedPL = otherBean.getUnrealizedPL();
        this.realizedPL = otherBean.getRealizedPL();
        this.exitValue = otherBean.getExitValue();
        this.maxLoss = otherBean.getMaxLoss();
        this.margin = otherBean.getMargin();
        this.properties = otherBean.getProperties();
    }

    /**
     * The Id of the Position.
     * @return id int
     */
    public int getId() {

        return this.id;
    }

    /**
     * The Id of the Position.
     * @param value int
     */
    public void setId(final int value) {

        this.id = value;
        this.setId = true;
    }

    /**
     * Return true if the primitive attribute id is set, through the setter or constructor
     * @return true if the attribute value has been set
     */
    public boolean isSetId() {

        return this.setId;
    }

    /**
     * The Id of the Security.
     * @return securityId int
     */
    public int getSecurityId() {

        return this.securityId;
    }

    /**
     * The Id of the Security.
     * @param value int
     */
    public void setSecurityId(final int value) {

        this.securityId = value;
        this.setSecurityId = true;
    }

    /**
     * Return true if the primitive attribute securityId is set, through the setter or constructor
     * @return true if the attribute value has been set
     */
    public boolean isSetSecurityId() {

        return this.setSecurityId;
    }

    /**
     * The current quantity of this Position.
     * @return quantity long
     */
    public long getQuantity() {

        return this.quantity;
    }

    /**
     * The current quantity of this Position.
     * @param value long
     */
    public void setQuantity(final long value) {

        this.quantity = value;
        this.setQuantity = true;
    }

    /**
     * Return true if the primitive attribute quantity is set, through the setter or constructor
     * @return true if the attribute value has been set
     */
    public boolean isSetQuantity() {

        return this.setQuantity;
    }

    /**
     * The Symbol of the associated Security
     * @return name String
     */
    public String getName() {

        return this.name;
    }

    /**
     * The Symbol of the associated Security
     * @param value String
     */
    public void setName(final String value) {

        this.name = value;
    }

    /**
     * The name of the Strategy
     * @return strategy String
     */
    public String getStrategy() {

        return this.strategy;
    }

    /**
     * The name of the Strategy
     * @param value String
     */
    public void setStrategy(final String value) {

        this.strategy = value;
    }

    /**
     * The Currency of the associated Security
     * @return currency Currency
     */
    public Currency getCurrency() {

        return this.currency;
    }

    /**
     * The Currency of the associated Security
     * @param value Currency
     */
    public void setCurrency(final Currency value) {

        this.currency = value;
    }

    /**
     * Either {@code bid} or {@code ask} depending on the direction of the position.
     * @return marketPrice BigDecimal
     */
    public BigDecimal getMarketPrice() {

        return this.marketPrice;
    }

    /**
     * Either {@code bid} or {@code ask} depending on the direction of the position.
     * @param value BigDecimal
     */
    public void setMarketPrice(final BigDecimal value) {

        this.marketPrice = value;
    }

    /**
     * The value of the position based on either {@code bid} or {@code ask} depending on the
     * direction of the position.
     * @return marketValue BigDecimal
     */
    public BigDecimal getMarketValue() {

        return this.marketValue;
    }

    /**
     * The value of the position based on either {@code bid} or {@code ask} depending on the
     * direction of the position.
     * @param value BigDecimal
     */
    public void setMarketValue(final BigDecimal value) {

        this.marketValue = value;
    }

    /**
     * The average price of the position based on all relevant opening transactions.
     * @return averagePrice BigDecimal
     */
    public BigDecimal getAveragePrice() {

        return this.averagePrice;
    }

    /**
     * The average price of the position based on all relevant opening transactions.
     * @param value BigDecimal
     */
    public void setAveragePrice(final BigDecimal value) {

        this.averagePrice = value;
    }

    /**
     * The total cost of the position based on all relevant opening transactions.
     * @return cost BigDecimal
     */
    public BigDecimal getCost() {

        return this.cost;
    }

    /**
     * The total cost of the position based on all relevant opening transactions.
     * @param value BigDecimal
     */
    public void setCost(final BigDecimal value) {

        this.cost = value;
    }

    /**
     * The unrealized Profit-and-Loss for this Position.
     * @return unrealizedPL BigDecimal
     */
    public BigDecimal getUnrealizedPL() {

        return this.unrealizedPL;
    }

    /**
     * The unrealized Profit-and-Loss for this Position.
     * @param value BigDecimal
     */
    public void setUnrealizedPL(final BigDecimal value) {

        this.unrealizedPL = value;
    }

    /**
     * The realized Profit-and-Loss for this Position.
     * @return realizedPL BigDecimal
     */
    public BigDecimal getRealizedPL() {

        return this.realizedPL;
    }

    /**
     * The realized Profit-and-Loss for this Position.
     * @param value BigDecimal
     */
    public void setRealizedPL(final BigDecimal value) {

        this.realizedPL = value;
    }

    /**
     * The price at which the Position will be closed.
     * @return exitValue BigDecimal
     */
    public BigDecimal getExitValue() {

        return this.exitValue;
    }

    /**
     * The price at which the Position will be closed.
     * @param value BigDecimal
     */
    public void setExitValue(final BigDecimal value) {

        this.exitValue = value;
    }

    /**
     * The potential loss that would incur, if the price reached the {@code exitValue}
     * @return maxLoss BigDecimal
     */
    public BigDecimal getMaxLoss() {

        return this.maxLoss;
    }

    /**
     * The potential loss that would incur, if the price reached the {@code exitValue}
     * @param value BigDecimal
     */
    public void setMaxLoss(final BigDecimal value) {

        this.maxLoss = value;
    }

    /**
     * The current margin needed by this Position.
     * @return margin BigDecimal
     */
    public BigDecimal getMargin() {

        return this.margin;
    }

    /**
     * The current margin needed by this Position.
     * @param value BigDecimal
     */
    public void setMargin(final BigDecimal value) {

        this.margin = value;
    }

    /**
     * Any {@link ch.algotrader.entity.property.Property Property Properties} associated with the Position
     * @return properties Map
     */
    public Map getProperties() {

        return this.properties;
    }

    /**
     * Any {@link ch.algotrader.entity.property.Property Property Properties} associated with the Position
     * @param value Map
     */
    public void setProperties(final Map value) {

        this.properties = value;
    }

    @Override
    public String toString() {

        StringBuilder builder = new StringBuilder();
        builder.append("PositionVO [id=");
        builder.append(this.id);
        builder.append(", setId=");
        builder.append(this.setId);
        builder.append(", securityId=");
        builder.append(this.securityId);
        builder.append(", setSecurityId=");
        builder.append(this.setSecurityId);
        builder.append(", quantity=");
        builder.append(this.quantity);
        builder.append(", setQuantity=");
        builder.append(this.setQuantity);
        builder.append(", name=");
        builder.append(this.name);
        builder.append(", strategy=");
        builder.append(this.strategy);
        builder.append(", currency=");
        builder.append(this.currency);
        builder.append(", marketPrice=");
        builder.append(this.marketPrice);
        builder.append(", marketValue=");
        builder.append(this.marketValue);
        builder.append(", averagePrice=");
        builder.append(this.averagePrice);
        builder.append(", cost=");
        builder.append(this.cost);
        builder.append(", unrealizedPL=");
        builder.append(this.unrealizedPL);
        builder.append(", realizedPL=");
        builder.append(this.realizedPL);
        builder.append(", exitValue=");
        builder.append(this.exitValue);
        builder.append(", maxLoss=");
        builder.append(this.maxLoss);
        builder.append(", margin=");
        builder.append(this.margin);
        builder.append(", properties=");
        builder.append(this.properties);
        builder.append("]");

        return builder.toString();
    }

}
