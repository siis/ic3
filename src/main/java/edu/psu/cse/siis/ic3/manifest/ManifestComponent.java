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

import java.util.HashSet;
import java.util.Set;

public class ManifestComponent {
  private final String name;
  private boolean exported;
  private final boolean foundExported;
  private final String type;
  private Set<ManifestIntentFilter> intentFilters = null;
  private final String permission;
  // Target activity.
  private final String target;
  private final Integer missingIntentFilters;

  public ManifestComponent(String type, String name, boolean exported, boolean foundExported,
      String permission, String target, Integer missingIntentFilters) {
    this.type = type;
    this.exported = exported;
    this.foundExported = foundExported;
    this.name = name;
    this.permission = permission;
    this.target = target;
    this.missingIntentFilters = missingIntentFilters;
  }

  /**
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * @return the exported
   */
  public boolean isExported() {
    return exported;
  }

  /**
   * @return the type
   */
  public String getType() {
    return type;
  }

  /**
   * @return the intentFilters
   */
  public Set<ManifestIntentFilter> getIntentFilters() {
    return intentFilters;
  }

  /**
   * @param intentFilters the intentFilters to set
   */
  public void setIntentFilters(Set<ManifestIntentFilter> intentFilters) {
    this.intentFilters = intentFilters;
  }

  public void addIntentFilters(Set<ManifestIntentFilter> intentFilters) {
    if (intentFilters != null) {
      if (this.intentFilters == null) {
        this.intentFilters = new HashSet<>();
      }

      this.intentFilters.addAll(intentFilters);
    }
  }

  /**
   * Sets the intent filter and sets the exported flag accordingly.
   * 
   * @param intentFilters The intent filters to set.
   */
  public void setIntentFiltersAndExported(Set<ManifestIntentFilter> intentFilters) {
    this.intentFilters = intentFilters;
    if (!foundExported) {
      exported = (intentFilters != null && intentFilters.size() > 0);
    }
  }

  /**
   * @return the foundExported
   */
  public boolean isFoundExported() {
    return foundExported;
  }

  /**
   * @return the permission
   */
  public String getPermission() {
    return permission;
  }

  /**
   * @return the target
   */
  public String getTarget() {
    return target;
  }

  /**
   * @return the missingIntentFilters
   */
  public Integer missingIntentFilters() {
    return missingIntentFilters;
  }
}
