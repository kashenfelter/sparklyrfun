package Sparklyrfun

import org.apache.spark.ml.linalg.{DenseVector, SparseVector, Vector, Vectors}
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.functions._

import scala.collection.mutable.ArrayBuffer


  /* object name need TOUPPPER ??!!! */

object MyUdfs {
    /**
    * 
    * @param aidVec
    * @param pkgVec
    * @param tarpkg
    */
  def getRank(aidVec:DataFrame, pkgVec:DataFrame, aidVecCol:String = "aidarrayrun_vec", pkgVecCol:String = "runapp_vec", aidCol:String = "aid", tarpkg:String = "com.cmcm.live") = {
    val pkgVec1 = pkgVec.filter(col(pkgVecCol) === tarpkg).collect()(0).
      getAs[SparseVector](pkgVecCol).toArray

    val aidVec1 = aidVec.collect()(0).getAs[SparseVector](aidVecCol)


    val udf1 = udf((runapp_vec: SparseVector, pkgVecArr:Seq[Double]) =>{
      val elemWiseProd: Array[Double] = runapp_vec.toArray.zip(pkgVecArr.toArray[Double]).map(entryTuple => entryTuple._1 * entryTuple._2)
      elemWiseProd.sum
    })
    
    aidVec.select(col(aidCol), col(aidVecCol)).
    withColumn("pkgVecArr",lit(pkgVec1)).
    select(udf1(col(pkgVecCol), col("pkgVecArr")).alias("rank")) //udf case array => wrapped array
  }
      /**
      *
      * @param df
      * @param inputCol
      * @param outputCol
      * @return
      */
    def vectorToArray(df:DataFrame, inputCol:String, outputCol:String) = {
      val vectorToSeqUDF = udf((V:Vector) => V.toArray)
      df.withColumn(outputCol, vectorToSeqUDF(col(inputCol)))
  
    }
  
  
    /**
      *
      * @param df
      * @param inputCol
      * @param outputCol
      * @param num_dot
      * @return
      */
    def vectorDotVector(df:DataFrame, inputCol:String, outputCol:String, num_dot:Int) = {
      // def dotVec(inputVec:Vector) = {
      // //  V.flatMap(x => for (y <- V) yield if(x != y) x*y else None).filter(_ != None).toVector
      // var buffer1 = new ArrayBuffer[Double]
      // val outputVec = for (x <- inputVec.toArray) yield {
      //   buffer1 +=
      //   for {
      //     y <- inputVec.toArray
      //     if (!buffer1.contains(y))
      //   }
      //     yield x*y
      // }
      def dotVec(inputVec:Vector, num_dot:Int) = {
        if (num_dot > inputVec.size - 1) {
          val num_dot = inputVec.size - 1
          printf("num_dot > inputVec.size and reduce to" + inputVec.size)
        }
    
        val outputVec = inputVec.toArray.combinations(num_dot).toArray.map(x => x.reduce(_ * _))
        Vectors.dense(outputVec)
      }
      val dotVecUDF = udf((inputVec:Vector) => dotVec(inputVec,num_dot))
      df.withColumn(outputCol, dotVecUDF(col(inputCol)))
    }


}