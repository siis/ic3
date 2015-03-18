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

public class FilterDataTable extends Table {
  private static final String INSERT = "INSERT INTO IFData "
      + "(filter_id, scheme, host, port, path, type, subtype) VALUES (?, ?, ?, ?, ?, ?, ?)";

  public int insert(int filterId, String scheme, String host, String port, String path,
      String type, String subtype) throws SQLException {
    if (insertStatement == null || insertStatement.isClosed()) {
      insertStatement = getConnection().prepareStatement(INSERT);
    }

    insertStatement.setInt(1, filterId);
    if (scheme == null) {
      insertStatement.setNull(2, Types.VARCHAR);
    } else {
      insertStatement.setString(2, scheme);
    }
    if (host == null) {
      insertStatement.setNull(3, Types.VARCHAR);
    } else {
      insertStatement.setString(3, host);
    }
    if (port == null) {
      insertStatement.setNull(4, Types.VARCHAR);
    } else {
      insertStatement.setString(4, port);
    }
    if (path == null) {
      insertStatement.setNull(5, Types.INTEGER);
    } else {
      insertStatement.setString(5, path);
    }
    if (type == null) {
      insertStatement.setNull(6, Types.VARCHAR);
    } else {
      insertStatement.setString(6, type);
    }
    if (subtype == null) {
      insertStatement.setNull(7, Types.VARCHAR);
    } else {
      insertStatement.setString(7, subtype);
    }

    if (insertStatement.executeUpdate() == 0) {
      return NOT_FOUND;
    }
    return findAutoIncrement();
  }
}
