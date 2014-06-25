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

import static com.google.common.base.Strings.nullToEmpty;
import static com.google.common.net.HttpHeaders.CONNECTION;
import static com.google.common.net.HttpHeaders.LOCATION;
import static com.google.common.net.MediaType.PLAIN_TEXT_UTF_8;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import foo.domaintest.action.HttpErrorException.BadRequestException;
import foo.domaintest.metrics.Metrics;
import foo.domaintest.util.Sleeper;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

/** A configurable HTTP response. */
public class Response {

  private static final int MIN_STATUS_CODE = 200;
  private static final int MAX_STATUS_CODE = 499;
  private static final int MAX_SLEEP_SECONDS = 10;

  private final Sleeper sleeper;
  private final HttpServletResponse servletResponse;
  private final Metrics metrics;

  private Integer status;
  private Integer sleepSeconds;
  private String mimeType;
  private List<String> cookiesToDelete;
  private Map<String, String> cookiesToAdd;
  private Map<String, String> headers;
  private String payload;

  @Inject
  public Response(Sleeper sleeper, HttpServletResponse servletResponse, Metrics metrics) {
    this.sleeper = sleeper;
    this.servletResponse = servletResponse;
    this.metrics = metrics;
  }

  public Response setStatus(Integer status) {
    if (status != null
        && (status < MIN_STATUS_CODE || status > MAX_STATUS_CODE)) {
      throw new BadRequestException("Invalid status code (must be >= 200 and < 500)");
    }
    this.status = status;
    return this;
  }

  public Response setSleepSeconds(Integer sleepSeconds) {
    if (sleepSeconds != null
        && (sleepSeconds < 0 || sleepSeconds > MAX_SLEEP_SECONDS)) {
      throw new BadRequestException("Invalid sleep seconds (must be >= 0 and <= 10)");
    }
    this.sleepSeconds = sleepSeconds;
    return this;
  }

  public Response setMimeType(String mimeType) {
    this.mimeType = mimeType;
    return this;
  }

  public Response setCookiesToDelete(List<String> cookiesToDelete) {
    this.cookiesToDelete = cookiesToDelete;
    return this;
  }

  public Response setCookiesToAdd(Map<String, String> cookiesToAdd) {
    this.cookiesToAdd = cookiesToAdd;
    return this;
  }

  public Response setHeaders(Map<String, String> headers) {
    this.headers = headers;
    return this;
  }

  public Response setPayload(String payload) {
    this.payload = payload;
    return this;
  }

  private void setCookie(String name, String value, int maxAge) {
    try {
      Cookie cookie = new Cookie(name, value);
      cookie.setMaxAge(maxAge);
      servletResponse.addCookie(cookie);
    } catch (IllegalArgumentException e) {
      throw new BadRequestException("Illegal cookie", e);
    }
  }

  public void send() {
    setDefaults();
    sleeper.sleep(sleepSeconds);
    servletResponse.setStatus(status);
    for (String cookieToDelete : cookiesToDelete) {
      setCookie(cookieToDelete, "", 0);
      metrics.addActivity("delete_cookie");
    }
    for (Entry<String, String> cookieToAdd : cookiesToAdd.entrySet()) {
      // A maxAge of -1 means session lifetime (goes away when the browser closes).
      setCookie(cookieToAdd.getKey(), cookieToAdd.getValue(), -1);
      metrics.addActivity("add_cookie");
    }
    for (Entry<String, String> header : headers.entrySet()) {
      // Not sure what use header splitting would be since we let you set headers explicitly, but
      // prevent it just in case.
      servletResponse.addHeader(header.getKey(), header.getValue().replaceAll("\n", " "));
      metrics.addActivity("set_header");
    }
    if (status >= 300 && status < 400) {
      // Handle a redirect, if requested.
      // The location should be in the payload field. If it's empty, redirect to the landing page.
      try {
        servletResponse.setHeader(LOCATION, payload.isEmpty()
            ? "/"
            : new URL(payload).toString());
      } catch (MalformedURLException e) {
        throw new BadRequestException("Invalid redirect url", e);
      }
      servletResponse.setHeader(CONNECTION, "close");
    } else {
      servletResponse.setContentType(mimeType);
      try {
        servletResponse.getWriter().write(payload);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    metrics.setResponseCode(status);
  }

  protected void setDefaults() {
    status = Optional.fromNullable(status).or(HttpServletResponse.SC_OK);
    sleepSeconds = Optional.fromNullable(sleepSeconds).or(0);
    mimeType = Optional.fromNullable(mimeType).or(PLAIN_TEXT_UTF_8.toString());
    cookiesToDelete = Optional.fromNullable(cookiesToDelete).or(ImmutableList.<String>of());
    cookiesToAdd = Optional.fromNullable(cookiesToAdd).or(ImmutableMap.<String, String>of());
    headers = Optional.fromNullable(headers).or(ImmutableMap.<String, String>of());
    payload = nullToEmpty(payload);
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof Response
        && Arrays.equals(getFieldsArray(), ((Response) other).getFieldsArray());
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(getFieldsArray());
  }

  private Object[] getFieldsArray() {
    return new Object[] {
        status, sleepSeconds, mimeType, cookiesToDelete, cookiesToAdd, headers, payload };
  }
}
