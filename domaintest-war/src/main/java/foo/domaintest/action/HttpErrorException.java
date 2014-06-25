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

import javax.servlet.http.HttpServletResponse;

/** Base for exceptions that cause an HTTP error response. */
public abstract class HttpErrorException extends RuntimeException {

  private final int responseCode;

  private HttpErrorException(int responseCode, String message, Throwable cause) {
    super(message, cause);
    this.responseCode = responseCode;
  }

  public int getResponseCode() {
    return responseCode;
  }

  /** Exception that causes a 400 response. */
  public static class BadRequestException extends HttpErrorException {
    public BadRequestException(String message) {
      this(message, null);
    }

    public BadRequestException(String message, Throwable cause) {
      super(HttpServletResponse.SC_BAD_REQUEST, message, cause);
    }
  }

  /** Exception that causes a 404 response. */
  public static class NotFoundException extends HttpErrorException {
    public NotFoundException(String message) {
      super(HttpServletResponse.SC_NOT_FOUND, message, null);
    }
  }
}
