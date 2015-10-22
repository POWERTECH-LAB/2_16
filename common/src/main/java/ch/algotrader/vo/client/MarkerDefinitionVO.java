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
package ch.algotrader.vo.client;

import ch.algotrader.enumeration.Color;

/**
 * Defines a Marker Series.
 */
public class MarkerDefinitionVO extends SeriesDefinitionVO {

    private static final long serialVersionUID = 2617488033337525607L;

    /**
     * The name of this Marker Series.
     */
    private String name;

    /**
     * If true this Marker will be represented as a horizontal Band, otherwise a horizontal Line.
     */
    private boolean interval = false;

    /**
     * Default Constructor
     */
    public MarkerDefinitionVO() {

        super();
    }

    /**
     * Constructor with all properties
     * @param labelIn String
     * @param selectedIn boolean
     * @param colorIn Color
     * @param dashedIn boolean
     * @param nameIn String
     * @param intervalIn boolean
     */
    public MarkerDefinitionVO(final String labelIn, final boolean selectedIn, final Color colorIn, final boolean dashedIn, final String nameIn, final boolean intervalIn) {

        super(labelIn, selectedIn, colorIn, dashedIn);

        this.name = nameIn;
        this.interval = intervalIn;
    }

    /**
     * Copies constructor from other MarkerDefinitionVO
     *
     * @param otherBean Cannot be <code>null</code>
     * @throws NullPointerException if the argument is <code>null</code>
     */
    public MarkerDefinitionVO(final MarkerDefinitionVO otherBean) {

        super(otherBean);

        this.name = otherBean.getName();
        this.interval = otherBean.isInterval();
    }

    /**
     * The name of this Marker Series.
     * @return name String
     */
    public String getName() {

        return this.name;
    }

    /**
     * The name of this Marker Series.
     * @param value String
     */
    public void setName(final String value) {

        this.name = value;
    }

    /**
     * If true this Marker will be represented as a horizontal Band, otherwise a horizontal Line.
     * @return interval boolean
     */
    public boolean isInterval() {

        return this.interval;
    }

    /**
     * If true this Marker will be represented as a horizontal Band, otherwise a horizontal Line.
     * Duplicates isBoolean method, for use as Jaxb2 compatible object
     * @return interval boolean
     */
    @Deprecated
    public boolean getInterval() {

        return this.interval;
    }

    /**
     * If true this Marker will be represented as a horizontal Band, otherwise a horizontal Line.
     * @param value boolean
     */
    public void setInterval(final boolean value) {

        this.interval = value;
    }

    @Override
    public String toString() {

        StringBuilder builder = new StringBuilder();
        builder.append("MarkerDefinitionVO [name=");
        builder.append(name);
        builder.append(", interval=");
        builder.append(interval);
        builder.append(", ");
        builder.append(super.toString());
        builder.append("]");

        return builder.toString();
    }

}
