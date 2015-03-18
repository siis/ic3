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

import java.util.Objects;

public class Pair<T, U> {
  protected T o1;
  protected U o2;

  public Pair() {
    o1 = null;
    o2 = null;
  }

  public Pair(T o1, U o2) {
    this.o1 = o1;
    this.o2 = o2;
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.o1, this.o2);
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof Pair)) {
      return false;
    }
    Pair<?, ?> pair = (Pair<?, ?>) other;
    return Objects.equals(this.o1, pair.o1) && Objects.equals(this.o2, pair.o2);
  }

  @Override
  public String toString() {
    return "Pair " + o1 + "," + o2;
  }

  public T getO1() {
    return o1;
  }

  public U getO2() {
    return o2;
  }

  public void setO1(T no1) {
    o1 = no1;
  }

  public void setO2(U no2) {
    o2 = no2;
  }

  public void setPair(T no1, U no2) {
    o1 = no1;
    o2 = no2;
  }
}
