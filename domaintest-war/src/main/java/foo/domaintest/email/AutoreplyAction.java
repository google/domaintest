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

package foo.domaintest.email;

import static com.google.common.base.CharMatcher.WHITESPACE;
import static com.google.common.base.Strings.emptyToNull;
import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.base.Strings.nullToEmpty;
import static com.google.common.collect.Iterables.getFirst;
import static foo.domaintest.util.Key.Type.STASH;
import static foo.domaintest.util.Key.Type.TOKEN;
import static java.util.concurrent.TimeUnit.MINUTES;

import com.google.appengine.api.memcache.Expiration;
import com.google.common.base.CharMatcher;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import foo.domaintest.action.Action.PostAction;
import foo.domaintest.action.HttpErrorException.BadRequestException;
import foo.domaintest.action.annotation.ForPath;
import foo.domaintest.action.annotation.RequestData;
import foo.domaintest.config.SystemProperty;
import foo.domaintest.email.EmailApiModule.EmailHeader;
import foo.domaintest.email.EmailApiModule.RawEmailHeaders;
import foo.domaintest.util.Key;
import foo.domaintest.util.Memcache;
import foo.domaintest.util.TempUrlFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.inject.Inject;

/** Action that receives email from sendgrid and sends autoreplies. */
@ForPath("/autoreply")
public class AutoreplyAction implements PostAction {

  private static final Logger logger = Logger.getLogger(AutoreplyAction.class.getName());

  /**
   * Stash emails for 15 minutes.
   * <p>
   * This is longer than /stash caches things, due to the vagaries of email delivery.
   */
  private static final Expiration STASH_EXPIRATION =
      Expiration.byDeltaSeconds((int) MINUTES.toSeconds(15));

  @Inject @SystemProperty("autoreplykey") String requiredApiKey;
  @Inject @RequestData("queryString") String presentedApiKey; // The whole query string is the key.
  @Inject @RawEmailHeaders String rawHeaders;
  @Inject @EmailHeader("from") String from;
  @Inject @EmailHeader("to") String to;
  @Inject @EmailHeader("reply-to") String replyTo;
  @Inject @EmailHeader("subject") String subject;
  @Inject @EmailHeader("message-id") String messageId;
  @Inject @EmailHeader("in-reply-to") String inReplyTo;
  @Inject @EmailHeader("references") String references;
  @Inject Emailer emailer;
  @Inject Memcache memcache;
  @Inject TempUrlFactory tempUrlFactory;

  @Override
  public void run() {
    if (!presentedApiKey.equals(requiredApiKey)) {
      throw new BadRequestException("Invalid API key");
    }
    Iterable<String> subjectWords = Splitter
        .on(CharMatcher.WHITESPACE)
        .omitEmptyStrings()
        .trimResults()
        .split(nullToEmpty(subject));
    // For spam-ignoring purposes, we only respond if the subject starts with the word "TEST".
    if (!"TEST".equalsIgnoreCase(getFirst(subjectWords, null))) {
      logger.info("Subject did not begin with the word 'TEST'");
      return;
    }
    String newBody = null;
    // Try to consider the second word (if any) as a stash token. If it is one, stash the headers.
    String token = Iterables.get(subjectWords, 1, null);
    if (token != null && memcache.load(new Key(TOKEN, token)) != null) {
    Map<String, Object> params = new HashMap<>();
      params.put("payload", rawHeaders);
      memcache.save(new Key(STASH, token), params, STASH_EXPIRATION);
      newBody = tempUrlFactory.getTempUrl(token);
    }
    String newSender = "tester@" + Splitter.on('@').splitToList(to).get(1);
    String newRecipient = Optional.fromNullable(replyTo).or(from);
    // We construct the references and in-reply-to headers according to RFC 5322 3.6.4. The new
    // references is the old message id. The new in-reply-to is the old references plus the old
    // message id. If there's no old references but there is an old in-reply-to with exactly one id,
    // we use that in place of the references.
    String newInReplyTo = messageId;
    if (isNullOrEmpty(references)
        && !isNullOrEmpty(inReplyTo)
        && !WHITESPACE.matchesAnyOf(inReplyTo.trim())) {
      references = inReplyTo;
    }
    String newReferences = emptyToNull(Joiner.on(' ').skipNulls().join(references, messageId));
    logger.info(String.format("Sending from %s to %s", newSender, newRecipient));
    logger.info(String.format("in-reply-to: %s, references: %s", newInReplyTo, newReferences));
    emailer.send(
        newSender,
        newRecipient,
        newBody,
        newInReplyTo,
        newReferences);
  }
}
