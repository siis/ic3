package edu.psu.cse.siis.ic3;

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soot.Scene;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;

import com.google.protobuf.TextFormat;

import edu.psu.cse.siis.coal.AnalysisParameters;
import edu.psu.cse.siis.coal.Constants;
import edu.psu.cse.siis.coal.Model;
import edu.psu.cse.siis.coal.PropagationTimers;
import edu.psu.cse.siis.coal.Result;
import edu.psu.cse.siis.coal.Results;
import edu.psu.cse.siis.coal.arguments.Argument;
import edu.psu.cse.siis.coal.field.values.FieldValue;
import edu.psu.cse.siis.coal.field.values.ScalarFieldValue;
import edu.psu.cse.siis.coal.field.values.TopFieldValue;
import edu.psu.cse.siis.coal.values.BasePropagationValue;
import edu.psu.cse.siis.coal.values.BottomPropagationValue;
import edu.psu.cse.siis.coal.values.PathValue;
import edu.psu.cse.siis.coal.values.PropagationValue;
import edu.psu.cse.siis.coal.values.TopPropagationValue;
import edu.psu.cse.siis.ic3.Ic3Data.Application.Component;
import edu.psu.cse.siis.ic3.Ic3Data.Application.Component.ComponentKind;
import edu.psu.cse.siis.ic3.Ic3Data.Application.Component.ExitPoint;
import edu.psu.cse.siis.ic3.Ic3Data.Application.Component.ExitPoint.Intent;
import edu.psu.cse.siis.ic3.Ic3Data.Application.Component.ExitPoint.Uri;
import edu.psu.cse.siis.ic3.Ic3Data.Attribute;
import edu.psu.cse.siis.ic3.Ic3Data.AttributeKind;
import edu.psu.cse.siis.ic3.manifest.ManifestComponent;
import edu.psu.cse.siis.ic3.manifest.ManifestData;
import edu.psu.cse.siis.ic3.manifest.ManifestIntentFilter;
import edu.psu.cse.siis.ic3.manifest.ManifestPullParser;

public class ProtobufResultProcessor {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  private static final String ENTRY_POINT_INTENT = "<INTENT>";

  private final int[] preciseNonLinking = { 0, 0, 0, 0 };
  private final int[] preciseLinking = { 0, 0, 0, 0 };
  private final int[] imprecise = { 0, 0, 0, 0, 0 };
  private final int[] top = { 0, 0, 0 };
  private final int[] bottom = { 0, 0, 0 };
  private final int[] nonexistent = { 0, 0, 0 };
  private final int[] preciseFieldValueCount = { 0, 0, 0 };
  private final int[] partiallyPreciseFieldValueCount = { 0, 0, 0 };
  private final int[] impreciseFieldValueCount = { 0, 0, 0 };
  private int intentWithData = 0;
  private int providerArgument = 0;

