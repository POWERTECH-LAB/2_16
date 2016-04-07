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
package ch.algotrader.wiring.server;

import org.hibernate.SessionFactory;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseFactory;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import ch.algotrader.config.CoreConfig;
import ch.algotrader.esper.Engine;
import ch.algotrader.lifecycle.LifecycleManager;
import ch.algotrader.wiring.HibernateNoCachingWiring;
import ch.algotrader.wiring.DefaultConfigTestBase;
import ch.algotrader.wiring.common.CommonConfigWiring;
import ch.algotrader.wiring.common.EngineManagerWiring;
import ch.algotrader.wiring.common.EventDispatchWiring;
import ch.algotrader.wiring.core.CoreConfigWiring;
import ch.algotrader.wiring.core.LifecycleManagerWiring;
import ch.algotrader.wiring.core.ServerEngineWiring;

/**
 * @author <a href="mailto:okalnichevski@algotrader.ch">Oleg Kalnichevski</a>
 */
public class WiringTest extends DefaultConfigTestBase {

    @Test
    public void testCoreConfigWiring() throws Exception {

        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.register(CoreConfigWiring.class);
        context.refresh();

        Assert.assertNotNull(context.getBean(CoreConfig.class));

        context.close();
    }

    @Test
    public void testServerEngineWiring() throws Exception {

        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.register(CommonConfigWiring.class, ServerEngineWiring.class);
        context.refresh();

        Engine serverEngine = context.getBean("serverEngine", Engine.class);
        Assert.assertNotNull(serverEngine);
        serverEngine.destroy();

        context.close();
    }

    @Test
    public void testLifecycleManagerWiring() throws Exception {

        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

        context.register(
                CommonConfigWiring.class, EngineManagerWiring.class, ServerEngineWiring.class,
                EventDispatchWiring.class, LifecycleManagerWiring.class);
        context.refresh();

        Engine serverEngine = context.getBean("serverEngine", Engine.class);
        Assert.assertNotNull(serverEngine);
        serverEngine.destroy();

        Assert.assertNotNull(context.getBean(LifecycleManager.class));

        context.close();
    }

    @Test
    public void testHibernateWiring() throws Exception {

        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

        EmbeddedDatabaseFactory dbFactory = new EmbeddedDatabaseFactory();
        dbFactory.setDatabaseType(EmbeddedDatabaseType.H2);
        dbFactory.setDatabaseName("testdb;MODE=MYSQL;DATABASE_TO_UPPER=FALSE");

        EmbeddedDatabase database = dbFactory.getDatabase();
        context.getDefaultListableBeanFactory().registerSingleton("dataSource", database);

        context.register(HibernateNoCachingWiring.class);
        context.refresh();

        Assert.assertNotNull(context.getBean(SessionFactory.class));
        Assert.assertNotNull(context.getBean(PlatformTransactionManager.class));
        Assert.assertNotNull(context.getBean(TransactionTemplate.class));

        context.close();
    }

}
