/*
 * Copyright 2020 Lenses.io
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

package io.lenses.streamreactor.connect.aws.s3.formats

import java.io.InputStream

import io.lenses.streamreactor.connect.aws.s3.model.{RemotePathLocation, StringSourceData}


class CsvFormatStreamReader(inputStreamFn: () => InputStream, bucketAndPath: RemotePathLocation, hasHeaders: Boolean)
  extends TextFormatStreamReader(inputStreamFn, bucketAndPath) {

  private var firstRun : Boolean = true;

  override def next(): StringSourceData = {
    if(firstRun) {
      if(hasHeaders) {
        if(sourceLines.hasNext) {
          sourceLines.next()
          lineNumber += 1
        } else {
          throw new IllegalStateException("No column headers are available")
        }
      }
      firstRun = false
    }
    super.next()
  }


}