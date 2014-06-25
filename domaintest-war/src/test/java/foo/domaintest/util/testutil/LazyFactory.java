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

package foo.domaintest.util.testutil;

import dagger.Lazy;

/** A factory for {@link Lazy} instances. */
public class LazyFactory {

  public static <T> Lazy<T> lazy(final T t) {
    return new Lazy<T>() {
      @Override
      public T get() {
        return t;
      }};
  }

  public static <T> Lazy<T> throwingLazy() {
    return new Lazy<T>() {
      @Override
      public T get() {
        throw new IllegalStateException();
      }};
  }
}
