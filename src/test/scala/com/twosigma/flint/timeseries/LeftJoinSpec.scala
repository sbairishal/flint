/*
 *  Copyright 2015-2016 TWO SIGMA OPEN SOURCE, LLC
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.twosigma.flint.timeseries

import com.twosigma.flint.{ SpecUtils, SharedSparkContext }
import org.apache.spark.sql.types.{ LongType, IntegerType, DoubleType, StructType }
import org.scalatest.FlatSpec

class LeftJoinSpec extends FlatSpec with SharedSparkContext {

  private val defaultPartitionParallelism: Int = 5

  private val resourceDir: String = "/timeseries/leftjoin"

  private def from(filename: String, schema: StructType): TimeSeriesRDD =
    SpecUtils.withResource(s"$resourceDir/$filename") { source =>
      CSV.from(
        sqlContext,
        s"file://$source",
        header = true,
        sorted = true,
        schema = schema
      ).repartition(defaultPartitionParallelism)
    }

  "LeftJoin" should "pass `JoinOnTime` test." in {
    val priceTSRdd = from("Price.csv", Schema("tid" -> IntegerType, "price" -> DoubleType))
    val volumeTSRdd = from("Volume.csv", Schema("tid" -> IntegerType, "volume" -> LongType))
    val resultsTSRdd = from("JoinOnTime.results", Schema("tid" -> IntegerType, "price" -> DoubleType, "volume" -> LongType))
    val joinedTSRdd = priceTSRdd.leftJoin(volumeTSRdd, "0ns", Seq("tid"))
    assert(resultsTSRdd.schema == joinedTSRdd.schema)
    assert(resultsTSRdd.collect().deep == joinedTSRdd.collect().deep)
  }

  it should "pass `JoinOnTimeWithMissingMatching` test." in {
    val priceTSRdd = from("Price.csv", Schema("tid" -> IntegerType, "price" -> DoubleType))
    val volumeTSRdd = from("Volume.csv", Schema("tid" -> IntegerType, "volume" -> LongType))
    val resultsTSRdd = from(
      "JoinOnTimeWithMissingMatching.results",
      Schema("tid" -> IntegerType, "price" -> DoubleType, "volume" -> LongType)
    )
    val joinedTSRdd = priceTSRdd.leftJoin(volumeTSRdd.deleteRows(row => row.getAs[Long]("time") == 1050L), "0ns", Seq("tid"))
    assert(resultsTSRdd.schema == joinedTSRdd.schema)
    assert(resultsTSRdd.collect().deep == joinedTSRdd.collect().deep)
  }

  it should "pass `JoinOnTimeAndMultipleKeys` test." in {
    val priceTSRdd = from(
      "PriceWithIndustryGroup.csv",
      Schema("tid" -> IntegerType, "group" -> IntegerType, "price" -> DoubleType)
    )
    val volumeTSRdd = from(
      "VolumeWithIndustryGroup.csv",
      Schema("tid" -> IntegerType, "group" -> IntegerType, "volume" -> LongType)
    )
    val resultsTSRdd = from(
      "JoinOnTimeAndMultipleKeys.results",
      Schema("tid" -> IntegerType, "group" -> IntegerType, "price" -> DoubleType, "volume" -> LongType)
    )
    val joinedTSRdd = priceTSRdd.leftJoin(volumeTSRdd, "0ns", Seq("tid", "group"))
    assert(resultsTSRdd.schema == joinedTSRdd.schema)
    assert(resultsTSRdd.collect().deep == joinedTSRdd.collect().deep)
  }

}
