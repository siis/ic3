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
package edu.psu.cse.siis.ic3.db;

import java.sql.SQLException;
import java.sql.Types;

public class ProviderTable extends Table {
  private static final String INSERT = "INSERT INTO Providers "
      + "(component_id, grant_uri_permissions, read_permission, write_permission) "
      + "VALUES (?, ?, ?, ?)";

  public int insert(int componentId, boolean grantUriPermissions, String readPermission,
      String writePermission) throws SQLException {
    if (insertStatement == null || insertStatement.isClosed()) {
      insertStatement = getConnection().prepareStatement(INSERT);
    }

    insertStatement.setInt(1, componentId);
    insertStatement.setBoolean(2, grantUriPermissions);
    if (readPermission == null) {
      insertStatement.setNull(3, Types.VARCHAR);
    } else {
      insertStatement.setString(3, readPermission);
    }
    if (writePermission == null) {
      insertStatement.setNull(4, Types.VARCHAR);
    } else {
      insertStatement.setString(4, writePermission);
    }

    if (insertStatement.executeUpdate() == 0) {
      return NOT_FOUND;
    }
    return findAutoIncrement();
  }
}
