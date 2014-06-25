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

import static com.google.common.base.Strings.nullToEmpty;
import static com.google.common.collect.Iterables.getFirst;
import static foo.domaintest.util.QueryStringHelper.parseQuery;
import static java.util.UUID.randomUUID;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;
import com.google.common.io.CharStreams;
import foo.domaintest.action.Action;
import foo.domaintest.action.RequestModule;
import foo.domaintest.action.annotation.RequestData;
import foo.domaintest.config.ConfigModule.EasterEggs;
import foo.domaintest.metrics.Metrics;
import foo.domaintest.metrics.impl.MetricsModule;

import dagger.Lazy;
import dagger.Module;
import dagger.Provides;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Documented;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.inject.Qualifier;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletResponse;

/** Dagger module for creating the HTTP API actions. */
@Module(
    addsTo = RequestModule.class,
    includes = MetricsModule.class,
    injects = {
        EchoAction.class,
        StashAction.class,
        TempAction.class,
        TokenAction.class })
public class HttpApiModule {

  /** Binding annotation for a random token for memcache. */
  @Qualifier
  @Documented
  @interface RandomToken {}

  /** Binding annotation for request parameters. */
  @Qualifier
  @Documented
  @interface Param {
    String value();
  }

  public static final Set<Class<? extends Action>> ACTIONS =
      ImmutableSet.<Class<? extends Action>>of(
          EchoAction.class,
          StashAction.class,
          TempAction.class,
          TokenAction.class);

  @Provides
  @Singleton
  Multimap<String, String> provideParameterMap(
      @RequestData("queryString") String queryString,
      @RequestData("postBody") Lazy<String> lazyPostBody,
      @RequestData("charset") String requestCharset,
      FileItemIterator multipartIterator) {
    // Calling request.getParameter() or request.getParameterMap() etc. consumes the POST body. If
    // we got the "postpayload" param we don't want to parse the body, so use only the query params.
    // Note that specifying both "payload" and "postpayload" will result in the "payload" param
    // being honored and the POST body being completely ignored.
    ImmutableMultimap.Builder<String, String> params = new ImmutableMultimap.Builder<>();
    Multimap<String, String> getParams = parseQuery(queryString);
    params.putAll(getParams);
    if (getParams.containsKey("postpayload")) {
      // Treat the POST body as if it was the "payload" param.
      return params.put("payload", nullToEmpty(lazyPostBody.get())).build();
    }
    // No "postpayload" so it's safe to consume the POST body and look for params there.
    if (multipartIterator == null) {  // Handle GETs and form-urlencoded POST requests.
      params.putAll(parseQuery(nullToEmpty(lazyPostBody.get())));
    } else {  // Handle multipart/form-data requests.
      try {
        while (multipartIterator != null && multipartIterator.hasNext()) {
          FileItemStream item = multipartIterator.next();
          try (InputStream stream = item.openStream()) {
            params.put(
                item.isFormField() ? item.getFieldName() : item.getName(),
                CharStreams.toString(new InputStreamReader(stream, requestCharset)));
          }
        }
      } catch (FileUploadException | IOException e) {
        // Ignore the failure and fall through to return whatever params we managed to parse.
      }
    }
    return params.build();
  }

  @Provides
  @Singleton
  @EasterEggs
  String provideEasterEggUrl(
      Multimap<String, String> params, @EasterEggs Table<String, String, String> easterEggs) {
    for (Entry<String, String> param : params.entries()) {
      String easterEggUrl = easterEggs.get(param.getKey(), param.getValue());
      if (easterEggUrl != null) {
        return easterEggUrl;
      }
    }
    return null;
  }

  @Provides
  @Param("status")
  Integer provideStatus(Multimap<String, String> params, @EasterEggs String easterEggUrl) {
    if (easterEggUrl == null) {
      String statusString = getFirst(params.get("status"), null);
      return statusString == null ? null : parseInt(statusString);
    }
    return HttpServletResponse.SC_FOUND;
  }

  @Provides
  @Param("sleep")
  Integer provideSleep(Multimap<String, String> params) {
    String sleepString = getFirst(params.get("sleep"), null);
    return sleepString == null ? null : parseInt(sleepString);
  }

  @Provides
  @Param("mime")
  String provideMime(Multimap<String, String> params) {
    return getFirst(params.get("mime"), null);
  }

  @Provides
  @Param("payload")
  String providePayload(Multimap<String, String> params, @EasterEggs String easterEggUrl) {
    return easterEggUrl == null ? getFirst(params.get("payload"), null) : easterEggUrl;
  }

  @Provides
  @Param("token")
  String provideToken(Multimap<String, String> params) {
    return getFirst(params.get("token"), null);
  }

  @Provides
  @Param("delcookie")
  List<String> provideDelCookie(Multimap<String, String> params) {
    return ImmutableList.copyOf(params.get("delcookie"));
  }

  @Provides
  @Param("addcookie")
  Map<String, String> provideAddCookie(Multimap<String, String> params) {
    return parseMap(params.get("addcookie"));
  }

  @Provides
  @Param("header")
  Map<String, String> provideHeader(Multimap<String, String> params) {
    return parseMap(params.get("header"));
  }

  @Provides
  @RandomToken
  String provideRandomToken(Metrics metrics) {
    metrics.addActivity("random_token");
    return randomUUID().toString();
  }

  private Integer parseInt(String value) {
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private Map<String, String> parseMap(Collection<String> values) {
    ImmutableMap.Builder<String, String> builder = new ImmutableMap.Builder<>();
    for (String value : values) {
      List<String> parts = Splitter.on('=').limit(2).splitToList(value);
      // Note that this puts "" in the map if there's no '=' in the value.
      builder.put(parts.get(0), Iterables.get(parts, 1, ""));
    }
    return builder.build();
  }
}
