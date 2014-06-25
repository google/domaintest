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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Maps.uniqueIndex;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableSortedMap;
import foo.domaintest.action.annotation.ForPath;
import foo.domaintest.action.annotation.NoMetrics;

import java.util.NavigableMap;

/** Delegator to return an {@link Action} for a given path. */
public class ActionDelegator {

  private final NavigableMap<String, RegisteredAction> actionPaths;

  private static final RegisteredAction NOT_FOUND_ACTION =
      new RegisteredAction(NotFoundAction.class, null);

  /** A mapping of a recognized path to an {@link Action}. */
   public static class RegisteredAction {
    final String path;
    final Class<? extends Action> actionClass;
    final boolean exportingMetrics;

    RegisteredAction(Class<? extends Action> actionClass) {
      this(actionClass, actionClass.getAnnotation(ForPath.class).value());
      checkArgument(path.startsWith("/"));
    }

    RegisteredAction(Class<? extends Action> actionClass, String path) {
      this.actionClass = actionClass;
      this.path = path;
      this.exportingMetrics = !actionClass.isAnnotationPresent(NoMetrics.class);
    }

    public String getPath() {
      return path;
    }

    public boolean isExportingMetrics() {
      return exportingMetrics;
    }

    public Action getAction() {
      try {
        return actionClass.newInstance();
      } catch (InstantiationException | IllegalAccessException e) {
        throw new RuntimeException(e);
      }
    }
  }

  public ActionDelegator(Iterable<Class<? extends Action>> actionClasses) {
    // Using unique index here means we will get an early runtime error if there are two actions
    // with duplicate paths. Note that this will not detect duplications due to wildcards.
    this.actionPaths = ImmutableSortedMap.copyOf(uniqueIndex(
        transform(actionClasses, new Function<Class<? extends Action>, RegisteredAction>() {
          @Override
          public RegisteredAction apply(Class<? extends Action> action) {
            return new RegisteredAction(action);
          }}),
        new Function<RegisteredAction, String>() {
          @Override
          public String apply(RegisteredAction registeredAction) {
            // We strip out wildcards in the keys to support the NavigableMap.floorKey() call.
            String path = registeredAction.getPath();
            return path.endsWith("/*") ? path.substring(0, path.length() - 1) : path;
          }}));
  }

  public RegisteredAction getRegisteredAction(String requestPath) {
    String floorPath = actionPaths.floorKey(requestPath);
    if (floorPath != null && actionPaths.get(floorPath).getPath().endsWith("/*")
        ? requestPath.startsWith(floorPath)
        : requestPath.equals(floorPath)) {
      return actionPaths.get(floorPath);
    }
    return NOT_FOUND_ACTION;
  }
}
