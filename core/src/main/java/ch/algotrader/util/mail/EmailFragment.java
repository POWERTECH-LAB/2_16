/***********************************************************************************
 * AlgoTrader Enterprise Trading Framework
 *
 * Copyright (C) 2013 Flury Trading - All rights reserved
 *
 * All information contained herein is, and remains the property of Flury Trading.
 * The intellectual and technical concepts contained herein are proprietary to
 * Flury Trading. Modification, translation, reverse engineering, decompilation,
 * disassembly or reproduction of this material is strictly forbidden unless prior
 * written permission is obtained from Flury Trading
 *
 * Fur detailed terms and conditions consult the file LICENSE.txt or contact
 *
 * Flury Trading
 * Badenerstrasse 16
 * 8004 Zurich
 ***********************************************************************************/
package ch.algotrader.util.mail;

/**
 * POJO representing an Email Fragment with its binary data and {@code fileName}.
 *
 * @author <a href="mailto:aflury@algotrader.ch">Andy Flury</a>
 *
 * @version $Revision$ $Date$
 */
public class EmailFragment {

    private byte[] data;
    private String filename;

    public EmailFragment(String filename, byte[] data) {
        this.filename = filename;
        this.data = data;
    }

    public byte[] getData() {
        return this.data;
    }

    public String getFilename() {
        return this.filename;
    }

    @Override
    public String toString() {
        return this.filename;
    }
}
