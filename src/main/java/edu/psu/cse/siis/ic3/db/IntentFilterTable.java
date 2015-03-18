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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class IntentFilterTable extends Table {
  private static final String INSERT = "INSERT INTO IntentFilters (component_id, alias) "
      + "VALUES (?, ?)";
  private static final String FIND = "SELECT IntentFilters.id, COUNT(IntentFilters.id) AS cnt "
      + "FROM IntentFilters " + "LEFT JOIN IFActions ON IntentFilters.id = IFActions.filter_id "
      + "LEFT JOIN IFCategories ON IntentFilters.id = IFCategories.filter_id "
      + "LEFT JOIN IFMimeTypes ON IntentFilters.id = IFMimeTypes.filter_id "
      + "WHERE component_id = ? AND alias = ?";

  public int forceInsert(int componentId, boolean alias) throws SQLException {
    // int id = find(connection, componentId, actions, categories, mimeTypes);
    // if (id != NOT_FOUND) {
    // return id;
    // }
    if (insertStatement == null || insertStatement.isClosed()) {
      insertStatement = getConnection().prepareStatement(INSERT);
    }
    insertStatement.setInt(1, componentId);
    insertStatement.setBoolean(2, alias);
    if (insertStatement.executeUpdate() == 0) {
      return NOT_FOUND;
    }
    return findAutoIncrement();
  }

  public int find(int componentId, List<Integer> actions, List<Integer> categories,
      Set<String> mimeTypes, boolean alias) throws SQLException {
    StringBuilder queryBuilder = new StringBuilder(FIND);

    int finalCount = 1;

    if (actions != null && actions.size() > 0) {
      queryBuilder.append(" AND (1 = 0");
      for (int i = 0; i < actions.size(); ++i) {
        queryBuilder.append(" OR action = ?");
      }
      queryBuilder.append(")");
      finalCount *= actions.size();
    } else {
      queryBuilder.append(" AND action IS ?");
    }
    if (categories != null && categories.size() > 0) {
      queryBuilder.append(" AND (1 = 0");
      for (int i = 0; i < categories.size(); ++i) {
        queryBuilder.append(" OR category = ?");
      }
      queryBuilder.append(")");
      finalCount *= categories.size();
    } else {
      queryBuilder.append(" AND category IS ?");
    }
    List<String[]> typePartList = null;
    if (mimeTypes != null && mimeTypes.size() > 0) {
      typePartList = new ArrayList<String[]>();
      queryBuilder.append(" AND (1 = 0");
      for (String mimeType : mimeTypes) {
        if (mimeType.equals(Constants.ANY_STRING)) {
          typePartList.add(new String[] { "*", "*" });
          queryBuilder.append(" OR (type = ? AND subtype = ?)");
        } else {
          String[] typeParts = mimeType.split("/");
          if (typeParts.length == 2) {
            typePartList.add(typeParts);
            queryBuilder.append(" OR (type = ? AND subtype = ?)");
          }
        }
      }
      queryBuilder.append(")");
      if (typePartList.size() > 0) {
        finalCount *= mimeTypes.size();
      }
    } else {
      queryBuilder.append(" AND type IS ? AND subtype IS ?");
    }

    findStatement = getConnection().prepareStatement(queryBuilder.toString());

    findStatement.setInt(1, componentId);

    findStatement.setBoolean(2, alias);

    int parameterIndex = 3;

    if (actions != null && actions.size() > 0) {
      for (Integer action : actions) {
        findStatement.setInt(parameterIndex++, action);
      }
    } else {
      findStatement.setNull(parameterIndex++, Types.INTEGER);
    }
    if (categories != null && categories.size() > 0) {
      for (Integer category : categories) {
        findStatement.setInt(parameterIndex++, category);
      }
    } else {
      findStatement.setNull(parameterIndex++, Types.INTEGER);
    }
    if (mimeTypes != null && mimeTypes.size() > 0) {
      for (String[] typePart : typePartList) {
        findStatement.setString(parameterIndex++, typePart[0]);
        findStatement.setString(parameterIndex++, typePart[1]);
      }
    } else {
      findStatement.setNull(parameterIndex++, Types.INTEGER);
      findStatement.setNull(parameterIndex, Types.INTEGER);
    }

    // System.out.println(findStatement);
    ResultSet resultSet = findStatement.executeQuery();
    int result;
    if (resultSet.next() && resultSet.getInt("cnt") == finalCount) {
      result = resultSet.getInt("IntentFilters.id");
    } else {
      result = NOT_FOUND;
    }
    resultSet.close();
    findStatement.close();

    return result;
  }
}
