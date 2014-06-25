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

package foo.domaintest.http;

import static foo.domaintest.util.testutil.LazyFactory.lazy;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.appengine.tools.development.testing.LocalMemcacheServiceTestConfig;
import foo.domaintest.action.Response;
import foo.domaintest.metrics.Metrics;
import foo.domaintest.util.Memcache;
import foo.domaintest.util.Sleeper;
import foo.domaintest.util.TempUrlFactory;
import foo.domaintest.util.testutil.TestEnvironment;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.PrintWriter;
import java.io.StringWriter;

import javax.servlet.http.HttpServletResponse;

/** Unit tests for the integration between /stash and /temp. */
@RunWith(MockitoJUnitRunner.class)
public class StashToTempTest {

  @Rule public TestEnvironment env = new TestEnvironment(new LocalMemcacheServiceTestConfig());

  @Mock Sleeper sleeper;
  @Mock HttpServletResponse servletResponse;
  @Mock Metrics metrics;

  @Test
  public void testIntegration() throws Exception {
    StashAction stash = new StashAction();
    stash.response = new Response(sleeper, servletResponse, metrics);
    stash.memcache = new Memcache(null, metrics);
    StringWriter stashWriter = new StringWriter();
    when(servletResponse.getWriter()).thenReturn(new PrintWriter(stashWriter));
    stash.tempUrlFactory = new TempUrlFactory("http://testing.example/stash");
    stash.lazyRandomToken = lazy("token");
    stash.status = 234;
    stash.payload = "foo";
    stash.run();

    TempAction temp = new TempAction();
    temp.response = new Response(sleeper, servletResponse, metrics);
    temp.memcache = new Memcache(null, metrics);
    StringWriter tempWriter = new StringWriter();
    when(servletResponse.getWriter()).thenReturn(new PrintWriter(tempWriter));
    temp.requestPath = stashWriter.toString().replaceFirst(".*/", "");
    temp.run();
    verify(servletResponse).setStatus(234);
    assertEquals("foo", tempWriter.toString());
  }
}
