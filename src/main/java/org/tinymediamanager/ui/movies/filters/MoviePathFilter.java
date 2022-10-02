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
package org.tinymediamanager.ui.movies.filters;

import java.util.regex.Matcher;

import javax.swing.JLabel;

import org.apache.commons.lang3.StringUtils;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.scraper.util.StrgUtils;
import org.tinymediamanager.ui.components.TmmLabel;

/**
 * the class {@link MoviePathFilter} is used to offer a filter for the movie path
 * 
 * @author Wolfgang Janes
 */
public class MoviePathFilter extends AbstractTextMovieUIFilter {
  @Override
  protected JLabel createLabel() {
    return new TmmLabel(TmmResourceBundle.getString("metatag.path"));
  }

  @Override
  public String getId() {
    return "moviePath";
  }

  @Override
  public boolean accept(Movie movie) {
    if (StringUtils.isBlank(normalizedFilterText)) {
      return true;
    }

    try {
      Matcher matcher = filterPattern.matcher(StrgUtils.normalizeString(movie.getPath()));
      if (matcher.find()) {
        return true;
      }
    }
    catch (Exception e) {
      // if any exceptions are thrown, just return true
      return true;
    }

    return false;
  }
}
