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

package foo.domaintest.metrics.impl;

import com.google.common.collect.ImmutableSet;
import foo.domaintest.action.Action;
import foo.domaintest.action.RequestModule;
import foo.domaintest.metrics.Metrics;

import dagger.Module;
import dagger.Provides;

import java.lang.annotation.Documented;
import java.util.Set;

import javax.inject.Qualifier;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;

/** Dagger module for metrics. */
@Module(
    addsTo = RequestModule.class,
    injects = { Metrics.class, MetricsTaskAction.class })
public class MetricsModule {

    /** Binding annotation for request parameters. */
  @Qualifier
  @Documented
  @interface Param {
    String value();
  }

  public static final Set<Class<? extends Action>> ACTIONS =
      ImmutableSet.<Class<? extends Action>>of(MetricsTaskAction.class);

  @Provides
  @Singleton
  public Metrics provideMetrics(MetricsImpl metricsImpl) {
    return metricsImpl;
  }

  @Provides
  @Param("insertid")
  String provideInsertId(HttpServletRequest request) {
    return request.getParameter("insertid");
  }

  @Provides
  @Param("path")
  String providePath(HttpServletRequest request) {
    return request.getParameter("path");
  }

  @Provides
  @Param("method")
  String provideMethod(HttpServletRequest request) {
    return request.getParameter("method");
  }

  @Provides
  @Param("tld")
  String provideTld(HttpServletRequest request) {
    return request.getParameter("tld");
  }

  @Provides
  @Param("starttime")
  String provideStartTime(HttpServletRequest request) {
    return request.getParameter("starttime");
  }

  @Provides
  @Param("endtime")
  String provideEndTime(HttpServletRequest request) {
    return request.getParameter("endtime");
  }

  @Provides
  @Param("activity")
  String provideActivity(HttpServletRequest request) {
    return request.getParameter("activity");
  }

  @Provides
  @Param("responsecode")
  int provideResponseCode(HttpServletRequest request) {
    return Integer.parseInt(request.getParameter("responsecode"));
  }
}
