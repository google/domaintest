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

import static foo.domaintest.util.Key.Type.TOKEN;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import com.google.appengine.tools.development.testing.LocalMemcacheServiceTestConfig;
import foo.domaintest.metrics.Metrics;
import foo.domaintest.util.Key;
import foo.domaintest.util.Memcache;
import foo.domaintest.util.testutil.FakeResponse;
import foo.domaintest.util.testutil.TestEnvironment;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Unit tests for {@link EchoAction}. */
@RunWith(JUnit4.class)
public class TokenActionTest {

  @Rule public TestEnvironment env = new TestEnvironment(new LocalMemcacheServiceTestConfig());

  @Test
  public void testSavesAndReturnsToken() {
    TokenAction action = new TokenAction();
    action.memcache = new Memcache(null, mock(Metrics.class));
    action.response = new FakeResponse();
    action.randomToken = "token";
    action.run();
    assertEquals(
        new FakeResponse().setPayload("token"),
        action.response);
    assertTrue(((FakeResponse) action.response).isResponseSent());
    assertTrue((boolean) action.memcache.load(new Key(TOKEN, "token")));
  }
}
