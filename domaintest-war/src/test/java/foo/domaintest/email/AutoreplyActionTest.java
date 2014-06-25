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

package foo.domaintest.email;

import static foo.domaintest.util.Key.Type.STASH;
import static foo.domaintest.util.Key.Type.TOKEN;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import com.google.appengine.tools.development.testing.LocalMemcacheServiceTestConfig;
import foo.domaintest.action.HttpErrorException.BadRequestException;
import foo.domaintest.metrics.Metrics;
import foo.domaintest.util.Key;
import foo.domaintest.util.Memcache;
import foo.domaintest.util.TempUrlFactory;
import foo.domaintest.util.testutil.TestEnvironment;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Map;

/** Unit tests for {@link AutoreplyAction}. */
@RunWith(MockitoJUnitRunner.class)
public class AutoreplyActionTest {

  @Rule public TestEnvironment env = new TestEnvironment(new LocalMemcacheServiceTestConfig());
  @Rule public ExpectedException thrown = ExpectedException.none();

  @Mock Emailer emailer;

  AutoreplyAction action = new AutoreplyAction();
  Memcache memcache = new Memcache(null, mock(Metrics.class));

  @Before
  public void before() {
    action.emailer = emailer;
    action.memcache = memcache;
    action.tempUrlFactory = new TempUrlFactory("http://testing.example/autoreply");
    action.rawHeaders = "raw headers";
    action.from = "developer@example.com";
    action.to = "foobar@testing.example";
    action.subject = "test";
    action.presentedApiKey = "apikey";
    action.requiredApiKey = "apikey";
  }

  @Test
  public void testSuccess() {
    action.run();
    verify(emailer).send("tester@testing.example", "developer@example.com", null, null, null);
  }

  @Test
  public void testPrefersReplyTo() {
    action.replyTo = "replyto@example.com";
    action.run();
    verify(emailer).send("tester@testing.example", "replyto@example.com", null, null, null);
  }

  @Test
  public void testSubjectMustStartWithTest() {
    action.subject = "a test";
    action.run();
    action.subject = "tes";
    action.run();
    action.subject = null;
    action.run();
    verifyZeroInteractions(emailer);
  }

  @Test
  public void testInReplyTo() {
    action.messageId = "message-id";
    action.run();
    // If there's no in-reply-to or references headers, the message-id will be used as both the new
    // in-reply-to and the new references.
    verify(emailer).send(
        "tester@testing.example",
        "developer@example.com",
        null,
        "message-id",
        "message-id");
  }

  @Test
  public void testReferences_noMessageId() {
    action.references = "reference-id";
    action.run();
    verify(emailer).send(
        "tester@testing.example",
        "developer@example.com",
        null,
        null,
        "reference-id");
  }

  @Test
  public void testReferences_hasMessageId() {
    action.messageId = "message-id";
    action.references = "reference-id";
    action.run();
    verify(emailer).send(
        "tester@testing.example",
        "developer@example.com",
        null,
        "message-id",
        "reference-id message-id");
  }

  @Test
  public void testReferences_inReplyToSubstituted() {
    action.messageId = "message-id";
    action.inReplyTo = "inreplyto-id";
    action.run();
    verify(emailer).send(
        "tester@testing.example",
        "developer@example.com",
        null,
        "message-id",
        "inreplyto-id message-id");
  }

  @Test
  public void testReferences_inReplyToCantBeSubstituted() {
    action.inReplyTo = "inreplyto-id1 inreplyto-id2";
    action.run();
    verify(emailer).send("tester@testing.example", "developer@example.com", null, null, null);
  }

  @Test
  public void testBadApiKey() {
    thrown.expect(BadRequestException.class);
    action.presentedApiKey = "foobar";
    action.run();
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testStash() {
    action.subject = "test stashtoken";
    memcache.save(new Key(TOKEN, "stashtoken"), true, null);
    action.run();
    verify(emailer).send(
        "tester@testing.example",
        "developer@example.com",
        "http://testing.example/temp/stashtoken",
        null,
        null);
    assertEquals(
        "raw headers",
        ((Map<String, ?>) memcache.load(new Key(STASH, "stashtoken"))).get("payload"));
  }
}
