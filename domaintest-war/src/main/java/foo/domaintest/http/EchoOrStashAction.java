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

import foo.domaintest.action.Action.GetAction;
import foo.domaintest.action.Action.PostAction;
import foo.domaintest.http.HttpApiModule.Param;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

/** Base action for /echo or /stash endpoints. */
public abstract class EchoOrStashAction implements GetAction, PostAction {
  @Inject @Param("status") Integer status;
  @Inject @Param("sleep") Integer sleepSeconds;
  @Inject @Param("mime") String mimeType;
  @Inject @Param("delcookie") List<String> cookiesToDelete;
  @Inject @Param("addcookie") Map<String, String> cookiesToAdd;
  @Inject @Param("header") Map<String, String> headers;
  @Inject @Param("payload") String payload;
}
