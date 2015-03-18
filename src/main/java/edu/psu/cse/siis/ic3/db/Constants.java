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

public class Constants {
  public static final class PermissionLevel {
    public static final String NORMAL_SHORT = "n";
    public static final String DANGEROUS_SHORT = "d";
    public static final String SIGNATURE_SHORT = "s";
    public static final String SIGNATURE_OR_SYSTEM_SHORT = "o";
  }

  public static final class ComponentShortType {
    public static final String ACTIVITY = "a";
    public static final String SERVICE = "s";
    public static final String RECEIVER = "r";
    public static final String PROVIDER = "p";
    public static final String DYNAMIC_RECEIVER = "d";
  }

  public static final class ValueLimit {
    public static final int BUNDLE = 20 * 1024;
    public static final int INTENT = 20 * 1024;
    public static final int FILTER = 20 * 1024;
  }

  public static final String ANY_STRING = "(.*)";
  public static final String ANY_CLASS = "<ANY_CLASS>";
  public static final int NOT_FOUND = -1;
}
