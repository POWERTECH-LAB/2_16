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
package ch.algotrader.esper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.collections15.CollectionUtils;
import org.apache.commons.collections15.Predicate;
import org.apache.commons.collections15.Transformer;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jms.core.JmsTemplate;

import ch.algotrader.ServiceLocator;
import ch.algotrader.entity.security.Security;
import ch.algotrader.entity.strategy.Strategy;
import ch.algotrader.entity.strategy.StrategyImpl;
import ch.algotrader.entity.trade.Order;
import ch.algotrader.esper.annotation.Condition;
import ch.algotrader.esper.annotation.Listeners;
import ch.algotrader.esper.annotation.RunTimeOnly;
import ch.algotrader.esper.annotation.SimulationOnly;
import ch.algotrader.esper.annotation.Subscriber;
import ch.algotrader.esper.callback.ClosePositionCallback;
import ch.algotrader.esper.callback.OpenPositionCallback;
import ch.algotrader.esper.callback.TickCallback;
import ch.algotrader.esper.callback.TradeCallback;
import ch.algotrader.esper.io.BatchDBTickInputAdapter;
import ch.algotrader.esper.io.CollectionInputAdapter;
import ch.algotrader.esper.io.CsvBarInputAdapter;
import ch.algotrader.esper.io.CsvBarInputAdapterSpec;
import ch.algotrader.esper.io.CsvTickInputAdapter;
import ch.algotrader.esper.io.CsvTickInputAdapterSpec;
import ch.algotrader.esper.io.CustomSender;
import ch.algotrader.esper.subscriber.SubscriberCreator;
import ch.algotrader.util.MyLogger;
import ch.algotrader.util.collection.CollectionUtil;
import ch.algotrader.util.metric.MetricsUtil;

import com.espertech.esper.adapter.InputAdapter;
import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.ConfigurationEngineDefaults.Threading;
import com.espertech.esper.client.ConfigurationOperations;
import com.espertech.esper.client.ConfigurationVariable;
import com.espertech.esper.client.EPAdministrator;
import com.espertech.esper.client.EPOnDemandQueryResult;
import com.espertech.esper.client.EPPreparedStatement;
import com.espertech.esper.client.EPRuntime;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.SafeIterator;
import com.espertech.esper.client.StatementAwareUpdateListener;
import com.espertech.esper.client.UpdateListener;
import com.espertech.esper.client.annotation.Name;
import com.espertech.esper.client.deploy.DeploymentInformation;
import com.espertech.esper.client.deploy.EPDeploymentAdmin;
import com.espertech.esper.client.deploy.Module;
import com.espertech.esper.client.deploy.ModuleItem;
import com.espertech.esper.client.soda.AnnotationPart;
import com.espertech.esper.client.soda.EPStatementObjectModel;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.client.time.TimerControlEvent;
import com.espertech.esper.core.deploy.EPLModuleUtil;
import com.espertech.esper.core.service.EPServiceProviderImpl;
import com.espertech.esper.core.service.EPServiceProviderSPI;
import com.espertech.esper.epl.annotation.AnnotationUtil;
import com.espertech.esper.epl.core.EngineImportService;
import com.espertech.esper.epl.spec.AnnotationDesc;
import com.espertech.esper.epl.spec.StatementSpecMapper;
import com.espertech.esper.util.JavaClassHelper;
import com.espertech.esperio.AdapterCoordinator;
import com.espertech.esperio.AdapterCoordinatorImpl;
import com.espertech.esperio.csv.CSVInputAdapter;
import com.espertech.esperio.csv.CSVInputAdapterSpec;

/**
 * @author <a href="mailto:aflury@algotrader.ch">Andy Flury</a>
 *
 * @version $Revision$ $Date$
 */
public class EngineImpl extends AbstractEngine {

    private static final Logger logger = MyLogger.getLogger(EngineImpl.class.getName());
    private static final String newline = System.getProperty("line.separator");

    private static boolean simulation = ServiceLocator.instance().getConfiguration().getSimulation();
    private static boolean singleVM = ServiceLocator.instance().getConfiguration().getBoolean("misc.singleVM");
    private static List<String> moduleDeployExcludeStatements = Arrays.asList((ServiceLocator.instance().getConfiguration().getString("misc.moduleDeployExcludeStatements")).split(","));
    private static int outboundThreads = ServiceLocator.instance().getConfiguration().getInt("misc.outboundThreads");

