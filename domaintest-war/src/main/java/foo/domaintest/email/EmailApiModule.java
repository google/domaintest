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

import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.CharStreams;
import foo.domaintest.action.Action;
import foo.domaintest.action.RequestModule;
import foo.domaintest.metrics.impl.MetricsModule;

import dagger.Module;
import dagger.Provides;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Documented;
import java.util.Collections;
import java.util.Set;

import javax.inject.Qualifier;
import javax.inject.Singleton;
import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.InternetHeaders;

/** Dagger module for creating the email api actions. */
@Module(
    addsTo = RequestModule.class,
    includes = MetricsModule.class,
    injects = { AutoreplyAction.class })
public class EmailApiModule {

  public static final Set<Class<? extends Action>> ACTIONS =
      ImmutableSet.<Class<? extends Action>>of(AutoreplyAction.class);

  /** Binding annotation for all unparsed email headers. */
  @Qualifier
  @Documented
  public @interface RawEmailHeaders {}

  /** Binding annotation for parsed email headers. */
  @Qualifier
  @Documented
  public @interface EmailHeader {
    String value();
  }

  /**
   * Provides parsed email headers from the "headers" param in a multipart/form-data request.
   * <p>
   * Although SendGrid parses some headers for us, it doesn't parse "reply-to", so we need to do
   * this. Once we are doing it, it's easier to be consistent and use this as the sole source of
   * truth for information that originates in the headers.
   */
  @Provides
  @Singleton
  InternetHeaders provideHeaders(FileItemIterator iterator) {
    try {
      while (iterator != null && iterator.hasNext()) {
        FileItemStream item = iterator.next();
        // SendGrid sends us the headers in the "headers" param.
        if (item.getFieldName().equals("headers")) {
          try (InputStream stream = item.openStream()) {
            // SendGrid always sends headers in UTF-8 encoding.
            return new InternetHeaders(new ByteArrayInputStream(
                CharStreams.toString(new InputStreamReader(stream, UTF_8.name())).getBytes(UTF_8)));
          }
        }
      }
    } catch (MessagingException | FileUploadException | IOException e) {
      // If we fail parsing the headers fall through returning the empty header object below.
    }
    return new InternetHeaders();  // Parsing failed or there was no "headers" param.
  }

  @Provides
  @RawEmailHeaders
  @SuppressWarnings("unchecked")
  String provideRawEmailHeaders(InternetHeaders headers) {
    return Joiner.on("\r\n").join(Collections.list(headers.getAllHeaderLines()));
  }

  @Provides
  @EmailHeader("from")
  String provideFrom(InternetHeaders headers) {
    return getAddressHeader(headers, "from");
  }

  @Provides
  @EmailHeader("reply-to")
  String provideReplyTo(InternetHeaders headers) {
    return getAddressHeader(headers, "reply-to");
  }

  @Provides
  @EmailHeader("to")
  String provideTo(InternetHeaders headers) {
    return getAddressHeader(headers, "to");
  }

  @Provides
  @EmailHeader("subject")
  String provideSubject(InternetHeaders headers) {
    return getHeader(headers, "subject");
  }

  @Provides
  @EmailHeader("message-id")
  String provideMessageId(InternetHeaders headers) {
    return getHeader(headers, "message-id");
  }

  @Provides
  @EmailHeader("references")
  String provideReferences(InternetHeaders headers) {
    return getHeader(headers, "references");
  }

  @Provides
  @EmailHeader("in-reply-to")
  String provideInReplyTo(InternetHeaders headers) {
    return getHeader(headers, "in-reply-to");
  }

  /** Parse an email from a header that may be of the form {@code First Last <name@example.tld>}. */
  public String getAddressHeader(InternetHeaders headers, String headerName) {
    try {
      String header = getHeader(headers, headerName);
      if (header == null) {
        return null;
      }
      // The "false" argument means use loose validation when parsing addresses out of the header.
      InternetAddress[] addresses = InternetAddress.parseHeader(header, false);
      return addresses == null || addresses.length == 0 ? null : addresses[0].getAddress();
    } catch (AddressException e) {
      return null;
    }
  }

  private String getHeader(InternetHeaders headers, String headerName) {
    // The "null" argument means ignore all but the first instance of the header.
    return headers.getHeader(headerName, null);
  }
}
