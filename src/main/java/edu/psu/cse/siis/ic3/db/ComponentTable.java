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

public class ComponentTable extends Table {
  private static final String INSERT = "INSERT INTO Components "
      + "(class_id, kind, exported, permission, missing) VALUES (?, ?, ?, ?, ?)";

  private static final String FIND =
      "SELECT id FROM Components WHERE class_id = ? AND kind = ? AND exported = ? "
          + "AND permission %s ? AND missing %s ?";

  // private static final String FIND_COMPONENT_FOR_EXPLICIT_INTENT =
  // "SELECT Components.id, kind FROM Applications " +
  // "JOIN Classes ON Applications.id = Classes.app_id " +
  // "JOIN Components ON Classes.id = Components.class_id " +
  // "LEFT JOIN UsesPermissions ON Applications.id = UsesPermissions.app_id " +
  // "WHERE (Applications.id = ? OR exported = ?) " +
  // "AND class = ? " +
  // "AND (permission IS ?";
  // private PreparedStatement findComponentForIntentStatement = null;

  public int insert(int classId, String type, boolean exported, int permission,
      Integer missingIntentFilters) throws SQLException {
    int id = find(classId, type, exported, permission, missingIntentFilters);
    if (id != NOT_FOUND) {
      return id;
    }
    if (insertStatement == null || insertStatement.isClosed()) {
      insertStatement = getConnection().prepareStatement(INSERT);
    }
    insertStatement.setInt(1, classId);
    insertStatement.setString(2, type);
    insertStatement.setBoolean(3, exported);
    if (permission == NOT_FOUND) {
      insertStatement.setNull(4, Types.INTEGER);
    } else {
      insertStatement.setInt(4, permission);
    }
    if (missingIntentFilters == null) {
      insertStatement.setNull(5, Types.INTEGER);
    } else {
      insertStatement.setInt(5, missingIntentFilters);
    }

    if (insertStatement.executeUpdate() == 0) {
      return NOT_FOUND;
    }
    return findAutoIncrement();
  }

  public int find(int classId, String type, boolean exported, int permission,
      Integer missingIntentFilters) throws SQLException {
    String formatArg1 = (permission == NOT_FOUND) ? "IS" : "=";
    String formatArg2 = (missingIntentFilters == null) ? "IS" : "=";
    findStatement = getConnection().prepareStatement(String.format(FIND, formatArg1, formatArg2));
    findStatement.setInt(1, classId);
    findStatement.setString(2, type);
    findStatement.setBoolean(3, exported);
    if (permission == NOT_FOUND) {
      findStatement.setNull(4, Types.VARCHAR);
    } else {
      findStatement.setInt(4, permission);
    }
    if (missingIntentFilters == null) {
      findStatement.setNull(5, Types.INTEGER);
    } else {
      findStatement.setInt(5, missingIntentFilters);
    }

    return processIntFindQuery(findStatement);
  }

  // public int findComponentForIntent(int appId, String clazz, Set<String> usesPermissions)
  // throws SQLException {
  // StringBuilder queryBuilder = new StringBuilder(FIND_COMPONENT_FOR_EXPLICIT_INTENT);
  //
  // if (usesPermissions != null) {
  // for (int i = 0; i < usesPermissions.size(); ++i) {
  // queryBuilder.append(" OR permission = ?");
  // }
  // }
  // queryBuilder.append(")");
  //
  // findComponentForIntentStatement = getConnection().prepareStatement(queryBuilder.toString());
  //
  // int parameterIndex = 1;
  // findComponentForIntentStatement.setInt(parameterIndex++, appId);
  // findComponentForIntentStatement.setBoolean(parameterIndex++, true);
  // findComponentForIntentStatement.setString(parameterIndex++, clazz.replace("/", "."));
  // findComponentForIntentStatement.setNull(parameterIndex++, Types.VARCHAR);
  // if (usesPermissions != null) {
  // for (String usesPermission : usesPermissions) {
  // findComponentForIntentStatement.setString(parameterIndex++, usesPermission);
  // }
  // }
  //
  // // System.out.println("Explicit matching: " + findComponentForIntentStatement.toString());
  // // long startTime = System.nanoTime();
  // ResultSet resultSet = findComponentForIntentStatement.executeQuery();
  // // long endTime = System.nanoTime();
  // // long duration = endTime - startTime;
  // // System.out.println(duration);
  // int result;
  // if (resultSet.next()) {
  // result = resultSet.getInt("Components.id");
  // } else {
  // result = NOT_FOUND;
  // }
  // resultSet.close();
  // return result;
  // }
}
