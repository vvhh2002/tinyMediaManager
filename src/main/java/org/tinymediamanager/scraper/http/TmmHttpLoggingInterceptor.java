/*
 * Copyright 2012 - 2022 Manuel Laggner
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
package org.tinymediamanager.scraper.http;

import static java.net.HttpURLConnection.HTTP_NOT_MODIFIED;
import static java.net.HttpURLConnection.HTTP_NO_CONTENT;

import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import okhttp3.Connection;
import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;
import okio.GzipSource;

/**
 * This class is used to provide a logging interceptor for tinyMediaManager which is able to log request headers/responses and the body of text
 * responses at trace logging level
 *
 * @author Manuel Laggner
 */
public class TmmHttpLoggingInterceptor implements Interceptor {
  private static final Logger  LOGGER               = LoggerFactory.getLogger(TmmHttpLoggingInterceptor.class);
  private static final Charset UTF8                 = StandardCharsets.UTF_8;
  private static final int     HTTP_CONTINUE        = 100;
  private static final int     MAX_TEXT_BODY_LENGTH = 1000;

  @Override
  public Response intercept(Chain chain) throws IOException {
    Request request = chain.request();

    try {
      RequestBody requestBody = request.body();
      boolean hasRequestBody = requestBody != null;

      Connection connection = chain.connection();
      String requestStartMessage = "--> " + request.method() + ' ' + request.url() + (connection != null ? " " + connection.protocol() : "");

      LOGGER.trace(requestStartMessage);

      Headers headersRequest = request.headers();
      if (headersRequest.size() > 0) {
        LOGGER.trace("{}", headersRequest.toMultimap());
      }

      if (!hasRequestBody || bodyHasUnknownEncoding(request.headers())) {
        LOGGER.trace("--> END {}", request.method());
      }
      else {
        Buffer buffer = new Buffer();
        requestBody.writeTo(buffer);

        Charset charset = UTF8;
        MediaType contentType = requestBody.contentType();
        if (contentType != null) {
          charset = contentType.charset(UTF8);
        }

        LOGGER.trace("");
        if (isPlaintext(buffer)) {
          String content = buffer.readString(charset);
          // only log the first 1k characters
          if (content.length() > MAX_TEXT_BODY_LENGTH) {
            LOGGER.trace("{}...", content.substring(0, MAX_TEXT_BODY_LENGTH)); // NOSONAR
          }
          else {
            LOGGER.trace(content);
          }
          LOGGER.trace("--> END {} ({}-byte body)", request.method(), requestBody.contentLength());
        }
        else {
          LOGGER.trace("--> END {}", request.method());
        }
      }
    }
    catch (Exception e) {
      LOGGER.error("Problem in HTTP logging detected: {}", e.getMessage());
    }

    long startNs = System.nanoTime();
    Response response;
    try {
      response = chain.proceed(request);
    }
    catch (Exception e) {
      LOGGER.trace("<-- HTTP FAILED: {}", e.getMessage());
      throw e;
    }
    long tookMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs);

    Buffer buffer = null;

