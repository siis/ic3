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

public abstract class IntentOrFilterMimeTypeTable extends Table {
  private static final String INSERT = "INSERT INTO %s (%s, type, subtype) VALUES (?, ?, ?)";
  private static final String FIND = "SELECT id FROM %s WHERE %s = ? AND type = ? AND subtype = ?";

  IntentOrFilterMimeTypeTable(String table, String column) {
    insertString = String.format(INSERT, table, column);
    findString = String.format(FIND, table, column);
  }

  public int insert(int intentOrFilterId, String type, String subtype) throws SQLException {
    int id = find(intentOrFilterId, type, subtype);
    if (id != NOT_FOUND) {
      return id;
    }
    return forceInsert(intentOrFilterId, type, subtype);
  }

  public int forceInsert(int intentOrFilterId, String type, String subtype) throws SQLException {
    if (insertStatement == null || insertStatement.isClosed()) {
      insertStatement = getConnection().prepareStatement(insertString);
    }
    insertStatement.setInt(1, intentOrFilterId);
    insertStatement.setString(2, type);
    insertStatement.setString(3, subtype);
    if (insertStatement.executeUpdate() == 0) {
      return NOT_FOUND;
    }
    return findAutoIncrement();
  }

  public int find(int intentOrFilterId, String type, String subtype) throws SQLException {
    if (findStatement == null || findStatement.isClosed()) {
      findStatement = getConnection().prepareStatement(findString);
    }
    findStatement.setInt(1, intentOrFilterId);
    findStatement.setString(2, type);
    findStatement.setString(3, subtype);
    return processIntFindQuery(findStatement);
  }
}
