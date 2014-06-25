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

import static foo.domaintest.util.Key.Type.TOKEN;
import static java.util.concurrent.TimeUnit.HOURS;

import com.google.appengine.api.memcache.Expiration;
import foo.domaintest.action.Action.GetAction;
import foo.domaintest.action.Action.PostAction;
import foo.domaintest.action.Response;
import foo.domaintest.action.annotation.ForPath;
import foo.domaintest.http.HttpApiModule.RandomToken;
import foo.domaintest.util.Key;
import foo.domaintest.util.Memcache;

import javax.inject.Inject;

/** Action for /token endpoint, which returns a token that can be used with /stash or an email. */
@ForPath("/token")
public class TokenAction implements GetAction, PostAction {

  @Inject Response response;
  @Inject @RandomToken String randomToken;
  @Inject Memcache memcache;

  /** Expire tokens after 1 hour. */
  private static final Expiration TOKEN_EXPIRATION =
      Expiration.byDeltaSeconds((int) HOURS.toSeconds(1));

  /** Store a random token in memcache and return it. */
  @Override
  public void run() {
    memcache.save(new Key(TOKEN, randomToken), true, TOKEN_EXPIRATION);
    response.setPayload(randomToken).send();
  }
}