    try {
      ResponseBody responseBody = response.body();
      long contentLength = responseBody.contentLength();
      String bodySize = contentLength != -1 ? contentLength + "-byte" : "unknown-length";
      String logUrl = response.request()
          .url()
          .toString()
          .replaceAll("api_key=\\w+", "api_key=<API_KEY>")
          .replaceAll("api/\\d+\\w+", "api/<API_KEY>")
          .replaceAll("apikey=\\w+", "apikey=<API_KEY>");
      LOGGER.debug("<-- " + response.code() + (response.message().isEmpty() ? "" : ' ' + response.message()) + ' ' + logUrl + " (" + tookMs + "ms"
          + ", " + bodySize + " body" + ')');

      Headers headersResponse = response.headers();
      if (headersResponse.size() > 0) {
        LOGGER.trace("{}", headersResponse.toMultimap());
      }

      if (!hasBody(response) || bodyHasUnknownEncoding(response.headers())) {
        LOGGER.trace("<-- END HTTP");
      }
      else if (isTextResponse(response)) {
        BufferedSource source = responseBody.source();
        source.request(Long.MAX_VALUE); // Buffer the entire body.
        buffer = source.getBuffer();

        Long gzippedLength = null;
        if ("gzip".equalsIgnoreCase(headersResponse.get("Content-Encoding"))) {
          gzippedLength = buffer.size();
          GzipSource gzippedResponseBody = null;
          try {
            gzippedResponseBody = new GzipSource(buffer.clone());
            buffer = new Buffer();
            buffer.writeAll(gzippedResponseBody);
          }
          finally {
            if (gzippedResponseBody != null) {
              gzippedResponseBody.close();
            }
          }
        }

        Charset charset = UTF8;
        MediaType contentType = responseBody.contentType();
        if (contentType != null) {
          charset = contentType.charset(UTF8);
        }

        if (!isPlaintext(buffer)) {
          LOGGER.trace("");
          LOGGER.trace("<-- END HTTP (binary {}-byte body omitted)", buffer.size());
          return response;
        }

        if (contentLength != 0) {
          LOGGER.trace("");
          String content = buffer.clone().readString(charset);
          // only log the first 1k characters
          if (content.length() > MAX_TEXT_BODY_LENGTH) {
            LOGGER.trace("{}...", content.substring(0, MAX_TEXT_BODY_LENGTH)); // NOSONAR
          }
          else {
            LOGGER.trace(content);
          }
        }

        if (gzippedLength != null) {
          LOGGER.trace("<-- END HTTP ({}-byte, {}-gzipped-byte body)", buffer.size(), gzippedLength);
        }
        else {
          LOGGER.trace("<-- END HTTP ({}-byte body)", buffer.size());
        }
      }
    }
    catch (Exception e) {
      LOGGER.error("Problem in HTTP logging detected: {}", e.getMessage());
    }
    finally {
      if (buffer != null) {
        buffer.close();
      }
    }

    return response;
  }

  private static boolean bodyHasUnknownEncoding(Headers headers) {
    String contentEncoding = headers.get("Content-Encoding");
    return contentEncoding != null && !contentEncoding.equalsIgnoreCase("identity") && !contentEncoding.equalsIgnoreCase("gzip");
  }

  private static boolean isTextResponse(Response response) {
    MediaType type = response.body().contentType();
    if (type == null || type.subtype() == null) {
      return false;
    }

    // only log XML/JSON responses
    switch (type.subtype().toLowerCase(Locale.ROOT)) {
      case "json":
      case "xml":
        return true;

      default:
        return false;
    }
  }

  private static long contentLength(Headers headers) {
    return stringToLong(headers.get("Content-Length"));
  }

  private static long stringToLong(String s) {
    if (s == null)
      return -1;
    try {
      return Long.parseLong(s);
    }
    catch (NumberFormatException e) {
      return -1;
    }
  }

  /** Returns true if the response must have a (possibly 0-length) body. See RFC 7231. */
  public static boolean hasBody(Response response) {
    // HEAD requests never yield a body regardless of the response headers.
    if (response.request().method().equals("HEAD")) {
      return false;
    }

    int responseCode = response.code();
    if ((responseCode < HTTP_CONTINUE || responseCode >= 200) && responseCode != HTTP_NO_CONTENT && responseCode != HTTP_NOT_MODIFIED) {
      return true;
    }

    // If the Content-Length or Transfer-Encoding headers disagree with the response code, the
    // response is malformed. For best compatibility, we honor the headers.
    return contentLength(response.headers()) != -1 || "chunked".equalsIgnoreCase(response.header("Transfer-Encoding"));
  }

  /**
   * Returns true if the body in question probably contains human readable text. Uses a small sample of code points to detect unicode control
   * characters commonly used in binary file signatures.
   */
  static boolean isPlaintext(Buffer buffer) {
    try {
      Buffer prefix = new Buffer();
      long byteCount = buffer.size() < 64 ? buffer.size() : 64;
      buffer.copyTo(prefix, 0, byteCount);
      for (int i = 0; i < 16; i++) {
        if (prefix.exhausted()) {
          break;
        }
        int codePoint = prefix.readUtf8CodePoint();
        if (Character.isISOControl(codePoint) && !Character.isWhitespace(codePoint)) {
          return false;
        }
      }
      return true;
    }
    catch (EOFException e) {
      return false; // Truncated UTF-8 sequence.
    }
  }
}
