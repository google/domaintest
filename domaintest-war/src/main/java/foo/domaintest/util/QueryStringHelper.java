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

import static com.google.common.base.Strings.nullToEmpty;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;

/** Utility class to create and parse query strings. */
public class QueryStringHelper {

  public static Multimap<String, String> parseQuery(String query) {
    ImmutableListMultimap.Builder<String, String> params = new ImmutableListMultimap.Builder<>();
    for (String piece : Splitter.on('&').omitEmptyStrings().split(nullToEmpty(query))) {
      List<String> keyAndValue = Splitter.on('=').limit(2).splitToList(piece);
      try {
        params.put(
            URLDecoder.decode(keyAndValue.get(0), UTF_8.name()),
            URLDecoder.decode(Iterables.get(keyAndValue, 1, ""),  UTF_8.name()));
      } catch (UnsupportedEncodingException e) {
        throw new AssertionError(e);
      }
    }
    return params.build();
  }

  public static String encodeQuery(Map<String, String> params) {
    return Joiner.on('&').withKeyValueSeparator("=").join(Maps.transformValues(
        params,
        new Function<String, String>() {
          @Override
          public String apply(String input) {
            try {
              return URLEncoder.encode(input, UTF_8.name());
            } catch (UnsupportedEncodingException e) {
              throw new AssertionError(e);
            }
          }}));
  }
}
