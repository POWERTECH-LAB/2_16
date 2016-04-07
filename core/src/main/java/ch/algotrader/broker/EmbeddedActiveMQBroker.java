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

package ch.algotrader.broker;

import java.net.URI;

import javax.management.MBeanServer;

import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.broker.jmx.ManagementContext;
import org.apache.activemq.broker.region.policy.LastImageSubscriptionRecoveryPolicy;
import org.apache.activemq.broker.region.policy.PolicyEntry;
import org.apache.activemq.broker.region.policy.PolicyMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.algotrader.enumeration.InitializingServiceType;
import ch.algotrader.service.InitializationPriority;
import ch.algotrader.service.InitializingServiceI;

@InitializationPriority(value = InitializingServiceType.CONNECTOR, priority = 1)
public class EmbeddedActiveMQBroker implements InitializingServiceI {

    private final Logger LOGGER = LogManager.getLogger(EmbeddedActiveMQBroker.class);

    private final BrokerService broker;
    private final int port;
    private final int wsPort;

    public EmbeddedActiveMQBroker(final int port, final int wsPort, final MBeanServer mbeanServer) {
        this.port = port;
        this.wsPort = wsPort;
        this.broker = new BrokerService();

        if (mbeanServer != null) {
            ManagementContext managementContext = new ManagementContext(mbeanServer);
            managementContext.setCreateMBeanServer(false);
            managementContext.setCreateConnector(false);
            this.broker.setManagementContext(managementContext);
            this.broker.setUseJmx(true);
        } else {
            this.broker.setUseJmx(false);
        }
        this.broker.setPersistent(false);
        this.broker.setAdvisorySupport(true);

        LastImageSubscriptionRecoveryPolicy policy = new LastImageSubscriptionRecoveryPolicy();
        PolicyEntry policyEntry = new PolicyEntry();
        policyEntry.setSubscriptionRecoveryPolicy(policy);

        PolicyMap policyMap = new PolicyMap();
        policyMap.setDefaultEntry(policyEntry);

        this.broker.setDestinationPolicy(policyMap);
    }

    public void start() throws Exception {

        this.broker.start();
    }

    public void stop() throws Exception {

        this.broker.stop();
    }

    @Override
    public void init() throws Exception {

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("JMS connector on port {}", this.port);
        }
        this.broker.addConnector(new URI("tcp://localhost:" + this.port));
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Websocket connector on port {}", this.wsPort);
        }
        this.broker.addConnector(new URI("ws://localhost:" + this.wsPort));

        this.broker.startAllConnectors();

    }

}
