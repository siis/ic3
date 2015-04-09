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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import soot.MethodOrMethodContext;
import soot.Scene;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.toolkits.callgraph.ReachableMethods;
import soot.toolkits.graph.ExceptionalUnitGraph;
import edu.psu.cse.siis.coal.AnalysisParameters;
import edu.psu.cse.siis.coal.Model;
import edu.psu.cse.siis.coal.PropagationSolver;
import edu.psu.cse.siis.coal.PropagationTimers;
import edu.psu.cse.siis.coal.Result;
import edu.psu.cse.siis.coal.ResultBuilder;
import edu.psu.cse.siis.coal.arguments.Argument;
import edu.psu.cse.siis.coal.arguments.ArgumentValueManager;
import edu.psu.cse.siis.coal.values.BasePropagationValue;
import edu.psu.cse.siis.coal.values.PropagationValue;

public class Ic3ResultBuilder implements ResultBuilder {
  private Map<SootMethod, Set<String>> entryPointMap;

  public void setEntryPointMap(Map<SootMethod, Set<String>> entryPointMap) {
    this.entryPointMap = entryPointMap;
  }

  @Override
  public Result buildResult(PropagationSolver solver) {
    PropagationTimers.v().resultGeneration.start();
    Result result = new Ic3Result(entryPointMap);

    List<MethodOrMethodContext> eps =
        new ArrayList<MethodOrMethodContext>(Scene.v().getEntryPoints());
    ReachableMethods reachableMethods =
        new ReachableMethods(Scene.v().getCallGraph(), eps.iterator(), null);
    reachableMethods.update();
    long reachableStatements = 0;
    for (Iterator<MethodOrMethodContext> iter = reachableMethods.listener(); iter.hasNext();) {
      SootMethod method = iter.next().method();
      if (method.hasActiveBody()
          && !Model.v().isExcludedClass(method.getDeclaringClass().getName())
          && !method.getDeclaringClass().getName().equals("dummyMainClass")
          && !method.getDeclaringClass().getName().startsWith("android.support")
          && !method.getDeclaringClass().getName().startsWith("android.provider")) {
        ++PropagationTimers.v().reachableMethods;
        ExceptionalUnitGraph cfg = new ExceptionalUnitGraph(method.getActiveBody());

        Stack<Unit> stack = new Stack<>();
        for (Unit unit : cfg.getHeads()) {
          stack.push(unit);
        }
        Set<Unit> visited = new HashSet<>();

        while (!stack.empty()) {
          Unit unit = stack.pop();

          if (visited.contains(unit)) {
            continue;
          } else {
            visited.add(unit);
          }

          for (Unit successor : cfg.getSuccsOf(unit)) {
            stack.push(successor);
          }

          Argument[] arguments = Model.v().getArgumentsForQuery((Stmt) unit);
          if (arguments != null) {
            boolean foundModeledType = false;
            for (Argument argument : arguments) {
              if (Model.v().isModeledType(argument.getType())) {
                foundModeledType = true;
                break;
              }
            }

            Stmt stmt = (Stmt) unit;
            for (Argument argument : arguments) {
              if (Model.v().isModeledType(argument.getType())) {
                int argnum = argument.getArgnum()[0];
                BasePropagationValue basePropagationValue;
                InvokeExpr invokeExpr = stmt.getInvokeExpr();
                if (argnum >= 0) {
                  basePropagationValue = solver.resultAt(unit, invokeExpr.getArg(argnum));
                } else if (invokeExpr instanceof InstanceInvokeExpr && argnum == -1) {
                  InstanceInvokeExpr instanceInvokeExpr = (InstanceInvokeExpr) invokeExpr;
                  basePropagationValue = solver.resultAt(stmt, instanceInvokeExpr.getBase());
                } else {
                  throw new RuntimeException("Unexpected argument number " + argnum
                      + " for invoke expression " + invokeExpr);
                }
                if (basePropagationValue instanceof PropagationValue) {
                  PropagationValue propagationValue = (PropagationValue) basePropagationValue;

                  PropagationTimers.v().resultGeneration.end();
                  PropagationTimers.v().valueComposition.start();
                  propagationValue.makeFinalValue(solver);
                  PropagationTimers.v().valueComposition.end();
                  PropagationTimers.v().resultGeneration.start();

                  result.addResult(unit, argument.getArgnum()[0], propagationValue);
                } else {
                  result.addResult(unit, argument.getArgnum()[0], basePropagationValue);
                }
              } else if (foundModeledType || AnalysisParameters.v().inferNonModeledTypes()) {
                // We infer non-modeled types if one of the arguments of the query is a modeled type
                // or if the analysis settings tell us to do so.
                result.addResult(unit, argument.getArgnum()[0], ArgumentValueManager.v()
                    .getArgumentValues(argument, unit));
              }
            }
          }
        }

        reachableStatements += visited.size();
      }

      PropagationTimers.v().reachableStatements += reachableStatements;
    }

    PropagationTimers.v().resultGeneration.end();
    return result;
  }
}