  public void processResult(String appName, Ic3Data.Application.Builder ic3Builder,
      String protobufDestination, boolean binary,
      Map<String, Ic3Data.Application.Component.Builder> componentNameToBuilderMap,
      int analysisClassesCount, Writer writer) throws IOException {
    for (Result result : Results.getResults()) {
      ((Ic3Result) result).dump();
      analyzeResult(result);
      writeResultToProtobuf(result, ic3Builder, componentNameToBuilderMap);
    }

    ic3Builder.setAnalysisEnd(System.currentTimeMillis() / 1000);

    String extension = binary ? "dat" : "txt";
    String path =
        String.format("%s/%s_%s.%s", protobufDestination, ic3Builder.getName(),
            ic3Builder.getVersion(), extension);
    System.out.println("PATH: " + path);
    if (binary) {
      FileOutputStream fileOutputStream = new FileOutputStream(path);
      ic3Builder.build().writeTo(fileOutputStream);
      fileOutputStream.close();
    } else {
      FileWriter fileWriter = new FileWriter(path);
      TextFormat.print(ic3Builder, fileWriter);
      fileWriter.close();
    }

    Timers.v().totalTimer.end();

    String statistics =
        appName + " " + analysisClassesCount + " " + PropagationTimers.v().reachableMethods + " "
            + preciseNonLinking[0] + " " + preciseNonLinking[3] + " " + preciseNonLinking[1] + " "
            + preciseNonLinking[2] + " " + preciseLinking[0] + " " + preciseLinking[3] + " "
            + preciseLinking[1] + " " + preciseLinking[2] + " " + imprecise[0] + " " + imprecise[3]
            + " " + imprecise[1] + " " + imprecise[2] + " " + bottom[0] + " " + bottom[1] + " "
            + bottom[2] + " " + top[0] + " " + top[1] + " " + top[2] + " " + nonexistent[0] + " "
            + nonexistent[1] + " " + nonexistent[2] + " " + providerArgument + " " + imprecise[4]
            + " " + preciseFieldValueCount[0] + " " + preciseFieldValueCount[1] + " "
            + preciseFieldValueCount[2] + " " + partiallyPreciseFieldValueCount[0] + " "
            + partiallyPreciseFieldValueCount[1] + " " + partiallyPreciseFieldValueCount[2] + " "
            + impreciseFieldValueCount[0] + " " + impreciseFieldValueCount[1] + " "
            + impreciseFieldValueCount[2] + " " + PropagationTimers.v().modelParsing.getTime()
            + " " + Timers.v().mainGeneration.getTime() + " "
            + Timers.v().entryPointMapping.getTime() + " " + Timers.v().classLoading.getTime()
            + " " + PropagationTimers.v().problemGeneration.getTime() + " "
            + PropagationTimers.v().ideSolution.getTime() + " "
            + PropagationTimers.v().valueComposition.getTime() + " "
            + PropagationTimers.v().resultGeneration.getTime() + " "
            + (PropagationTimers.v().soot.getTime() - PropagationTimers.v().totalTimer.getTime())
            + " " + (Timers.v().misc.getTime() + PropagationTimers.v().misc.getTime()) + " "
            + Timers.v().totalTimer.getTime() + "\n";

    if (logger.isInfoEnabled()) {
      logger.info(statistics);
    }
    if (writer != null) {
      writer.write(statistics);
      writer.close();
    }
  }

