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
import edu.psu.cse.siis.coal.Constants;
import edu.psu.cse.siis.coal.arguments.Argument;
import edu.psu.cse.siis.coal.arguments.ArgumentValueAnalysis;
import edu.psu.cse.siis.coal.arguments.ArgumentValueManager;

public class AuthorityValueAnalysis implements ArgumentValueAnalysis {

  private static final Object TOP_VALUE = new DataAuthority(Constants.ANY_STRING,
      Constants.ANY_STRING);

  @Override
  public Set<Object> computeArgumentValues(Argument argument, Unit callSite) {
    Argument argument0 = new Argument(argument);
    argument0.setArgnum(new int[] { argument.getArgnum()[0] });
    argument0.setType(Constants.DefaultArgumentTypes.STRING);
    Argument argument1 = new Argument(argument);
    argument1.setArgnum(new int[] { argument.getArgnum()[1] });
    argument1.setType(Constants.DefaultArgumentTypes.STRING);

    Set<Object> hosts = ArgumentValueManager.v().getArgumentValues(argument0, callSite);
    Set<Object> ports = ArgumentValueManager.v().getArgumentValues(argument1, callSite);
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

}
