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

package foo.domaintest.config;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;

import dagger.Module;
import dagger.Provides;

import org.json.simple.JSONValue;

import java.lang.annotation.Documented;
import java.util.List;
import java.util.Map;

import javax.inject.Qualifier;
import javax.inject.Singleton;

/** Dagger module for global singleton values. */
@Module(library = true)
public class ConfigModule {

  /** Binding annotation for Easter eggs definitions from Datastore. */
  @Qualifier
  @Documented
  public @interface EasterEggs {}

  /** Provides the project id for things like BigQuery. */
  @Provides
  @Singleton
  @SystemProperty("projectid")
  String provideProjectId() {
    return System.getProperty("foo.domaintest.projectid");
  }

  @Provides
  @Singleton
  @SystemProperty("sourceurl")
  String provideSourceUrl() {
    return System.getProperty("foo.domaintest.sourceurl");
  }

  @Provides
  @Singleton
  @EasterEggs
  @SuppressWarnings("unchecked")
  Table<String, String, String> provideEasterEggs() {
    ImmutableTable.Builder<String, String, String> table = new ImmutableTable.Builder<>();
    for (Map<String, String> egg
        : ((List<Map<String, String>>) JSONValue.parse(
            Optional.fromNullable(System.getProperty("foo.domaintest.eastereggs"))
                .or("[]")))) {
      table.put(egg.get("key"), egg.get("value"), egg.get("url"));
    }
    return table.build();
  }

  @Provides
  @Singleton
  @SystemProperty("sendgriduser")
  String provideSendGridUser() {
    return System.getProperty("foo.domaintest.sendgriduser");
  }

  @Provides
  @Singleton
  @SystemProperty("sendgridkey")
  String provideSendGridKey() {
    return System.getProperty("foo.domaintest.sendgridkey");
  }

  @Provides
  @Singleton
  @SystemProperty("autoreplykey")
  String provideAutoreplyKey() {
    return System.getProperty("foo.domaintest.autoreplykey");
  }
}