  @SuppressWarnings("unchecked")
  private void writeResultToProtobuf(Result result, Ic3Data.Application.Builder ic3Builder,
      Map<String, Component.Builder> componentNameToBuilderMap) {
    Map<String, Set<String>> componentToExtrasMap = new HashMap<>();
    Map<String, ManifestComponent> dynamicReceivers = new HashMap<>();
    Map<SootMethod, Set<String>> entryPointMap = ((Ic3Result) result).getEntryPointMap();

    for (Map.Entry<Unit, Map<Integer, Object>> entry : result.getResults().entrySet()) {
      Unit unit = entry.getKey();

      Argument[] arguments = Model.v().getArgumentsForQuery((Stmt) unit);

      if (arguments != null) {
        SootMethod method = AnalysisParameters.v().getIcfg().getMethodOf(unit);
        int unitId = getIdForUnit(unit, method);
        Map<String, Object> valueMap = new HashMap<>(arguments.length);
        Map<Integer, Object> argnumToValueMap = entry.getValue();

        for (Argument argument : arguments) {
          valueMap.put(argument.getProperty("valueType"),
              argnumToValueMap.get(argument.getArgnum()[0]));
        }

        String className = method.getDeclaringClass().getName();
        String methodSignature = method.getSignature();
        if (valueMap.containsKey("activity")) {
          insertProtobufExitPoint(className, methodSignature, unitId,
              (BasePropagationValue) valueMap.get("activity"), ComponentKind.ACTIVITY, null, null,
              entryPointMap.get(method), componentNameToBuilderMap);
        } else if (valueMap.containsKey("service")) {
          insertProtobufExitPoint(className, methodSignature, unitId,
              (BasePropagationValue) valueMap.get("service"), ComponentKind.SERVICE, null, null,
              entryPointMap.get(method), componentNameToBuilderMap);
        } else if (valueMap.containsKey("receiver")) {
          insertProtobufExitPoint(className, methodSignature, unitId,
              (BasePropagationValue) valueMap.get("receiver"), ComponentKind.RECEIVER,
              (Set<String>) valueMap.get("permission"), null, entryPointMap.get(method),
              componentNameToBuilderMap);
        } else if (valueMap.containsKey("intentFilter")) {
          insertDynamicReceiver(dynamicReceivers, (Set<String>) valueMap.get("permission"),
              (Set<String>) valueMap.get("receiverType"),
              (BasePropagationValue) valueMap.get("intentFilter"));
        } else if (valueMap.containsKey("provider")) {
          insertProtobufExitPoint(className, methodSignature, unitId,
              (BasePropagationValue) valueMap.get("provider"), ComponentKind.PROVIDER, null, null,
              entryPointMap.get(method), componentNameToBuilderMap);
        } else if (valueMap.containsKey("authority")) {
          insertProtobufExitPoint(className, methodSignature, unitId,
              getUriValueForAuthorities((Set<String>) valueMap.get("authority")),
              ComponentKind.PROVIDER, null, null, entryPointMap.get(method),
              componentNameToBuilderMap);
        } else if (valueMap.containsKey("pendingIntent")) {
          BasePropagationValue baseCollectingValue =
              (BasePropagationValue) valueMap.get("pendingIntent");
          String targetType =
              baseCollectingValue instanceof PropagationValue ? (String) ((PropagationValue) baseCollectingValue)
                  .getValuesForField("targetType").iterator().next().getValue()
                  : null;
          Set<String> permissions = (Set<String>) valueMap.get("permission");
          if (targetType != null) {
            insertProtobufExitPoint(className, methodSignature, unitId, baseCollectingValue,
                stringToComponentKind(targetType), permissions, null, entryPointMap.get(method),
                componentNameToBuilderMap);
          } else {
            for (ComponentKind target : Arrays.asList(ComponentKind.ACTIVITY,
                ComponentKind.RECEIVER, ComponentKind.SERVICE)) {
              insertProtobufExitPoint(className, methodSignature, unitId, baseCollectingValue,
                  target, null, null, entryPointMap.get(method), componentNameToBuilderMap);
            }
          }
        } else if (valueMap.containsKey("componentExtra")) {
          Set<String> extras = (Set<String>) valueMap.get("componentExtra");
          if (extras != null) {
            for (String component : entryPointMap.get(method)) {
              Set<String> existingExtras = componentToExtrasMap.get(component);
              if (existingExtras == null) {
                existingExtras = new HashSet<>();
                componentToExtrasMap.put(component, existingExtras);
              }
              existingExtras.addAll(extras);
            }
          }
        }
      }
    }

    for (Map.Entry<String, Set<String>> entry : componentToExtrasMap.entrySet()) {
      componentNameToBuilderMap.get(entry.getKey()).addAllExtras(entry.getValue());
    }

    for (Component.Builder componentBuilder : componentNameToBuilderMap.values()) {
      ic3Builder.addComponents(componentBuilder);
    }

    for (ManifestComponent manifestComponent : dynamicReceivers.values()) {
      ic3Builder.addComponents(ManifestPullParser.makeProtobufComponentBuilder(manifestComponent,
          ComponentKind.DYNAMIC_RECEIVER));
    }
  }

