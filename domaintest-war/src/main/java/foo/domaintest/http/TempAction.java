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

import static foo.domaintest.util.Key.Type.STASH;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import foo.domaintest.action.Action.GetAction;
import foo.domaintest.action.Action.PostAction;
import foo.domaintest.action.HttpErrorException.NotFoundException;
import foo.domaintest.action.Response;
import foo.domaintest.action.annotation.ForPath;
import foo.domaintest.action.annotation.RequestData;
import foo.domaintest.util.Key;
import foo.domaintest.util.Memcache;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

/** Action for retrieving /stash results and serving them on /temp. */
@ForPath("/temp/*")
public class TempAction implements GetAction, PostAction {

  @Inject Memcache memcache;
  @Inject Response response;
  @Inject @RequestData("path") String requestPath;

  /** Serve stashed requests. */
  @Override
  @SuppressWarnings("unchecked")
  public void run() {
    String token = Iterables.getLast(Splitter.on('/').split(requestPath), "");
    Map<String, Object> params = memcache.load(new Key(STASH, token));
    if (params == null) {
      throw new NotFoundException("No stashed request for this token");
    } else {
      response
          .setStatus((Integer) params.get("status"))
          .setSleepSeconds((Integer) params.get("sleepSeconds"))
          .setMimeType((String) params.get("mimeType"))
          .setCookiesToDelete((List<String>) params.get("cookiesToDelete"))
          .setCookiesToAdd((Map<String, String>) params.get("cookiesToAdd"))
          .setHeaders((Map<String, String>) params.get("headers"))
          .setPayload((String) params.get("payload"))
          .send();
      memcache.delete(new Key(STASH, token));
    }
  }
}
