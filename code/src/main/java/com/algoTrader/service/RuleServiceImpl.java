package com.algoTrader.service;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.algoTrader.entity.Strategy;
import com.algoTrader.entity.StrategyImpl;
import com.algoTrader.esper.annotation.Condition;
import com.algoTrader.esper.annotation.Listeners;
import com.algoTrader.esper.annotation.RunTimeOnly;
import com.algoTrader.esper.annotation.SimulationOnly;
import com.algoTrader.esper.annotation.Subscriber;
import com.algoTrader.esper.io.BatchDBTickInputAdapter;
import com.algoTrader.esper.io.CsvBarInputAdapter;
import com.algoTrader.esper.io.CsvBarInputAdapterSpec;
import com.algoTrader.esper.io.CsvTickInputAdapter;
import com.algoTrader.esper.io.CsvTickInputAdapterSpec;
import com.algoTrader.esper.io.DBInputAdapter;
import com.algoTrader.esper.subscriber.SubscriberCreator;
import com.algoTrader.util.ConfigurationUtil;
import com.algoTrader.util.MyLogger;
import com.algoTrader.util.StrategyUtil;
import com.espertech.esper.adapter.InputAdapter;
import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.ConfigurationVariable;
import com.espertech.esper.client.EPAdministrator;
import com.espertech.esper.client.EPOnDemandQueryResult;
import com.espertech.esper.client.EPPreparedStatementImpl;
import com.espertech.esper.client.EPRuntime;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.SafeIterator;
import com.espertech.esper.client.StatementAwareUpdateListener;
import com.espertech.esper.client.UpdateListener;
import com.espertech.esper.client.deploy.DeploymentInformation;
import com.espertech.esper.client.deploy.DeploymentOptions;
import com.espertech.esper.client.deploy.DeploymentResult;
import com.espertech.esper.client.deploy.EPDeploymentAdmin;
import com.espertech.esper.client.deploy.Module;
import com.espertech.esper.client.deploy.ModuleItem;
import com.espertech.esper.client.soda.AnnotationAttribute;
import com.espertech.esper.client.soda.AnnotationPart;
import com.espertech.esper.client.soda.EPStatementObjectModel;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.client.time.TimerControlEvent;
import com.espertech.esper.core.EPServiceProviderImpl;
import com.espertech.esper.util.JavaClassHelper;
import com.espertech.esperio.AdapterCoordinator;
import com.espertech.esperio.AdapterCoordinatorImpl;
import com.espertech.esperio.csv.CSVInputAdapter;
import com.espertech.esperio.csv.CSVInputAdapterSpec;

public class RuleServiceImpl extends RuleServiceBase {

    private static Logger logger = MyLogger.getLogger(RuleServiceImpl.class.getName());
    private static boolean simulation = ConfigurationUtil.getBaseConfig().getBoolean("simulation");

    private Map<String, AdapterCoordinator> coordinators = new HashMap<String, AdapterCoordinator>();
    private Map<String, Boolean> internalClock = new HashMap<String, Boolean>();
    private Map<String, EPServiceProvider> serviceProviders = new HashMap<String, EPServiceProvider>();

    @Override
    protected void handleInitServiceProvider(String strategyName) {

        String providerURI = getProviderURI(strategyName);

        Configuration configuration = new Configuration();
        configuration.configure("esper-" + providerURI.toLowerCase() + ".cfg.xml");

        initVariables(strategyName, configuration);

        Strategy strategy = getLookupService().getStrategyByNameFetched(strategyName);
        configuration.getVariables().get("engineStrategy").setInitializationValue(strategy);

        EPServiceProvider serviceProvider = EPServiceProviderManager.getProvider(providerURI, configuration);

        // must send time event before first schedule pattern
        serviceProvider.getEPRuntime().sendEvent(new CurrentTimeEvent(0));
        this.internalClock.put(strategyName, false);

        logger.debug("initialized service provider: " + strategyName);

        this.serviceProviders.put(providerURI, serviceProvider);
    }

    @Override
    protected boolean handleIsInitialized(String strategyName) throws Exception {

        return this.serviceProviders.containsKey(getProviderURI(strategyName));
    }

    @Override
    protected void handleDestroyServiceProvider(String strategyName) {

        getServiceProvider(strategyName).destroy();
        this.serviceProviders.remove(getProviderURI(strategyName));

        logger.debug("destroyed service provider: " + strategyName);
    }

    @Override
    protected void handleDeployRule(String strategyName, String moduleName, String ruleName) throws Exception {

        deployRule(strategyName, moduleName, ruleName, null, new Object[] {});
    }

