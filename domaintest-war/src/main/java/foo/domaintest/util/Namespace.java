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

package foo.domaintest.util;

import com.google.appengine.api.NamespaceManager;

/** A class that mildly abuses try-with-resources to implement temporary namespace blocks. */
public class Namespace implements AutoCloseable {

  private final String oldNamespace;

  public Namespace(String newNamespace) {
    this.oldNamespace = NamespaceManager.get();
    NamespaceManager.set(newNamespace);
  }

  @Override
  public void close() {
    NamespaceManager.set(oldNamespace);
  }
}
