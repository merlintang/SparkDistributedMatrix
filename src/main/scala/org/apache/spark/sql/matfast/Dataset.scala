/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.spark.sql.matfast

import org.apache.spark.sql.catalyst.encoders.RowEncoder
import org.apache.spark.sql.catalyst.expressions.Attribute
import org.apache.spark.sql.catalyst.plans.logical._
import org.apache.spark.sql.execution.QueryExecution
import org.apache.spark.sql.matfast.plans._
import org.apache.spark.sql.{matfast, Dataset => SQLDataSet, Row, Encoder}

class Dataset[T] private[matfast]
(@transient val matfastSession: matfast.MatfastSession,
 @transient override val queryExecution: QueryExecution,
 encoder: Encoder[T]) extends SQLDataSet[T](matfastSession, queryExecution.logical, encoder)
{
  def this(sparkSession: matfast.MatfastSession, logicalPlan: LogicalPlan, encoder: Encoder[T]) = {
    this(sparkSession, sparkSession.sessionState.executePlan(logicalPlan), encoder)
  }

  def t(): DataFrame = transpose()

  def transpose(data: Seq[Attribute] = this.queryExecution.analyzed.output): DataFrame = withPlan {
    TranposeOperator(this.logicalPlan)
  }

  def rowSum(nrows: Long, ncols: Long,
             data: Seq[Attribute] = this.queryExecution.analyzed.output): DataFrame = withPlan {
    RowSumOperator(this.logicalPlan, nrows, ncols)
  }

  def colSum(nrows: Long, ncols: Long,
             data: Seq[Attribute] = this.queryExecution.analyzed.output): DataFrame = withPlan {
    ColumnSumOperator(this.logicalPlan, nrows, ncols)
  }

  def sum(nrows: Long, ncols: Long,
          data: Seq[Attribute] = this.queryExecution.analyzed.output): DataFrame = withPlan {
    SumOperator(this.logicalPlan, nrows, ncols)
  }

  def vec(nrows: Long, ncols: Long, blkSize: Int,
          data: Seq[Attribute] = this.queryExecution.analyzed.output): DataFrame = withPlan {
    VectorizeOperator(this.logicalPlan, nrows, ncols, blkSize)
  }

  def addScalar(alpha: Double, data: Seq[Attribute] =
      this.queryExecution.analyzed.output): DataFrame = withPlan {
    MatrixScalarAddOperator(this.logicalPlan, alpha)
  }

  def multiplyScalar(alpha: Double, data: Seq[Attribute] =
      this.queryExecution.analyzed.output): DataFrame = withPlan {
    MatrixScalarMultiplyOperator(this.logicalPlan, alpha)
  }

  def power(alpha: Double, data: Seq[Attribute] =
      this.queryExecution.analyzed.output): DataFrame = withPlan {
    MatrixPowerOperator(this.logicalPlan, alpha)
  }

  def addElement(leftRowNum: Long, leftColNum: Long,
                 right: DataFrame,
                 rightRowNum: Long, rightColNum: Long,
                 blkSize: Int,
                 data: Seq[Attribute] = this.queryExecution.analyzed.output): DataFrame = withPlan {
    MatrixElementAddOperator(this.logicalPlan, leftRowNum,
      leftColNum, right.logicalPlan, rightRowNum, rightColNum, blkSize)
  }

  def multiplyElement(leftRowNum: Long, leftColNum: Long,
                      right: DataFrame,
                      rightRowNum: Long, rightColNum: Long,
                      blkSize: Int, data: Seq[Attribute] = this.queryExecution.analyzed.output): DataFrame = withPlan {
    MatrixElementMultiplyOperator(this.logicalPlan, leftRowNum,
      leftColNum, right.logicalPlan, rightRowNum, rightColNum, blkSize)
  }

  def divideElement(leftRowNum: Long, leftColNum: Long,
                    right: DataFrame,
                    rightRowNum: Long, rightColNum: Long,
                    blkSize: Int,
                    data: Seq[Attribute] = this.queryExecution.analyzed.output): DataFrame = withPlan {
    MatrixElementDivideOperator(this.logicalPlan, leftRowNum, leftColNum, right.logicalPlan, rightRowNum, rightColNum, blkSize)
  }

  def matrixMultiply(leftRowNum: Long, leftColNum: Long,
                     right: DataFrame,
                     rightRowNum: Long, rightColNum: Long,
                     blkSize: Int,
                     data: Seq[Attribute] = this.queryExecution.analyzed.output): DataFrame = withPlan {
    MatrixMatrixMultiplicationOperator(this.logicalPlan, leftRowNum, leftColNum, right.logicalPlan, rightRowNum, rightColNum, blkSize)
  }

  def matrixRankOneUpdate(leftRowNum: Long, leftColNum: Long,
                          right: DataFrame,
                          rightRowNum: Long, rightColNum: Long,
                          blkSize: Int,
                          data: Seq[Attribute] = this.queryExecution.analyzed.output): DataFrame = withPlan {
    RankOneUpdateOperator(this.logicalPlan, leftRowNum, leftColNum, right.logicalPlan, rightRowNum, rightColNum, blkSize)
  }

  private def getAttributes(keys: Array[String],
                            attrs: Seq[Attribute] = this.queryExecution.analyzed.output): Array[Attribute] = {
    keys.map(key => {
      val tmp = attrs.indexWhere(_.name == key)
      if (tmp >= 0) attrs(tmp)
      else null
    })
  }

  @inline private def withPlan(logicalPlan: => LogicalPlan): DataFrame = {
    Dataset.ofRows(matfastSession, logicalPlan)
  }
}

private[matfast] object Dataset {
  def apply[T: Encoder](sparkSession: matfast.MatfastSession, logicalPlan: LogicalPlan): Dataset[T] = {
    new Dataset(sparkSession, logicalPlan, implicitly[Encoder[T]])
  }

  def ofRows(sparkSession: matfast.MatfastSession, logicalPlan: LogicalPlan): DataFrame = {
    val qe = sparkSession.sessionState.executePlan(logicalPlan)
    qe.assertAnalyzed()
    new Dataset[Row](sparkSession, qe, RowEncoder(qe.analyzed.schema))
  }
}


















