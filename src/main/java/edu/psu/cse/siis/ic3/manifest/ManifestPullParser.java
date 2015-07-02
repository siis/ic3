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
package edu.psu.cse.siis.ic3.manifest;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import edu.psu.cse.siis.ic3.Ic3Data;
import edu.psu.cse.siis.ic3.Ic3Data.Application.Builder;
import edu.psu.cse.siis.ic3.Ic3Data.Application.Component;
import edu.psu.cse.siis.ic3.Ic3Data.Application.Component.ComponentKind;
import edu.psu.cse.siis.ic3.Ic3Data.Application.Component.IntentFilter;
import edu.psu.cse.siis.ic3.Ic3Data.Attribute;
import edu.psu.cse.siis.ic3.Ic3Data.AttributeKind;
import edu.psu.cse.siis.ic3.db.Constants;
import edu.psu.cse.siis.ic3.db.SQLConnection;
import edu.psu.cse.siis.ic3.manifest.binary.AXmlResourceParser;

public class ManifestPullParser {
  private static final String MANIFEST = "manifest";
  private static final String MANIFEST_FILE_NAME = "AndroidManifest.xml";
  private static final String ACTIVITY = "activity";
  private static final String ACTIVITY_ALIAS = "activity-alias";
  private static final String SERVICE = "service";
  private static final String PROVIDER = "provider";
  private static final String AUTHORITIES = "authorities";
  private static final String READ_PERMISSION = "readPermissions";
  private static final String WRITE_PERMISSION = "writePermission";
  private static final String RECEIVER = "receiver";
  private static final String APPLICATION = "application";
  private static final String NAMESPACE = "http://schemas.android.com/apk/res/android";
  // private static final String ENABLED = "android:enabled";
  private static final String NAME = "name";
  private static final String INTENT_FILTER = "intent-filter";
  private static final String ACTION = "action";
  private static final String CATEGORY = "category";
  private static final String MIME_TYPE = "mimeType";
  private static final String PACKAGE = "package";
  private static final String DATA = "data";
  private static final String FALSE = "false";
  private static final String VERSION = "versionCode";
  private static final String PERMISSION = "permission";
  private static final String EXPORTED = "exported";
  private static final String TRUE = "true";
  private static final String USES_PERMISSION = "uses-permission";
  private static final String PROTECTION_LEVEL = "protectionLevel";
  private static final String TARGET_ACTIVITY = "targetActivity";
  private static final String SCHEME = "scheme";
  private static final String HOST = "host";
  private static final String PORT = "port";
  private static final String PATH = "path";
  private static final String PATH_PATTERN = "pathPattern";
  private static final String PATH_PREFIX = "pathPrefix";
  private static final String GRANT_URI_PERMISSIONS = "grantUriPermissions";
  private static final String PRIORITY = "priority";

  private static final String NORMAL = "normal";
  private static final String DANGEROUS = "dangerous";
  private static final String SIGNATURE = "signature";
  private static final String SIGNATURE_OR_SYSTEM = "signatureOrSytem";

  private String applicationName;
  private String packageName;

  private static final String[] levelValueToShortString = { Constants.PermissionLevel.NORMAL_SHORT,
      Constants.PermissionLevel.DANGEROUS_SHORT, Constants.PermissionLevel.SIGNATURE_SHORT,
      Constants.PermissionLevel.SIGNATURE_OR_SYSTEM_SHORT };

  private static Map<String, Integer> tagDepthMap = null;

  // map a content provider to one or more authorities
  // private final Map<String, Set<String>> providersAuthorities = new HashMap<String,
  // Set<String>>();
  // private final Map<String, Set<String>> providersRPermission = new HashMap<String,
  // Set<String>>();
  // private final Map<String, Set<String>> providersWPermission = new HashMap<String,
  // Set<String>>();

