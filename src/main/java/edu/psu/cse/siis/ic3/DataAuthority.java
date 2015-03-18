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
package edu.psu.cse.siis.ic3;

import java.util.Objects;

import edu.psu.cse.siis.coal.Constants;

public class DataAuthority {

  private final String host;
  private final String port;

  public DataAuthority(String host, String port) {
    this.host = host;
    this.port = port;
  }

  public String getHost() {
    return host;
  }

  public String getPort() {
    return port;
  }

  public boolean isPrecise() {
    return !(Constants.ANY_STRING.equals(host) || Constants.ANY_STRING.equals(port));
  }

  @Override
  public String toString() {
    return "host " + host + ", port " + port;
  }

  @Override
  public int hashCode() {
    return Objects.hash(host, port);
  }

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof DataAuthority)) {
      return false;
    }
    DataAuthority secondDataAuthority = (DataAuthority) other;
    return Objects.equals(this.host, secondDataAuthority.host)
        && Objects.equals(this.port, secondDataAuthority.port);
  }

}
