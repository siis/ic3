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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import soot.Scene;
import soot.SootClass;
import soot.Unit;
import soot.jimple.Stmt;
import edu.psu.cse.siis.coal.arguments.Argument;
import edu.psu.cse.siis.coal.arguments.ArgumentValueAnalysis;

public class ClassTypeValueAnalysis implements ArgumentValueAnalysis {

  private static final String BROADCAST_RECEIVER = "android.content.BroadcastReceiver";
  private static final String TOP_VALUE = BROADCAST_RECEIVER;

  @Override
  public Set<Object> computeArgumentValues(Argument argument, Unit callSite) {
    Stmt stmt = (Stmt) callSite;
    String classType = stmt.getInvokeExpr().getArg(argument.getArgnum()[0]).getType().toString();
    if (classType.equals(BROADCAST_RECEIVER)) {
      List<SootClass> subclasses =
          Scene.v().getActiveHierarchy()
              .getSubclassesOf(Scene.v().getSootClass(BROADCAST_RECEIVER));
      Set<Object> subclassStrings = new HashSet<>();
      for (SootClass sootClass : subclasses) {
        subclassStrings.add(sootClass.getName());
      }
      if (subclassStrings.size() == 0) {
        subclassStrings.add(BROADCAST_RECEIVER);
      }
      return subclassStrings;
    }
    return Collections.singleton((Object) classType);
  }

  @Override
  public Set<Object> computeInlineArgumentValues(String[] inlineValue) {
    return new HashSet<Object>(Arrays.asList(inlineValue));
  }

  @Override
  public Object getTopValue() {
    return TOP_VALUE;
  }

}
