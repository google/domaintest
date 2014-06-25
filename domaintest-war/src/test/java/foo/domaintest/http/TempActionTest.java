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

import static foo.domaintest.util.Key.Type.STASH;
import static junit.framework.Assert.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import com.google.appengine.tools.development.testing.LocalMemcacheServiceTestConfig;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import foo.domaintest.action.HttpErrorException.NotFoundException;
import foo.domaintest.metrics.Metrics;
import foo.domaintest.util.Key;
import foo.domaintest.util.Memcache;
import foo.domaintest.util.testutil.FakeResponse;
import foo.domaintest.util.testutil.TestEnvironment;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Unit tests for {@link TempAction}. */
@RunWith(JUnit4.class)
public class TempActionTest {

  @Rule public TestEnvironment env = new TestEnvironment(new LocalMemcacheServiceTestConfig());

  Memcache memcache = new Memcache(null, mock(Metrics.class));
  TempAction action = new TempAction();

  public TempActionTest() {
    action.memcache = memcache;
    action.requestPath = "http://testing.example/temp/token";
  }

  @Test
  public void testTemp() {
    action.response = new FakeResponse();
    memcache.save(
        new Key(STASH, "token"),
        new ImmutableMap.Builder<String, Object>()
            .put("status", 234)
            .put("sleepSeconds", 5)
            .put("mimeType", "a/b")
            .put("cookiesToDelete", ImmutableList.of("x", "y"))
            .put("cookiesToAdd", ImmutableMap.of("j", "k", "m", ""))
            .put("headers", ImmutableMap.of("aa", "bb", "cc", ""))
            .put("payload", "foo")
            .build(),
        null);
    action.run();
    assertEquals(
        new FakeResponse()
            .setStatus(234)
            .setSleepSeconds(5)
            .setMimeType("a/b")
            .setCookiesToDelete(ImmutableList.of("x", "y"))
            .setCookiesToAdd(ImmutableMap.of("j", "k", "m", ""))
            .setHeaders(ImmutableMap.of("aa", "bb", "cc", ""))
            .setPayload("foo"),
        action.response);
    assertTrue(((FakeResponse) action.response).isResponseSent());
    try {
      action.run();
      fail("Should have thrown a NotFoundException");
    } catch (NotFoundException e) {
      // Expected.
    }
  }
}
