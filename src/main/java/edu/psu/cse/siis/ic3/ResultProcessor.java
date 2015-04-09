package edu.psu.cse.siis.ic3;

import java.io.IOException;
import java.io.Writer;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
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
import edu.psu.cse.siis.coal.AnalysisParameters;
import edu.psu.cse.siis.coal.Constants;
import edu.psu.cse.siis.coal.Model;
import edu.psu.cse.siis.coal.PropagationTimers;
import edu.psu.cse.siis.coal.Result;
import edu.psu.cse.siis.coal.Results;
import edu.psu.cse.siis.coal.arguments.Argument;
import edu.psu.cse.siis.coal.field.values.FieldValue;
import edu.psu.cse.siis.coal.field.values.TopFieldValue;
import edu.psu.cse.siis.coal.values.BasePropagationValue;
import edu.psu.cse.siis.coal.values.BottomPropagationValue;
import edu.psu.cse.siis.coal.values.PathValue;
import edu.psu.cse.siis.coal.values.PropagationValue;
import edu.psu.cse.siis.coal.values.TopPropagationValue;
import edu.psu.cse.siis.ic3.db.DbConnection;
import edu.psu.cse.siis.ic3.db.SQLConnection;
import edu.psu.cse.siis.ic3.manifest.ManifestComponent;
import edu.psu.cse.siis.ic3.manifest.ManifestData;
import edu.psu.cse.siis.ic3.manifest.ManifestIntentFilter;

public class ResultProcessor {
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

  public void processResult(boolean writeToDb, String appName,
      Map<String, Integer> componentToIdMap, int analysisClassesCount, Writer writer)
      throws IOException, SQLException {
    for (Result result : Results.getResults()) {
      ((Ic3Result) result).dump();
      analyzeResult(result);
      if (writeToDb) {
        writeResultToDb(result, componentToIdMap);
      }
    }

    if (writeToDb) {
      SQLConnection.closeConnection();
    }

    Timers.v().totalTimer.end();

    String statistics =
        appName + " " + analysisClassesCount + " " + PropagationTimers.v().reachableMethods + " "
            + PropagationTimers.v().reachableStatements + " " + preciseNonLinking[0] + " "
            + preciseNonLinking[3] + " " + preciseNonLinking[1] + " " + preciseNonLinking[2] + " "
            + preciseLinking[0] + " " + preciseLinking[3] + " " + preciseLinking[1] + " "
            + preciseLinking[2] + " " + imprecise[0] + " " + imprecise[3] + " " + imprecise[1]
            + " " + imprecise[2] + " " + bottom[0] + " " + bottom[1] + " " + bottom[2] + " "
            + top[0] + " " + top[1] + " " + top[2] + " " + nonexistent[0] + " " + nonexistent[1]
            + " " + nonexistent[2] + " " + providerArgument + " " + imprecise[4] + " "
            + preciseFieldValueCount[0] + " " + preciseFieldValueCount[1] + " "
            + preciseFieldValueCount[2] + " " + partiallyPreciseFieldValueCount[0] + " "
            + partiallyPreciseFieldValueCount[1] + " " + partiallyPreciseFieldValueCount[2] + " "
            + impreciseFieldValueCount[0] + " " + impreciseFieldValueCount[1] + " "
            + impreciseFieldValueCount[2] + " " + PropagationTimers.v().pathValues + " "
            + PropagationTimers.v().modelParsing.getTime() + " "
            + Timers.v().mainGeneration.getTime() + " " + Timers.v().entryPointMapping.getTime()
            + " " + Timers.v().classLoading.getTime() + " "
            + PropagationTimers.v().problemGeneration.getTime() + " "
            + PropagationTimers.v().ideSolution.getTime() + " "
            + PropagationTimers.v().valueComposition.getTime() + " "
            + PropagationTimers.v().resultGeneration.getTime() + " "
            + (PropagationTimers.v().soot.getTime() - PropagationTimers.v().totalTimer.getTime())
            + " " + (Timers.v().misc.getTime() + PropagationTimers.v().misc.getTime()) + " "
            /* + PropagationTimers.v().argumentValueTime + " " */+ Timers.v().totalTimer.getTime()
            + "\n";

    if (logger.isInfoEnabled()) {
      logger.info(statistics);
    }
    writer.write(statistics);
    writer.close();
  }