  private void insertProtobufExitPoint(String className, String method, int instruction,
      BasePropagationValue intentValue, ComponentKind componentKind, Set<String> intentPermissions,
      Integer missingIntents, Set<String> exitPointComponents,
      Map<String, Component.Builder> componentNameToBuilderMap) {
    for (String exitPointComponent : exitPointComponents) {
      ExitPoint.Builder exitPointBuilder = ExitPoint.newBuilder();
      exitPointBuilder.setClassName(className).setMethod(method).setInstruction(instruction)
          .setKind(componentKind);
      PropagationValue collectingValue = null;
      if (intentValue == null || intentValue instanceof TopPropagationValue
          || intentValue instanceof BottomPropagationValue) {
        missingIntents = 0;
      } else if (intentValue instanceof PropagationValue) {
        collectingValue = (PropagationValue) intentValue;
        if (collectingValue.getPathValues() == null || collectingValue.getPathValues().size() == 0) {
          missingIntents = 0;
        }
      } else {
        throw new RuntimeException("Unknown CollectingValue type: " + intentValue.getClass());
      }

      if (missingIntents != null) {
        exitPointBuilder.setMissing(missingIntents);
      } else {
        Set<PathValue> pathValues = collectingValue.getPathValues();
        if (pathValues != null) {
          for (PathValue pathValue : pathValues) {
            if (componentKind.equals(ComponentKind.PROVIDER)) {
              exitPointBuilder.addUris(makeProtobufUriBuilder(pathValue));
            } else {
              if (intentPermissions != null && intentPermissions.size() != 0) {
                for (String intentPermission : intentPermissions) {
                  exitPointBuilder.addIntents(makeProtobufIntentBuilder(pathValue).setPermission(
                      intentPermission));
                }
              } else {
                exitPointBuilder.addIntents(makeProtobufIntentBuilder(pathValue));
              }
            }
          }
        }
      }

      Component.Builder componentBuilder = componentNameToBuilderMap.get(exitPointComponent);
      componentBuilder.addExitPoints(exitPointBuilder);
    }
  }

  private Intent.Builder makeProtobufIntentBuilder(PathValue intentValue) {
    Intent.Builder intentBuilder = Intent.newBuilder();

    insertSingleValuedIntentAttribute(intentValue, "action", AttributeKind.ACTION, intentBuilder);

    Set<String> categories = intentValue.getSetStringFieldValue("categories");
    if (categories != null) {
      if (categories.contains(null)) {
        categories.remove(null);
        categories.add(Constants.NULL_STRING);
      }
      intentBuilder.addAttributes(Attribute.newBuilder().setKind(AttributeKind.CATEGORY)
          .addAllValue(categories));
    }

    Set<Integer> flags = intentValue.getSetFieldValue("flags", Integer.class);
    if (flags != null) {
      intentBuilder.addAttributes(Attribute.newBuilder().setKind(AttributeKind.FLAG)
          .addAllIntValue(flags));
    }

    // String mimeType = intentValue.getSingleStringFieldValue("dataType");
    // if (mimeType != null) {
    // String[] typeParts = mimeType.split("/");
    // String type;
    // String subtype;
    // if (typeParts.length == 2) {
    // type = typeParts[0];
    // subtype = typeParts[1];
    // } else {
    // type = Constants.ANY_STRING;
    // subtype = Constants.ANY_STRING;
    // }
    // intentBuilder
    // .addAttributes(Attribute.newBuilder().setKind(AttributeKind.TYPE).addValue(type));
    // intentBuilder.addAttributes(Attribute.newBuilder().setKind(AttributeKind.SUBTYPE)
    // .addValue(subtype));
    // }
    insertSingleValuedIntentAttribute(intentValue, "dataType", AttributeKind.TYPE, intentBuilder);

    Set<String> extras = intentValue.getSetStringFieldValue("extras");
    if (extras != null) {
      if (extras.contains(null)) {
        extras.remove(null);
        extras.add(Constants.NULL_STRING);
      }
      intentBuilder.addAttributes(Attribute.newBuilder().setKind(AttributeKind.EXTRA)
          .addAllValue(extras));
    }

    insertSingleValuedIntentAttribute(intentValue, "clazz", AttributeKind.CLASS, intentBuilder);
    insertSingleValuedIntentAttribute(intentValue, "package", AttributeKind.PACKAGE, intentBuilder);
    insertSingleValuedIntentAttribute(intentValue, "scheme", AttributeKind.SCHEME, intentBuilder);
    insertSingleValuedIntentAttribute(intentValue, "ssp", AttributeKind.SSP, intentBuilder);
    insertSingleValuedIntentAttribute(intentValue, "uri", AttributeKind.URI, intentBuilder);
    insertSingleValuedIntentAttribute(intentValue, "path", AttributeKind.PATH, intentBuilder);
    insertSingleValuedIntentAttribute(intentValue, "query", AttributeKind.QUERY, intentBuilder);
    insertSingleValuedIntentAttribute(intentValue, "authority", AttributeKind.AUTHORITY,
        intentBuilder);

    return intentBuilder;
  }

