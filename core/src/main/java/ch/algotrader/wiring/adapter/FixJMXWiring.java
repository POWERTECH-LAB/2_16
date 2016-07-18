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
package ch.algotrader.wiring.adapter;

import org.quickfixj.jmx.JmxExporter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import quickfix.SocketInitiator;

/**
 * Fix core JMX configuration.
 */
@Configuration
@Profile({"jPMFix", "iBFix", "dCFix", "rTFix", "dcMarkteData", "lMAXFix", "lMAXMarketData", "fXCMFix", "fXCMMarketData", "cNXFix", "cNXMarketData", "tTFix", "tTMarketData"})
public class FixJMXWiring {

    @Bean(name = "fixSocketInitiatorMBean")
    public JmxExporter createMBean(final SocketInitiator fixSocketInitiator) throws Exception {

        JmxExporter exporter = new JmxExporter();
        exporter.register(fixSocketInitiator);
        return exporter;
    }

}
