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

import static com.google.common.io.Resources.getResource;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.mail.internet.InternetHeaders;

/** Unit tests for {@link EmailApiModule}. */
@RunWith(MockitoJUnitRunner.class)
public class EmailApiModuleTest {

  @Mock FileItemIterator fileItemIterator;
  @Mock FileItemStream fileItemStream;

  EmailApiModule module = new EmailApiModule();

  @Before
  public void before() throws Exception {
    when(fileItemIterator.hasNext()).thenReturn(true);
    when(fileItemIterator.next()).thenReturn(fileItemStream);
    when(fileItemStream.getFieldName()).thenReturn("headers");
    when(fileItemStream.openStream()).thenReturn(
        getResource(EmailApiModuleTest.class, "testdata/headers.txt").openStream());
  }

  private InternetHeaders loadHeaders() {
    return module.provideHeaders(fileItemIterator);
  }

  @Test
  public void testProvideHeaders() {
    InternetHeaders headers = loadHeaders();
    assertEquals("recipient@testing.example", headers.getHeader("to", null));
    assertEquals("Developer Person <developer@example.com>", headers.getHeader("from", null));
  }

  @Test
  public void testProvideTo() {
    assertEquals("recipient@testing.example", module.provideTo(loadHeaders()));
  }

  @Test
  public void testProvideFrom() {
    assertEquals("developer@example.com", module.provideFrom(loadHeaders()));
  }

  @Test
  public void testProvideReplyTo() {
    assertEquals("alternative@example.com", module.provideReplyTo(loadHeaders()));
  }

  @Test
  public void testProvideSubject() {
    assertEquals("test this thing!", module.provideSubject(loadHeaders()));
  }

  @Test
  public void testProvideReferences() {
    assertEquals("<references-id>", module.provideReferences(loadHeaders()));
  }

  @Test
  public void testProvideMessageId() {
    assertEquals("<message-id>", module.provideMessageId(loadHeaders()));
  }

  @Test
  public void testProvideInReplyTo() {
    assertEquals("<in-reply-to-id>", module.provideInReplyTo(loadHeaders()));
  }
}
