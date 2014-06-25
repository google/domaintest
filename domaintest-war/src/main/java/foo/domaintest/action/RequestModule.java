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
import static com.google.common.collect.Iterables.getLast;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.fileupload.servlet.ServletFileUpload.isMultipartContent;

import com.google.common.base.Optional;
import com.google.common.io.CharStreams;
import com.google.common.net.InternetDomainName;
import foo.domaintest.action.annotation.RequestData;

import dagger.Module;
import dagger.Provides;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Dagger library module for request scoped values. */
@Module(
    addsTo = GlobalModule.class,
    injects = NotFoundAction.class,
    library = true)
public class RequestModule {

  private final HttpServletRequest request;
  private final HttpServletResponse response;
  private final String actionPath;
  private final long startTimeMillis;

  public RequestModule(
      HttpServletRequest request,
      HttpServletResponse response,
      String actionPath) {
    this.request = request;
    this.response = response;
    this.actionPath = actionPath;
    this.startTimeMillis = System.currentTimeMillis();
  }

  @Provides
  HttpServletRequest provideRequest() {
    return request;
  }

  @Provides
  HttpServletResponse provideResponse() {
    return response;
  }

  /** Provides the request start time in millis since the epoch. */
  @Provides
  @RequestData("startTime")
  long provideStartTime(HttpServletRequest request) {
    return startTimeMillis;
  }

  /** Provides the request method (GET or POST). */
  @Provides
  @RequestData("method")
  String provideMethod(HttpServletRequest request) {
    return request.getMethod();
  }

  @Provides
  @RequestData("actionPath")
  String provideActionPath() {
    return actionPath;
  }

  /** Provides the request URL up to but not including the query string. */
  @Provides
  @RequestData("url")
  String provideRequestUrl(HttpServletRequest request) {
    return request.getRequestURL().toString();
  }

  /** Provides the request path. */
  @Provides
  @RequestData("path")
  String provideRequestPath(HttpServletRequest request) {
    return request.getRequestURI();  // Poorly named, but this gives the path.
  }

  /** Provides the request charset. */
  @Provides
  @RequestData("charset")
  String provideRequestCharset(HttpServletRequest request) {
    // Guess UTF-8 if none specified.
    return Optional.fromNullable(request.getCharacterEncoding()).or(UTF_8.name());
  }

  /** Provides the query string of the request URL. */
  @Provides
  @RequestData("queryString")
  String provideQueryString(HttpServletRequest request) {
    return nullToEmpty(request.getQueryString());
  }

  /** Provides the full domain name from the request URL, including subdomains. */
  @Provides
  @RequestData("domainName")
  InternetDomainName provideDomainName(@RequestData("url") String requestUrl) {
    try {
      return InternetDomainName.from(new URI(requestUrl).getHost());
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  /** Provides the top level domain name from the request URL. */
  @Provides
  @RequestData("tld")
  String provideTld(@RequestData("domainName") InternetDomainName domainName) {
    return getLast(domainName.parts());
  }

  /** Provides a streaming {@link FileItemIterator} for parsing multipart/form-data requests. */
  @Provides
  FileItemIterator provideFileItemIterator(HttpServletRequest request) {
    try {
      return isMultipartContent(request) ? new ServletFileUpload().getItemIterator(request) : null;
    } catch (FileUploadException | IOException e) {
      return null;
    }
  }

  /** Provides the POST body. Note that this consumes the request's input stream. */
  @Provides
  @RequestData("postBody")
  String providePostBody(
      HttpServletRequest request, @RequestData("charset") String requestCharset) {
    try {
      return CharStreams.toString(request.getReader());
    } catch (IOException e) {
      return "";
    }
  }
}
