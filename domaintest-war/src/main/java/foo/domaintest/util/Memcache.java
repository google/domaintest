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

import static com.google.appengine.api.memcache.ErrorHandlers.getConsistentLogAndContinue;
import static com.google.appengine.api.memcache.MemcacheServiceFactory.getMemcacheService;

import com.google.appengine.api.memcache.Expiration;
import com.google.appengine.api.memcache.MemcacheService;
import foo.domaintest.action.annotation.RequestData;
import foo.domaintest.metrics.Metrics;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.logging.Level;

import javax.inject.Inject;

/** Thin wrapper around {@link MemcacheService} to transparently handle serialization . */
public class Memcache {

  private final MemcacheService memcacheService = initMemcacheService();

  /** The tld of the request is used as the memcache namespace. */
  private final String namespace;

  private final Metrics metrics;

  @Inject
  public Memcache(@RequestData("tld") String namespace, Metrics metrics) {
    this.namespace = namespace;
    this.metrics = metrics;
  }

  private MemcacheService initMemcacheService() {
    MemcacheService memcacheService = getMemcacheService();
    memcacheService.setErrorHandler(getConsistentLogAndContinue(Level.INFO));
    return memcacheService;
  }

  public void save(Key key, Object value, Expiration expiration) {
    try (Namespace n = new Namespace(namespace);
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        ObjectOutputStream objectStream = new ObjectOutputStream(byteStream)) {
      objectStream.writeObject(value);
      memcacheService.put(key.getRawKey(), byteStream.toByteArray(), expiration);
      metrics.addActivity(activity("save", key));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @SuppressWarnings("unchecked")
  public <T> T load(Key key) {
    byte[] bytes;
    try (Namespace n = new Namespace(namespace)) {
      bytes = (byte[]) memcacheService.get(key.getRawKey());
    }
    if (bytes == null) {
      return null;
    }
      metrics.addActivity(activity("load", key));
    try (ByteArrayInputStream byteStream = new ByteArrayInputStream(bytes);
        ObjectInputStream inputStream = new ObjectInputStream(byteStream)) {
      return (T) inputStream.readObject();
    } catch (IOException | ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  public void delete(Key key) {
    try (Namespace n = new Namespace(namespace)) {
      metrics.addActivity(activity("delete", key));
      memcacheService.delete(key.getRawKey());
    }
  }

  private static String activity(String activityPrefix, Key key) {
    return String.format("%s_%s", activityPrefix, key.getTypeName().toLowerCase());
  }
}