    private String engineName;
    private EPServiceProvider serviceProvider;
    private boolean internalClock;
    private AdapterCoordinator coordinator;
    private JmsTemplate strategyTemplate;

    private static ThreadLocal<AtomicBoolean> processing = new ThreadLocal<AtomicBoolean>() {
        @Override
        protected AtomicBoolean initialValue() {
            return new AtomicBoolean(false);
        }
    };

    /**
     * Initializes a new Engine by the given {@code engineName}.
     * The following steps are exectued:
     * <ul>
     * <li>{@code corresponding esper-xxx.cfg.xml} files are loaded from the classpath</li>
     * <li>Esper variables are initilized</li>
     * <li>Esper threading is configured</li>
     * <li>The {@link Strategy} itself is configured as an Esper variable {@code engineStrategy}</li>
     * <li>Esper Time is set to zero</li>
     * </ul>
     */
    public EngineImpl(String engineName) {

        this.engineName = engineName;

        Configuration configuration = new Configuration();

        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath*:/META-INF/esper-**.cfg.xml");
            for (Resource resource : resources) {
                if (StrategyImpl.BASE.equals(engineName)) {

                    // for Base only load esper-common.cfg.xml and esper-core.cfg.xml
                    if (resource.toString().contains("esper-common.cfg.xml") || resource.toString().contains("esper-core.cfg.xml")) {
                        configuration.configure(resource.getURL());
                    }
                } else {

                    // for Strategies to not load esper-core.cfg.xml
                    if (!resource.toString().contains("esper-core.cfg.xml")) {
                        configuration.configure(resource.getURL());
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("problem loading esper config", e);
        }

        initVariables(configuration);

        // outbound threading for BASE
        if (StrategyImpl.BASE.equals(engineName) && !simulation) {

            Threading threading = configuration.getEngineDefaults().getThreading();

            threading.setThreadPoolOutbound(true);
            threading.setThreadPoolOutboundNumThreads(outboundThreads);
        }

        Strategy strategy = ServiceLocator.instance().getLookupService().getStrategyByName(engineName);
        configuration.getVariables().get("engineStrategy").setInitializationValue(strategy);

        this.serviceProvider = EPServiceProviderManager.getProvider(engineName, configuration);

        // must send time event before first schedule pattern
        this.serviceProvider.getEPRuntime().sendEvent(new CurrentTimeEvent(0));

        this.internalClock = false;

        logger.debug("initialized service provider: " + engineName);
    }

    @Override
    public String getName() {

        return this.engineName;
    }

    /**
     * Destroys the specified Esper Engine.
     */
    @Override
    public void destroy() {

        this.serviceProvider.destroy();

        logger.debug("destroyed service provider: " + this.engineName);
    }

    /**
     * Deploys the statement from the given {@code moduleName} with the given {@code statementName} into the specified Esper Engine.
     */
    @Override
    public void deployStatement(String moduleName, String statementName) {

        deployStatement(moduleName, statementName, null, new Object[] {}, null);
    }

    /**
     * Deploys the statement from the given {@code moduleName} with the given {@code statementName} into the specified Esper Engine.
     * In addition the given {@code alias} and Prepared Statement {@code params} are set on the statement
     */
    @Override
    public void deployStatement(String moduleName, String statementName, String alias, Object[] params) {

        deployStatement(moduleName, statementName, alias, params, null);
    }

    /**
     * Deploys the statement from the given {@code moduleName} with the given {@code statementName} into the specified Esper Engine.
     * In addition the given {@code alias}, Prepared Statement {@code params} and {@code callback} are set on the statement.
     */
    @Override
    public void deployStatement(String moduleName, String statementName, String alias, Object[] params, Object callback) {

        // do nothing if the statement already exists
        EPStatement oldStatement = this.serviceProvider.getEPAdministrator().getStatement(statementName);
        if (oldStatement != null && oldStatement.isStarted()) {
            logger.warn(statementName + " is already deployed and started");
            return;
        }

        Module module = getModule(moduleName);

        // go through all statements in the module
        EPStatement newStatement = null;
        for (ModuleItem moduleItem : module.getItems()) {

            if (isEligibleStatement(this.serviceProvider, moduleItem, statementName)) {

                newStatement = startStatement(moduleItem, this.serviceProvider, alias, params, callback);

                // break iterating over the statements
                break;

            } else {
                continue;
            }
        }

        if (newStatement == null) {
            logger.warn("statement " + statementName + " was not found");
        } else {
            logger.debug("deployed statement " + newStatement.getName());
        }
    }

    /**
     * Deploys all modules defined for given Strategy
     */
    @Override
    public void deployAllModules() {

        deployInitModules();
        deployRunModules();
    }

    /**
     * Deploys all init-modules defined for given Strategy
     */
    @Override
    public void deployInitModules() {

        Strategy strategy = ServiceLocator.instance().getLookupService().getStrategyByName(this.engineName);
        String initModules = strategy.getInitModules();
        if (initModules != null) {
            String[] modules = initModules.split(",");
            for (String module : modules) {
                deployModule(module.trim());
            }
        }
    }

    /**
     * Deploys all run-modules defined for given Strategy
     */
    @Override
    public void deployRunModules() {

        Strategy strategy = ServiceLocator.instance().getLookupService().getStrategyByName(this.engineName);
        String runModules = strategy.getRunModules();
        if (runModules != null) {
            String[] modules = runModules.split(",");
            for (String module : modules) {
                deployModule(module.trim());
            }
        }
    }

    /**
     * Deploys the specified module into the specified Esper Engine.
     */
    @Override
    public void deployModule(String moduleName) {

        Module module = getModule(moduleName);

        for (ModuleItem moduleItem : module.getItems()) {

            if (isEligibleStatement(this.serviceProvider, moduleItem, null)) {

                startStatement(moduleItem, this.serviceProvider, null, new Object[] {}, null);

            } else {
                continue;
            }
        }

        logger.debug("deployed module " + moduleName);
    }

    /**
     * Returns true if the statement by the given {@code statementNameRegex} is deployed.
     *
     * @param statementNameRegex statement name regular expression
     */
    @Override
    public boolean isDeployed(final String statementNameRegex) {

        // find the first statement that matches the given statementName regex
        String[] statementNames = findStatementNames(statementNameRegex);

        if (statementNames.length == 0) {
            return false;
        } else if (statementNames.length > 1) {
            logger.error("more than one statement matches: " + statementNameRegex);
        }

        // get the statement
        EPAdministrator administrator = this.serviceProvider.getEPAdministrator();
        EPStatement statement = administrator.getStatement(statementNames[0]);

        if (statement != null && statement.isStarted()) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Undeploys the specified statement from the specified Esper Engine.
     */
    @Override
    public void undeployStatement(String statementName) {

        // destroy the statement
        EPStatement statement = this.serviceProvider.getEPAdministrator().getStatement(statementName);

        if (statement != null && statement.isStarted()) {
            statement.destroy();
            logger.debug("undeployed statement " + statementName);
        }
    }

    /**
     * Restarts the given Statement
     */
    @Override
    public void restartStatement(String statementName) {

        // destroy the statement
        EPStatement statement = this.serviceProvider.getEPAdministrator().getStatement(statementName);

        if (statement != null && statement.isStarted()) {
            statement.stop();
            statement.start();
            logger.debug("restarted statement " + statementName);
        }
    }

    /**
     * Undeploys the specified module from the specified Esper Engine.
     */
    @Override
    public void undeployModule(String moduleName) {

        EPAdministrator administrator = this.serviceProvider.getEPAdministrator();
        EPDeploymentAdmin deployAdmin = administrator.getDeploymentAdmin();
        for (DeploymentInformation deploymentInformation : deployAdmin.getDeploymentInformation()) {
            if (deploymentInformation.getModule().getName().equals(moduleName)) {
                try {
                    deployAdmin.undeploy(deploymentInformation.getDeploymentId());
                } catch (Exception e) {
                    throw new RuntimeException("module " + moduleName + " could no be undeployed", e);
                }
            }
        }

        logger.debug("undeployed module " + moduleName);
    }

    /**
     * Adds the given Event Type to the specified Esper Engine.
     * this method can be used if an Event Type is not known at compile time and can therefore not be configured
     * inside an {@code esper-xxx.cfg.xml} file
     */
    @Override
    public void addEventType(String eventTypeName, String eventClassName) {

        ConfigurationOperations configuration = this.serviceProvider.getEPAdministrator().getConfiguration();
        if (configuration.getEventType(eventTypeName) == null) {
            configuration.addEventType(eventTypeName, eventClassName);
        }
    }

    /**
     * Sends an Event into the corresponding Esper Engine.
     */
    @Override
    public void sendEvent(Object obj) {

        if (simulation || singleVM) {

            long startTime = System.nanoTime();

            internalSendEvent(obj);

            MetricsUtil.accountEnd("EsperManager." + obj.getClass(), startTime);

        } else {

            // check if it is the localStrategy
            if (ServiceLocator.instance().getConfiguration().getStartedStrategyName().equals(this.engineName)) {
                internalSendEvent(obj);
            } else {
                externalSendEvent(obj);
            }
        }
    }

    /**
     * Executes an arbitrary EPL query on the Esper Engine.
     */
    @Override
    @SuppressWarnings("rawtypes")
    public List executeQuery(String query) {

        List<Object> objects = new ArrayList<Object>();
        EPOnDemandQueryResult result = this.serviceProvider.getEPRuntime().executeQuery(query);
        for (EventBean row : result.getArray()) {
            Object object = row.getUnderlying();
            objects.add(object);
        }
        return objects;
    }

    /**
     * Executes an arbitrary EPL query that is supposed to return one single object on the Esper Engine.
     */
    @Override
    @SuppressWarnings("unchecked")
    public Object executeSingelObjectQuery(String query) {

        List<Object> events = executeQuery(query);
        if (events.size() == 0) {
            return null;
        } else if (events.size() == 1) {
            return events.get(0);
        } else {
            throw new IllegalArgumentException("query returned more than one object");
        }
    }

    /**
     * Retrieves the last event currently held by the given statement.
     */
    @Override
    public Object getLastEvent(String statementName) {

        EPStatement statement = this.serviceProvider.getEPAdministrator().getStatement(statementName);
        if (statement == null) {
            throw new IllegalStateException("statement " + statementName + " does not exist");
        } else if (!statement.isStarted()) {
            throw new IllegalStateException("statement " + statementName + " is not started");
        } else {
            SafeIterator<EventBean> it = statement.safeIterator();
            try {
                if (it.hasNext()) {
                    return it.next().getUnderlying();
                } else {
                    return null;
                }
            } finally {
                it.close();
            }
        }
    }

    /**
     * Retrieves the last event currently held by the given statement and returns the property by the {@code propertyName}
     */
    @Override
    public Object getLastEventProperty(String statementName, String propertyName) {

        EPStatement statement = this.serviceProvider.getEPAdministrator().getStatement(statementName);
        if (statement == null) {
            throw new IllegalStateException("statement " + statementName + " does not exist");
        } else if (!statement.isStarted()) {
            throw new IllegalStateException("statement " + statementName + " is not started");
        } else {
            SafeIterator<EventBean> it = statement.safeIterator();
            try {
                if (it.hasNext()) {
                    return it.next().get(propertyName);
                } else {
                    return null;
                }
            } finally {
                it.close();
            }
        }
    }

    /**
     * Retrieves all events currently held by the given statement.
     */
    @Override
    @SuppressWarnings("rawtypes")
    public List getAllEvents(String statementName) {

        EPStatement statement = this.serviceProvider.getEPAdministrator().getStatement(statementName);
        if (statement == null) {
            throw new IllegalStateException("statement " + statementName + " does not exist");
        } else if (!statement.isStarted()) {
            throw new IllegalStateException("statement " + statementName + " is not started");
        } else {
            List<Object> list = new ArrayList<Object>();
            SafeIterator<EventBean> it = statement.safeIterator();
            try {
                while (it.hasNext()) {
                    EventBean bean = it.next();
                    Object underlying = bean.getUnderlying();
                    list.add(underlying);
                }
                return list;
            } finally {
                it.close();
            }
        }
    }

    /**
     * Retrieves all events currently held by the given statement and returns the property by the {@code propertyName} for all events.
     */
    @Override
    @SuppressWarnings("rawtypes")
    public List getAllEventsProperty(String statementName, String property) {

        EPStatement statement = this.serviceProvider.getEPAdministrator().getStatement(statementName);
        if (statement == null) {
            throw new IllegalStateException("statement " + statementName + " does not exist");
        } else if (!statement.isStarted()) {
            throw new IllegalStateException("statement " + statementName + " is not started");
        } else {
            List<Object> list = new ArrayList<Object>();
            SafeIterator<EventBean> it = statement.safeIterator();
            try {
                while (it.hasNext()) {
                    EventBean bean = it.next();
                    Object underlying = bean.get(property);
                    list.add(underlying);
                }
                return list;
            } finally {
                it.close();
            }
        }
    }

    /**
     * Sets the internal clock of the specified Esper Engine
     */
    @Override
    public void setInternalClock(boolean internal) {

        this.internalClock = internal;

        if (internal) {
            sendEvent(new TimerControlEvent(TimerControlEvent.ClockType.CLOCK_INTERNAL));
            EPServiceProviderImpl provider = (EPServiceProviderImpl) this.serviceProvider;
            provider.getTimerService().enableStats();
        } else {
            sendEvent(new TimerControlEvent(TimerControlEvent.ClockType.CLOCK_EXTERNAL));
            EPServiceProviderImpl provider = (EPServiceProviderImpl) this.serviceProvider;
            provider.getTimerService().disableStats();
        }

        setVariableValue("internal_clock", internal);

        logger.debug("set internal clock to: " + internal);
    }

    /**
     * Returns true if the specified Esper Engine uses internal clock
     */
    @Override
    public boolean isInternalClock() {

        return this.internalClock;
    }

    /**
     * Retruns the current time of the given Esper Engine
     */
    @Override
    public long getCurrentTime() {

        return this.serviceProvider.getEPRuntime().getCurrentTime();
    }

    /**
     * Prepares the given Esper Engine for coordinated input of CSV-Files
     */
    @Override
    public void initCoordination() {

        this.coordinator = new AdapterCoordinatorImpl(this.serviceProvider, true, true);

        ((AdapterCoordinatorImpl) this.coordinator).setSender(new CustomSender());
    }

    /**
     * Queues the specified {@code csvInputAdapterSpec} for coordination with the given Esper Engine.
     */
    @Override
    public void coordinate(CSVInputAdapterSpec csvInputAdapterSpec) {

        InputAdapter inputAdapter;
        if (csvInputAdapterSpec instanceof CsvTickInputAdapterSpec) {
            inputAdapter = new CsvTickInputAdapter(this.serviceProvider, (CsvTickInputAdapterSpec) csvInputAdapterSpec);
        } else if (csvInputAdapterSpec instanceof CsvBarInputAdapterSpec) {
            inputAdapter = new CsvBarInputAdapter(this.serviceProvider, (CsvBarInputAdapterSpec) csvInputAdapterSpec);
        } else {
            inputAdapter = new CSVInputAdapter(this.serviceProvider, csvInputAdapterSpec);
        }
        this.coordinator.coordinate(inputAdapter);
    }

    /**
     * Queues the specified {@code collection} for coordination with the given Esper Engine.
     * The property by the name of {@code timeStampProperty} is used to identify the current time.
     */
    @Override
    @SuppressWarnings({ "rawtypes" })
    public void coordinate(Collection collection, String timeStampProperty) {

        InputAdapter inputAdapter = new CollectionInputAdapter(this.serviceProvider, collection, timeStampProperty);
        this.coordinator.coordinate(inputAdapter);
    }

    /**
     * Queues subscribed Ticks for coordination with the given Esper Engine.
     */
    @Override
    public void coordinateTicks(Date startDate) {

        InputAdapter inputAdapter = new BatchDBTickInputAdapter(this.serviceProvider, startDate);
        this.coordinator.coordinate(inputAdapter);
    }

    /**
     * Starts coordination for the given Esper Engine.
     */
    @Override
    public void startCoordination() {

        this.coordinator.start();
    }

    /**
     * sets the value of the specified Esper Variable
     */
    @Override
    public void setVariableValue(String variableName, Object value) {

        variableName = variableName.replace(".", "_");
        EPRuntime runtime = this.serviceProvider.getEPRuntime();
        if (runtime.getVariableValueAll().containsKey(variableName)) {
            runtime.setVariableValue(variableName, value);
            logger.debug("set variable " + variableName + " to value " + value);
        }
    }

    /**
     * sets the value of the specified Esper Variable by parsing the given String value.
     */
    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void setVariableValueFromString(String variableName, String value) {

        variableName = variableName.replace(".", "_");
        EPRuntime runtime = this.serviceProvider.getEPRuntime();
        if (runtime.getVariableValueAll().containsKey(variableName)) {
            Class clazz = runtime.getVariableValue(variableName).getClass();

            Object castedObj = null;
            if (clazz.isEnum()) {
                castedObj = Enum.valueOf(clazz, value);
            } else {
                castedObj = JavaClassHelper.parse(clazz, value);
            }
            runtime.setVariableValue(variableName, castedObj);
            logger.debug("set variable " + variableName + " to value " + value);
        }
    }

    /**
     * Returns the value of the specified Esper Variable
     */
    @Override
    public Object getVariableValue(String variableName) {

        variableName = variableName.replace(".", "_");
        EPRuntime runtime = this.serviceProvider.getEPRuntime();
        return runtime.getVariableValue(variableName);
    }

    /**
     * Adds a {@link TradeCallback} to the given Esper Engine that will be invoked as soon as all {@code orders} have been
     * fully exectured or cancelled.
     */
    @Override
    public void addTradeCallback(Collection<Order> orders, TradeCallback callback) {

        // get the securityIds sorted asscending and check that all orders are from the same strategy
        final Order firstOrder = CollectionUtil.getFirstElement(orders);
        Set<Integer> sortedSecurityIds = new TreeSet<Integer>(CollectionUtils.collect(orders, new Transformer<Order, Integer>() {

            @Override
            public Integer transform(Order order) {
                if (!order.getStrategy().equals(firstOrder.getStrategy())) {
                    throw new IllegalArgumentException("cannot addTradeCallback for orders of different strategies");
                }
                return order.getSecurity().getId();
            }
        }));

        if (sortedSecurityIds.size() < orders.size()) {
            throw new IllegalArgumentException("cannot addTradeCallback for multiple orders on the same security");
        }

        // get the statement alias based on all security ids
        String alias = "ON_TRADE_COMPLETED_" + StringUtils.join(sortedSecurityIds, "_") + "_" + firstOrder.getStrategy().getName();

        if (isDeployed(alias)) {

            logger.warn(alias + " is already deployed");
        } else {

            Object[] params = new Object[] { sortedSecurityIds.size(), sortedSecurityIds, firstOrder.getStrategy().getName(), firstOrder.isAlgoOrder() };
            deployStatement("prepared", "ON_TRADE_COMPLETED", alias, params, callback);
        }
    }

    /**
     * Adds a {@link TickCallback} to the given Esper Engine that will be invoked as soon as at least one Tick has arrived
     * for each of the specified {@code securities}
     */
    @Override
    public void addFirstTickCallback(Collection<Security> securities, TickCallback callback) {

        // create a list of unique security ids
        Set<Integer> securityIds = new TreeSet<Integer>();
        securityIds.addAll(CollectionUtils.collect(securities, new Transformer<Security, Integer>() {
            @Override
            public Integer transform(Security security) {
                return security.getId();
            }
        }));

        String alias = "ON_FIRST_TICK_" + StringUtils.join(securityIds, "_");

        if (isDeployed(alias)) {

            logger.warn(alias + " is already deployed");
        } else {

            int[] securityIdsArray = ArrayUtils.toPrimitive(securityIds.toArray(new Integer[0]));
            deployStatement("prepared", "ON_FIRST_TICK", alias, new Object[] { securityIds.size(), securityIdsArray }, callback);
        }
    }

    /**
     * Adds a {@link OpenPositionCallback} to the given Esper Engine that will be invoked as soon as a new Position
     * on the given Security has been opened.
     */
    @Override
    public void addOpenPositionCallback(int securityId, OpenPositionCallback callback) {

        String alias = "ON_OPEN_POSITION_" + securityId;

        if (isDeployed(alias)) {

            logger.warn(alias + " is already deployed");
        } else {

            deployStatement("prepared", "ON_OPEN_POSITION", alias, new Object[] { securityId }, callback);
        }
    }

    /**
     * Adds a {@link OpenPositionCallback} to the given Esper Engine that will be invoked as soon as a Position
     * on the given Security has been closed.
     */
    @Override
    public void addClosePositionCallback(int securityId, ClosePositionCallback callback) {

        String alias = "ON_CLOSE_POSITION_" + securityId;

        if (isDeployed(alias)) {

            logger.warn(alias + " is already deployed");
        } else {

            deployStatement("prepared", "ON_CLOSE_POSITION", alias, new Object[] { securityId }, callback);
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static void initVariables(Configuration configuration) {

        try {
            Map<String, ConfigurationVariable> variables = configuration.getVariables();
            for (Map.Entry<String, ConfigurationVariable> entry : variables.entrySet()) {
                String variableName = entry.getKey().replace("_", ".");
                String value = ServiceLocator.instance().getConfiguration().getString(variableName);
                if (value != null) {
                    Class clazz = Class.forName(entry.getValue().getType());
                    Object castedObj = null;
                    if (clazz.isEnum()) {
                        castedObj = Enum.valueOf(clazz, value);
                    } else if (clazz == BigDecimal.class) {
                        castedObj = new BigDecimal(value);
                    } else {
                        castedObj = JavaClassHelper.parse(clazz, value);
                    }
                    entry.getValue().setInitializationValue(castedObj);
                }
            }
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private EPStatement startStatement(ModuleItem moduleItem, EPServiceProvider serviceProvider, String alias, Object[] params, Object callback) {

        // create the statement and set the prepared statement params if a prepared statement
        EPStatement statement;
        String expression = moduleItem.getExpression();
        EPAdministrator administrator = serviceProvider.getEPAdministrator();
        if (expression.contains("?")) {
            EPPreparedStatement prepared = administrator.prepareEPL(expression);
            for (int i = 0; i < params.length; i++) {
                prepared.setObject(i + 1, params[i]);
            }
            if (alias != null) {
                statement = administrator.create(prepared, alias);
            } else {
                statement = administrator.create(prepared);
            }
        } else {
            if (alias != null) {
                statement = administrator.createEPL(expression, alias);
            } else {
                statement = administrator.createEPL(expression);
            }
        }

        // process annotations
        Annotation[] annotations = statement.getAnnotations();
        for (Annotation annotation : annotations) {
            if (annotation instanceof Subscriber) {

                Subscriber subscriber = (Subscriber) annotation;
                try {
                    Object obj = getSubscriber(subscriber.className());
                    statement.setSubscriber(obj);
                } catch (Exception e) {
                    throw new RuntimeException("subscriber " + subscriber.className() + " could not be created for statement " + statement.getName(), e);
                }

            } else if (annotation instanceof Listeners) {

                Listeners listeners = (Listeners) annotation;
                for (String className : listeners.classNames()) {
                    try {
                        Class<?> cl = Class.forName(className);
                        Object obj = cl.newInstance();
                        if (obj instanceof StatementAwareUpdateListener) {
                            statement.addListener((StatementAwareUpdateListener) obj);
                        } else {
                            statement.addListener((UpdateListener) obj);
                        }
                    } catch (Exception e) {
                        throw new RuntimeException("listener " + className + " could not be created for statement " + statement.getName(), e);
                    }
                }
            }
        }

        // attach the callback if supplied
        // will override the Subscriber defined in Annotations
        // in live trading stop the statement before attaching (and restart afterwards)
        // to make sure that the subscriber receives the first event
        if (callback != null) {
            if (simulation) {
                statement.setSubscriber(callback);
            } else {
                statement.stop();
                statement.setSubscriber(callback);
                statement.start();
            }
        }
        return statement;
    }

    private Module getModule(String moduleName) {

        String fileName = "module-" + moduleName + ".epl";
        InputStream stream = EsperManager.class.getResourceAsStream("/" + fileName);
        if (stream == null) {
            throw new IllegalArgumentException(fileName + " does not exist");
        }

        // process loads
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            StringWriter buffer = new StringWriter();
            String strLine;
            while ((strLine = reader.readLine()) != null) {
                if (!strLine.startsWith("load")) {
                    buffer.append(strLine);
                    buffer.append(newline);
                } else {
                    String argument = StringUtils.substringAfter(strLine, "load").trim();
                    String moduleBaseName = argument.split("\\.")[0];
                    String statementName = argument.split("\\.")[1].split(";")[0];
                    Module module = EPLModuleUtil.readResource("module-" + moduleBaseName + ".epl");
                    for (ModuleItem item : module.getItems()) {
                        if (item.getExpression().contains("@Name('" + statementName + "')")) {
                            buffer.append(item.getExpression());
                            buffer.append(";");
                            buffer.append(newline);
                            break;
                        }
                    }
                }
            }
            reader.close();
            stream.close();

            return EPLModuleUtil.parseInternal(buffer.toString(), fileName);

        } catch (Exception e) {
            throw new RuntimeException(moduleName + " could not be deployed", e);
        }
    }

    private boolean isEligibleStatement(EPServiceProvider serviceProvider, ModuleItem item, String statementName) {

        if (item.isCommentOnly()) {
            return false;
        }

        String expression = item.getExpression().replace("?", "1"); // replace ? to prevent error during compile
        EPStatementObjectModel objectModel = serviceProvider.getEPAdministrator().compileEPL(expression);
        List<AnnotationPart> annotationParts = objectModel.getAnnotations();
        List<AnnotationDesc> annotationDescs = StatementSpecMapper.mapAnnotations(annotationParts);

        EngineImportService engineImportService = ((EPServiceProviderSPI) serviceProvider).getEngineImportService();
        Annotation[] annotations = AnnotationUtil.compileAnnotations(annotationDescs, engineImportService, item.getExpression());

        for (Annotation annotation : annotations) {
            if (annotation instanceof Name) {

                Name name = (Name) annotation;
                if (statementName != null && !statementName.equals(name.value())) {
                    return false;
                } else if (moduleDeployExcludeStatements.contains(name.value())) {
                    return false;
                }

            } else if (annotation instanceof RunTimeOnly && simulation) {

                return false;

            } else if (annotation instanceof SimulationOnly && !simulation) {

                return false;

            } else if (annotation instanceof Condition) {

                Condition condition = (Condition) annotation;
                String key = condition.key();
                if (!ServiceLocator.instance().getConfiguration().getBoolean(key)) {
                    return false;
                }
            }
        }

        return true;
    }

    private String[] findStatementNames(final String statementNameRegex) {

        EPAdministrator administrator = this.serviceProvider.getEPAdministrator();

        // find the first statement that matches the given statementName regex
        return CollectionUtils.select(Arrays.asList(administrator.getStatementNames()), new Predicate<String>() {

            @Override
            public boolean evaluate(String statement) {
                return statement.matches(statementNameRegex);
            }
        }).toArray(new String[] {});
    }

    private void internalSendEvent(Object obj) {

        // if the engine is currently processing and event route the new event
        if (processing.get().get()) {
            this.serviceProvider.getEPRuntime().route(obj);
        } else {
            processing.get().set(true);
            this.serviceProvider.getEPRuntime().sendEvent(obj);
            processing.get().set(false);
        }
    }

    /**
     * manual lookup of templates since they are only available if applicationContext-jms.xml is active
     */
    private void externalSendEvent(Object obj) {

        if (this.strategyTemplate == null) {
            this.strategyTemplate = ServiceLocator.instance().getService("strategyTemplate", JmsTemplate.class);
        }

        // sent to the strateyg queue
        this.strategyTemplate.convertAndSend(this.engineName + ".QUEUE", obj);

        logger.trace("propagated event to " + this.engineName + " " + obj);
    }

    private static Object getSubscriber(String fqdn) throws ClassNotFoundException {

        // try to see if the fqdn represents a class
        try {
            Class<?> cl = Class.forName(fqdn);
            return cl.newInstance();
        } catch (Exception e) {
            // do nothin
        }

        // otherwise the fqdn represents a method, in this case treate a subscriber
        return SubscriberCreator.createSubscriber(fqdn);
    }

}