  private void insertSingleValuedIntentAttribute(PathValue pathValue, String attribute,
      AttributeKind kind, Intent.Builder intentBuilder) {
    String attributeValue = pathValue.getScalarStringFieldValue(attribute);
    if (attributeValue != null) {
      intentBuilder.addAttributes(Attribute.newBuilder().setKind(kind).addValue(attributeValue));
    }
  }

  private Uri.Builder makeProtobufUriBuilder(PathValue uriValue) {
    Uri.Builder uriBuilder = Uri.newBuilder();
    insertSingleValuedUriAttribute(uriValue, "scheme", AttributeKind.SCHEME, uriBuilder);
    insertSingleValuedUriAttribute(uriValue, "ssp", AttributeKind.SSP, uriBuilder);
    insertSingleValuedUriAttribute(uriValue, "uri", AttributeKind.URI, uriBuilder);
    insertSingleValuedUriAttribute(uriValue, "path", AttributeKind.PATH, uriBuilder);
    insertSingleValuedUriAttribute(uriValue, "query", AttributeKind.QUERY, uriBuilder);
    insertSingleValuedUriAttribute(uriValue, "authority", AttributeKind.AUTHORITY, uriBuilder);

    return uriBuilder;
  }

  private void insertSingleValuedUriAttribute(PathValue pathValue, String attribute,
      AttributeKind kind, Uri.Builder uriBuilder) {
    String attributeValue = pathValue.getScalarStringFieldValue(attribute);
    if (attributeValue != null) {
      uriBuilder.addAttributes(Attribute.newBuilder().setKind(kind).addValue(attributeValue));
    }
  }

  private ComponentKind stringToComponentKind(String componentKind) {
    switch (componentKind) {
      case "a":
        return ComponentKind.ACTIVITY;
      case "s":
        return ComponentKind.SERVICE;
      case "r":
        return ComponentKind.RECEIVER;
      default:
        throw new RuntimeException("Unknown component kind: " + componentKind);
    }
  }

  private void insertDynamicReceiver(Map<String, ManifestComponent> dynamicReceivers,
      Set<String> permissions, Set<String> receiverTypes, BasePropagationValue intentFilters) {
    if (permissions == null) {
      permissions = Collections.singleton(null);
    }

    for (String receiverType : receiverTypes) {
      for (String permission : permissions) {
        insertDynamicReceiverHelper(dynamicReceivers, permission, receiverType, intentFilters);
      }
    }
  }

  private void insertDynamicReceiverHelper(Map<String, ManifestComponent> dynamicReceivers,
      String permission, String receiverType, BasePropagationValue intentFilters) {
    Integer missingIntentFilters;
    Set<ManifestIntentFilter> manifestIntentFilters;

    if (intentFilters == null || intentFilters instanceof TopPropagationValue
        || intentFilters instanceof BottomPropagationValue) {
      missingIntentFilters = 0;
      manifestIntentFilters = null;
    } else if (intentFilters instanceof PropagationValue) {
      missingIntentFilters = null;
      PropagationValue collectingValue = (PropagationValue) intentFilters;
      manifestIntentFilters = new HashSet<>();
      for (PathValue branchValue : collectingValue.getPathValues()) {
        Integer filterPriority = null;
        FieldValue priorityFieldValue = branchValue.getFieldValue("priority");
        if (priorityFieldValue != null) {
          filterPriority = (Integer) priorityFieldValue.getValue();
        }
        manifestIntentFilters.add(new ManifestIntentFilter(branchValue
            .getSetStringFieldValue("actions"), branchValue.getSetStringFieldValue("categories"),
            false, makeManifestData(branchValue), filterPriority));

      }
    } else {
      throw new RuntimeException("Unknown intent filter type: " + intentFilters.getClass());
    }

    ManifestComponent manifestComponent = dynamicReceivers.get(receiverType);
    if (manifestComponent == null) {
      manifestComponent =
          new ManifestComponent(
              edu.psu.cse.siis.ic3.db.Constants.ComponentShortType.DYNAMIC_RECEIVER, receiverType,
              true, true, permission, null, missingIntentFilters);
      dynamicReceivers.put(receiverType, manifestComponent);
    }

    manifestComponent.addIntentFilters(manifestIntentFilters);
  }