    @Override
    protected void handleDeployRule(String strategyName, String moduleName, String ruleName, String alias, Object[] params) throws Exception {

        EPAdministrator administrator = getServiceProvider(strategyName).getEPAdministrator();

        // do nothing if the statement already exists
        EPStatement oldStatement = administrator.getStatement(ruleName);
        if (oldStatement != null && oldStatement.isStarted()) {
            logger.warn(ruleName + " is already deployed and started");
            return;
        }

        // read the statement from the module
        EPDeploymentAdmin deployAdmin = administrator.getDeploymentAdmin();
        Module module = deployAdmin.read("module-" + moduleName + ".epl");
        List<ModuleItem> items = module.getItems();

        // go through all statements in the module
        EPStatement newStatement = null;
        items: for (ModuleItem item : items) {
            String exp = item.getExpression();

            // get the ObjectModel for the statement
            EPStatementObjectModel model;
            if (exp.contains("?")) {
                EPPreparedStatementImpl prepared = ((EPPreparedStatementImpl) administrator.prepareEPL(exp));
                model = prepared.getModel();
            } else {
                model = administrator.compileEPL(exp);
            }

            // go through all annotations and check if the statement has the 'name' 'ruleName'
            List<AnnotationPart> annotationParts = model.getAnnotations();
            for (AnnotationPart annotationPart : annotationParts) {
                if (annotationPart.getName().equals("Name")) {
                    for (AnnotationAttribute attribute : annotationPart.getAttributes()) {
                        if (attribute.getValue().equals(ruleName)) {

                            // set the alias
                            if (alias != null) {
                                attribute.setValue(alias);
                            }

                            // set the prepared statement params
                            if (exp.contains("?")) {
                                EPPreparedStatementImpl prepared = ((EPPreparedStatementImpl) administrator.prepareEPL(exp));
                                for (int i = 0; i < params.length; i++) {
                                    prepared.setObject(i + 1, params[i]);
                                }
                                model = prepared.getModel();
                            }

                            // create the statement
                            newStatement = administrator.createEPL(model.toEPL());

                            // check if the statement is elgible, other destory it righ away
                            processAnnotations(strategyName, newStatement);

                            // break iterating over the statements
                            break items;
                        }
                    }
                }
            }
        }

        if (newStatement == null) {
            logger.warn("statement " + ruleName + " was not found");
        } else {
            logger.debug("deployed rule " + newStatement.getName() + " on service provider: " + strategyName);
        }
    }

    @Override
    protected void handleDeployModule(String strategyName, String moduleName) throws java.lang.Exception {

        EPAdministrator administrator = getServiceProvider(strategyName).getEPAdministrator();
        EPDeploymentAdmin deployAdmin = administrator.getDeploymentAdmin();
        Module module = deployAdmin.read("module-" + moduleName + ".epl");
        DeploymentResult deployResult = deployAdmin.deploy(module, new DeploymentOptions());
        List<EPStatement> statements = deployResult.getStatements();

        for (EPStatement statement : statements) {

            // check if the statement is elgible, other destory it righ away
            processAnnotations(strategyName, statement);
        }

        logger.debug("deployed module " + moduleName + " on service provider: " + strategyName);
    }

    @Override
    protected void handleDeployAllModules(String strategyName) throws Exception {

        Strategy strategy = getLookupService().getStrategyByName(strategyName);
        String[] modules = strategy.getModules().split(",");
        for (String module : modules) {
            deployModule(strategyName, module);
        }
    }

