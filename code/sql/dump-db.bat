"D:\MySQL Server 5.1\bin\mysqldump.exe" --skip-triggers --no-data -u @localhost -r ^
D:\AlgoTrader\code\sql\db-structure.sql ^
algotrader

"D:\MySQL Server 5.1\bin\mysqldump.exe" ^
--skip-extended-insert  --skip-triggers ^
--ignore-table=algotrader.ask ^
--ignore-table=algotrader.bar ^
--ignore-table=algotrader.bid ^
--ignore-table=algotrader.dividend ^
--ignore-table=algotrader.history ^
--ignore-table=algotrader.position ^
--ignore-table=algotrader.sabr_params ^
--ignore-table=algotrader.trade ^
--complete-insert --no-create-info -u @localhost ^
-r D:\AlgoTrader\code\sql\db-data.sql ^
algotrader

"D:\AlgoTrader\code\sql\unix2dos.exe" D:\AlgoTrader\code\sql\db-structure.sql
"D:\AlgoTrader\code\sql\unix2dos.exe" D:\AlgoTrader\code\sql\db-data.sql
