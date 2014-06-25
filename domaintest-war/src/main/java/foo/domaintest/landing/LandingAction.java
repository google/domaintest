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

package foo.domaintest.landing;

import static com.google.common.io.Resources.getResource;
import static com.google.common.net.MediaType.HTML_UTF_8;
import static java.net.IDN.toUnicode;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.io.Resources;
import com.google.common.net.InternetDomainName;
import foo.domaintest.action.Action.GetAction;
import foo.domaintest.action.Response;
import foo.domaintest.action.annotation.ForPath;
import foo.domaintest.action.annotation.RequestData;
import foo.domaintest.config.SystemProperty;
import foo.domaintest.metrics.Metrics;

import java.io.IOException;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;

/** Landing page action. */
@ForPath("/")
public class LandingAction implements GetAction {

  private static final String TEMPLATE = loadTemplate();

  @Inject @SystemProperty("sourceurl") String sourceurl;
  @Inject @RequestData("domainName") InternetDomainName domainName;
  @Inject @RequestData("tld") String tld;
  @Inject Response response;
  @Inject Metrics metrics;

  private static String loadTemplate() {
    try {
      return Resources.toString(getResource(LandingAction.class, "template.html"), UTF_8);
    } catch (IOException e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  @Override
  public void run() {
    if (domainName.parts().size() == 3 && "src".equals(domainName.parts().get(0))) {
      response
          .setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY)
          .setPayload(sourceurl)
          .send();
      metrics.addActivity("src");
      return;
    }
    response
        .setMimeType(HTML_UTF_8.toString())
        .setPayload(TEMPLATE.replace("{{TLD}}", toUnicode(tld)))
        .send();
    metrics.addActivity("landing");
  }
}