  private List<ManifestData> makeManifestData(PathValue branchValue) {
    Set<String> mimeTypes = branchValue.getSetStringFieldValue("dataType");
    Set<DataAuthority> authorities =
        branchValue.getSetFieldValue("authorities", DataAuthority.class);
    Set<String> paths = branchValue.getSetStringFieldValue("paths");
    Set<String> schemes = branchValue.getSetStringFieldValue("schemes");

    if (mimeTypes == null && authorities == null && paths == null && schemes == null) {
      return null;
    }

    if (mimeTypes == null) {
      mimeTypes = Collections.singleton(null);
    }
    if (authorities == null) {
      authorities = Collections.singleton(new DataAuthority(null, null));
    }
    if (paths == null) {
      paths = Collections.singleton(null);
    }
    if (schemes == null) {
      schemes = Collections.singleton(null);
    }

    List<ManifestData> result = new ArrayList<>();
    for (String mimeType : mimeTypes) {
      for (DataAuthority dataAuthority : authorities) {
        for (String dataPath : paths) {
          for (String scheme : schemes) {
            result.add(new ManifestData(scheme, dataAuthority.getHost(), dataAuthority.getPort(),
                dataPath, mimeType));
          }
        }
      }
    }

    return result;
  }

  private BasePropagationValue getUriValueForAuthorities(Set<String> authorities) {
    if (authorities == null) {
      return null;
    }

    PropagationValue collectingValue = new PropagationValue();
    for (String authority : authorities) {
      PathValue branchValue = new PathValue();
      ScalarFieldValue schemeFieldValue = new ScalarFieldValue("content");
      branchValue.addFieldEntry("scheme", schemeFieldValue);
      ScalarFieldValue authorityFieldValue = new ScalarFieldValue(authority);
      branchValue.addFieldEntry("authority", authorityFieldValue);
      collectingValue.addPathValue(branchValue);
    }

    return collectingValue;
  }

  private int getIdForUnit(Unit unit, SootMethod method) {
    int id = 0;
    for (Unit currentUnit : method.getActiveBody().getUnits()) {
      if (currentUnit == unit) {
        return id;
      }
      ++id;
    }

    return -1;
  }

