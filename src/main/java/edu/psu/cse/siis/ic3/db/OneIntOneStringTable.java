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

import edu.psu.cse.siis.coal.Constants;

public abstract class OneIntOneStringTable extends Table {
  private static final String INSERT = "INSERT INTO %s (%s, %s) VALUES (?, ?)";
  private static final String FIND = "SELECT id FROM %s WHERE %s = ? AND %s = ?";

  OneIntOneStringTable(String table, String firstColumn, String secondColumn) {
    insertString = String.format(INSERT, table, firstColumn, secondColumn);
    findString = String.format(FIND, table, firstColumn, secondColumn);
  }

  public int insert(int firstValue, String secondValue) throws SQLException {
    int id = find(firstValue, secondValue);
    if (id != NOT_FOUND) {
      return id;
    }
    return forceInsert(firstValue, secondValue);
  }

  public int forceInsert(int firstValue, String secondValue) throws SQLException {
    if (insertStatement == null || insertStatement.isClosed()) {
      insertStatement = getConnection().prepareStatement(insertString);
    }
    if (secondValue == null) {
      secondValue = Constants.NULL_STRING;
    }
    insertStatement.setInt(1, firstValue);
    insertStatement.setString(2, secondValue);
    if (insertStatement.executeUpdate() == 0) {
      return NOT_FOUND;
    }
    return findAutoIncrement();
  }

  public int find(int firstValue, String secondValue) throws SQLException {
    if (findStatement == null || findStatement.isClosed()) {
      findStatement = getConnection().prepareStatement(findString);
    }
    findStatement.setInt(1, firstValue);
    findStatement.setString(2, secondValue);
    return processIntFindQuery(findStatement);
  }
}
