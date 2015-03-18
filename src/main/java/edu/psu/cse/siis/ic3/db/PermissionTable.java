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

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class PermissionTable extends Table {
  private static final String INSERT = "INSERT INTO Permissions (id, level) VALUES (?, ?)";
  private static final String FIND = "SELECT id FROM Permissions WHERE id = ? AND level = ?";
  private static final String FIND_SIGNATURE_OR_SYSTEM = "SELECT Permissions.id FROM Permissions "
      + "JOIN PermissionStrings ON Permissions.id  = PermissionStrings.id "
      + "WHERE st = ? AND (level = ? OR level = ?)";

  private PreparedStatement findSignatureOrSystemStatement;

  PermissionTable() {
  }

  public int insert(int permissionId, String level) throws SQLException {
    int id = find(permissionId, level);
    if (id != NOT_FOUND) {
      return id;
    }
    return forceInsert(permissionId, level);
  }

  public int forceInsert(int permissionId, String level) throws SQLException {
    if (insertStatement == null || insertStatement.isClosed()) {
      insertStatement = getConnection().prepareStatement(INSERT);
    }
    insertStatement.setInt(1, permissionId);
    insertStatement.setString(2, level);
    if (insertStatement.executeUpdate() == 0) {
      return NOT_FOUND;
    }
    return permissionId;
  }

  public int find(int permissionId, String level) throws SQLException {
    if (findStatement == null || findStatement.isClosed()) {
      findStatement = getConnection().prepareStatement(FIND);
    }
    findStatement.setInt(1, permissionId);
    findStatement.setString(2, level);
    return processIntFindQuery(findStatement);
  }

  /**
   * Figure out if a permission is a signature or signatureOrSystem one.
   * 
   * @param permission The permission to look for.
   * @return True if the permission is found and it is has a signature or signatureOrSystem
   *         protection level. Note that this returns false if the permission is not found, even if
   *         it is in fact a signature or signatureOrSystem permission.
   * @throws SQLException
   */
  public boolean isSignatureOrSystem(String permission) throws SQLException {
    if (findSignatureOrSystemStatement == null || findSignatureOrSystemStatement.isClosed()) {
      findStatement = getConnection().prepareStatement(FIND_SIGNATURE_OR_SYSTEM);
    }
    findStatement.setString(1, permission);
    findStatement.setString(2, Constants.PermissionLevel.SIGNATURE_SHORT);
    findStatement.setString(3, Constants.PermissionLevel.SIGNATURE_OR_SYSTEM_SHORT);
    return processIntFindQuery(findStatement) != NOT_FOUND;
  }
}
