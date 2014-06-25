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

import static com.google.appengine.api.urlfetch.URLFetchServiceFactory.getURLFetchService;
import static com.google.common.base.Predicates.equalTo;
import static com.google.common.base.Predicates.not;
import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.base.Strings.nullToEmpty;
import static com.google.common.collect.Maps.filterValues;
import static com.google.common.net.HttpHeaders.CONTENT_TYPE;
import static foo.domaintest.util.QueryStringHelper.encodeQuery;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.json.simple.JSONObject.toJSONString;

import com.google.appengine.api.urlfetch.HTTPHeader;
import com.google.appengine.api.urlfetch.HTTPMethod;
import com.google.appengine.api.urlfetch.HTTPRequest;
import com.google.appengine.api.urlfetch.HTTPResponse;
import com.google.common.collect.ImmutableMap;
import foo.domaintest.config.SystemProperty;
import foo.domaintest.metrics.Metrics;

import java.io.IOException;
import java.net.URL;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;

/** Simple client for the SendGrid HTTP API. */
public class Emailer {

  private static final Logger logger = Logger.getLogger(Emailer.class.getName());
  private static final String FORM_UTF8_MIME = "application/x-www-form-urlencoded; charset=utf-8";
  private static final String SENDGRID_SEND_URL = "https://api.sendgrid.com/api/mail.send.json";

  @Inject @SystemProperty("sendgriduser") String apiUser;
  @Inject @SystemProperty("sendgridkey") String apiKey;
  @Inject Metrics metrics;

  public void send(String from, String to, String body, String inReplyTo, String references) {
    try {
      HTTPRequest request = new HTTPRequest(new URL(SENDGRID_SEND_URL), HTTPMethod.POST);
      request.addHeader(new HTTPHeader(CONTENT_TYPE, FORM_UTF8_MIME));
      request.setPayload(encodeQuery(new ImmutableMap.Builder<String, String>()
          .put("api_user", apiUser)
          .put("api_key", apiKey)
          .put("to", to)
          .put("from", from)
          .put("subject", "Automated testing service response")
          .put("text", isNullOrEmpty(body) ? " " : body)  // SendGrid rejects an empty body.
          .put("headers", toJSONString(filterValues(
              ImmutableMap.of(
                  "In-Reply-To", nullToEmpty(inReplyTo),
                  "References", nullToEmpty(references)),
              not(equalTo("")))))
          .build()).getBytes(UTF_8));
      HTTPResponse response = getURLFetchService().fetch(request);
      if (response.getResponseCode() != HttpServletResponse.SC_OK) {
        logger.warning(String.format(
            "%d: %s",
            response.getResponseCode(),
            new String(response.getContent(), UTF_8)));
      }
      metrics.addActivity("autoreply");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