  private final List<ManifestComponent> activities = new ArrayList<ManifestComponent>();
  private final List<ManifestComponent> activityAliases = new ArrayList<ManifestComponent>();
  private final List<ManifestComponent> services = new ArrayList<ManifestComponent>();
  private final List<ManifestComponent> receivers = new ArrayList<ManifestComponent>();
  private final List<ManifestComponent> providers = new ArrayList<ManifestComponent>();
  private int version = -1;
  private ManifestComponent currentComponent = null;
  private Set<ManifestIntentFilter> currentIntentFilters = null;
  private ManifestIntentFilter currentIntentFilter = null;
  private String skipToEndTag = null;
  private String applicationPermission = null;
  private final Set<String> usesPermissions = new HashSet<>();
  private Map<String, String> permissions = new HashMap<String, String>();
  private final Set<String> entryPointClasses = new HashSet<>();

  public List<ManifestComponent> getActivities() {
    return activities;
  }

  public List<ManifestComponent> getActivityAliases() {
    return activityAliases;
  }

  public List<ManifestComponent> getServices() {
    return services;
  }

  public List<ManifestComponent> getReceivers() {
    return receivers;
  }

  public List<ManifestComponent> getProviders() {
    return providers;
  }

  public List<String> getComponents() {
    List<String> componentNames = new ArrayList<>();
    addComponentNamesToList(activities, componentNames);
    addComponentNamesToList(services, componentNames);
    addComponentNamesToList(receivers, componentNames);
    addComponentNamesToList(providers, componentNames);

    return componentNames;
  }

  private void setApplicationName(String applicationName) {
    this.applicationName = applicationName;
  }

  public String getApplicationName() {
    return applicationName;
  }

  private void setPackageName(String packageName) {
    this.packageName = packageName;
  }

  public String getPackageName() {
    return packageName;
  }

  public Set<String> getEntryPointClasses() {
    return entryPointClasses;
  }

  private void addComponentNamesToList(List<ManifestComponent> components, List<String> output) {
    for (ManifestComponent manifestComponent : components) {
      output.add(manifestComponent.getName());
    }
  }

