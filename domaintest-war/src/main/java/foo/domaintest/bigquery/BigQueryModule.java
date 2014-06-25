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

package foo.domaintest.bigquery;

import com.google.api.client.extensions.appengine.http.UrlFetchTransport;
import com.google.api.client.googleapis.extensions.appengine.auth.oauth2.AppIdentityCredential;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.bigquery.Bigquery;
import com.google.api.services.bigquery.BigqueryScopes;

import dagger.Module;
import dagger.Provides;

import javax.inject.Singleton;

/** Dagger module for BigQuery. */
@Module(library = true)
public class BigQueryModule {
  /** Provides the authenticated Bigquery API object. */
  @Provides
  @Singleton
  Bigquery provideBigquery() {
    return new Bigquery.Builder(
        new UrlFetchTransport(),
        new JacksonFactory(),
        new AppIdentityCredential(BigqueryScopes.all()))
            .setApplicationName(BigQueryModule.class.getName())
            .build();
  }
}
