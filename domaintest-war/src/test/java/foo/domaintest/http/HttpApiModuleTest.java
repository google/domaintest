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
import static java.nio.charset.StandardCharsets.UTF_16;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ForwardingIterator;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import foo.domaintest.util.testutil.LazyFactory;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Iterator;

/** Unit tests for {@link HttpApiModule}. */
@RunWith(JUnit4.class)
public class HttpApiModuleTest {

  HttpApiModule module = new HttpApiModule();
  Multimap<String, String> params = LinkedListMultimap.create();

  /** A fake {@link FileItemIterator} that delegates to a regular iterator. */
  static class FakeFileItemIterator
      extends ForwardingIterator<FileItemStream> implements FileItemIterator {

    final Iterator<FileItemStream> delegate;

    FakeFileItemIterator(FileItemStream... items) {
      this.delegate = Arrays.asList(items).iterator();
    }

    @Override
    protected Iterator<FileItemStream> delegate() {
      return delegate;
    }}

  FileItemStream createItem(String name, String value, boolean isFormField) throws Exception {
    FileItemStream item = mock(FileItemStream.class);
    when(item.isFormField()).thenReturn(isFormField);
    when(isFormField ? item.getFieldName() : item.getName()).thenReturn(name);
    // Use UTF_16 and specify it to provideParameterMap to make sure we honor the request encoding.
    when(item.openStream()).thenReturn(new ByteArrayInputStream(value.getBytes(UTF_16)));
    return item;
  }

  @Test
  public void testProvideStatus() {
    assertNull(module.provideStatus(params, null));
    params.put("status", "foo");
    assertNull(module.provideStatus(params, null));
    params.removeAll("status");
    params.put("status", "123");
    assertEquals(123, (int) module.provideStatus(params, null));
    assertEquals(302, (int) module.provideStatus(params, "foo"));
  }

  @Test
  public void testProvideSleep() {
    assertNull(module.provideSleep(params));
    params.put("sleep", "foo");
    assertNull(module.provideSleep(params));
    params.removeAll("sleep");
    params.put("sleep", "123");
    assertEquals(123, (int) module.provideSleep(params));
  }

  @Test
  public void testProvideMime() {
    assertNull(module.provideMime(params));
    params.put("mime", "mime");
    assertEquals("mime", module.provideMime(params));
  }

  @Test
  public void testProvidePayload() {
    assertNull(module.providePayload(params, null));
    params.put("payload", "payload");
    assertEquals("payload", module.providePayload(params, null));
    assertEquals("foo", module.providePayload(params, "foo"));
  }

  @Test
  public void testProvideDelCookie() {
    assertEquals(ImmutableList.of(), module.provideDelCookie(params));
    params.putAll("delcookie", ImmutableList.of("a", "b"));
    assertEquals(ImmutableList.of("a", "b"), module.provideDelCookie(params));
  }

  @Test
  public void testProvideAddCookie() {
    assertEquals(ImmutableMap.of(), module.provideAddCookie(params));
    params.putAll("addcookie", ImmutableList.of("a=b=c", "c"));
    assertEquals(ImmutableMap.of("a", "b=c", "c", ""), module.provideAddCookie(params));
  }

  @Test
  public void testProvideHeader() {
    assertEquals(ImmutableMap.of(), module.provideHeader(params));
    params.putAll("header", ImmutableList.of("a=b=c", "c"));
    assertEquals(ImmutableMap.of("a", "b=c", "c", ""), module.provideHeader(params));
  }

  @Test
  public void testProvideEasterEggUrl() {
    ImmutableTable<String, String, String> eggs = ImmutableTable.of("easter", "egg", "redirect");
    assertNull(module.provideEasterEggUrl(params, eggs));
    params.put("easter", "egg");
    assertEquals("redirect", module.provideEasterEggUrl(params, eggs));
  }

  @Test
  public void testProvideParameterMap_queryOnly() {
    params.put("status", "123");
    params.put("sleep", "456");
    params.put("mime", "a/b");
    params.put("payload", "foo");
    params.putAll("delcookie", ImmutableList.of("a", "b"));
    params.putAll("addcookie", ImmutableList.of("a=b", "c"));
    params.putAll("header", ImmutableList.of("a=b", "c"));
    assertEquals(
        params,
        module.provideParameterMap(
            "status=123&"
                + "sleep=456&"
                + "mime=a/b&"
                + "payload=foo&"
                + "delcookie=a&delcookie=b&"
                + "addcookie=a=b&addcookie=c&"
                + "header=a=b&header=c",
            lazy(""),
            null,
            null));
  }

  @Test
  public void testProvideParameterMap_queryAndFormUrlEncoded() {
    // Ordering matters, and we expect the query parameters to come before the body ones.
    params.put("status", "123");
    params.put("sleep", "456");
    params.put("mime", "a/b");
    params.put("payload", "foo");
    params.put("delcookie", "a");
    params.put("addcookie", "a=b");
    params.put("header", "a=b");
    params.put("delcookie", "b");
    params.put("addcookie", "c");
    params.put("header", "c");
    params.put("payload", "ignorable_second_payload");
    assertEquals(
        params,
        module.provideParameterMap(
            "status=123&"
                + "sleep=456&"
                + "mime=a/b&"
                + "payload=foo&"
                + "delcookie=a&"
                + "addcookie=a=b&"
                + "header=a=b",
            lazy("delcookie=b&addcookie=c&header=c&payload=ignorable_second_payload"),
            null,
            null));
  }

  @Test
  public void testProvideParameterMap_queryAndMultipartFormData() throws Exception {
    // Ordering matters, and we expect the query parameters to come before the body ones.
    params.put("sleep", "456");
    params.put("mime", "a/b");
    params.put("status", "123");
    params.put("payload", "foo");
    assertEquals(
        params,
        module.provideParameterMap(
            "sleep=456&mime=a/b",
            LazyFactory.<String>throwingLazy(),  // Reading the body directly would be an error.
            "UTF-16",
            new FakeFileItemIterator(
                createItem("status", "123", false),
                createItem("payload", "foo", true))));
  }

  @Test
  public void testProvideParameterMap_postPayload() throws Exception {
    // The post body should not be interpreted, and should be stuffed into the "payload" param.
    params.put("status", "123");
    params.put("postpayload", "");
    params.put("payload", "foo=bar&baz=qux");
    assertEquals(
        params,
        module.provideParameterMap(
            "status=123&postpayload",
            lazy("foo=bar&baz=qux"),
            null,
            new FakeFileItemIterator(
                createItem("oops", "this shouldn't have been processed", false))));
  }

  @Test
  public void testProvideParameterMap_payloadAndPostPayload() throws Exception {
    // The post body should not be interpreted and should be ordered after the literal "payload".
    params.put("status", "123");
    params.put("postpayload", "");
    params.putAll("payload", ImmutableList.of("foo", "foo=bar&baz=qux"));
    assertEquals(
        params,
        module.provideParameterMap(
            "status=123&postpayload&payload=foo",
            lazy("foo=bar&baz=qux"),
            null,
            new FakeFileItemIterator(
                createItem("oops", "this shouldn't have been processed", false))));
  }
}
