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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import foo.domaintest.util.testutil.FakeResponse;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Unit tests for {@link EchoAction}. */
@RunWith(JUnit4.class)
public class EchoActionTest {

  EchoAction action  = new EchoAction();

  @Test
  public void testEcho() {
    action.response = new FakeResponse();
    action.status = 234;
    action.sleepSeconds = 5;
    action.mimeType = "a/b";
    action.cookiesToDelete = ImmutableList.of("x", "y");
    action.cookiesToAdd = ImmutableMap.of("j", "k", "m", "");
    action.headers = ImmutableMap.of("aa", "bb", "cc", "");
    action.payload = "foo";
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
  }
}
