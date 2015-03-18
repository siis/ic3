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

public class IntentTable extends Table {
  private static final String INSERT = "INSERT INTO Intents (exit_id, implicit, alias) "
      + "VALUES (?, ?, ?)";

  // private static final String FIND = "SELECT Intents.id, COUNT(Intents.id) AS cnt "
  // + "FROM Intents " + "LEFT JOIN IActions ON Intents.id = IActions.intent_id "
  // + "LEFT JOIN ICategories ON Intents.id = ICategories.intent_id "
  // + "LEFT JOIN IMimeTypes ON Intents.id = IMimeTypes.intent_id "
  // + "LEFT JOIN IExtras ON Intents.id = IExtras.intent_id " + "WHERE exit_id = ? AND alias = ?";

  public int forceInsert(int exitId, boolean implicit, boolean alias) throws SQLException {
    // int id = find(connection, componentId, actions, categories, mimeTypes);
    // if (id != NOT_FOUND) {
    // return id;
    // }
    if (insertStatement == null || insertStatement.isClosed()) {
      insertStatement = getConnection().prepareStatement(INSERT);
    }
    insertStatement.setInt(1, exitId);
    insertStatement.setBoolean(2, implicit);
    insertStatement.setBoolean(3, alias);
    if (insertStatement.executeUpdate() == 0) {
      return NOT_FOUND;
    }
    return findAutoIncrement();
  }

  // public int find(int exitId, List<Integer> actions, List<Integer> categories, String mimeType,
  // Set<String> extras, boolean alias) throws SQLException {
  // StringBuilder queryBuilder = new StringBuilder(FIND);
  //
  // if (actions != null && actions.size() > 0) {
  // queryBuilder.append(" AND (1 = 0");
  // for (int i = 0; i < actions.size(); ++i) {
  // queryBuilder.append(" OR action = ?");
  // }
  // queryBuilder.append(")");
  // } else {
  // queryBuilder.append(" AND action IS ?");
  // }
  // int finalCount = 1;
  // if (categories != null && categories.size() > 0) {
  // queryBuilder.append(" AND (1 = 0");
  // for (int i = 0; i < categories.size(); ++i) {
  // queryBuilder.append(" OR category = ?");
  // }
  // queryBuilder.append(")");
  // finalCount *= categories.size();
  // } else {
  // queryBuilder.append(" AND category IS ?");
  // }
  // String[] typeParts = null;
  // if (mimeType != null) {
  // if (mimeType.equals(Constants.ANY_STRING)) {
  // queryBuilder.append(" AND type = ? AND subtype = ?");
  // typeParts = new String[] { "*", "*" };
  // } else {
  // typeParts = mimeType.split("/");
  // if (typeParts.length == 2) {
  // queryBuilder.append(" AND type = ? AND subtype = ?");
  // } else {
  // System.err.println("Warning: invalid type: " + mimeType);
  // queryBuilder.append(" AND type IS ? AND subtype IS ?");
  // typeParts = null;
  // }
  // }
  // } else {
  // queryBuilder.append(" AND type IS ? AND subtype IS ?");
  // }
  // if (extras != null && extras.size() > 0) {
  // queryBuilder.append(" AND (1 = 0");
  // for (int i = 0; i < extras.size(); ++i) {
  // queryBuilder.append(" OR extra = ?");
  // }
  // queryBuilder.append(")");
  // finalCount *= extras.size();
  // } else {
  // queryBuilder.append(" AND extra IS ?");
  // }
  //
  // findStatement = getConnection().prepareStatement(queryBuilder.toString());
  //
  // findStatement.setInt(1, exitId);
  //
  // findStatement.setBoolean(2, alias);
  //
  // int parameterIndex = 3;
  //
  // if (actions != null && actions.size() > 0) {
  // for (Integer action : actions) {
  // findStatement.setInt(parameterIndex++, action);
  // }
  // } else {
  // findStatement.setNull(parameterIndex++, Types.INTEGER);
  // }
  // if (categories != null && categories.size() > 0) {
  // for (Integer category : categories) {
  // findStatement.setInt(parameterIndex++, category);
  // }
  // } else {
  // findStatement.setNull(parameterIndex++, Types.INTEGER);
  // }
  // if (typeParts != null && typeParts.length == 2) {
  // findStatement.setString(parameterIndex++, typeParts[0]);
  // findStatement.setString(parameterIndex++, typeParts[1]);
  // } else {
  // findStatement.setNull(parameterIndex++, Types.VARCHAR);
  // findStatement.setNull(parameterIndex++, Types.VARCHAR);
  // }
  // if (extras != null && extras.size() > 0) {
  // for (String extra : extras) {
  // findStatement.setString(parameterIndex++, extra);
  // }
  // } else {
  // findStatement.setNull(parameterIndex, Types.VARCHAR);
  // }
  //
  // // System.out.println(findStatement);
  // ResultSet resultSet = findStatement.executeQuery();
  // int result;
  // if (resultSet.next() && resultSet.getInt("cnt") == finalCount) {
  // result = resultSet.getInt("Intents.id");
  // } else {
  // result = NOT_FOUND;
  // }
  // resultSet.close();
  // findStatement.close();
  //
  // return result;
  // }
}
