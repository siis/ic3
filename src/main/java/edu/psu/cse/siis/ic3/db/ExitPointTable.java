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

public class ExitPointTable extends Table {
  private static final String INSERT = "INSERT INTO ExitPoints "
      + "(class_id, method, instruction, exit_kind, missing) VALUES (?, ?, ?, ?, ?)";

  // private static final String FIND =
  // "SELECT id FROM ExitPoints WHERE class_id = ? AND method = ? AND instruction = ? " +
  // "AND exit_kind = ?";

  public int insert(int classId, String method, int instruction, String exit_kind,
      Integer missingIntents) throws SQLException {
    // int id = find(classId, method, instruction, exit_kind);
    // if (id != NOT_FOUND) {
    // return id;
    // }
    if (insertStatement == null || insertStatement.isClosed()) {
      insertStatement = getConnection().prepareStatement(INSERT);
    }
    insertStatement.setInt(1, classId);
    if (method.length() > 512) {
      method = method.substring(0, 512);
    }
    insertStatement.setString(2, method);
    insertStatement.setInt(3, instruction);
    insertStatement.setString(4, exit_kind);
    if (missingIntents == null) {
      insertStatement.setNull(5, Types.INTEGER);
    } else {
      insertStatement.setInt(5, missingIntents);
    }

    if (insertStatement.executeUpdate() == 0) {
      return NOT_FOUND;
    }
    return findAutoIncrement();
  }

  // public int find(int classId, String method, int instruction, String exit_kind)
  // throws SQLException {
  // if (findStatement == null || findStatement.isClosed()) {
  // findStatement = getConnection().prepareStatement(FIND);
  // }
  // findStatement.setInt(1, classId);
  // findStatement.setString(2, method);
  // findStatement.setInt(3, instruction);
  // findStatement.setString(4, exit_kind);
  // return processIntFindQuery(findStatement);
  // }
}
