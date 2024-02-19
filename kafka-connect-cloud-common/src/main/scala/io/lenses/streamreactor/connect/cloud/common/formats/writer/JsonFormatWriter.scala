/*
 * Copyright 2017-2024 Lenses.io Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lenses.streamreactor.connect.cloud.common.formats.writer

import JsonFormatWriter._
import io.lenses.streamreactor.connect.cloud.common.formats.writer.LineSeparatorUtil.LineSeparatorBytes
import io.lenses.streamreactor.connect.cloud.common.model.CompressionCodec
import io.lenses.streamreactor.connect.cloud.common.model.CompressionCodecName
import io.lenses.streamreactor.connect.cloud.common.model.CompressionCodecName.GZIP
import io.lenses.streamreactor.connect.cloud.common.model.CompressionCodecName.UNCOMPRESSED
import io.lenses.streamreactor.connect.cloud.common.sink.SinkError
import io.lenses.streamreactor.connect.cloud.common.sink.conversion.ToJsonDataConverter
import io.lenses.streamreactor.connect.cloud.common.stream.CloudOutputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream
import org.apache.commons.compress.compressors.gzip.GzipParameters
import org.apache.kafka.connect.json.JsonConverter
import org.apache.kafka.connect.json.DecimalFormat
import org.apache.kafka.connect.json.JsonConverterConfig

import scala.jdk.CollectionConverters.MapHasAsJava
import scala.util.Try

class JsonFormatWriter(outputStream: CloudOutputStream)(implicit compressionCodec: CompressionCodec)
    extends FormatWriter {
  private var gzipOutputStream: GzipCompressorOutputStream = _

  private val jsonCompressionCodec: CompressionCodec = {
    compressionCodec match {
      case CompressionCodec(UNCOMPRESSED, _) => CompressionCodecName.UNCOMPRESSED.toCodec()
      case CompressionCodec(GZIP, _)         => CompressionCodecName.GZIP.withLevel(compressionCodec.level.getOrElse(6))
      case _                                 => throw new IllegalArgumentException("Invalid or missing `compressionCodec` specified.")
    }
  }

  override def write(messageDetail: MessageDetail): Either[Throwable, Unit] =
    Try {
      val dataBytes: Array[Byte] = ToJsonDataConverter.convertMessageValueToByteArray(
        Converter,
        messageDetail.topic,
        messageDetail.value,
      )

      if (jsonCompressionCodec.compressionCodec == CompressionCodecName.GZIP) {
        if (gzipOutputStream == null) {
          val parameters = new GzipParameters()

          // `get` is safe due to `getOrElse(6)` default in case assignment.
          parameters.setCompressionLevel(jsonCompressionCodec.level.get)

          gzipOutputStream = new GzipCompressorOutputStream(outputStream, parameters)
        }

        gzipOutputStream.write(dataBytes)
        gzipOutputStream.flush()
      } else {
        outputStream.write(dataBytes)
        outputStream.write(LineSeparatorBytes)
        outputStream.flush()
      }
    }.toEither

  override def rolloverFileOnSchemaChange(): Boolean = false

  override def complete(): Either[SinkError, Unit] =
    if (gzipOutputStream != null)
      for {
        _      <- Suppress(gzipOutputStream.flush())
        _      <- Suppress(gzipOutputStream.close())
        closed <- outputStream.complete()
      } yield closed
    else
      for {
        _      <- Suppress(outputStream.flush())
        _      <- Suppress(outputStream.close())
        closed <- outputStream.complete()
      } yield closed

  override def getPointer: Long = outputStream.getPointer

}

object JsonFormatWriter {

  private val Converter = new JsonConverter()

  Converter.configure(
    Map("schemas.enable" -> "false", JsonConverterConfig.DECIMAL_FORMAT_CONFIG -> DecimalFormat.NUMERIC.name()).asJava,
    false,
  )
}
