package vectorpipe.sources

import java.net.URI
import java.util

import org.apache.spark.sql.catalyst.InternalRow
import org.apache.spark.sql.sources.v2.DataSourceOptions
import org.apache.spark.sql.sources.v2.reader.{InputPartition, InputPartitionReader}
import vectorpipe.model.AugmentedDiff

import scala.collection.JavaConverters._
import scala.compat.java8.OptionConverters._

case class AugmentedDiffStreamBatchTask(baseURI: URI, sequences: Seq[Int])
    extends InputPartition[InternalRow] {
  override def createPartitionReader(): InputPartitionReader[InternalRow] =
    AugmentedDiffStreamBatchReader(baseURI, sequences)
}

case class AugmentedDiffStreamBatchReader(baseURI: URI, sequences: Seq[Int])
    extends ReplicationStreamBatchReader[AugmentedDiff](baseURI, sequences) {

  override def getSequence(baseURI: URI, sequence: Int): Seq[AugmentedDiff] =
    AugmentedDiffSource.getSequence(baseURI, sequence)
}

case class AugmentedDiffMicroBatchReader(options: DataSourceOptions, checkpointLocation: String)
    extends ReplicationStreamMicroBatchReader[AugmentedDiff](options, checkpointLocation) {

  override def getCurrentSequence: Option[Int] =
    AugmentedDiffSource.getCurrentSequence(baseURI)

  private def baseURI: URI =
    options
      .get(Source.BaseURI)
      .asScala
      .map(new URI(_))
      .getOrElse(
        throw new RuntimeException(
          s"${Source.BaseURI} is a required option for ${Source.AugmentedDiffs}"
        )
      )

  override def planInputPartitions(): util.List[InputPartition[InternalRow]] =
    sequenceRange
      .map(seq =>
        AugmentedDiffStreamBatchTask(baseURI, Seq(seq)).asInstanceOf[InputPartition[InternalRow]])
      .asJava
}
