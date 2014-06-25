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

package foo.domaintest.action;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableSet;
import foo.domaintest.action.annotation.ForPath;
import foo.domaintest.action.annotation.NoMetrics;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Unit tests for {@link ActionDelegator}. */
@RunWith(JUnit4.class)
public class ActionDelegatorTest {

  @ForPath("/foo/path")
  static class FooAction implements Action {
    @Override
    public void run() {}}

  @NoMetrics
  @ForPath("/bar/path/*")
  static class BarAction implements Action {
    @Override
    public void run() {}}

  ActionDelegator delegator =
      new ActionDelegator(ImmutableSet.of(FooAction.class, BarAction.class));

  @Test
  public void testGetAction() {
    assertTrue(delegator.getRegisteredAction("/foo/path").getAction() instanceof FooAction);
    assertTrue(delegator.getRegisteredAction("/foo/path/").getAction() instanceof NotFoundAction);
    assertTrue(
        delegator.getRegisteredAction("/the/foo/path").getAction() instanceof NotFoundAction);
    assertTrue(delegator.getRegisteredAction("/bar/path/").getAction() instanceof BarAction);
    assertTrue(delegator.getRegisteredAction("/bar/path/baz").getAction() instanceof BarAction);
    assertTrue(delegator.getRegisteredAction("/bar/path/baz/").getAction() instanceof BarAction);
    assertTrue(delegator.getRegisteredAction("/bar/path/baz//").getAction() instanceof BarAction);
    assertTrue(delegator.getRegisteredAction("/bar/path/baz/qux").getAction() instanceof BarAction);
    assertTrue(delegator.getRegisteredAction("/bar/path").getAction() instanceof NotFoundAction);
    assertTrue(
        delegator.getRegisteredAction("/the/bar/path/").getAction() instanceof NotFoundAction);
  }

  @Test
  public void testIsExportingMetrics() {
    assertTrue(delegator.getRegisteredAction("/foo/path").isExportingMetrics());
    assertFalse(delegator.getRegisteredAction("/bar/path").isExportingMetrics());
  }
}
