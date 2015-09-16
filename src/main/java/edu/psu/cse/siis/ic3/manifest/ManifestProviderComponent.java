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

import java.util.Set;

public class ManifestProviderComponent extends ManifestComponent {

  private final String readPermission;
  private final String writePermission;
  private final Set<String> authorities;
  private final boolean grantUriPermissions;

  ManifestProviderComponent(String type, String name, boolean exported, boolean foundExported,
      String readPermission, String writePermission, Set<String> authorities,
      boolean grantUriPermissions) {
    super(type, name, exported, foundExported, "", null, null, null, null);
    this.readPermission = readPermission;
    this.writePermission = writePermission;
    this.authorities = authorities;
    this.grantUriPermissions = grantUriPermissions;
  }

  public String getReadPermission() {
    return readPermission;
  }

  public String getWritePermission() {
    return writePermission;
  }

  public Set<String> getAuthorities() {
    return authorities;
  }

  public boolean getGrantUriPermissions() {
    return grantUriPermissions;
  }

}
