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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.psu.cse.siis.ic3.manifest.ManifestComponent;
import edu.psu.cse.siis.ic3.manifest.ManifestData;
import edu.psu.cse.siis.ic3.manifest.ManifestIntentFilter;
import edu.psu.cse.siis.ic3.manifest.ManifestProviderComponent;

public class SQLConnection {
  protected static ApplicationTable applicationTable = new ApplicationTable();
  protected static IntentTable intentTable = new IntentTable();
  protected static IntentFilterTable intentFilterTable = new IntentFilterTable();
  protected static IntentMimeTypeTable intentMimeTypeTable = new IntentMimeTypeTable();
  protected static IntentActionTable intentActionTable = new IntentActionTable();
  protected static FilterActionTable filterActionTable = new FilterActionTable();
  protected static IntentCategoryTable intentCategoryTable = new IntentCategoryTable();
  protected static FilterCategoryTable filterCategoryTable = new FilterCategoryTable();
  protected static IntentDataTable intentDataTable = new IntentDataTable();
  protected static FilterDataTable filterDataTable = new FilterDataTable();
  protected static IntentClassTable intentClassTable = new IntentClassTable();
  protected static IntentPackageTable intentPackageTable = new IntentPackageTable();
  protected static StringTable actionStringTable = new StringTable("ActionStrings");
  protected static StringTable categoryStringTable = new StringTable("CategoryStrings");
  protected static StringTable permissionStringTable = new StringTable("PermissionStrings");
  protected static UsesPermissionTable usesPermissionTable = new UsesPermissionTable();
  protected static IntentPermissionTable intentPermissionTable = new IntentPermissionTable();
  protected static ClassTable classTable = new ClassTable();
  protected static IntentExtraTable intentExtraTable = new IntentExtraTable();
  protected static ComponentTable componentTable = new ComponentTable();
  protected static ComponentExtraTable componentExtraTable = new ComponentExtraTable();
  protected static ExitPointTable exitPointTable = new ExitPointTable();
  protected static ExitPointComponentTable exitPointComponentTable = new ExitPointComponentTable();
  protected static PermissionTable permissionTable = new PermissionTable();
  protected static UriDataTable uriDataTable = new UriDataTable();
  protected static UriTable uriTable = new UriTable();
  protected static ProviderTable providerTable = new ProviderTable();
  protected static ProviderAuthorityTable providerAuthorityTable = new ProviderAuthorityTable();
  protected static int appId = Constants.NOT_FOUND;

  public static void init(String dbName, String dbPropertiesPath, String sshPropertiesPath,
      int localDbPort) {
    Table.init(dbName, dbPropertiesPath, sshPropertiesPath, localDbPort);
  }

  public static void closeConnection() {
    Table.closeConnection();
  }

  public static Map<String, Integer> insert(String app, int version,
      List<ManifestComponent> intentFilters, Set<String> usesPermissions,
      Map<String, String> permissions, boolean skipEntryPoints) {
    try {
      if (appId == Constants.NOT_FOUND) {
        appId = applicationTable.insert(app, version);
      }
      if (usesPermissions != null && !insertUsesPermissions(usesPermissions)) {
        return null;
      }
      if (permissions != null && !insertPermissions(permissions)) {
        return null;
      }
      if (skipEntryPoints) {
        return Collections.emptyMap();
      } else {
        return insertIntentFilters(intentFilters);
      }
    } catch (SQLException e) {
      e.printStackTrace();
      return null;
    }
  }

  protected static boolean insertUsesPermissions(Set<String> usesPermissions) throws SQLException {
    if (usesPermissions == null) {
      return true;
    }
    Set<Integer> permissionIds = permissionStringTable.batchInsert(usesPermissions, null);
    for (int permissionId : permissionIds) {
      usesPermissionTable.insert(appId, permissionId);
    }
    return true;
  }

  protected static boolean insertPermissions(Map<String, String> permissions) throws SQLException {
    for (Map.Entry<String, String> entry : permissions.entrySet()) {
      int permissionId = permissionStringTable.insert(entry.getKey());
      permissionTable.insert(permissionId, entry.getValue());
    }
    return true;
  }

