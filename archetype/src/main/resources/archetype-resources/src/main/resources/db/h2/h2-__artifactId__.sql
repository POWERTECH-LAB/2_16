INSERT INTO "strategy" (ID, NAME, AUTO_ACTIVATE, ALLOCATION, INIT_MODULES, VERSION) VALUES (${strategyId},'${serviceName.toUpperCase()}',1,1,'${artifactId}',0);

#foreach( $subscriptionId in $subscriptionIds.split(",") )
INSERT INTO "subscription" (PERSISTENT, SECURITY_FK, STRATEGY_FK, FEED_TYPE, VERSION) VALUES (1,${subscriptionId},${strategyId},'IB',0);
#end