  @SuppressWarnings("unchecked")
  private void writeResultToDb(Result result, Map<String, Integer> componentToIdMap)
      throws SQLException {
    for (Map.Entry<Unit, Map<Integer, Object>> entry : result.getResults().entrySet()) {
      Unit unit = entry.getKey();

      Argument[] arguments = Model.v().getArgumentsForQuery((Stmt) unit);
      Map<SootMethod, Set<String>> entryPointMap = ((Ic3Result) result).getEntryPointMap();

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
          DbConnection.insertIntentAtExitPoint(className, methodSignature, unitId,
              (BasePropagationValue) valueMap.get("activity"),
              edu.psu.cse.siis.ic3.db.Constants.ComponentShortType.ACTIVITY, null, null,
              entryPointMap.get(method), componentToIdMap);
        } else if (valueMap.containsKey("service")) {
          DbConnection.insertIntentAtExitPoint(className, methodSignature, unitId,
              (BasePropagationValue) valueMap.get("service"),
              edu.psu.cse.siis.ic3.db.Constants.ComponentShortType.SERVICE, null, null,
              entryPointMap.get(method), componentToIdMap);
        } else if (valueMap.containsKey("receiver")) {
          DbConnection.insertIntentAtExitPoint(className, methodSignature, unitId,
              (BasePropagationValue) valueMap.get("receiver"),
              edu.psu.cse.siis.ic3.db.Constants.ComponentShortType.RECEIVER,
              (Set<String>) valueMap.get("permission"), null, entryPointMap.get(method),
              componentToIdMap);
        } else if (valueMap.containsKey("intentFilter")) {
          insertDynamicReceiver((Set<String>) valueMap.get("permission"),
              (Set<String>) valueMap.get("receiverType"),
              (BasePropagationValue) valueMap.get("intentFilter"));
        } else if (valueMap.containsKey("provider")) {
          DbConnection.insertIntentAtExitPoint(className, methodSignature, unitId,
              (BasePropagationValue) valueMap.get("provider"),
              edu.psu.cse.siis.ic3.db.Constants.ComponentShortType.PROVIDER, null, null,
              entryPointMap.get(method), componentToIdMap);
        } else if (valueMap.containsKey("authority")) {
          DbConnection.insertIntentAtExitPoint(className, methodSignature, unitId,
              getUriValueForAuthorities((Set<String>) valueMap.get("authority")),
              edu.psu.cse.siis.ic3.db.Constants.ComponentShortType.PROVIDER, null, null,
              entryPointMap.get(method), componentToIdMap);
        } else if (valueMap.containsKey("pendingIntent")) {
          BasePropagationValue basePropagationValue =
              (BasePropagationValue) valueMap.get("pendingIntent");
          String targetType =
              basePropagationValue instanceof PropagationValue ? (String) ((PropagationValue) basePropagationValue)
                  .getValuesForField("targetType").iterator().next().getValues().iterator().next()
                  : null;
          Set<String> permissions = (Set<String>) valueMap.get("permission");
          if (targetType != null) {
            DbConnection.insertIntentAtExitPoint(className, methodSignature, unitId,
                basePropagationValue, targetType, permissions, null, entryPointMap.get(method),
                componentToIdMap);
          } else {
            for (String target : Arrays.asList("a", "r", "s")) {
              DbConnection.insertIntentAtExitPoint(className, methodSignature, unitId,
                  basePropagationValue, target, permissions, null, entryPointMap.get(method),
                  componentToIdMap);
            }
          }
        } else if (valueMap.containsKey("componentExtra")) {
          DbConnection.insertComponentExtras(entryPointMap.get(method), componentToIdMap,
              (Set<String>) valueMap.get("componentExtra"));
        }
      }
    }
  }

  private void insertDynamicReceiver(Set<String> permissions, Set<String> receiverTypes,
      BasePropagationValue intentFilters) throws SQLException {
    if (permissions == null) {
      permissions = Collections.singleton(null);
    }

    for (String receiverType : receiverTypes) {
      for (String permission : permissions) {
        insertDynamicReceiverHelper(permission, receiverType, intentFilters);
      }
    }
  }

  private void insertDynamicReceiverHelper(String permission, String receiverType,
      BasePropagationValue intentFilters) throws SQLException {
    Integer missingIntentFilters;
    Set<ManifestIntentFilter> manifestIntentFilters;
    if (intentFilters == null || intentFilters instanceof TopPropagationValue
        || intentFilters instanceof BottomPropagationValue) {
      missingIntentFilters = 0;
      manifestIntentFilters = null;
    } else if (intentFilters instanceof PropagationValue) {
      missingIntentFilters = null;
      PropagationValue propagationValue = (PropagationValue) intentFilters;
      manifestIntentFilters = new HashSet<>();
      for (PathValue branchValue : propagationValue.getPathValues()) {
        Integer filterPriority = null;
        FieldValue priorityFieldValue = branchValue.getFieldValue("priority");
        if (priorityFieldValue != null) {
          filterPriority = (Integer) priorityFieldValue.getValues().iterator().next();
        }
        manifestIntentFilters.add(new ManifestIntentFilter(branchValue
            .getStringFieldValue("actions"), branchValue.getStringFieldValue("categories"), false,
            makeManifestData(branchValue), filterPriority));
      }
    } else {
      throw new RuntimeException("Unknown intent filter type: " + intentFilters.getClass());
    }

    ManifestComponent manifestComponent =
        new ManifestComponent(edu.psu.cse.siis.ic3.db.Constants.ComponentShortType.RECEIVER,
            receiverType, true, true, permission, null, missingIntentFilters);
    manifestComponent.setIntentFilters(manifestIntentFilters);
    SQLConnection.insertIntentFilters(Collections.singletonList(manifestComponent));
  }

  private List<ManifestData> makeManifestData(PathValue branchValue) {
    Set<String> mimeTypes = branchValue.getStringFieldValue("dataType");
    Set<DataAuthority> authorities = branchValue.getFieldValue("authorities", DataAuthority.class);
    Set<String> paths = branchValue.getStringFieldValue("paths");
    Set<String> schemes = branchValue.getStringFieldValue("schemes");

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
      FieldValue schemeFieldValue = new FieldValue();
      schemeFieldValue.addAll(Collections.singleton((Object) "content"));
      branchValue.addFieldEntry("scheme", schemeFieldValue);
      FieldValue authorityFieldValue = new FieldValue();
      authorityFieldValue.addAll(Collections.singleton((Object) authority));
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

  private void analyzeResult(Result result) {
    Set<String> nonLinkingFieldNames = new HashSet<>();
    nonLinkingFieldNames.add("extras");
    nonLinkingFieldNames.add("flags");
    nonLinkingFieldNames.add("fragment");
    nonLinkingFieldNames.add("query");

    for (Map.Entry<Unit, Map<Integer, Object>> entry0 : result.getResults().entrySet()) {
      Map<Integer, Object> value = entry0.getValue();
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

      for (Object value2 : value.values()) {
        if (value2 == null) {
          nonexistent = true;
        } else if (value2 instanceof TopPropagationValue) {
          top = true;
        } else if (value2 instanceof BottomPropagationValue) {
          bottom = true;
        } else if (value2 instanceof PropagationValue) {
          Set<PathValue> pathValues = ((PropagationValue) value2).getPathValues();
          PropagationTimers.v().pathValues += pathValues.size();

          for (PathValue branchValue : pathValues) {

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
                Set<Object> values = fieldValue.getValues();
                if (values == null) {
                  continue;
                } else if (values.contains(Constants.ANY_STRING)
                    || values.contains(Constants.ANY_CLASS)
                    || values.contains(Constants.INVALID_FLAG)
                    || values.contains(ENTRY_POINT_INTENT) || values.contains("(.*)")
                    || values.contains("top")
                /* || containsPartialDefinition(values) */) {
                  for (Object singleFieldValue : values) {
                    if (singleFieldValue.equals(Constants.ANY_STRING)
                        || singleFieldValue.equals(Constants.ANY_CLASS)
                        || singleFieldValue.equals(Constants.INVALID_FLAG)
                        || singleFieldValue.equals("(.*)")) {
                      ++this.impreciseFieldValueCount[resultIndex];
                    } else if (singleFieldValue instanceof String
                        && ((String) singleFieldValue).contains("(.*)")) {
                      ++this.partiallyPreciseFieldValueCount[resultIndex];
                    } else {
                      ++this.preciseFieldValueCount[resultIndex];
                    }
                  }
                  if (values.contains(ENTRY_POINT_INTENT)) {
                    entryPointIntent = true;
                  }
                  if (nonLinkingFieldNames.contains(fieldName)) {
                    preciseNonLinking = false;
                  } else {
                    preciseNonLinking = false;
                    preciseLinking = false;
                  }
                } else {
                  if (containsPartialDefinition(values)) {
                    for (Object singleFieldValue : values) {
                      if (singleFieldValue.equals(Constants.ANY_STRING)
                          || singleFieldValue.equals(Constants.ANY_CLASS)
                          || singleFieldValue.equals(Constants.INVALID_FLAG)
                          || singleFieldValue.equals("(.*)")) {
                        ++this.impreciseFieldValueCount[resultIndex];
                      } else if (singleFieldValue instanceof String
                          && ((String) singleFieldValue).contains("(.*)")) {
                        ++this.partiallyPreciseFieldValueCount[resultIndex];
                      } else {
                        ++this.preciseFieldValueCount[resultIndex];
                      }
                    }
                  } else {
                    this.preciseFieldValueCount[resultIndex] += values.size();
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
      if ((fields.contains("uri") && fieldMap.get("uri") != null
          && fieldMap.get("uri").getValues() != null && fieldMap.get("uri").getValues().size() != 0)
          || (fields.contains("path") && fieldMap.get("path") != null
              && fieldMap.get("path").getValues() != null && fieldMap.get("path").getValues()
              .size() != 0)
          || (fields.contains("scheme") && fieldMap.get("scheme") != null
              && fieldMap.get("scheme").getValues() != null && fieldMap.get("scheme").getValues()
              .size() != 0)
          || (fields.contains("ssp") && fieldMap.get("ssp") != null
              && fieldMap.get("ssp").getValues() != null && fieldMap.get("ssp").getValues().size() != 0)) {
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
