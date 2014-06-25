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

import static com.google.common.collect.Iterables.limit;
import static foo.domaintest.util.Key.Type.STASH;
import static foo.domaintest.util.Key.Type.TOKEN;
import static java.util.concurrent.TimeUnit.MINUTES;

import com.google.appengine.api.memcache.Expiration;
import foo.domaintest.action.HttpErrorException.BadRequestException;
import foo.domaintest.action.Response;
import foo.domaintest.action.annotation.ForPath;
import foo.domaintest.http.HttpApiModule.Param;
import foo.domaintest.http.HttpApiModule.RandomToken;
import foo.domaintest.util.Key;
import foo.domaintest.util.Memcache;
import foo.domaintest.util.TempUrlFactory;

import dagger.Lazy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;

/** Action for /stash endpoint. */
@ForPath("/stash")
public class StashAction extends EchoOrStashAction {

  private static final int MAX_PARAM_REPETITION = 20;
  private static final int NAME_MAX_LENGTH = 1000;
  private static final int VALUE_MAX_LENGTH = 4000;
  private static final int MIMETYPE_MAX_LENGTH = 1000;
  private static final int PAYLOAD_MAX_LENGTH = 10240;

  /** Expire stashed content after 5 minutes. */
  private static final Expiration STASH_EXPIRATION =
      Expiration.byDeltaSeconds((int) MINUTES.toSeconds(5));

  @Inject Memcache memcache;
  @Inject @RandomToken Lazy<String> lazyRandomToken;
  @Inject @Param("token") String tokenParam;
  @Inject Response response;
  @Inject TempUrlFactory tempUrlFactory;

  /** Stash requests for later retrieval. */
  @Override
  public void run() {
    // If there's a valid user-supplied token, use it. If not, get a random one.
    String token;
    if (tokenParam == null) {
      token = lazyRandomToken.get();
    } else if (memcache.load(new Key(TOKEN, tokenParam)) != null) {
      token = tokenParam;
    } else {
      throw new BadRequestException("Invalid token");
    }
    // For safety we truncate all string fields so that an attacker can't blow out our memcache.
    Map<String, Object> params = new HashMap<>();
    params.put("status", status);
    params.put("sleepSeconds", sleepSeconds);
    params.put("mimeType", truncate(mimeType, MIMETYPE_MAX_LENGTH));
    params.put("cookiesToDelete", truncate(cookiesToDelete, MAX_PARAM_REPETITION, NAME_MAX_LENGTH));
    params.put(
        "cookiesToAdd",
        truncate(cookiesToAdd, MAX_PARAM_REPETITION, NAME_MAX_LENGTH, VALUE_MAX_LENGTH));
    params.put(
        "headers",
        truncate(headers, MAX_PARAM_REPETITION, NAME_MAX_LENGTH, VALUE_MAX_LENGTH));
    params.put("payload", truncate(payload, PAYLOAD_MAX_LENGTH));
    memcache.save(new Key(STASH, token), params, STASH_EXPIRATION);
    response.setPayload(tempUrlFactory.getTempUrl(token)).send();
  }

  private String truncate(String input, int length) {
    return input == null ? null : input.substring(0, Math.min(input.length(), length));
  }

  private List<String> truncate(Iterable<String> input, int maxValues, int valueLength) {
    if (input == null) {
      return null;
    }
    List<String> truncated = new ArrayList<>();
    for (String value : limit(input, maxValues)) {
      truncated.add(truncate(value, valueLength));
    }
    return truncated;
  }

  private Map<String, String> truncate(
      Map<String, String> input, int maxEntries, int keyLength, int valueLength) {
    if (input == null) {
      return null;
    }
    Map<String, String> truncated = new HashMap<>();
    for (Entry<String, String> entry : limit(input.entrySet(), maxEntries)) {
      truncated.put(truncate(entry.getKey(), keyLength), truncate(entry.getValue(), valueLength));
    }
    return truncated;
  }
}
