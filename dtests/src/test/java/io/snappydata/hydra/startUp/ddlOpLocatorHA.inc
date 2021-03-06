io.snappydata.hydra.cluster.SnappyPrms-waitTimeBeforeNextCycleVM = 60; //how long to wait before
//nodes will be cycled.
io.snappydata.hydra.cluster.SnappyPrms-cycleVms = true;

util.StopStartPrms-stopModes = NICE_KILL;
util.StopStartPrms-numVMsToStop = RANGE 1 ${numVMsToStop} EGNAR;

INITTASK    taskClass   = util.StopStartVMs  taskMethod = StopStart_initTask
            threadGroups = snappyThreads, locatorThreads, snappyStoreThreads, leadThreads;

TASK       taskClass   = io.snappydata.hydra.cluster.SnappyLocatorHATest taskMethod  = HydraTask_ddlOpDuringLocatorHA
           maxThreads = 1
           startInterval = 60
           threadGroups = snappyThreads
           maxTimesToRun = 1
           ;

CLOSETASK  taskClass   = io.snappydata.hydra.cluster.SnappyTest taskMethod  = restoreLocatorConfigData
           threadGroups = snappyThreads;

CLOSETASK   taskClass   = io.snappydata.hydra.cluster.SnappyTest taskMethod  = HydraTask_executeSnappyJob
            io.snappydata.hydra.cluster.SnappyPrms-jobClassNames = io.snappydata.hydra.northwind.ValidateNWQueriesJob
            io.snappydata.hydra.cluster.SnappyPrms-appPropsForJobServer =
            "dataFilesLocation=${dataFilesLocation},tableType=${tableType},fullResultSetValidation=${fullResultSetValidation},isSmokeRun=${isSmokeRun},numRowsValidation=${numRowsValidation}"
            io.snappydata.hydra.cluster.SnappyPrms-userAppJar = snappydata-store-scala-tests*tests.jar
            threadGroups = snappyThreads;

CLOSETASK   taskClass   = io.snappydata.hydra.cluster.SnappyTest taskMethod  = HydraTask_stopSnappyCluster
            threadGroups = snappyThreads;

CLOSETASK   taskClass   = io.snappydata.hydra.cluster.SnappyTest taskMethod  = HydraTask_startSnappyCluster
            threadGroups = snappyThreads;