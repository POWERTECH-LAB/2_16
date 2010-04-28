-- MySQL dump 10.13  Distrib 5.1.41, for Win32 (ia32)
--
-- Host: localhost    Database: algotrader
-- ------------------------------------------------------
-- Server version    5.1.41-community

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
INSERT INTO `rule` VALUES (1,'RETRIEVE_TICKS',0,'every timer:at (*, 9:17, *, *, 1:5)',NULL,'RetrieveTickListener','','','\0',NULL),(2,'PRINT_PORTFOLIO_VALUE',0,'every timer:at (0, 9:17, *, *, 1:5)',NULL,'PrintPortfolioValueListener','','\0','\0',NULL),(3,'SET_MARGIN',0,'every timer:at (0, 1, *, *, *)',NULL,'SetMarginListener','','','\0',NULL),(4,'CREATE_END_OF_DAY_SIGNAL',0,'insert into EndOfDaySignal\r\nselect *\r\nfrom pattern[every timer:at(59,15,*,*,*)]\r\nwhere DateUtil.toHour(current_timestamp) >= 15',NULL,NULL,'\0','','\0',NULL),(10,'GET_LAST_TICK',10,'select tick.security.id as securityId, tick.* as tick \r\nfrom Tick.std:groupby(security.id).win:time(7 days).win:length(1) as tick',NULL,NULL,'\0','','\0',NULL),(11,'PRINT_TICK',9,'select *\r\nfrom Tick','PrintTickSubscriber',NULL,'\0','','\0',NULL),(12,'SIMULATE_DUMMY_SECURITIES',8,'insert into Tick\r\nselect DateUtil.toDate(current_timestamp()) as dateTime,\r\nRoundUtil.getBigDecimal(StockOptionUtil.getFairValue(option, indexTick.currentValueDouble, volaTick.currentValueDouble / 100)) as last,\r\nDateUtil.toDate(current_timestamp()) as lastDateTime,\r\n0 as vol, 0 as volBid, 0 as volAsk, cast(0.0, BigDecimal) as bid, cast(0.0, BigDecimal) as ask, 0 as openIntrest, cast(0.0, BigDecimal) as settlement,\r\noption as security\r\nfrom pattern [every (indexTick=Tick(security.isin = var_isin) -> volaTick=Tick(security.id=indexTick.security.volatility.id))],\r\nmethod:LookupUtil.getDummySecuritiesOnWatchlist() as option \r\nwhere option.underlaying.id = indexTick.security.id\r\nand var_simulation=true',NULL,NULL,'\0','','\0',NULL),(13,'CREATE_END_OF_DAY_TICK',7,'insert into EndOfDayTick\r\nselect tick.*\r\nfrom pattern [every EndOfDaySignal -> (every-distinct(tick.security) tick=Tick and not EndOfDaySignal)]',NULL,NULL,'\0','','\0',NULL),(14,'EXPIRE_POSITION',6,'select option.position.id as positionId,\r\nindexTick.security.id,\r\nindexTick.currentValue\r\nfrom Tick(security.isin = var_isin) as indexTick,\r\nmethod:LookupUtil.getStockOptionsOnWatchlist() as option \r\nwhere option.expiration.time < current_timestamp()\r\nand option.position != null\r\nand option.position.quantity != 0','ExpirePositionSubscriber',NULL,'\0','','\0',NULL),(15,'CLOSE_POSITION',5,'select\r\ntick.security.position.id\r\nfrom Tick as tick\r\nwhere tick.security.position.quantity != 0\r\nand tick.security.position.exitValue != null\r\nand tick.currentValue  > tick.security.position.exitValue','ClosePositionSubscriber',NULL,'\0','','\0',NULL),(16,'CREATE_EXIT_VALUE',4,'insert into ExitValue\r\nselect optionTick.security as security,\r\nStockOptionUtil.getExitValue(optionTick.security, indexTick.currentValueDouble, optionTick.currentValueDouble) as value\r\nfrom pattern [every indexTick=EndOfDayTick(security.isin = var_isin) -> every optionTick=EndOfDayTick(security.underlaying.id=indexTick.security.id) and not EndOfDayTick(security.isin = var_isin)]\r\nwhere optionTick.security.position != null \r\nand optionTick.security.position.quantity != 0',NULL,NULL,'\0','','\0',NULL),(17,'ROLL_POSITION',3,'select \r\noptionTick.security.position.id,\r\nindexTick.security.id,\r\nindexTick.currentValue\r\nfrom pattern [every indexTick=Tick(security.isin = var_isin) -> every optionTick=Tick(security.underlaying.id=indexTick.security.id) and not Tick(security.isin = var_isin)]\r\nwhere StockOptionUtil.isExpirationTimeToLong(optionTick.security, optionTick.currentValueDouble, optionTick.settlementDouble, indexTick.currentValueDouble)\r\nand optionTick.security.position != null\r\nand optionTick.security.position.quantity != 0','RollPositionSubscriber',NULL,'\0','','\0',NULL),(18,'OPEN_POSITION',2,'select stockOptionTick.security.id,\r\nstockOptionTick.settlement,\r\nstockOptionTick.currentValue,\r\nindexTick.currentValue\r\nfrom pattern [(indexTick=EndOfDayTick(security.isin = var_isin) -> stockOptionTick=EndOfDayTick(security.id = ?,security.underlaying.id=indexTick.security.id))]','OpenPositionSubscriber',NULL,'\0','\0','',NULL),(19,'CREATE_K_FAST',1,'insert into KFast\r\nselect security,\r\n(last(currentValueDouble) - min(currentValueDouble))/(max(currentValueDouble) - min(currentValueDouble)) as value\r\nfrom EndOfDayTick(security.isin = var_isin).win:length(cast(var_kFast_days * var_events_per_day, int))\r\nhaving max(currentValueDouble) != min(currentValueDouble)\r\n',NULL,NULL,'\0','','\0',NULL),(20,'IMMEDIATE_BUY_SIGNAL',0,'select security.id,\r\ncurrentValue\r\nfrom Tick(security.isin=var_isin).std:firstevent()','BuySignalSubscriber',NULL,'\0','\0','\0',NULL),(31,'CREATE_K_SLOW',0,'insert into KSlow\r\nselect security,\r\navg(value) as value\r\nfrom KFast.win:length(cast(var_kSlow_days * var_events_per_day, int))\r\nhaving avg(value) != null',NULL,NULL,'\0','','\0',NULL),(32,'CREATE_D_SLOW',0,'insert into DSlow\r\nselect security,\r\navg(value) as value\r\nfrom KSlow.win:length(cast(var_dSlow_days * var_events_per_day,int))\r\nhaving avg(value) != null',NULL,NULL,'\0','','\0',NULL),(33,'GET_BUY_SIGNAL',0,'select \r\nindexTick.security.id,\r\nindexTick.currentValue\r\nfrom pattern [every (indexTick=EndOfDayTick(security.isin=var_isin) -> k=KSlow -> d=DSlow)]\r\nwhere k.value > d.value\r\nand prior(1, k.value) < prior(1, d.value)\r\nand d.value < var_buy_trigger','BuySignalSubscriber',NULL,'\0','','\0',NULL),(34,'PRINT_STOCHASTIC',0,'select kFast,\r\nkSlow,\r\ndSlow\r\nfrom pattern [every (kFast=KFast -> kSlow=KSlow -> dSlow=DSlow)]','PrintStochasticSubscriber',NULL,'\0','\0','\0',NULL),(35,'SET_EXIT_VALUE',0,'select \r\nsecurity.position.id as positionId,\r\nvalue as exitValue\r\nfrom ExitValue\r\nwhere security.position.quantity != 0 \r\nand value < security.position.exitValue','SetExitValueSubscriber',NULL,'\0','','\0',NULL),(40,'CREATE_INTERPOLATION',0,'insert into Interpolation\r\nselect Math.exp(linest.YIntercept) as a, \r\nlinest.slope as b, \r\nMath.pow(correl.correlation,2) as r \r\nfrom Transaction.win:keepall().stat:linest(current_timestamp() / 31536000000, Math.log(account.totalValueDouble)) as linest, \r\nTransaction.win:keepall().stat:correl(current_timestamp() / 31536000000, Math.log(account.totalValueDouble)) as correl',NULL,NULL,'\0','','\0',NULL);
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

-- Dump completed on 2010-04-28 16:21:20
