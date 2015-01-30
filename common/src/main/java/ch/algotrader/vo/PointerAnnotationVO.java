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

import java.util.Date;

/**
 * Contains a Pointer Annotation represented as an Arrow in the Chart
 */
public class PointerAnnotationVO extends AnnotationVO {

    private static final long serialVersionUID = -2750294989105077126L;

    /**
     * The dateTime of this Pointer Annotation
     */
    private Date dateTime;

    /**
     * The value of this Pointer Annotation
     */
    private double value;

    /**
     * boolean setter for primitive attribute, so we can tell if it's initialized
     */
    private boolean setValue = false;

    /**
     * The text to display.
     */
    private String text;

    /**
     * Default Constructor
     */
    public PointerAnnotationVO() {

        super();
    }

    /**
     * Constructor with all properties
     * @param dateTimeIn Date
     * @param valueIn double
     * @param textIn String
     */
    public PointerAnnotationVO(final Date dateTimeIn, final double valueIn, final String textIn) {

        super();

        this.dateTime = dateTimeIn;
        this.value = valueIn;
        this.setValue = true;
        this.text = textIn;
    }

    /**
     * Copies constructor from other PointerAnnotationVO
     *
     * @param otherBean Cannot be <code>null</code>
     * @throws NullPointerException if the argument is <code>null</code>
     */
    public PointerAnnotationVO(final PointerAnnotationVO otherBean) {

        super(otherBean);

        this.dateTime = otherBean.getDateTime();
        this.value = otherBean.getValue();
        this.setValue = true;
        this.text = otherBean.getText();
    }

    /**
     * The dateTime of this Pointer Annotation
     * @return dateTime Date
     */
    public Date getDateTime() {

        return this.dateTime;
    }

    /**
     * The dateTime of this Pointer Annotation
     * @param value Date
     */
    public void setDateTime(final Date value) {

        this.dateTime = value;
    }

    /**
     * The value of this Pointer Annotation
     * @return value double
     */
    public double getValue() {

        return this.value;
    }

    /**
     * The value of this Pointer Annotation
     * @param value double
     */
    public void setValue(final double value) {

        this.value = value;
        this.setValue = true;
    }

    /**
     * Return true if the primitive attribute value is set, through the setter or constructor
     * @return true if the attribute value has been set
     */
    public boolean isSetValue() {

        return this.setValue;
    }

    /**
     * The text to display.
     * @return text String
     */
    public String getText() {

        return this.text;
    }

    /**
     * The text to display.
     * @param value String
     */
    public void setText(final String value) {

        this.text = value;
    }

    @Override
    public String toString() {

        StringBuilder builder = new StringBuilder();
        builder.append("PointerAnnotationVO [dateTime=");
        builder.append(this.dateTime);
        builder.append(", value=");
        builder.append(this.value);
        builder.append(", setValue=");
        builder.append(this.setValue);
        builder.append(", text=");
        builder.append(this.text);
        builder.append(", ");
        builder.append(super.toString());
        builder.append("]");

        return builder.toString();
    }

}