    @Override
    protected boolean handleIsDeployed(String strategyName, String ruleName) throws Exception {

        EPStatement statement = getServiceProvider(strategyName).getEPAdministrator().getStatement(ruleName);

        if (statement != null && statement.isStarted()) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected void handleUndeployRule(String strategyName, String ruleName) throws Exception {

        // destroy the statement
        EPStatement statement = getServiceProvider(strategyName).getEPAdministrator().getStatement(ruleName);

        if (statement != null && statement.isStarted()) {
            statement.destroy();
            logger.debug("undeployed rule " + ruleName);
        }
    }

    @Override
    protected void handleUndeployModule(String strategyName, String moduleName) throws java.lang.Exception {

        EPAdministrator administrator = getServiceProvider(strategyName).getEPAdministrator();
        EPDeploymentAdmin deployAdmin = administrator.getDeploymentAdmin();
        for (DeploymentInformation deploymentInformation : deployAdmin.getDeploymentInformation()) {
            if (deploymentInformation.getModule().getName().equals(moduleName)) {
                deployAdmin.undeploy(deploymentInformation.getDeploymentId());
            }
        }

        logger.debug("undeployed module " + moduleName);
    }

    @Override
    protected void handleSendEvent(String strategyName, Object obj) {

        if (simulation) {
            Strategy strategy = getLookupService().getStrategyByName(strategyName);
            if (strategy.isAutoActivate()) {
                getServiceProvider(strategyName).getEPRuntime().sendEvent(obj);
            }
        } else {

            // check if it is the localStrategy
            if (StrategyUtil.getStartedStrategyName().equals(strategyName)) {
                getServiceProvider(strategyName).getEPRuntime().sendEvent(obj);
            } else {
                getStrategyService().sendEvent(strategyName, obj);
            }
        }
    }

    @Override
    protected void handleRouteEvent(String strategyName, Object obj) {

        // routing always goes to the local engine
        getServiceProvider(strategyName).getEPRuntime().route(obj);
    }

    protected List<Object> handleExecuteQuery(String strategyName, String query) {

        List<Object> objects = new ArrayList<Object>();
        EPOnDemandQueryResult result = getServiceProvider(strategyName).getEPRuntime().executeQuery(query);
        for (EventBean row : result.getArray()) {
            Object object = row.getUnderlying();
            objects.add(object);
        }
        return objects;
    }

    @Override
    protected Object handleGetLastEvent(String strategyName, String ruleName) {

        EPStatement statement = getServiceProvider(strategyName).getEPAdministrator().getStatement(ruleName);
        if (statement != null && statement.isStarted()) {
            SafeIterator<EventBean> it = statement.safeIterator();
            try {
                if (it.hasNext()) {
                    return it.next().getUnderlying();
                }
            } finally {
                it.close();
            }
        }
        return null;
    }

    @Override
    protected Object handleGetLastEventProperty(String strategyName, String ruleName, String property) throws Exception {

        EPStatement statement = getServiceProvider(strategyName).getEPAdministrator().getStatement(ruleName);
        if (statement != null && statement.isStarted()) {
            SafeIterator<EventBean> it = statement.safeIterator();
            try {
                return it.next().get(property);
            } finally {
                it.close();
            }
        }
        return null;
    }

    @Override
    protected List<Object> handleGetAllEvents(String strategyName, String ruleName) {

        EPStatement statement = getServiceProvider(strategyName).getEPAdministrator().getStatement(ruleName);
        List<Object> list = new ArrayList<Object>();
        if (statement != null && statement.isStarted()) {
            SafeIterator<EventBean> it = statement.safeIterator();
            try {
                while (it.hasNext()) {
                    EventBean bean = it.next();
                    Object underlaying = bean.getUnderlying();
                    list.add(underlaying);
                }
            } finally {
                it.close();
            }
        }
        return list;
    }

    @Override
    protected List<Object> handleGetAllEventsProperty(String strategyName, String ruleName, String property) throws Exception {

        EPStatement statement = getServiceProvider(strategyName).getEPAdministrator().getStatement(ruleName);
        List<Object> list = new ArrayList<Object>();
        if (statement != null && statement.isStarted()) {
            SafeIterator<EventBean> it = statement.safeIterator();
            try {
                while (it.hasNext()) {
                    EventBean bean = it.next();
                    Object underlaying = bean.get(property);
                    list.add(underlaying);
                }
            } finally {
                it.close();
            }
        }
        return list;
    }

    @Override
    protected void handleSetInternalClock(String strategyName, boolean internal) {

        this.internalClock.put(strategyName, internal);

        if (internal) {
            sendEvent(strategyName, new TimerControlEvent(TimerControlEvent.ClockType.CLOCK_INTERNAL));
            EPServiceProviderImpl provider = (EPServiceProviderImpl) getServiceProvider(strategyName);
            provider.getTimerService().enableStats();
        } else {
            sendEvent(strategyName, new TimerControlEvent(TimerControlEvent.ClockType.CLOCK_EXTERNAL));
            EPServiceProviderImpl provider = (EPServiceProviderImpl) getServiceProvider(strategyName);
            provider.getTimerService().disableStats();
        }

        logger.debug("set internal clock to: " + internal + " for strategy: " + strategyName);
    }

    @Override
    protected boolean handleIsInternalClock(String strategyName) {

        return this.internalClock.get(strategyName);
    }

    @Override
    protected void handleSetCurrentTime(CurrentTimeEvent currentTimeEvent) {

        // sent currentTime to all local engines
        for (String providerURI : EPServiceProviderManager.getProviderURIs()) {
            sendEvent(providerURI, currentTimeEvent);
        }
    }

    @Override
    protected long handleGetCurrentTime(String strategyName) {

        return getServiceProvider(strategyName).getEPRuntime().getCurrentTime();
    }

    @Override
    protected void handleInitCoordination(String strategyName) throws Exception {

        this.coordinators.put(strategyName, new AdapterCoordinatorImpl(getServiceProvider(strategyName), true, true));
    }

    @Override
    protected void handleCoordinate(String strategyName, CSVInputAdapterSpec csvInputAdapterSpec) throws Exception {

        InputAdapter inputAdapter;
        if (csvInputAdapterSpec instanceof CsvTickInputAdapterSpec) {
            inputAdapter = new CsvTickInputAdapter(getServiceProvider(strategyName), (CsvTickInputAdapterSpec) csvInputAdapterSpec);
        } else if (csvInputAdapterSpec instanceof CsvBarInputAdapterSpec) {
            inputAdapter = new CsvBarInputAdapter(getServiceProvider(strategyName), (CsvBarInputAdapterSpec) csvInputAdapterSpec);
        } else {
            inputAdapter = new CSVInputAdapter(getServiceProvider(strategyName), csvInputAdapterSpec);
        }
        this.coordinators.get(strategyName).coordinate(inputAdapter);
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected void handleCoordinate(String strategyName, Collection baseObjects, String timeStampColumn) throws Exception {

        InputAdapter inputAdapter = new DBInputAdapter(getServiceProvider(strategyName), baseObjects, timeStampColumn);
        this.coordinators.get(strategyName).coordinate(inputAdapter);
    }

    @Override
    protected void handleCoordinateTicks(String strategyName, Date startDate) throws Exception {

        InputAdapter inputAdapter = new BatchDBTickInputAdapter(getServiceProvider(strategyName), startDate);
        this.coordinators.get(strategyName).coordinate(inputAdapter);
    }

    @Override
    protected void handleStartCoordination(String strategyName) throws Exception {

        this.coordinators.get(strategyName).start();
    }

    @Override
    protected void handleSetProperty(String strategyName, String key, String value) {

        key = key.replace(".", "_");
        EPRuntime runtime = getServiceProvider(strategyName).getEPRuntime();
        if (runtime.getVariableValueAll().containsKey(key)) {
            Class<?> clazz = runtime.getVariableValue(key).getClass();
            Object castedObj = JavaClassHelper.parse(clazz, value);
            runtime.setVariableValue(key, castedObj);
        }
    }

    @Override
    protected Object handleGetProperty(String strategyName, String key) {

        key = key.replace(".", "_");
        EPRuntime runtime = getServiceProvider(strategyName).getEPRuntime();
        return runtime.getVariableValue(key);
    }

    private String getProviderURI(String strategyName) {

        return (strategyName == null || "".equals(strategyName)) ? StrategyImpl.BASE : strategyName.toUpperCase();
    }

    private EPServiceProvider getServiceProvider(String strategyName) {

        String providerURI = getProviderURI(strategyName);

        EPServiceProvider serviceProvider = this.serviceProviders.get(providerURI);
        if (serviceProvider == null) {
            throw new RuleServiceException("strategy " + providerURI + " is not initialized yet!");
        }

        return serviceProvider;
    }

    /**
     * initialize all the variables from the Configuration
     */
    private void initVariables(String strategyName, Configuration configuration) {

        try {
            Map<String, ConfigurationVariable> variables = configuration.getVariables();
            for (Map.Entry<String, ConfigurationVariable> entry : variables.entrySet()) {
                String key = entry.getKey().replace("_", ".");
                String obj = ConfigurationUtil.getStrategyConfig(strategyName).getString(key);
                if (obj != null) {
                    Class<?> clazz = Class.forName(entry.getValue().getType());
                    Object castedObj = JavaClassHelper.parse(clazz, obj);
                    entry.getValue().setInitializationValue(castedObj);
                }
            }
        } catch (ClassNotFoundException e) {
            throw new RuleServiceException(e);
        }
    }

    private void processAnnotations(String strategyName, EPStatement statement) throws ClassNotFoundException, InstantiationException, IllegalAccessException {

        Annotation[] annotations = statement.getAnnotations();
        for (Annotation annotation : annotations) {
            if (annotation instanceof Subscriber) {

                Subscriber subscriber = (Subscriber) annotation;
                Object obj = getSubscriber(subscriber.className());
                statement.setSubscriber(obj);

            } else if (annotation instanceof Listeners) {

                Listeners listeners = (Listeners)annotation;
                for (String className : listeners.classNames()) {
                    Class<?> cl = Class.forName(className);
                    Object obj = cl.newInstance();
                    if (obj instanceof StatementAwareUpdateListener) {
                        statement.addListener((StatementAwareUpdateListener) obj);
                    } else {
                        statement.addListener((UpdateListener) obj);
                    }
                }
            } else if (annotation instanceof RunTimeOnly && simulation) {

                statement.destroy();
                return;

            } else if (annotation instanceof SimulationOnly && !simulation) {

                statement.destroy();
                return;

            } else if (annotation instanceof Condition) {

                Condition condition = (Condition) annotation;
                String key = condition.key();
                if (!ConfigurationUtil.getStrategyConfig(strategyName).getBoolean(key)) {
                    statement.destroy();
                    return;
                }
            }
        }
    }

    private Object getSubscriber(String fqdn) throws ClassNotFoundException {

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
