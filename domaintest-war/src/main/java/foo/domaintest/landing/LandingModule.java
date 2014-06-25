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

import com.google.common.collect.ImmutableSet;
import foo.domaintest.action.Action;
import foo.domaintest.action.RequestModule;
import foo.domaintest.metrics.impl.MetricsModule;

import dagger.Module;

import java.util.Set;

/** Dagger module for the landing page. */
@Module(
    addsTo = RequestModule.class,
    includes = MetricsModule.class,
    injects = LandingAction.class
)
public class LandingModule {
  public static final Set<Class<? extends Action>> ACTIONS =
      ImmutableSet.<Class<? extends Action>>of(LandingAction.class);
}
