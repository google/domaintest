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

import static com.google.appengine.api.taskqueue.QueueFactory.getDefaultQueue;
import static com.google.appengine.api.taskqueue.TaskOptions.Builder.withUrl;
import static com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import com.google.api.services.bigquery.Bigquery;
import com.google.appengine.api.taskqueue.TransientFailureException;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import foo.domaintest.action.annotation.RequestData;
import foo.domaintest.config.SystemProperty;
import foo.domaintest.metrics.Metrics;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;

/** A request-scoped collector of metric information. */
class MetricsImpl implements Metrics {

  private static final Logger logger = Logger.getLogger(MetricsImpl.class.getName());

  @Inject @RequestData("actionPath") String actionPath;
  @Inject @RequestData("startTime") long startTimeMillis;
  @Inject @RequestData("tld") String tld;
  @Inject @RequestData("method") String method;
  @Inject @SystemProperty("projectid") String projectId;
  @Inject Bigquery bigquery;

  private final Set<String> activities = new HashSet<>();

  private int responseCode = 0;

  @Override
  public void addActivity(String activity) {
    activities.add(activity);
  }

  @Override
  public void setResponseCode(int responseCode) {
    this.responseCode = responseCode;
  }

  @Override
  public void export() {
    if (projectId == null) {
      logger.info("Exporting metrics is disabled because no project id was specified");
      return;
    }
    // Export the metrics on a task queue so that we don't block the http response on BigQuery.
    // If this fails, retry twice, after 2 seconds and then 4 seconds, and then give up.
    for (int delaySeconds : ImmutableList.of(0, 2, 4)) {
      sleepUninterruptibly(delaySeconds, SECONDS);
      try {
        getDefaultQueue().add(withUrl("/task/metrics")
            .param("insertid", UUID.randomUUID().toString())
            .param("path", actionPath.endsWith("/*")
                ? actionPath.substring(0, actionPath.length() - 2)
                : actionPath)
            .param("method", method)
            .param("tld", tld)
            .param("starttime", String.valueOf(MILLISECONDS.toSeconds(startTimeMillis)))
            .param("endtime", String.valueOf(MILLISECONDS.toSeconds(System.currentTimeMillis())))
            .param("responsecode", String.valueOf(responseCode))
            .param("activity", Joiner.on(' ').join(activities)));
        return;
      } catch (TransientFailureException e) {
        // Log and swallow.
        logger.log(Level.WARNING, e.getMessage(), e);
      }
      logger.severe("Failed to create metric exporting task.");
    }
  }
}
