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

package foo.domaintest.http;

import foo.domaintest.action.Response;
import foo.domaintest.action.annotation.ForPath;

import javax.inject.Inject;

/** Action for /echo endpoint. */
@ForPath("/echo")
public class EchoAction extends EchoOrStashAction {

  @Inject Response response;

  /** Echo requests back immediately. */
  @Override
  public void run() {
    response
        .setStatus(status)
        .setSleepSeconds(sleepSeconds)
        .setMimeType(mimeType)
        .setCookiesToDelete(cookiesToDelete)
        .setCookiesToAdd(cookiesToAdd)
        .setHeaders(headers)
        .setPayload(payload)
        .send();
  }
}
