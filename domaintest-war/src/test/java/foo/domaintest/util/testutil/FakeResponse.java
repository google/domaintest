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

import foo.domaintest.action.Response;

/** A fake implementation of {@link Response} for use in tests. */
public class FakeResponse extends Response {

  boolean responseSent;

  public FakeResponse() {
    super(null, null, null);
  }

  @Override
  public void send() {
    this.responseSent = true;
  }

  public boolean isResponseSent() {
    setDefaults();
    return responseSent;
  }
}
