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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import soot.SootMethod;
import soot.Unit;
import edu.psu.cse.siis.coal.AnalysisParameters;
import edu.psu.cse.siis.coal.Result;
import edu.psu.cse.siis.coal.arguments.Argument;

public class Ic3Result extends Result {

  private final Map<SootMethod, Set<String>> entryPointMap;
  private final Map<Unit, Map<Integer, Object>> result = new HashMap<>();
  private String statistics;

  public Ic3Result(Map<SootMethod, Set<String>> entryPointMap) {
    this.entryPointMap = entryPointMap;
  }

  @Override
  public Map<Unit, Map<Integer, Object>> getResults() {
    return result;
  }

  @Override
  public Object getResult(Unit unit, Argument argument) {
    Map<Integer, Object> unitResult = result.get(unit);
    if (unitResult != null) {
      return unitResult.get(argument.getArgnum());
    }
    return null;
  }

  @Override
  public String getStatistics() {
    return statistics;
  }

  public Map<SootMethod, Set<String>> getEntryPointMap() {
    return entryPointMap;
  }

  @Override
  public void addResult(Unit unit, int argnum, Object value) {
    Map<Integer, Object> unitResult = result.get(unit);
    if (unitResult == null) {
      unitResult = new HashMap<>();
      result.put(unit, unitResult);
    }
    unitResult.put(argnum, value);
  }

  @Override
  public void setStatistics(String statistics) {
    this.statistics = statistics;
  }

  public void dump() {
    System.out.println("*****Result*****");
    List<String> results = new ArrayList<>();
    boolean outputComponents = entryPointMap != null;

    for (Map.Entry<Unit, Map<Integer, Object>> entry : result.entrySet()) {
      Unit unit = entry.getKey();
      SootMethod method = AnalysisParameters.v().getIcfg().getMethodOf(unit);
      String current =
          method.getDeclaringClass().getName() + "/" + method.getSubSignature() + " : " + unit
              + "\n";

      if (outputComponents) {
        Set<String> components = entryPointMap.get(method);
        if (components != null) {
          current += "Components: " + components + "\n";
        } else {
          current += "Unknown components" + "\n";
        }
      }

      for (Map.Entry<Integer, Object> entry2 : entry.getValue().entrySet()) {
        current += "    " + entry2.getKey() + " : " + entry2.getValue() + "\n";
      }
      results.add(current);
    }
    Collections.sort(results);

    for (String result : results) {
      System.out.println(result);
    }
  }
}
