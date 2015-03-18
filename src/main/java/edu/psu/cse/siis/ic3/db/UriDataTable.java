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

public class UriDataTable extends Table {
  private static final String INSERT = "INSERT INTO UriData "
      + "(scheme, ssp, uri, path, query, authority) VALUES (?, ?, ?, ?, ?, ?)";

  public int forceInsert(String scheme, String ssp, String uri, String path, String query,
      String authority) throws SQLException {
    // int id = find(connection, componentId, actions, categories, mimeTypes);
    // if (id != NOT_FOUND) {
    // return id;
    // }
    if (insertStatement == null || insertStatement.isClosed()) {
      insertStatement = getConnection().prepareStatement(INSERT);
    }
    if (scheme == null) {
      insertStatement.setNull(1, Types.VARCHAR);
    } else {
      insertStatement.setString(1, scheme);
    }
    if (ssp == null) {
      insertStatement.setNull(2, Types.VARCHAR);
    } else {
      insertStatement.setString(2, ssp);
    }
    if (uri == null) {
      insertStatement.setNull(3, Types.VARCHAR);
    } else {
      insertStatement.setString(3, uri);
    }
    if (path == null) {
      insertStatement.setNull(4, Types.VARCHAR);
    } else {
      insertStatement.setString(4, path);
    }
    if (query == null) {
      insertStatement.setNull(5, Types.VARCHAR);
    } else {
      insertStatement.setString(5, query);
    }
    if (authority == null) {
      insertStatement.setNull(6, Types.VARCHAR);
    } else {
      insertStatement.setString(6, authority);
    }

    if (insertStatement.executeUpdate() == 0) {
      return NOT_FOUND;
    }
    return findAutoIncrement();
  }
}
