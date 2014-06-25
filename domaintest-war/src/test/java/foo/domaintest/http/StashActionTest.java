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

import static com.google.common.base.Strings.repeat;
import static foo.domaintest.util.Key.Type.STASH;
import static foo.domaintest.util.Key.Type.TOKEN;
import static foo.domaintest.util.testutil.LazyFactory.lazy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import com.google.appengine.tools.development.testing.LocalMemcacheServiceTestConfig;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import foo.domaintest.action.HttpErrorException.BadRequestException;
import foo.domaintest.metrics.Metrics;
import foo.domaintest.util.Key;
import foo.domaintest.util.Memcache;
import foo.domaintest.util.TempUrlFactory;
import foo.domaintest.util.testutil.FakeResponse;
import foo.domaintest.util.testutil.TestEnvironment;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Unit tests for {@link StashAction}. */
@RunWith(JUnit4.class)
public class StashActionTest {

  private static final String TESTING_URL_BASE = "http://testing.example";

  StashAction action = new StashAction();
  Memcache memcache = new Memcache(null, mock(Metrics.class));

  @Rule public ExpectedException thrown = ExpectedException.none();
  @Rule public TestEnvironment env = new TestEnvironment(new LocalMemcacheServiceTestConfig());

  public StashActionTest() {
    action.memcache = memcache;
    action.lazyRandomToken = lazy("token");
    action.response = new FakeResponse();
    action.tempUrlFactory = new TempUrlFactory(TESTING_URL_BASE + "/stash");
  }

  void runServletVerifyCaching(Map<String, Object> expectedParams) throws Exception {
    action.run();
    assertEquals(
        new FakeResponse().setPayload(TESTING_URL_BASE + "/temp/token"),
        action.response);
    assertTrue(((FakeResponse) action.response).isResponseSent());
    assertEquals(expectedParams, memcache.load(new Key(STASH, "token")));
  }

  Map<?, ?> loadMap(String token) {
    return (Map<?, ?>) memcache.load(new Key(STASH, token));
  }

  @Test
  public void testAllSet() {
    action.status = 234;
    action.sleepSeconds = 5;
    action.mimeType = "a/b";
    action.cookiesToDelete = ImmutableList.of("x", "y");
    action.cookiesToAdd = ImmutableMap.of("j", "k", "m", "");
    action.headers = ImmutableMap.of("aa", "bb", "cc", "");
    action.payload = "foo";
    action.run();
    assertEquals(
        new FakeResponse().setPayload(TESTING_URL_BASE + "/temp/token"),
        action.response);
    assertTrue(((FakeResponse) action.response).isResponseSent());
    assertEquals(
        new ImmutableMap.Builder<String, Object>()
            .put("status", action.status)
            .put("sleepSeconds", action.sleepSeconds)
            .put("mimeType", action.mimeType)
            .put("cookiesToDelete", action.cookiesToDelete)
            .put("cookiesToAdd", action.cookiesToAdd)
            .put("headers", action.headers)
            .put("payload", action.payload)
            .build(),
        memcache.load(new Key(STASH, "token")));
  }

  @Test
  public void testNoneSet() {
    action.run();
    assertEquals(
        new FakeResponse().setPayload(TESTING_URL_BASE + "/temp/token"),
        action.response);
    assertTrue(((FakeResponse) action.response).isResponseSent());
    Map<String, Object> expected = new HashMap<>();
    expected.put("status", null);
    expected.put("sleepSeconds", null);
    expected.put("mimeType", null);
    expected.put("cookiesToDelete", null);
    expected.put("cookiesToAdd", null);
    expected.put("headers", null);
    expected.put("payload", null);
    assertEquals(expected, memcache.load(new Key(STASH, "token")));
  }

  @Test
  public void testTruncation() throws Exception {
    action.payload = repeat("A", 10241);
    action.run();
    assertEquals(repeat("A", 10240), loadMap("token").get("payload"));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testRepeatedValuesLimitedTo20() throws Exception {
    action.cookiesToDelete = new ArrayList<>();
    for (int i = 0; i < 21; i++) {
      action.cookiesToDelete.add("a");
    }
    action.run();
    assertEquals(20, ((List<String>) loadMap("token").get("cookiesToDelete")).size());
  }

  @Test
  public void testUserProvidedToken() throws Exception {
    memcache.save(new Key(TOKEN, "usertoken"), true, null);
    action.tokenParam = "usertoken";
    action.payload = "foo";
    action.run();
    assertEquals("foo", loadMap("usertoken").get("payload"));
  }

  @Test
  public void testUnknownUserProvidedToken() throws Exception {
    thrown.expect(BadRequestException.class);
    action.tokenParam = "usertoken";
    action.payload = "foo";
    action.run();
    assertEquals("foo", loadMap("usertoken").get("payload"));
  }
}
