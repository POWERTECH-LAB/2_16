-- MySQL dump 10.13  Distrib 5.1.48, for Win64 (unknown)
--
-- Host: localhost    Database: algotrader
-- ------------------------------------------------------
-- Server version    5.1.48-community-log

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Dumping data for table `rule`
--

LOCK TABLES `rule` WRITE;
/*!40000 ALTER TABLE `rule` DISABLE KEYS */;
INSERT INTO `rule` VALUES (1,'RETRIEVE_TICKS',0,'select * \r\nfrom pattern[every timer:at (*, 9:17, *, *, 1:5)]\r\nwhere simulation = false',NULL,'com.algoTrader.service.TickServiceImpl$RetrieveTickListener','','\0'),(2,'SET_MARGINS',0,'every timer:at (0, 7, *, *, 1:5)',NULL,'com.algoTrader.service.StockOptionServiceImpl$SetMarginsListener','','\0'),(3,'PROCESS_CASH_TRANSACTIONS',0,'select * \r\nfrom pattern[every timer:at (0, 8, *, *, 1:5)]\r\nwhere simulation = false',NULL,'com.algoTrader.service.AccountServiceImpl$ProcessCashTransactionsListener','','\0'),(4,'CREATE_PORTFOLIO_VALUE',0,'insert into Portfolio\r\nselect current_timestamp() as timestamp,\r\nLookupUtil.getPortfolioValue() as value,\r\ncashTransaction\r\nfrom pattern[every(timer:at (0, 10:18, *, *, 1:5) or cashTransaction=Transaction(type=TransactionType.CREDIT or type=TransactionType.DEBIT or type=TransactionType.INTREST or type=TransactionType.FEES))]\r\nwhere LookupUtil.hasLastTicks()','com.algoTrader.subscriber.PrintPortfolioValueSubscriber',NULL,'','\0'),(5,'CREATE_MONTHLY_PERFORMANCE',2,'insert into MonthlyPerformance\r\nselect DateUtil.toDate(current_timestamp()) as date,\r\nportfolio.value.netLiqValue / prior(1, portfolio.value.netLiqValue) - 1 as value\r\nfrom pattern[every timer:at (0, 18, lastweekday, *, *) -> portfolio=Portfolio]\r\nhaving prior(1, portfolio.value.netLiqValue) != null and \r\nportfolio.value.netLiqValue != prior(1, portfolio.value.netLiqValue)\r\n',NULL,NULL,'','\0'),(6,'REBALANCE_PORTFOLIO',1,'every timer:at (0, 18, lastweekday, *, *) -> Portfolio',NULL,'com.algoTrader.service.AccountServiceImpl$RebalancePortfolioListener','','\0'),(10,'GET_LAST_TICK',6,'select tick.security.id as securityId, tick.* as tick \r\nfrom Tick.std:groupwin(security.id).win:time(7 days).win:length(1) as tick\r\n',NULL,NULL,'','\0'),(13,'EXPIRE_POSITION',2,'select position.id as positionId\r\nfrom Tick(security.strategyUnderlaying = true) as indexTick,\r\nmethod:LookupUtil.getOpenPositions() as position\r\nwhere instanceof(position.security, com.algoTrader.entity.StockOption)\r\nand cast(position.security?.expiration.time, long) < current_timestamp()\r\nand position.quantity != 0','com.algoTrader.service.StockOptionServiceImpl$ExpirePositionSubscriber',NULL,'','\0'),(14,'CLOSE_POSITION',1,'select position.id\r\nfrom Tick as tick,\r\nmethod:LookupUtil.getPositions(tick.security) as position\r\nwhere position.quantity != 0\r\nand position.exitValue != null\r\nand tick.currentValue  >= position.exitValue','com.algoTrader.service.PositionServiceImpl$ClosePositionSubscriber',NULL,'','\0'),(15,'CREATE_AGGREGATED_TICK',5,'insert into AggregatedTick\r\nselect indexTick, volaTick, optionTick\r\nfrom pattern [every indexTick=Tick(security.isin = engineStrategy.underlaying.isin) \r\n-> volaTick=Tick(security.id=indexTick.security.volatility.id) and not Tick(security.isin = engineStrategy.underlaying.isin)\r\n-> every optionTick=Tick(security.underlaying.id=indexTick.security.id) and not Tick(security.isin = engineStrategy.underlaying.isin or security.id=indexTick.security.volatility.id)]',NULL,NULL,'','\0'),(16,'CREATE_EXIT_VALUE',4,'select position.id as positionId,\r\nexitValue.value as value\r\nfrom AggregatedTick,\r\nmethod:LookupUtil.getPositions(optionTick.security) as position,\r\nmethod:ThetaUtil.getExitValue(position.strategy.name, cast(optionTick.security, com.algoTrader.entity.StockOption), indexTick.currentValueDouble, volaTick.currentValueDouble / 100) as exitValue\r\nwhere exitValue.value < position.exitValue\r\nand position.quantity != 0\r\nand optionTick.currentValueDouble != 0','com.algoTrader.service.PositionServiceImpl$SetExitValueSubscriber',NULL,'','\0'),(17,'ROLL_POSITION',3,'select position.strategy.name as strategyName,\r\nposition.id as positionId,\r\nindexTick.security.id as underlayingId,\r\nindexTick.currentValue as underlayingSpot\r\nfrom AggregatedTick,\r\nmethod:LookupUtil.getPositions(optionTick.security) as position\r\nwhere ThetaUtil.isDeltaTooLow(position.strategy.name, cast(optionTick.security, com.algoTrader.entity.StockOption), optionTick.currentValueDouble, indexTick.currentValueDouble)\r\nand position != null\r\nand position.quantity != 0','com.algoTrader.service.theta.ThetaServiceImpl$RollPositionSubscriber',NULL,'','\0'),(18,'OPEN_POSITION',2,'select \r\nengineStrategy.name as strategyName,\r\noptionTick.security.id as securityId,\r\noptionTick.currentValue as currentValue,\r\nindexTick.currentValue as underlayingSpot,\r\noptionTick.settlement as stockOptionSettlement,\r\nindexTick.settlement as underlayingSettlement,\r\nvolaTick.currentValueDouble / 100 as volatility\r\nfrom AggregatedTick\r\nwhere optionTick.security.id = ?','com.algoTrader.service.theta.ThetaServiceImpl$OpenPositionSubscriber',NULL,'\0','\0'),(19,'CREATE_K_FAST',1,'insert into KFast\r\nselect security,\r\n(select (last(currentValueDouble) - min(currentValueDouble))/(max(currentValueDouble) - min(currentValueDouble)) as call\r\nfrom Tick(security.isin = engineStrategy.underlaying.isin).win:length(cast(callKFastDays * simulation_eventsPerDay, int))),\r\n(select (last(currentValueDouble) - min(currentValueDouble))/(max(currentValueDouble) - min(currentValueDouble)) as put\r\nfrom Tick(security.isin = engineStrategy.underlaying.isin).win:length(cast(putKFastDays * simulation_eventsPerDay, int)))\r\nfrom Tick(security.isin = engineStrategy.underlaying.isin)\r\nhaving count(id) >= cast(Math.max(callKFastDays,putKFastDays) * simulation_eventsPerDay ,int)',NULL,NULL,'',''),(20,'CREATE_END_OF_DAY_TICK',0,'insert into EndOfDayTick\r\nselect tick.*\r\nfrom pattern [every tick=Tick(security.isin = engineStrategy.underlaying.isin) -> (timer:at (0, 18, *, *, 1:5) and not Tick(security.isin = engineStrategy.underlaying.isin))]',NULL,NULL,'',''),(21,'CREATE_DAILY_CHANGE',0,'insert into DailyChange\r\nselect security,\r\ncase when currentValueDouble > prior(1, currentValueDouble) then currentValueDouble - prior(1, currentValueDouble) else 0.0 end as uValue,\r\ncase when currentValueDouble < prior(1, currentValueDouble) then prior(1, currentValueDouble) - currentValueDouble else 0.0 end as dValue\r\nfrom EndOfDayTick\r\n\r\n\r\n',NULL,NULL,'','\0'),(23,'CREATE_MACD',0,'insert into Macd\r\nselect security,\r\nema(currentValueDouble,macdFast) - ema(currentValueDouble,macdSlow) as value\r\nfrom EndOfDayTick\r\nhaving count(*) > Math.round(macdSlow * macdWeight)\r\n',NULL,NULL,'',''),(30,'CREATE_K_SLOW',0,'insert into KSlow\r\nselect security,\r\n(select avg(call) from KFast.win:length(cast(callKSlowDays * simulation_eventsPerDay, int))) as call,\r\n(select avg(put) from KFast.win:length(cast(putKSlowDays * simulation_eventsPerDay, int))) as put\r\nfrom KFast\r\nhaving count(security) >= cast(Math.max(callKSlowDays,putKSlowDays) * simulation_eventsPerDay ,int)',NULL,NULL,'',''),(31,'CREATE_D_SLOW',0,'insert into DSlow\r\nselect security,\r\n(select avg(call) from KSlow.win:length(cast(callDSlowDays * simulation_eventsPerDay,int))) as call,\r\n(select avg(put) from KSlow.win:length(cast(putDSlowDays * simulation_eventsPerDay,int))) as put\r\nfrom KSlow\r\nhaving count(security) >= cast(Math.max(callDSlowDays,putDSlowDays) * simulation_eventsPerDay ,int)',NULL,NULL,'',''),(32,'GO_LONG',0,'select\r\nengineStrategy.name as strategyName, \r\nindexTick.security.id as underlayingid,\r\nindexTick.currentValue as underlayingSpot\r\nfrom pattern [every (indexTick=Tick(security.isin=engineStrategy.underlaying.isin) -> k=KSlow -> d=DSlow)]\r\nwhere k.put > d.put\r\nand prior(1, k.put) < prior(1, d.put)\r\nand d.put < putTrigger\r\nand putEnabled = true','com.algoTrader.service.theta.ThetaServiceImpl$GoLongSubscriber',NULL,'','\0'),(33,'TAKE_PROFIT_LONG',0,'select\r\nengineStrategy.name as strategyName, \r\nposition.id\r\nfrom pattern [every (indexTick=Tick(security.isin=engineStrategy.underlaying.isin) -> k=KSlow -> d=DSlow)],\r\nmethod:LookupUtil.getBullishPositionsByStrategy(engineStrategy.name) as position\r\nwhere k.call < d.call\r\nand prior(1, k.call) > prior(1, d.call)\r\nand d.call > callTrigger','com.algoTrader.service.theta.ThetaServiceImpl$TakeProfitSubscriber',NULL,'','\0'),(34,'GO_SHORT',0,'select\r\nengineStrategy.name as strategyName, \r\nindexTick.security.id as underlayingid,\r\nindexTick.currentValue as underlayingSpot\r\nfrom pattern [every (indexTick=Tick(security.isin=engineStrategy.underlaying.isin) -> k=KSlow -> d=DSlow)]\r\nwhere k.call < d.call\r\nand prior(1, k.call) > prior(1, d.call)\r\nand d.call > callTrigger\r\nand callEnabled = true','com.algoTrader.service.theta.ThetaServiceImpl$GoShortSubscriber',NULL,'','\0'),(35,'TAKE_PROFIT_SHORT',0,'select\r\nengineStrategy.name as strategyName, \r\nposition.id\r\nfrom pattern [every (indexTick=Tick(security.isin=engineStrategy.underlaying.isin) -> k=KSlow -> d=DSlow)],\r\nmethod:LookupUtil.getBearishPositionsByStrategy(engineStrategy.name) as position\r\nwhere k.put > d.put\r\nand prior(1, k.put) < prior(1, d.put)','com.algoTrader.service.theta.ThetaServiceImpl$TakeProfitSubscriber',NULL,'','\0'),(36,'CREATE_STOCHASTIC_VO',0,'insert into StochasticVO\r\nselect indexTick.currentValueDouble as underlaying,\r\nDateUtil.toDate(current_timestamp()) as dateTime,\r\nkFast.call as KFastCall,\r\nkFast.put as KFastPut,\r\nkSlow.call as KSlowCall,\r\nkSlow.put as KSlowPut,\r\ndSlow.call as DSlowCall,\r\ndSlow.put as DSlowPut\r\nfrom pattern [every (indexTick=Tick(security.isin=engineStrategy.underlaying.isin) -> kFast=KFast -> kSlow=KSlow -> dSlow=DSlow)]',NULL,NULL,'',''),(37,'KEEP_STOCHASTIC_VO',0,'select * from StochasticVO.win:length(cast((Math.max(callKSlowDays,putKSlowDays) + Math.max(callDSlowDays,putDSlowDays) + Math.max(callKFastDays,putKFastDays)) * simulation_eventsPerDay, int))',NULL,NULL,'',''),(40,'CREATE_MACD_SIGNAL',0,'insert into MacdSignal\r\nselect security,\r\nema(value,macdSignal) as value\r\nfrom Macd\r\nhaving count(*) > Math.round(macdSignal * macdWeight)',NULL,NULL,'',''),(41,'SET_TREND',0,'select \r\nengineStrategy.name as strategyName,\r\ncase when macd.value > signal.value then true else false end as bullish\r\nfrom pattern [every (macd=Macd -> signal=MacdSignal)]\r\nwhere ((select last(size) from MacdSignal.std:size()) = 1) or\r\n(macd.value > signal.value and prior(1,macd.value) <= prior(1,signal.value)) or\r\n(macd.value < signal.value and prior(1,macd.value) >= prior(1,signal.value))\r\n','com.algoTrader.service.theta.ThetaServiceImpl$TrendSubscriber',NULL,'',''),(42,'CREATE_MACD_VO',0,'insert into MacdVO\r\nselect indexTick.currentValueDouble as underlaying,\r\nDateUtil.toDate(current_timestamp()) as dateTime,\r\nmacd.value as macd,\r\nsignal.value as signal\r\nfrom pattern [every (indexTick=Tick(security.isin=engineStrategy.underlaying.isin) -> macd=Macd -> signal=MacdSignal)]',NULL,NULL,'',''),(43,'KEEP_MACD_VO',0,'select *\r\nfrom MacdVO.win:keepall()',NULL,NULL,'',''),(51,'CREATE_PERFORMANCE_KEYS',0,'insert into PerformanceKeys\r\nselect uni.datapoints as n, \r\nuni.geomaverage as avgM,\r\nuni.stddev as stdM, \r\nMath.pow(1 + uni.geomaverage,12) - 1  as avgY,\r\nuni.stddev * Math.sqrt(12) as stdY,\r\n(Math.pow(1 + uni.geomaverage,12) - 1 - marketIntrest) / (uni.stddev * Math.sqrt(12)) as sharpRatio\r\nfrom MonthlyPerformance.win:keepall().stat:uni(value) as uni',NULL,NULL,'','\0'),(52,'KEEP_MONTHLY_PERFORMANCE',0,'select *\r\nfrom MonthlyPerformance.win:keepall()',NULL,NULL,'','\0'),(53,'CREATE_DRAW_DOWN',0,'insert into DrawDown\r\nselect 1 - portfolio.value.netLiqValue / max(portfolio.value.netLiqValue) as amount,\r\ncase when portfolio.value.netLiqValue = max(portfolio.value.netLiqValue) then DrawDownUtil.resetDrawDownPeriod() else DrawDownUtil.increaseDrawDownPeriod(current_timestamp - prior(1, portfolio.timestamp)) end as period\r\nfrom Portfolio as portfolio',NULL,NULL,'','\0'),(54,'CREATE_MAX_DRAW_DOWN',0,'insert into MaxDrawDown\r\nselect max(drawDown.amount) as amount,\r\nmax(drawDown.period) as period\r\nfrom DrawDown as drawDown',NULL,NULL,'','\0'),(60,'PROCESS_PREARRANGED_ORDERS',0,'select \'SMI\',* \r\nfrom PrearrangedOrder','com.algoTrader.service.TransactionServiceImpl$RerunOrderSubscriber',NULL,'\0','\0');
/*!40000 ALTER TABLE `rule` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2011-02-09 11:22:13
