/*
 * Copyright (C) 2015 The Pennsylvania State University and the University of Wisconsin
 * Systems and Internet Infrastructure Security Laboratory
 *
 * Author: Damien Octeau
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.psu.cse.siis.ic3.manifest;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ManifestIntentFilter {
  private static final String ACTION_MAIN = "android.intent.action.MAIN";
  // private static final String CATEGORY_LAUNCHER = "android.intent.category.LAUNCHER";
  private final boolean alias;
  private final Integer priority;
  private Set<String> actions = null;
  private Set<String> categories = null;
  private List<ManifestData> data = null;

  public ManifestIntentFilter(boolean alias, Integer priority) {
    this.alias = alias;
    this.priority = priority;
  }

  public ManifestIntentFilter(Set<String> actions, Set<String> categories, boolean alias,
      List<ManifestData> data, Integer priority) {
    this.alias = alias;
    this.actions = actions;
    this.categories = categories;
    this.data = data;
    this.priority = priority;
  }

  /**
   * @return the alias
   */
  public boolean isAlias() {
    return alias;
  }

  /**
   * @return the priority
   */
  public Integer getPriority() {
    return priority;
  }

  /**
   * @return the actions
   */
  public Set<String> getActions() {
    return actions;
  }

  /**
   * @param action the action to add
   */
  public void addAction(String action) {
    if (this.actions == null) {
      this.actions = new HashSet<String>();
    }
    this.actions.add(action);
  }

  /**
   * @return the categories
   */
  public Set<String> getCategories() {
    return categories;
  }

  /**
   * @param categories the category to add
   */
  public void addCategory(String category) {
    if (this.categories == null) {
      this.categories = new HashSet<String>();
    }
    this.categories.add(category);
  }

  /**
   * @return the data
   */
  public List<ManifestData> getData() {
    return this.data;
  }

  /**
   * @return the data
   */
  public void addData(ManifestData manifestData) {
    if (this.data == null) {
      this.data = new ArrayList<>();
    }
    this.data.add(manifestData);
  }

  /**
   * Specifies whether an intent filter corresponds to an activity which can be used as an app's
   * entry point. We do not necessarily consider applications which appear in the launcher.
   * 
   * @return True if the intent filter describes an activity which can be used as an entry point.
   */
  public boolean isEntryPoint() {
    return actions.contains(ACTION_MAIN)/* && categories.contains(CATEGORY_LAUNCHER) */;
  }

  public String toString(String indent) {
    StringBuilder result = new StringBuilder(indent);
    result.append("Intent filter:\n");
    if (actions != null && actions.size() != 0) {
      result.append(indent);
      result.append("  Actions: ");
      result.append(actions);
      result.append("\n");
    }
    if (categories != null && categories.size() != 0) {
      result.append(indent);
      result.append("  Categories: ");
      result.append(categories);
      result.append("\n");
    }
    if (data != null && data.size() != 0) {
      result.append(indent);
      result.append("  Data: \n");
      for (ManifestData manifestData : data) {
        result.append(manifestData.toString(indent + "  ") + "\n");
      }
    }

    return result.toString();
  }

  @Override
  public String toString() {
    return toString("");
  }
}