  /**
   * Figure out if a permission is a signature or signatureOrSystem one.
   * 
   * @param permission The permission to look for.
   * @return True if the permission is found and it is has a signature or signatureOrSystem
   *         protection level. Note that this returns false if the permission is not found, even if
   *         it is in fact a signature or signatureOrSystem permission.
   * @throws SQLException
   */
  public static boolean isSignatureOrSystem(String permission) throws SQLException {
    return permissionTable.isSignatureOrSystem(permission);
  }

  public static Map<String, Integer> insertIntentFilters(List<ManifestComponent> components)
      throws SQLException {
    if (appId == Constants.NOT_FOUND) {
      throw new RuntimeException("appId has not been set");
    }

    Map<String, Integer> componentIds = new HashMap<String, Integer>();

    for (ManifestComponent component : components) {
      int componentId;
      if (component instanceof ManifestProviderComponent) {
        componentId = insertProvider((ManifestProviderComponent) component);
        componentIds.put(component.getName(), componentId);
        continue;
      }
      componentId =
          insertComponent(component.getName(), component.getType(), component.isExported(),
              component.getPermission(), component.missingIntentFilters());
      componentIds.put(component.getName(), componentId);

      Set<ManifestIntentFilter> intentFilters = component.getIntentFilters();

      // System.out.println("Inserting " + intentFilters);
      if (intentFilters != null) {
        for (ManifestIntentFilter intentFilter : component.getIntentFilters()) {
          List<Integer> actionIds = new ArrayList<Integer>();
          List<Integer> categoryIds = new ArrayList<Integer>();

          // System.out.println(intentFilter.getActions());
          insertStrings(actionStringTable, intentFilter.getActions(), actionIds);
          insertStrings(categoryStringTable, intentFilter.getCategories(), categoryIds);

          // if (find) {
          // find =
          // (intentFilterTable.find(componentId, actionIds, categoryIds,
          // intentFilter.getMimeTypes(), intentFilter.isAlias()) != Constants.NOT_FOUND);
          // }
          // if (find) {
          // // System.out.println("Found");
          // continue;
          // }

          int filterId = intentFilterTable.forceInsert(componentId, intentFilter.isAlias());
          // System.out.println("Inserting actions " + actionIds);
          filterActionTable.batchForceInsert(filterId, actionIds);
          // for (int actionId : actionIds) {
          // filterActionTable.forceInsert(filterId, actionId);
          // }
          filterCategoryTable.batchForceInsert(filterId, categoryIds);
          // for (int categoryId : categoryIds) {
          // filterCategoryTable.forceInsert(filterId, categoryId);
          // }

          insertFilterData(filterId, intentFilter);
        }
      }
    }

    return componentIds;
  }

  protected static int insertProvider(ManifestProviderComponent component) throws SQLException {
    int componentId =
        insertComponent(component.getName(), component.getType(), component.isExported(), null,
            null);
    int providerId =
        providerTable.insert(componentId, component.getGrantUriPermissions(),
            component.getReadPermission(), component.getWritePermission());
    Set<String> authorities = component.getAuthorities();

    if (authorities != null) {
      for (String authority : authorities) {
        providerAuthorityTable.insert(providerId, authority);
      }
    }

    return componentId;
  }

  protected static void insertFilterData(Integer filterId, ManifestIntentFilter intentFilter)
      throws SQLException {
    List<ManifestData> data = intentFilter.getData();

    if (data != null) {
      for (ManifestData manifestData : data) {
        String type = null;
        String subtype = null;

        String mimeType = manifestData.getMimeType();
        if (mimeType != null) {
          String[] typeParts = null;
          if (mimeType.equals(Constants.ANY_STRING)) {
            type = "*";
            subtype = "*";
          } else {
            typeParts = mimeType.split("/");
            if (typeParts.length != 2) {
              type = null;
              subtype = null;
            } else {
              type = typeParts[0];
              subtype = typeParts[1];
            }
          }
        }

        filterDataTable.insert(filterId, manifestData.getScheme(), manifestData.getHost(),
            manifestData.getPort(), manifestData.getPath(), type, subtype);
      }
    }
  }

