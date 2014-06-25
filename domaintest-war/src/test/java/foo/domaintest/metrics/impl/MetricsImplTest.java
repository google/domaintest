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

import static com.google.appengine.tools.development.testing.LocalTaskQueueTestConfig.getLocalTaskQueue;
import static com.google.common.collect.Iterables.getOnlyElement;
import static foo.domaintest.util.QueryStringHelper.parseQuery;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.api.services.bigquery.Bigquery;
import com.google.appengine.api.taskqueue.dev.QueueStateInfo;
import com.google.appengine.tools.development.testing.LocalMemcacheServiceTestConfig;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import foo.domaintest.util.testutil.TestEnvironment;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/** Unit tests for {@link MetricsTaskAction}. */
@RunWith(MockitoJUnitRunner.class)
public class MetricsImplTest {

  @Rule public TestEnvironment env = new TestEnvironment(new LocalMemcacheServiceTestConfig());

  @Mock Bigquery bigquery;

  MetricsImpl metrics = new MetricsImpl();

  @Before
  public void setup() throws Exception {
    metrics.bigquery = bigquery;
    metrics.actionPath = "/path";
    metrics.tld = "tld";
    metrics.method = "GET";
    metrics.startTimeMillis = 0;
    metrics.addActivity("foo");
    metrics.addActivity("bar");
    metrics.setResponseCode(456);
    // This shouldn't be necessary, but Maven leaves the local queue in a bad state between tests.
    getLocalTaskQueue().flushQueue("default");
  }

  @Test
  public void testExport() throws Exception {
    metrics.projectId = "project id";
    metrics.export();
    QueueStateInfo queueInfo = getLocalTaskQueue().getQueueStateInfo().get("default");
    assertEquals(1, queueInfo.getCountTasks());
    assertEquals("/task/metrics", queueInfo.getTaskInfo().get(0).getUrl());
    Multimap<String, String> params = parseQuery(queueInfo.getTaskInfo().get(0).getBody());
    assertEquals("/path", getOnlyElement(params.get("path")));
    assertEquals("tld", getOnlyElement(params.get("tld")));
    assertEquals("GET", getOnlyElement(params.get("method")));
    assertEquals("0", getOnlyElement(params.get("starttime")));
    assertEquals(
        ImmutableSet.of("foo", "bar"),
        ImmutableSet.copyOf(Splitter.on(' ').split(getOnlyElement(params.get("activity")))));
    assertEquals("456", getOnlyElement(params.get("responsecode")));
    assertTrue(params.containsKey("endtime"));
    assertTrue(params.containsKey("insertid"));
  }

  @Test
  public void testExport_noProjectId() throws Exception {
    metrics.export();
    assertEquals(0, getLocalTaskQueue().getQueueStateInfo().get("default").getCountTasks());
  }
}
