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
import java.util.Set;

import soot.Unit;
import edu.psu.cse.siis.coal.Constants;
import edu.psu.cse.siis.coal.arguments.Argument;
import edu.psu.cse.siis.coal.arguments.ArgumentValueAnalysis;

public class ContextValueAnalysis implements ArgumentValueAnalysis {

  private static final String TOP_VALUE = Constants.ANY_STRING;

  private final String appName;

  public ContextValueAnalysis(String appName) {
    this.appName = appName != null ? appName : Constants.ANY_STRING;
  }

  @Override
  public Set<Object> computeArgumentValues(Argument argument, Unit callSite) {
    return Collections.singleton((Object) this.appName);
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