  @SuppressWarnings("unchecked")
  private void analyzeResult(Result result) {
    Set<String> nonLinkingFieldNames = new HashSet<>();
    nonLinkingFieldNames.add("extras");
    nonLinkingFieldNames.add("flags");
    nonLinkingFieldNames.add("fragment");
    nonLinkingFieldNames.add("query");

    for (Map.Entry<Unit, Map<Integer, Object>> entry0 : result.getResults().entrySet()) {
      Collection<Object> argumentValues = entry0.getValue().values();
      boolean top = false;
      boolean bottom = false;
      // This is true only if the linking field are precisely known.
      boolean preciseLinking = true;
      // This is true only if all fields are precisely known.
      boolean preciseNonLinking = true;
      boolean nonexistent = false;
      boolean intentWithUri = false;
      boolean entryPointIntent = false;

      int resultIndex = getResultIndex((Stmt) entry0.getKey());

      for (Object value2 : argumentValues) {
        if (value2 == null) {
          nonexistent = true;
        } else if (value2 instanceof TopPropagationValue) {
          top = true;
        } else if (value2 instanceof BottomPropagationValue) {
          bottom = true;
        } else if (value2 instanceof PropagationValue) {
          PropagationValue collectingValue = (PropagationValue) value2;
          for (PathValue branchValue : collectingValue.getPathValues()) {

            intentWithUri = intentWithUri || isIntentWithUri(branchValue.getFieldMap());

            for (Map.Entry<String, FieldValue> entry : branchValue.getFieldMap().entrySet()) {
              String fieldName = entry.getKey();
              FieldValue fieldValue = entry.getValue();

              if (fieldValue instanceof TopFieldValue) {
                if (nonLinkingFieldNames.contains(fieldName)) {
                  preciseNonLinking = false;
                } else {
                  preciseNonLinking = false;
                  preciseLinking = false;
                }
              } else {
                Object value = fieldValue.getValue();
                if (value == null) {
                  continue;
                }

                if (value instanceof Set) {
                  Set<Object> values = (Set<Object>) value;

                  if (values.contains(Constants.ANY_STRING) || values.contains(Constants.ANY_CLASS)
                      || values.contains(Constants.ANY_INT) || values.contains(ENTRY_POINT_INTENT)
                      || values.contains("top")) {
                    if (values.contains(ENTRY_POINT_INTENT)) {
                      entryPointIntent = true;
                    }
                    preciseNonLinking = false;
                    if (!nonLinkingFieldNames.contains(fieldName)) {
                      preciseLinking = false;
                    }
                  }
                } else {
                  if (value.equals(Constants.ANY_STRING) || value.equals(Constants.ANY_CLASS)
                      || value.equals(Constants.ANY_INT) || value.equals(ENTRY_POINT_INTENT)
                      || value.equals("top")) {
                    if (value.equals(ENTRY_POINT_INTENT)) {
                      entryPointIntent = true;
                    }
                    preciseNonLinking = false;
                    if (!nonLinkingFieldNames.contains(fieldName)) {
                      preciseLinking = false;
                    }
                  }
                }
              }
            }
          }
        }
      }

      if (intentWithUri) {
        ++this.intentWithData;
      }

      if (nonexistent) {
        if (Scene
            .v()
            .getActiveHierarchy()
            .isClassSubclassOfIncluding(
                AnalysisParameters.v().getIcfg().getMethodOf(entry0.getKey()).getDeclaringClass(),
                Scene.v().getSootClass("android.content.ContentProvider"))) {
          ++this.providerArgument;
        } else {
          ++this.nonexistent[resultIndex];
        }
      } else if (top) {
        ++this.top[resultIndex];
      } else if (bottom) {
        ++this.bottom[resultIndex];
      } else if (preciseNonLinking) {
        if (intentWithUri) {
          ++this.preciseNonLinking[3];
        } else {
          ++this.preciseNonLinking[resultIndex];
        }
      } else if (preciseLinking) {
        if (intentWithUri) {
          ++this.preciseLinking[3];
        } else {
          ++this.preciseLinking[resultIndex];
        }
      } else {
        if (entryPointIntent) {
          ++this.imprecise[4];
        } else if (intentWithUri) {
          ++this.imprecise[3];
        } else {
          ++this.imprecise[resultIndex];
        }
      }
    }
  }

  private boolean isIntentWithUri(Map<String, FieldValue> fieldMap) {
    Set<String> fields = fieldMap.keySet();

    if (fields.contains("action") || fields.contains("categories")) {
      if ((fields.contains("uri") && fieldMap.get("uri") != null && fieldMap.get("uri").getValue() != null)
          || (fields.contains("path") && fieldMap.get("path") != null && fieldMap.get("path")
              .getValue() != null)
          || (fields.contains("scheme") && fieldMap.get("scheme") != null && fieldMap.get("scheme")
              .getValue() != null)
          || (fields.contains("ssp") && fieldMap.get("ssp") != null && fieldMap.get("ssp")
              .getValue() != null)) {
        return true;
      }
    }

    return false;
  }

  private int getResultIndex(Stmt stmt) {
    InvokeExpr invokeExpr = stmt.getInvokeExpr();
    List<Type> types = invokeExpr.getMethod().getParameterTypes();

    for (Type type : types) {
      if (type.toString().equals("android.content.IntentFilter")) {
        return 1;
      } else if (type.toString().equals("android.net.Uri")) {
        return 2;
      }
    }

    return 0;
  }

  private boolean containsPartialDefinition(Set<Object> values) {
    for (Object value : values) {
      if (value instanceof String && ((String) value).contains("(.*)")) {
        return true;
      }
    }

    return false;
  }
}
