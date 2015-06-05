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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import soot.Unit;
import soot.Value;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import edu.psu.cse.siis.coal.Constants;
import edu.psu.cse.siis.coal.arguments.Argument;
import edu.psu.cse.siis.coal.arguments.ArgumentValueAnalysis;
import edu.psu.cse.siis.coal.arguments.ArgumentValueManager;

public class AuthorityValueAnalysis extends ArgumentValueAnalysis {

  private static final Object TOP_VALUE = new DataAuthority(Constants.ANY_STRING,
      Constants.ANY_STRING);

  @Override
  public Set<Object> computeArgumentValues(Argument argument, Unit callSite) {
    ArgumentValueAnalysis stringAnalysis =
        ArgumentValueManager.v().getArgumentValueAnalysis(
            Constants.DefaultArgumentTypes.Scalar.STRING);

    Stmt stmt = (Stmt) callSite;
    if (!stmt.containsInvokeExpr()) {
      throw new RuntimeException("Statement " + stmt + " does not contain an invoke expression");
    }
    InvokeExpr invokeExpr = stmt.getInvokeExpr();

    Set<Object> hosts =
        stringAnalysis.computeVariableValues(invokeExpr.getArg(argument.getArgnum()[0]), stmt);
    Set<Object> ports =
        stringAnalysis.computeVariableValues(invokeExpr.getArg(argument.getArgnum()[1]), stmt);

    Set<Object> result = new HashSet<>();
    for (Object host : hosts) {
      for (Object port : ports) {
        result.add(new DataAuthority((String) host, (String) port));
      }
    }

    return result;
  }

  @Override
  public Set<Object> computeInlineArgumentValues(String[] inlineValue) {
    return new HashSet<Object>(Arrays.asList(inlineValue));
  }

  @Override
  public Object getTopValue() {
    return TOP_VALUE;
  }

  @Override
  public Set<Object> computeVariableValues(Value value, Stmt callSite) {
    throw new RuntimeException("Should not be reached.");
  }

}
