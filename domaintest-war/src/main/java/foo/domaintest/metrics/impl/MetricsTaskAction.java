/**
 * Copyright 2014 Google Inc. All rights reserved.
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

package foo.domaintest.metrics.impl;

import com.google.api.services.bigquery.Bigquery;
import com.google.api.services.bigquery.model.TableDataInsertAllRequest;
import com.google.api.services.bigquery.model.TableDataInsertAllResponse;
import com.google.api.services.bigquery.model.TableDataInsertAllResponse.InsertErrors;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import foo.domaintest.action.Action.PostAction;
import foo.domaintest.action.annotation.ForPath;
import foo.domaintest.action.annotation.NoMetrics;
import foo.domaintest.config.SystemProperty;
import foo.domaintest.metrics.impl.MetricsModule.Param;

import java.io.IOException;

import javax.inject.Inject;

/** Action for exporting metrics to BigQuery. */
@NoMetrics  // Don't cause an infinite export metrics loop.
@ForPath("/task/metrics")
public class MetricsTaskAction implements PostAction {

  private static final String TABLE_ID = "streaming";
  private static final String DATASET_ID = "metrics";

  @Inject @Param("insertid") String insertId;
  @Inject @Param("path") String path;
  @Inject @Param("method") String method;
  @Inject @Param("tld") String tld;
  @Inject @Param("starttime") String startTime;
  @Inject @Param("endtime") String endTime;
  @Inject @Param("activity") String activity;
  @Inject @Param("responsecode") int responseCode;
  @Inject @SystemProperty("projectid") String projectId;
  @Inject Bigquery bigquery;

  /** Exports metrics to BigQuery. */
  @Override
  public void run() {
    try {
      TableDataInsertAllResponse response = bigquery.tabledata()
          .insertAll(projectId, DATASET_ID, TABLE_ID, new TableDataInsertAllRequest()
              .setRows(ImmutableList.of(new TableDataInsertAllRequest.Rows()
                  .setInsertId(insertId)
                  .setJson(new ImmutableMap.Builder<String, Object>()
                      .put("path", path)
                      .put("method", method)
                      .put("tld", tld)
                      .put("start_time", startTime)
                      .put("end_time", endTime)
                      .put("response_code", responseCode)
                      .put("activity", activity)
                      .build())))).execute();
      if (response.getInsertErrors() != null && !response.getInsertErrors().isEmpty()) {
        throw new RuntimeException(Joiner.on('\n').join(FluentIterable
            .from(response.getInsertErrors())
            .transform(new Function<InsertErrors, String>() {
                @Override
                public String apply(InsertErrors error) {
                  try {
                    return error.toPrettyString();
                  } catch (IOException e) {
                    return error.toString();
                  }
                }})));
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
