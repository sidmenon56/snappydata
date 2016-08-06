package io.snappydata.externalstore

import scala.util.Random

import io.snappydata.cluster.{ClusterManagerTestBase, ExecutorInitiator}
import io.snappydata.test.dunit.DistributedTestBase
import io.snappydata.test.dunit.DistributedTestBase.WaitCriterion

import org.apache.spark.Logging
import org.apache.spark.sql.{SaveMode, SnappyContext}

class ExecutorMessageDUnitTest(s: String) extends ClusterManagerTestBase(s) with Logging {

  val tableName = "ExecutorMessageDUnitTest_table"

  def test01StoreBlockMapUpdatesWithExecutorDown(): Unit = {
    val snc = SnappyContext(sc)
    var props = Map.empty[String, String]
    props += ("BUCKETS" -> "7")
    executeSomething(snc, props)
    verifyMap(snc, "stopExecutor")
    restartSpark()
    getLogWriter.info("test01StoreBlockMapUpdatesWithExecutorDown() Successful")
  }

  def test02StoreBlockMapUpdatesWithNodeDown(): Unit = {
    val snc = SnappyContext(sc)
    var props = Map.empty[String, String]
    props += ("BUCKETS" -> "7")
    props += ("REDUNDANCY" -> "2")
    executeSomething(snc, props)
    verifyMap(snc, "stopProcess")
    getLogWriter.info("test02StoreBlockMapUpdatesWithNodeDown() Successful")
  }

  def executeSomething(snc: SnappyContext,
      props: Map[String, String] = Map.empty[String, String]): Unit = {
    createAndPopulateTable(snc, props)

    val wc: WaitCriterion = new WaitCriterion {
      override def done(): Boolean = {
        SnappyContext.storeToBlockMap.size == 4 // 3 servers + 1 lead/driver
      }
      override def description(): String = {
        s"Expected SnappyContext.storeToBlockMap.size: 4, actual: " +
            s"${SnappyContext.storeToBlockMap.size}"
      }
    }
    DistributedTestBase.waitForCriterion(wc, 10000, 500, true)
    for ((dm, blockId) <- SnappyContext.storeToBlockMap) {
      assert(blockId != null)
    }
  }

  def createAndPopulateTable(snc: SnappyContext, props: Map[String, String]): Unit = {
    var data = Seq(Seq(1, 2, 3), Seq(7, 8, 9), Seq(9, 2, 3), Seq(4, 2, 3), Seq(5, 6, 7))
    1 to 1000 foreach { _ =>
      data = data :+ Seq.fill(3)(Random.nextInt)
    }

    val rdd = sc.parallelize(data, data.length).map(s => new Data(s(0), s(1), s(2)))
    val dataDF = snc.createDataFrame(rdd)

    snc.createTable(tableName, "column", dataDF.schema, props)

    dataDF.write.format("column").mode(SaveMode.Append)
        .options(props).saveAsTable(tableName)
  }

  def verifyMap(snc: SnappyContext, m: String): Unit = {
    vm0.invoke(getClass, m)
    assert(SnappyContext.storeToBlockMap.size == 3)
    for ((dm, blockId) <- SnappyContext.storeToBlockMap) {
      assert(blockId != null)
    }
    verifyTable(snc)

    vm1.invoke(getClass, m)
    assert(SnappyContext.storeToBlockMap.size == 2)
    for ((dm, blockId) <- SnappyContext.storeToBlockMap) {
      assert(blockId != null)
    }
    verifyTable(snc)

//    vm2.invoke(getClass, m) // Don't shutdown the last executor, else cleanup will fail.
//    assert(SnappyContext.storeToBlockMap.size == 1)
  }

  def restartSpark(): Unit = {
    ClusterManagerTestBase.stopSpark()
    ClusterManagerTestBase.startSnappyLead(locatorPort, bootProps)
  }

  def verifyTable (snc: SnappyContext): Unit = {
    assert(snc.sql("SELECT * FROM " + tableName).collect().length == 1005)
  }
}

object ExecutorMessageDUnitTest {

  def stopExecutor(): Unit = {
    ExecutorInitiator.stop()
    Thread.sleep(1000)
  }

  def stopProcess(): Unit = {
    ClusterManagerTestBase.stopAny()
    // Thread.sleep(2000)
  }
}