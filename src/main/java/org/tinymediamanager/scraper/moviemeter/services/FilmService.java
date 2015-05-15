/*
 * Copyright 2012 - 2015 Manuel Laggner
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
package org.tinymediamanager.scraper.moviemeter.services;

import org.tinymediamanager.scraper.moviemeter.entities.MMFilm;

import retrofit.http.GET;
import retrofit.http.Path;

public interface FilmService {
  @GET("/film/{id}")
  MMFilm getMovieInfo(@Path("id") int id);

  @GET("/film/{imdbId}")
  MMFilm getMovieInfoByImdbId(@Path("imdbId") String imdbId);
}
