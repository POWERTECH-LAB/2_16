constraints = {
    name()
    baseSymbol(nullable : true)
    isinRoot(nullable : true)
    ricRoot(nullable : true)
    market()
    currency()
    contractSize()
    scale()
    tickSizePattern()
    executionCommission(nullable : true)
    clearingCommission(nullable : true)
    marketOpenDay()
    marketOpen(format : 'HH:mm', attributes: [precision : 'minute'])
    marketClose(format : 'HH:mm', attributes: [precision : 'minute'])
    tradeable()
    synthetic()
    spreadSlope(nullable : true)
    spreadConstant(nullable : true)
    maxSpreadSlope(nullable : true)
    maxSpreadConstant(nullable : true)
    periodicity(nullable : true)
    maxGap(nullable : true)

    intrest()
    dividend()
    marginParameter()
    expirationType()
    expirationDistance()
    length()

    baseCurrency()

    underlying(nullable : true)
    brokerParameters()

    securities(display : false)
}