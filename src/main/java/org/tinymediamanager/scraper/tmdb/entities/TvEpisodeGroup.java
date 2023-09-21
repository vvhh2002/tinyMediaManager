/*
 * Copyright 2012 - 2023 Manuel Laggner
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
package org.tinymediamanager.scraper.tmdb.entities;

import java.util.List;

import com.google.gson.annotations.SerializedName;

public class TvEpisodeGroup {
  @SerializedName("id")
  public String               id;

  @SerializedName("type")
  public int                  type;

  @SerializedName("name")
  public String               name;

  @SerializedName("description")
  public String               description;

  @SerializedName("group_count")
  public int                  groupCount;

  @SerializedName("episode_count")
  public int                  episodeCount;

  @SerializedName("network")
  public Network              network;

  @SerializedName("groups")
  public List<TvEpisodeGroupSeason> groups;
}
