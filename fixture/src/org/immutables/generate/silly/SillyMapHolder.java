/*
    Copyright 2013-2014 Immutables.org authors

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package org.immutables.generate.silly;

import java.lang.annotation.RetentionPolicy;
import java.util.Map;
import java.util.Set;
import org.immutables.annotation.GenerateConstructorParameter;
import org.immutables.annotation.GenerateImmutable;
import org.immutables.annotation.GenerateMarshaled;
import org.immutables.annotation.GenerateMarshaler;

@GenerateImmutable
@GenerateMarshaler(importRoutines = SillyMarshalingRoutines.class)
abstract class SillyMapHolder {

  @GenerateConstructorParameter(order = 0)
  @GenerateMarshaled(forceEmpty = true)
  public abstract Map<SillyValue, Integer> holder1();

  @GenerateConstructorParameter(order = 1)
  public abstract Map<Integer, String> holder2();

  public abstract Map<String, SillyMapTup> holder3();

  public abstract Set<RetentionPolicy> zz();
}
