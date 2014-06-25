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

package foo.domaintest;

import static com.google.common.collect.Iterables.concat;
import static com.google.common.net.HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN;
import static com.google.common.net.HttpHeaders.ORIGIN;
import static com.google.common.net.HttpHeaders.X_XSS_PROTECTION;

import foo.domaintest.action.Action;
import foo.domaintest.action.Action.GetAction;
import foo.domaintest.action.Action.PostAction;
import foo.domaintest.action.ActionDelegator;
import foo.domaintest.action.ActionDelegator.RegisteredAction;
import foo.domaintest.action.GlobalModule;
import foo.domaintest.action.HttpErrorException;
import foo.domaintest.action.RequestModule;
import foo.domaintest.email.EmailApiModule;
import foo.domaintest.http.HttpApiModule;
import foo.domaintest.landing.LandingModule;
import foo.domaintest.metrics.Metrics;
import foo.domaintest.metrics.impl.MetricsModule;

import dagger.ObjectGraph;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Master servlet that loads and runs the correct action for the request. */
public class RootServlet extends HttpServlet {

  private static final Logger logger = Logger.getLogger(RootServlet.class.getName());

  @SuppressWarnings("unchecked")
  private static final ActionDelegator DELEGATOR = new ActionDelegator(concat(
      EmailApiModule.ACTIONS,
      HttpApiModule.ACTIONS,
      LandingModule.ACTIONS,
      MetricsModule.ACTIONS));

  /** An {@link ObjectGraph} with globally-scoped bindings. */
  private static final ObjectGraph GLOBAL_GRAPH = ObjectGraph.create(GlobalModule.class);

  /** Modules that supply request-scoped bindings and need a {@link RequestModule} in the graph. */
  private static final Object[] REQUEST_MODULES = new Object[] {
      EmailApiModule.class,
      HttpApiModule.class,
      LandingModule.class,
      MetricsModule.class};

  /** Execute a GET or POST request by injecting and running the appropriate {@link Action}. */
  @Override
  public void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
    // The poorly named method getRequestURI gives the path.
    RegisteredAction registeredAction = DELEGATOR.getRegisteredAction(request.getRequestURI());
    Action action = registeredAction.getAction();
    String origin = request.getHeader(ORIGIN);
    String method = request.getMethod();
    if (!(("GET".equals(request.getMethod()) && action instanceof GetAction)
        || ("POST".equals(request.getMethod()) && action instanceof PostAction)
        || ("OPTIONS".equals(method) && origin != null))) {
      response.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED, "Unsupported method.");
      return;
    }
    // Turn off reflected-XSS filtering in modern browsers. Without this, you couldn't execute a
    // script from a page served via /echo. This protection is useless to us because /stash allows
    // for a far more powerful stored-XSS (albeit short lived), and in any case we allow scripts.
    response.addHeader(X_XSS_PROTECTION, "0");
    // Enable Cross-Origin Resource Sharing. See http://www.w3.org/TR/cors/ for details.
    if (origin != null) {
      response.addHeader(ACCESS_CONTROL_ALLOW_ORIGIN, origin);
      if ("OPTIONS".equals(method)) {  // Only allow OPTIONS to support CORS preflight.
        return;
      }
    }
    ObjectGraph requestGraph = GLOBAL_GRAPH
        .plus(new RequestModule(request, response, registeredAction.getPath()))
        .plus(REQUEST_MODULES);
    Metrics metrics = requestGraph.get(Metrics.class);
    try {
      requestGraph.inject(action).run();
    } catch (HttpErrorException e) {
      metrics.setResponseCode(e.getResponseCode());
      metrics.addActivity("error");  // Mark this as user error.
      response.sendError(e.getResponseCode(), e.getMessage());
    } catch (Exception e) {
      logger.log(Level.SEVERE, e.getMessage(), e);
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Bad request");
      return;  // Return without exporting metrics.
    }
    if (registeredAction.isExportingMetrics()) {
      metrics.export();
    }
  }
}
