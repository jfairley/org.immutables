/*
    Copyright 2013-2014 Immutables.org authors

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package org.immutables.service.logging;

import com.google.common.annotations.Beta;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

@Beta
public class TracingLogEventListener implements LogEventListener {

  private final Logger logger;
  private final Locale locale;

  public TracingLogEventListener(Logger logger, Locale locale) {
    this.logger = logger;
    this.locale = locale;
  }

  public TracingLogEventListener() {
    this(LoggerFactory.getLogger(TracingLogEventListener.class.getPackage().getName()), Locale.ENGLISH);
  }

  @Override
  public void logEventPosted(LogEvent event) {
    switch (event.getSeverity()) {
    case ERROR:
      logger.error(markerFor(event), formatMessage(event));
      break;
    case WARNING:
      logger.warn(markerFor(event), formatMessage(event));
      break;
    case INFO:
      logger.info(markerFor(event), formatMessage(event));
      break;
    }
  }

  private String formatMessage(LogEvent event) {
    return Joiner.on(System.lineSeparator())
        .skipNulls()
        .join(
            event.getMessage(locale),
            Strings.emptyToNull(event.getDetails()));
  }

  private Marker markerFor(LogEvent event) {
    return MarkerFactory.getMarker(event.getSourceCategory() + "." + event.getDescriptiveCode());
  }
}
