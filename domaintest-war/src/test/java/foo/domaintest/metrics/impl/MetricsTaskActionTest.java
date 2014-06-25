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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.api.services.bigquery.Bigquery;
import com.google.api.services.bigquery.Bigquery.Tabledata;
import com.google.api.services.bigquery.Bigquery.Tabledata.InsertAll;
import com.google.api.services.bigquery.model.TableDataInsertAllRequest;
import com.google.api.services.bigquery.model.TableDataInsertAllResponse;
import com.google.api.services.bigquery.model.TableDataInsertAllResponse.InsertErrors;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;

/** Unit tests for {@link MetricsTaskAction}. */
@RunWith(MockitoJUnitRunner.class)
public class MetricsTaskActionTest {

  @Rule public ExpectedException thrown = ExpectedException.none();

  @Mock Bigquery bigquery;
  @Mock Tabledata tabledata;
  @Mock InsertAll insertAll;

  MetricsTaskAction action  = new MetricsTaskAction();
  TableDataInsertAllResponse response = new TableDataInsertAllResponse();

  @Before
  public void setup() throws Exception {
    action.insertId = "insert id";
    action.path = "/path";
    action.method = "GET";
    action.tld = "tld";
    action.startTime = "0";
    action.endTime = "1";
    action.activity = "foo bar";
    action.responseCode = 200;
    action.projectId = "project id";
    action.bigquery = bigquery;
    when(bigquery.tabledata()).thenReturn(tabledata);
    when(tabledata.insertAll(
        "project id",
        "metrics",
        "streaming",
        new TableDataInsertAllRequest()
            .setRows(ImmutableList.of(new TableDataInsertAllRequest.Rows()
            .setInsertId("insert id")
            .setJson(new ImmutableMap.Builder<String, Object>()
                .put("path", "/path")
                .put("method", "GET")
                .put("tld", "tld")
                .put("start_time", "0")
                .put("end_time", "1")
                .put("response_code", 200)
                .put("activity", "foo bar")
                .build()))))).thenReturn(insertAll);
  }

  @Test
  public void testSuccess_nullErrors() throws Exception {
    when(insertAll.execute()).thenReturn(response);
    response.setInsertErrors(null);
    action.run();
    verify(insertAll).execute();
  }

  @Test
  public void testSuccess_emptyErrors() throws Exception {
    when(insertAll.execute()).thenReturn(response);
    response.setInsertErrors(ImmutableList.<InsertErrors>of());
    action.run();
    verify(insertAll).execute();
  }

  @Test
  public void testFailure_exception() throws Exception {
    thrown.expect(RuntimeException.class);
    when(insertAll.execute()).thenThrow(new IOException());
    action.run();
  }

  @Test
  public void testFailure_errors() throws Exception {
    thrown.expect(RuntimeException.class);
    when(insertAll.execute()).thenReturn(response);
    response.setInsertErrors(ImmutableList.of(new InsertErrors()));
    action.run();
  }
}