  protected static boolean insertStrings(StringTable table, Set<String> strings,
      List<Integer> output) throws SQLException {
    boolean[] allThere = new boolean[] { false };

    output.addAll(table.batchInsert(strings, allThere));

    if (strings != null) {
      for (String string : strings) {
        if (string != null) {
          int stringId = table.find(string);
          if (stringId == Constants.NOT_FOUND) {

            throw new RuntimeException("error: string '" + string + "' not found in table.");
          } else {
            output.add(stringId);
          }
        }
      }
    }
    return allThere[0];
  }

  protected static boolean
      findStrings(StringTable table, Set<String> strings, List<Integer> output) throws SQLException {
    if (strings == null) {
      return true;
    }
    // if (strings.contains(ConnectedComponents.ANY_STRING)
    // || strings.contains(ConnectedComponents.ANY_CLASS)) {
    // output.add(Constants.NOT_FOUND);
    // return true;
    // }
    for (String string : strings) {
      if (string != null) {
        if (string.equals(Constants.ANY_STRING) || string.equals(Constants.ANY_CLASS)) {
          output.add(Constants.NOT_FOUND);
        }
        int stringId = table.find(string);
        if (stringId == Constants.NOT_FOUND) {
          return false;
        } else {
          output.add(stringId);
        }
      }
    }
    return true;
  }

  // protected static boolean insertAndSplitStrings(StringTable table, Set<String> strings,
  // List<Integer[]> output) throws SQLException {
  // if (strings == null) {
  // return true;
  // }
  // boolean allThere = true;
  // for (String string : strings) {
  // if (string != null) {
  // String[] typeParts;
  // if (string.equals(Constants.ANY_STRING)) {
  // typeParts = new String[] {"*", "*"};
  // } else {
  // typeParts = string.split("/");
  // if (typeParts.length != 2) {
  // System.err.println("Wrong MIME type format: " + string);
  // }
  // }
  // int part1 = table.find(typeParts[0]);
  // if (part1 == Constants.NOT_FOUND) {
  // allThere = false;
  // part1 = table.forceInsert(typeParts[0]);
  // }
  // int part2 = table.find(typeParts[1]);
  // if (part2 == Constants.NOT_FOUND) {
  // allThere = false;
  // part2 = table.forceInsert(typeParts[1]);
  // }
  // output.add(new Integer[] { part1, part2 });
  // }
  // }
  // return allThere;
  // }
  //
  // protected static boolean findAndSplitStrings(StringTable table, Set<String> strings,
  // List<Integer[]> output) throws SQLException {
  // if (strings == null) {
  // return true;
  // }
  // if (strings.contains(Constants.ANY_STRING)) {
  // output.add(null);
  // return true;
  // }
  // for (String string : strings) {
  // if (string != null) {
  // String[] typeParts;
  // if (string.equals(Constants.ANY_STRING)) {
  // typeParts = new String[] {"*", "*"};
  // } else {
  // typeParts = string.split("/");
  // if (typeParts.length != 2) {
  // System.err.println("Wrong MIME type format: " + string);
  // }
  // }
  // int part1 = table.find(typeParts[0]);
  // if (part1 == Constants.NOT_FOUND) {
  // return false;
  // }
  // int part2 = table.find(typeParts[1]);
  // if (part2 == Constants.NOT_FOUND) {
  // return false;
  // }
  // output.add(new Integer[] { part1, part2 });
  // }
  // }
  // return true;
  // }

  public static int insertComponent(String name, String type, boolean exported, String permission,
      Integer missingIntentFilters) throws SQLException {
    int classId = insertClass(name);
    int permissionId =
        (permission == null) ? Constants.NOT_FOUND : permissionStringTable.insert(permission);
    return componentTable.insert(classId, type, exported, permissionId, missingIntentFilters);
  }

  protected static int insertClass(String clazz) throws SQLException {
    return classTable.insert(appId, clazz);
  }

  protected static boolean insertIntentPermission(int exitPointId, String intentPermission)
      throws SQLException {
    int permissionId = permissionStringTable.insert(intentPermission);
    intentPermissionTable.insert(exitPointId, permissionId);
    return true;
  }

  protected static int insertExitPoint(String className, String method, int instruction,
      String exit_kind, Integer missingIntentFilters) throws SQLException {
    int classId = insertClass(className);
    return exitPointTable.insert(classId, method, instruction, exit_kind, missingIntentFilters);
  }
}
