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

package foo.domaintest.action;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import foo.domaintest.action.HttpErrorException.BadRequestException;
import foo.domaintest.metrics.Metrics;
import foo.domaintest.util.Sleeper;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.PrintWriter;
import java.io.StringWriter;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

/** Unit tests for {@link Response}. */
@RunWith(MockitoJUnitRunner.class)
public class ResponseTest {

  @Rule public ExpectedException thrown = ExpectedException.none();

  StringWriter writer = new StringWriter();

  @Mock HttpServletResponse servletResponse;
  @Mock Sleeper sleeper;
  @Mock Metrics metrics;

  Response response;

  @Before
  public void before() throws Exception {
    response = new Response(sleeper, servletResponse, metrics);
    when(servletResponse.getWriter()).thenReturn(new PrintWriter(writer));
  }

  Cookie cookie(final String name, final String value, final int age) {
    return argThat(new ArgumentMatcher<Cookie>() {
      @Override
      public boolean matches(Object cookieObj) {
        if (cookieObj instanceof Cookie) {
          Cookie cookie = (Cookie) cookieObj;
          return name.equals(cookie.getName())
              && value.equals(cookie.getValue())
              && age == cookie.getMaxAge();
        }
        return false;
      }});
  }

  @Test
  public void testAllSet() {
    response
        .setStatus(234)
        .setSleepSeconds(6)
        .setMimeType("a/b")
        .setCookiesToDelete(ImmutableList.of("x", "y"))
        .setCookiesToAdd(ImmutableMap.of("j", "k", "m", ""))
        .setHeaders(ImmutableMap.of("aa", "bb", "cc", ""))
        .setPayload("foo")
        .send();
    verify(sleeper).sleep(6);
    verify(servletResponse).setStatus(234);
    verify(servletResponse).setContentType("a/b");
    verify(servletResponse).addCookie(cookie("x", "", 0));
    verify(servletResponse).addCookie(cookie("y", "", 0));
    verify(servletResponse).addCookie(cookie("j", "k", -1));
    verify(servletResponse).addCookie(cookie("m", "", -1));
    verify(servletResponse).addHeader("cc", "");
    verify(servletResponse).addHeader("aa", "bb");
    verify(servletResponse).addHeader("cc", "");
    assertEquals("foo", writer.toString());
  }

  @Test
  public void testDefaults() {
    response.send();
    verify(sleeper).sleep(0);
    verify(servletResponse).setStatus(200);
    verify(servletResponse).setContentType("text/plain; charset=utf-8");
    assertEquals("", writer.toString());
  }

  @Test
  public void testMinStatus() {
    thrown.expect(BadRequestException.class);
    response.setStatus(199).send();
  }

  @Test
  public void testMaxStatus() {
    thrown.expect(BadRequestException.class);
    response.setStatus(500).send();
  }

  @Test
  public void testMinSleep() {
    thrown.expect(BadRequestException.class);
    response.setSleepSeconds(-1).send();
  }

  @Test
  public void testMaxSleep() {
    thrown.expect(BadRequestException.class);
    response.setSleepSeconds(11).send();
  }

  @Test
  public void testRedirect() throws Exception {
    response.setStatus(301).setPayload("http://example.com").send();
    verify(servletResponse).setHeader("Location", "http://example.com");
    verify(servletResponse).setHeader("Connection", "close");
    assertEquals("", writer.toString());
  }

  @Test
  public void testRedirectDefault() {
    response.setStatus(301).send();
    verify(servletResponse).setHeader("Location", "/");
    verify(servletResponse).setHeader("Connection", "close");
    assertEquals("", writer.toString());
  }
}