  public void loadManifestFile(String manifest) {
    try {
      if (manifest.endsWith(".xml")) {
        loadClassesFromTextManifest(new FileInputStream(manifest));
      } else {
        handleBinaryManifestFile(manifest);
      }
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
  }

  private void handleBinaryManifestFile(String apk) {
    try {
      ZipFile archive = new ZipFile(apk);
      ZipEntry manifestEntry = archive.getEntry(MANIFEST_FILE_NAME);
      if (manifestEntry == null) {
        archive.close();
        throw new RuntimeException("No manifest file found in apk");
      }
      loadClassesFromBinaryManifest(archive.getInputStream(manifestEntry));
      archive.close();
    } catch (IOException e) {
      throw new RuntimeException("Error while processing apk " + apk + ": " + e);
    }
  }

  protected void loadClassesFromBinaryManifest(InputStream manifestIS) {
    AXmlResourceParser aXmlResourceParser = new AXmlResourceParser();
    aXmlResourceParser.open(manifestIS);
    try {
      parse(aXmlResourceParser);
    } catch (XmlPullParserException | IOException e) {
      e.printStackTrace();
      throw new RuntimeException("Could not parse manifest file.");
    }
  }

  protected void loadClassesFromTextManifest(InputStream manifestIS) {
    try {
      XmlPullParserFactory xmlPullParserFactory = XmlPullParserFactory.newInstance();
      xmlPullParserFactory.setNamespaceAware(true);
      XmlPullParser parser = xmlPullParserFactory.newPullParser();
      parser.setInput(manifestIS, null);
      parse(parser);
    } catch (XmlPullParserException | IOException e) {
      e.printStackTrace();
      throw new RuntimeException("Could not parse manifest file.");
    }
  }

  /**
   * Parse the manifest file.
   *
   * @param reader Input if text XML.
   * @param is Input if binary XML.
   * @throws IOException
   * @throws XmlPullParserException
   */
  public void parse(XmlPullParser parser) throws XmlPullParserException, IOException {
    initializeTagDepthMap();

    int depth = 0;
    int eventType = 0;
    eventType = parser.next();

    boolean okayToContinue = true;

    while (okayToContinue && eventType != XmlPullParser.END_DOCUMENT) {
      switch (eventType) {
        case XmlPullParser.START_TAG:
          okayToContinue = handleStartTag(parser, depth);
          ++depth;
          break;
        case XmlPullParser.END_TAG:
          okayToContinue = handleEndTag(parser);
          --depth;
          break;
      }
      eventType = parser.next();
    }
  }

  public Map<String, Integer> writeToDb(boolean skipEntryPoints) {
    Map<String, Integer> componentIds = new HashMap<String, Integer>();

    componentIds.putAll(SQLConnection.insert(getPackageName(), version, activities,
        usesPermissions, permissions, skipEntryPoints));
    componentIds.putAll(SQLConnection.insert(getPackageName(), version, activityAliases, null,
        null, skipEntryPoints));
    componentIds.putAll(SQLConnection.insert(getPackageName(), version, services, null, null,
        skipEntryPoints));
    componentIds.putAll(SQLConnection.insert(getPackageName(), version, receivers, null, null,
        skipEntryPoints));
    componentIds.putAll(SQLConnection.insert(getPackageName(), version, providers, null, null,
        skipEntryPoints));

    return componentIds;
  }

  public boolean isComponent(String name) {
    return entryPointClasses.contains(name);
  }

  public Map<String, Ic3Data.Application.Component.Builder> populateProtobuf(Builder ic3Builder) {
    ic3Builder.setName(getPackageName());
    ic3Builder.setVersion(version);

    for (Map.Entry<String, String> permission : permissions.entrySet()) {
      Ic3Data.Application.Permission protobufPermission =
          Ic3Data.Application.Permission.newBuilder().setName(permission.getKey())
              .setLevel(stringToLevel(permission.getValue())).build();
      ic3Builder.addPermissions(protobufPermission);
    }

    ic3Builder.addAllUsedPermissions(usesPermissions);

    Map<String, Ic3Data.Application.Component.Builder> componentNameToBuilderMap = new HashMap<>();

    componentNameToBuilderMap.putAll(populateProtobufComponentBuilders(activities,
        ComponentKind.ACTIVITY));
    componentNameToBuilderMap.putAll(populateProtobufComponentBuilders(services,
        ComponentKind.SERVICE));
    componentNameToBuilderMap.putAll(populateProtobufComponentBuilders(receivers,
        ComponentKind.RECEIVER));
    componentNameToBuilderMap.putAll(populateProtobufComponentBuilders(providers,
        ComponentKind.PROVIDER));

    return componentNameToBuilderMap;
  }

  private Map<String, Component.Builder> populateProtobufComponentBuilders(
      List<ManifestComponent> components, ComponentKind componentKind) {
    Map<String, Component.Builder> componentNameToBuilderMap = new HashMap<>();

    for (ManifestComponent manifestComponent : components) {
      componentNameToBuilderMap.put(manifestComponent.getName(),
          makeProtobufComponentBuilder(manifestComponent, componentKind));
    }

    return componentNameToBuilderMap;
  }

  public static Component.Builder makeProtobufComponentBuilder(ManifestComponent manifestComponent,
      ComponentKind componentKind) {
    Component.Builder componentBuilder = Component.newBuilder();
    componentBuilder.setName(manifestComponent.getName());
    componentBuilder.setKind(componentKind);

    componentBuilder.setExported(manifestComponent.isExported());
    if (manifestComponent.getPermission() != null) {
      componentBuilder.setPermission(manifestComponent.getPermission());
    }

    if (manifestComponent.missingIntentFilters() != null) {
      componentBuilder.setMissing(manifestComponent.missingIntentFilters());
    }

    if (manifestComponent.getIntentFilters() != null) {
      for (ManifestIntentFilter filter : manifestComponent.getIntentFilters()) {
        IntentFilter.Builder filterBuilder = IntentFilter.newBuilder();
        if (filter.getPriority() != null) {
          filterBuilder.addAttributes(Attribute.newBuilder().setKind(AttributeKind.PRIORITY)
              .addIntValue(filter.getPriority()));
        }
        Set<String> value = filter.getActions();
        if (value != null) {
          if (value.contains(null)) {
            value.remove(null);
            value.add(edu.psu.cse.siis.coal.Constants.NULL_STRING);
          }
          filterBuilder.addAttributes(Attribute.newBuilder().setKind(AttributeKind.ACTION)
              .addAllValue(value));
        }
        value = filter.getCategories();
        if (value != null) {
          if (value.contains(null)) {
            value.remove(null);
            value.add(edu.psu.cse.siis.coal.Constants.NULL_STRING);
          }
          filterBuilder.addAttributes(Attribute.newBuilder().setKind(AttributeKind.CATEGORY)
              .addAllValue(value));
        }
        if (filter.getData() != null) {
          for (ManifestData data : filter.getData()) {
            if (data.getHost() != null) {
              filterBuilder.addAttributes(Attribute.newBuilder().setKind(AttributeKind.HOST)
                  .addValue(data.getHost()));
            }
            if (data.getMimeType() != null) {
              // String[] typeParts = data.getMimeType().split("/");
              // String type;
              // String subtype;
              // if (typeParts.length == 2) {
              // type = typeParts[0];
              // subtype = typeParts[1];
              // } else {
              // type = Constants.ANY_STRING;
              // subtype = Constants.ANY_STRING;
              // }
              filterBuilder.addAttributes(Attribute.newBuilder().setKind(AttributeKind.TYPE)
                  .addValue(data.getMimeType()));
              // filterBuilder.addAttributes(Attribute.newBuilder().setKind(AttributeKind.SUBTYPE)
              // .addValue(subtype));
            }
            if (data.getPath() != null) {
              filterBuilder.addAttributes(Attribute.newBuilder().setKind(AttributeKind.PATH)
                  .addValue(data.getPath()));
            }
            if (data.getPort() != null) {
              filterBuilder.addAttributes(Attribute.newBuilder().setKind(AttributeKind.PORT)
                  .addValue(data.getPort()));
            }
            if (data.getScheme() != null) {
              filterBuilder.addAttributes(Attribute.newBuilder().setKind(AttributeKind.SCHEME)
                  .addValue(data.getScheme()));
            }
          }
        }

        componentBuilder.addIntentFilters(filterBuilder);
      }
    }

    return componentBuilder;
  }

  private Ic3Data.Application.Permission.Level stringToLevel(String levelString) {
    if (levelString.equalsIgnoreCase(Constants.PermissionLevel.NORMAL_SHORT)) {
      return Ic3Data.Application.Permission.Level.NORMAL;
    } else if (levelString.equalsIgnoreCase(Constants.PermissionLevel.DANGEROUS_SHORT)) {
      return Ic3Data.Application.Permission.Level.DANGEROUS;
    } else if (levelString.equalsIgnoreCase(Constants.PermissionLevel.SIGNATURE_SHORT)) {
      return Ic3Data.Application.Permission.Level.SIGNATURE;
    } else if (levelString.equalsIgnoreCase(Constants.PermissionLevel.SIGNATURE_OR_SYSTEM_SHORT)) {
      return Ic3Data.Application.Permission.Level.SIGNATURE_OR_SYSTEM;
    } else {
      throw new RuntimeException("Unknown permission level: " + levelString);
    }
  }

  @Override
  public String toString() {
    StringBuilder result = new StringBuilder("Manifest file for ");
    result.append(getPackageName());
    if (version != -1) {
      result.append(" version ").append(version);
    }
    result.append("\n  Activities:\n");
    componentMapToString(activities, result);
    result.append("\n  Activity Aliases:\n");
    componentMapToString(activityAliases, result);
    result.append("  Services:\n");
    componentMapToString(services, result);
    result.append("  Receivers:\n");
    componentMapToString(receivers, result);

    result.append("  Providers:\n");
    componentMapToString(providers, result);

    return result.toString();
  }

  private void componentMapToString(List<ManifestComponent> components, StringBuilder out) {
    for (ManifestComponent component : components) {
      out.append("    ").append(component.getName()).append("\n");
      if (component.getIntentFilters() != null) {
        for (ManifestIntentFilter manifestIntentFilter : component.getIntentFilters()) {
          out.append(manifestIntentFilter.toString("      "));
        }
      }
      if (component instanceof ManifestProviderComponent) {
        ManifestProviderComponent provider = (ManifestProviderComponent) component;
        for (String authority : provider.getAuthorities()) {
          out.append("      authority: " + authority + "\n");
        }
        String readPermission = provider.getReadPermission();
        String writePermission = provider.getWritePermission();
        if (!readPermission.equals("")) {
          out.append("      read permission: " + readPermission + "\n");
        }
        if (!writePermission.equals("")) {
          out.append("      write permission: " + writePermission + "\n");
        }
      }
    }
  }

  private boolean handleEndTag(XmlPullParser parser) {
    String tagName = parser.getName();

    if (skipToEndTag != null) {
      if (skipToEndTag.equals(tagName)) {
        skipToEndTag = null;
      }
      return true;
    }

    if (tagName.equals(ACTIVITY)) {
      return handleActivityEnd(parser);
    }
    if (tagName.equals(ACTIVITY_ALIAS)) {
      return handleActivityAliasEnd(parser);
    }
    if (tagName.equals(SERVICE)) {
      return handleServiceEnd(parser);
    }
    if (tagName.equals(RECEIVER)) {
      return handleReceiverEnd(parser);
    }
    if (tagName.equals(INTENT_FILTER)) {
      return handleIntentFilterEnd(parser);
    }
    if (tagName.equals(PROVIDER)) {
      return handleProviderEnd(parser);
    }

    return true;
  }

  private void initializeTagDepthMap() {
    if (tagDepthMap == null) {
      tagDepthMap = new HashMap<String, Integer>();
      tagDepthMap.put(MANIFEST, 0);
      tagDepthMap.put(USES_PERMISSION, 1);
      tagDepthMap.put(PERMISSION, 1);
      tagDepthMap.put(APPLICATION, 1);
      tagDepthMap.put(ACTIVITY, 2);
      tagDepthMap.put(SERVICE, 2);
      tagDepthMap.put(RECEIVER, 2);
      tagDepthMap.put(INTENT_FILTER, 3);
      tagDepthMap.put(ACTION, 4);
      tagDepthMap.put(CATEGORY, 4);
      tagDepthMap.put(DATA, 4);

      tagDepthMap = Collections.unmodifiableMap(tagDepthMap);
    }
  }

  private boolean handleStartTag(XmlPullParser parser, int depth) {
    if (skipToEndTag != null) {
      return true;
    }

    String tagName = parser.getName();
    if (!checkDepth(tagName, depth)) {
      return true;
    }

    if (tagName.equals(ACTIVITY)) {
      return handleActivityStart(parser);
    }
    if (tagName.equals(ACTIVITY_ALIAS)) {
      return handleActivityAliasStart(parser);
    }
    if (tagName.equals(SERVICE)) {
      return handleServiceStart(parser);
    }
    if (tagName.equals(RECEIVER)) {
      return handleReceiverStart(parser);
    }
    if (tagName.equals(INTENT_FILTER)) {
      return handleIntentFilterStart(parser);
    }
    if (tagName.equals(ACTION)) {
      return handleActionStart(parser);
    }
    if (tagName.equals(CATEGORY)) {
      return handleCategoryStart(parser);
    }
    if (tagName.equals(MANIFEST)) {
      return handleManifestStart(parser);
    }
    if (tagName.equals(APPLICATION)) {
      return handleApplicationStart(parser);
    }
    if (tagName.equals(USES_PERMISSION)) {
      return handleUsesPermissionStart(parser);
    }
    if (tagName.equals(PERMISSION)) {
      return handlePermissionStart(parser);
    }
    if (tagName.equals(DATA)) {
      return handleDataStart(parser);
    }
    if (tagName.equals(PROVIDER)) {
      return handleProviderStart(parser);
    }

    return true;
  }

  private boolean handleIntentFilterStart(XmlPullParser parser) {
    Integer priority = null;
    String priorityString = parser.getAttributeValue(NAMESPACE, PRIORITY);
    if (priorityString != null && priorityString.length() != 0) {
      try {
        priority = Integer.valueOf(priorityString);
      } catch (NumberFormatException exception) {
        System.err.println("Bad priority: " + priorityString);
      }
    }
    currentIntentFilter = new ManifestIntentFilter(false, priority);
    return true;
  }

  private boolean handleIntentFilterEnd(XmlPullParser parser) {
    if (currentIntentFilters == null) {
      currentIntentFilters = new HashSet<ManifestIntentFilter>();
    }
    currentIntentFilters.add(currentIntentFilter);
    currentIntentFilter = null;
    return true;
  }

  private boolean handleActivityStart(XmlPullParser parser) {
    return handleComponentStart(parser, ACTIVITY, Constants.ComponentShortType.ACTIVITY);
  }

  private boolean handleActivityEnd(XmlPullParser parser) {
    return handleComponentEnd(parser, activities);
  }

  private boolean handleActivityAliasStart(XmlPullParser parser) {
    return handleComponentStart(parser, ACTIVITY_ALIAS, Constants.ComponentShortType.ACTIVITY);
  }

  private boolean handleActivityAliasEnd(XmlPullParser parser) {
    return handleComponentEnd(parser, activityAliases);
  }

  private boolean handleServiceStart(XmlPullParser parser) {
    return handleComponentStart(parser, SERVICE, Constants.ComponentShortType.SERVICE);
  }

  private boolean handleServiceEnd(XmlPullParser parser) {
    return handleComponentEnd(parser, services);
  }

  private boolean handleReceiverStart(XmlPullParser parser) {
    return handleComponentStart(parser, RECEIVER, Constants.ComponentShortType.RECEIVER);
  }

  private boolean handleReceiverEnd(XmlPullParser parser) {
    return handleComponentEnd(parser, receivers);
  }

  private boolean handleComponentStart(XmlPullParser parser, String endTag, String componentType) {
    String name = null;
    String targetActivity = null;
    boolean isExported = false;
    boolean foundExported = false;
    String permission = null;

    for (int i = 0; i < parser.getAttributeCount(); ++i) {
      if (!parser.getAttributeNamespace(i).equals(NAMESPACE)) {
        continue;
      }
      String attributeName = parser.getAttributeName(i);
      if (attributeName.equals(NAME)) {
        name = parser.getAttributeValue(i);
      } else if (attributeName.equals(EXPORTED)) {
        String value = parser.getAttributeValue(i);
        if (value.equals(TRUE)) {
          isExported = true;
          foundExported = true;
        } else if (value.equals(FALSE)) {
          foundExported = true;
        }
      } else if (attributeName.equals(PERMISSION)) {
        permission = parser.getAttributeValue(i);
      } else if (attributeName.equals(TARGET_ACTIVITY)) {
        targetActivity = parser.getAttributeValue(i);
      }
    }

    if (name == null
        || (targetActivity != null && !entryPointClasses
            .contains(canonicalizeComponentName(targetActivity)))) {
      skipToEndTag = endTag;
      return true;
    }

    if (permission == null) {
      permission = applicationPermission;
    }
    currentComponent =
        new ManifestComponent(componentType, canonicalizeComponentName(name), isExported,
            foundExported, permission, targetActivity, null);

    return true;
  }

  private boolean handleComponentEnd(XmlPullParser parser, List<ManifestComponent> componentSet) {
    currentComponent.setIntentFiltersAndExported(currentIntentFilters);
    entryPointClasses.add(currentComponent.getName());
    componentSet.add(currentComponent);
    currentComponent = null;
    currentIntentFilters = null;
    return true;
  }

  private boolean handleApplicationStart(XmlPullParser parser) {
    for (int i = 0; i < parser.getAttributeCount(); ++i) {
      if (!parser.getAttributeNamespace(i).equals(NAMESPACE)) {
        continue;
      }
      // The enabled setting can be changed in the code using the PackageManager class.
      if (parser.getAttributeName(i).equals(NAME)) {
        setApplicationName(parser.getAttributeValue(i));
      } else if (parser.getAttributeName(i).equals(PERMISSION)) {
        applicationPermission = parser.getAttributeValue(i);
      }
    }
    return true;
  }

  private boolean handleUsesPermissionStart(XmlPullParser parser) {
    String permission = parser.getAttributeValue(NAMESPACE, NAME);
    if (permission != null) {
      usesPermissions.add(permission);
    }
    return true;
  }

  private boolean handlePermissionStart(XmlPullParser parser) {
    String permission = parser.getAttributeValue(NAMESPACE, NAME);
    String protectionLevel = parser.getAttributeValue(NAMESPACE, PROTECTION_LEVEL);
    protectionLevel = transformProtectionLevel(protectionLevel);
    if (permission != null) {
      if (permissions == null) {
        permissions = new HashMap<String, String>();
      }
      permissions.put(permission, protectionLevel);
    }
    return true;
  }

  /**
   * Transform the protection level to something appropriate for the database.
   *
   * The protection level is stored in two different ways. In a binary manifest, it is an integer.
   * In a text manifest, it is the string designation of the protection level.
   *
   * @param protectionLevel The protection level found in the manifest.
   * @return A string representation for the protection level appropriate for the database.
   */
  private String transformProtectionLevel(String protectionLevel) {
    if (protectionLevel == null || protectionLevel.equals("")
        || NORMAL.equalsIgnoreCase(protectionLevel)) {
      return Constants.PermissionLevel.NORMAL_SHORT;
    } else if (DANGEROUS.equalsIgnoreCase(protectionLevel)) {
      return Constants.PermissionLevel.DANGEROUS_SHORT;
    } else if (SIGNATURE.equalsIgnoreCase(protectionLevel)) {
      return Constants.PermissionLevel.SIGNATURE_SHORT;
    } else if (SIGNATURE_OR_SYSTEM.equalsIgnoreCase(protectionLevel)) {
      return Constants.PermissionLevel.SIGNATURE_OR_SYSTEM_SHORT;
    } else {
      // We are dealing with a binary manifest file.
      if (protectionLevel.startsWith("0x")) {
        // Even if hexadecimal, we still don't care about the radix, since these only go up
        // to 3.
        protectionLevel = protectionLevel.substring(2);
      }
      int level = Integer.parseInt(protectionLevel);
      return levelValueToShortString[level];
    }
  }

  private boolean handleActionStart(XmlPullParser parser) {
    if (currentIntentFilter != null) {
      currentIntentFilter.addAction(parser.getAttributeValue(NAMESPACE, NAME));
    }
    return true;
  }

  private boolean handleCategoryStart(XmlPullParser parser) {
    if (currentIntentFilter != null) {
      currentIntentFilter.addCategory(parser.getAttributeValue(NAMESPACE, NAME));
    }
    return true;
  }

  private boolean handleDataStart(XmlPullParser parser) {
    if (currentIntentFilter != null) {
      ManifestData manifestData = new ManifestData();

      for (int i = 0; i < parser.getAttributeCount(); ++i) {
        if (!parser.getAttributeNamespace(i).equals(NAMESPACE)) {
          continue;
        }
        String attributeName = parser.getAttributeName(i);
        String attributeValue = parser.getAttributeValue(i);
        if (attributeName.equals(MIME_TYPE)) {
          manifestData.setMimeType(attributeValue);
        } else if (attributeName.equals(SCHEME)) {
          manifestData.setScheme(attributeValue);
        } else if (attributeName.equals(HOST)) {
          manifestData.setHost(attributeValue);
        } else if (attributeName.equals(PORT)) {
          manifestData.setPort(attributeValue);
        } else if (attributeName.equals(PATH)) {
          manifestData.setPath(attributeValue);
        } else if (attributeName.equals(PATH_PATTERN)) {
          manifestData.setPath(attributeValue);
        } else if (attributeName.equals(PATH_PREFIX)) {
          manifestData.setPath(String.format("%s(.*)", attributeValue));
        }
      }

      currentIntentFilter.addData(manifestData);
    }

    return true;
  }

  private String canonicalizeComponentName(String name) {
    if (name.length() == 0) {
      throw new RuntimeException("Component should have non-empty name.");
    } else if (!name.contains(".")) {
      // The non-official rule is that we also prefix the name with the package when
      // the component name does not contain any dot.
      // (http://stackoverflow.com/questions/3608017/activity-name-in-androidmanifest-xml)
      name = getPackageName() + "." + name;
    } else if (name.charAt(0) == '.') {
      // The official rule is to prefix the name with the package when the component
      // name starts with a dot.
      // (http://developer.android.com/guide/topics/manifest/activity-element.html#nm)
      name = getPackageName() + name;
    }

    // Found a case where there was a '/' instead of a '.'.
    return name.replace('/', '.');
  }

  private boolean handleManifestStart(XmlPullParser parser) {
    for (int i = 0; i < parser.getAttributeCount(); ++i) {
      String attributeName = parser.getAttributeName(i);
      if (attributeName.equals(PACKAGE)) {
        // No namespace requirement.
        setPackageName(parser.getAttributeValue(i));
      } else if (parser.getAttributeNamespace(i).equals(NAMESPACE) && attributeName.equals(VERSION)) {
        version = Integer.parseInt(parser.getAttributeValue(i));
      }
    }
    if (getPackageName() != null) {
      return true;
    } else {
      return false;
    }
  }

  private boolean handleProviderStart(XmlPullParser parser) {
    boolean r = handleComponentStart(parser, PROVIDER, Constants.ComponentShortType.PROVIDER);
    String readPermission = currentComponent.getPermission();
    String writePermission = currentComponent.getPermission();
    Set<String> authorities = new HashSet<String>();
    boolean grantUriPermissions = false;

    for (int i = 0; i < parser.getAttributeCount(); ++i) {
      if (!parser.getAttributeNamespace(i).equals(NAMESPACE)) {
        continue;
      }
      String attributeName = parser.getAttributeName(i);
      // permissions
      // Note: readPermission and writePermission attributes take precedence over
      // permission attribute
      // (http://developer.android.com/guide/topics/manifest/provider-element.html).
      if (attributeName.equals(READ_PERMISSION)) {
        readPermission = parser.getAttributeValue(i);
      } else if (attributeName.equals(WRITE_PERMISSION)) {
        writePermission = parser.getAttributeValue(i);
      } else if (attributeName.equals(AUTHORITIES)) {
        // the "AUTHORITIES" attribute contains a list of authorities separated by semicolons.
        String s = parser.getAttributeValue(i);
        for (String a : s.split(";")) {
          authorities.add(a);
        }
      } else if (attributeName.equals(GRANT_URI_PERMISSIONS)) {
        grantUriPermissions = !parser.getAttributeValue(i).equals("false");
      }
    }

    currentComponent =
        new ManifestProviderComponent(currentComponent.getType(), currentComponent.getName(),
            currentComponent.isExported(), currentComponent.isFoundExported(), readPermission,
            writePermission, authorities, grantUriPermissions);
    return r;
  }

  private boolean handleProviderEnd(XmlPullParser parser) {
    return handleComponentEnd(parser, providers);
  }

  private boolean checkDepth(String tagName, int depth) {
    Integer expectedDepth = tagDepthMap.get(tagName);
    if (expectedDepth != null && expectedDepth != depth) {
      skipToEndTag = tagName;
      System.err.println("Warning: malformed Manifest file: " + tagName + " at depth " + depth);
    }

    return true;
  }
}
