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

import com.google.common.collect.ImmutableMap;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Modifier;
import java.util.Collections;
import javax.ws.rs.POST;
import org.immutables.common.collect.ImmutableOrdinalSet;
import org.junit.Test;
import simple.GetterAnnotation;
import static org.immutables.check.Checkers.*;

public class ValuesTest {

  @Test
  public void generateGetters() throws Exception {
    ImmutableGetters g = ImmutableGetters.builder().ab(0).cd("").ef(true).build();
    check(g.getAb()).is(0);
    check(g.getCd()).is("");
    check(g.isEf());

    ModifiableGetters mg = ModifiableGetters.create().copy(g);
    check(g.getAb()).is(0);
    check(g.getCd()).is("");
    check(g.isEf());
    
    check(ImmutableGetterEncloser.builder().build().getOptional()).isNull();

    check(mg.getClass().getMethod("getCd").isAnnotationPresent(POST.class));
    check(mg.getClass().getMethod("isEf").getAnnotation(GetterAnnotation.class).value()).hasSize(2);
  }

  @Test
  public void ifaceValue() {
    check(ImmutableIfaceValue.builder().getNumber(1).build()).is(ImmutableIfaceValue.of(1));
  }

  @Test
  public void auxiliary() {
    ImmutableIfaceValue includesAuxiliary = ImmutableIfaceValue.builder().getNumber(1).addAuxiliary("x").build();
    ImmutableIfaceValue excludesAuxiliary = ImmutableIfaceValue.of(1);
    check(includesAuxiliary).is(excludesAuxiliary);
    check(includesAuxiliary.hashCode()).is(excludesAuxiliary.hashCode());
    check(includesAuxiliary).asString().not().contains("auxiliary");
  }

  @Test
  public void builderInheritence() {
    check(ImmutableSillyExtendedBuilder.builder().base);
  }

  @Test
  public void withMethods() {
    ImmutableSillyValidatedBuiltValue value = ImmutableSillyValidatedBuiltValue.builder()
        .value(-10)
        .negativeOnly(true)
        .build();

    try {
      value.withValue(10);
      check(false);
    } catch (Exception ex) {
    }

    check(value.withNegativeOnly(false).withValue(5).value()).is(5);
  }

  @Test
  public void withMethodSetsAndMaps() {
    ImmutableSillyMapHolder holder = ImmutableSillyMapHolder.builder()
        .addZz(RetentionPolicy.CLASS)
        .build();

    check(holder.withZz(Collections.<RetentionPolicy>emptySet()).zz()).isEmpty();
    check(holder.withHolder2(ImmutableMap.of(1, "")).holder2().size()).is(1);
  }

  @Test
  public void lazyValue() {
    SillyLazy v = ImmutableSillyLazy.builder().build();

    check(v.counter.get()).is(0);
    check(v.val1()).is(1);
    check(v.counter.get()).is(1);

    check(v.val2()).is(2);
    check(v.val1()).is(1);
    check(v.counter.get()).is(2);
  }

  @Test
  public void packagePrivateClassGeneration() {
    check(Modifier.isPublic(SillyEmpty.class.getModifiers()));
    check(Modifier.isPublic(ImmutableSillyEmpty.class.getModifiers()));
    check(!Modifier.isPublic(SillyMapHolder.class.getModifiers()));
    check(!Modifier.isPublic(ImmutableSillyMapHolder.class.getModifiers()));
  }

  @Test
  public void ordinalValue() {
    ImmutableSillyOrdinal a = ImmutableSillyOrdinal.of("a");
    ImmutableSillyOrdinal b = ImmutableSillyOrdinal.of("b");
    ImmutableSillyOrdinal c = ImmutableSillyOrdinal.of("c");

    checkAll(a.ordinal(), b.ordinal(), c.ordinal()).isOf(0, 1, 2);
    check(ImmutableSillyOrdinal.of("a")).same(a);
    check(ImmutableSillyOrdinal.of("b")).same(b);

    check(a.domain().get(1)).same(b);
    check(a.domain().get(0)).same(a);
    check(a.domain().length()).is(3);
    check(a.domain()).isOf(a, b, c);
  }

  @Test
  public void ordinalDomain() {
    ImmutableSillyOrdinal.Domain domain = new ImmutableSillyOrdinal.Domain();

    ImmutableSillyOrdinal a = ImmutableSillyOrdinal.of("a");

    ImmutableSillyOrdinal a1 = ImmutableSillyOrdinal.builder()
        .domain(domain)
        .name("a")
        .build();

    ImmutableSillyOrdinal a2 = ImmutableSillyOrdinal.builder()
        .domain(domain)
        .name("a")
        .build();

    check(a.domain()).not(domain);
    check(a.domain()).same(ImmutableSillyOrdinal.Domain.get());
    check(a1.domain()).same(domain);

    check(a).not(a1);
    check(a1).same(a2);
    check(domain.length()).is(1);
  }

  @Test
  public void ordinalValueSet() {
    check(ImmutableSillyOrdinalHolder.builder()
        .addSet(ImmutableSillyOrdinal.of("a"))
        .build()
        .set())
        .isA(ImmutableOrdinalSet.class);
  }

  @Test
  public void internedInstanceConstruction() {
    check(ImmutableSillyInterned.of(1, 2)).is(ImmutableSillyInterned.of(1, 2));
    check(ImmutableSillyInterned.of(1, 2)).same(ImmutableSillyInterned.of(1, 2));
    check(ImmutableSillyInterned.of(1, 2)).not(ImmutableSillyInterned.of(2, 2));

    check(ImmutableSillyInterned.builder()
        .arg1(1)
        .arg2(2)
        .build())
        .same(ImmutableSillyInterned.of(1, 2));

    check(ImmutableSillyInterned.of(1, 2).hashCode()).is(ImmutableSillyInterned.of(1, 2).hashCode());
    check(ImmutableSillyInterned.of(1, 2).hashCode()).not(ImmutableSillyInterned.of(2, 2).hashCode());
  }

  @Test(expected = IllegalStateException.class)
  public void cannotBuildWrongInvariants() {
    ImmutableSillyValidatedBuiltValue.builder()
        .value(10)
        .negativeOnly(true)
        .build();
  }

  @Test
  public void canBuildCorrectInvariants() {

    ImmutableSillyValidatedBuiltValue.builder()
        .value(-10)
        .negativeOnly(true)
        .build();

    ImmutableSillyValidatedBuiltValue.builder()
        .value(10)
        .negativeOnly(false)
        .build();

    ImmutableSillyValidatedBuiltValue.builder()
        .value(-10)
        .negativeOnly(false)
        .build();
  }

  @Test(expected = IllegalStateException.class)
  public void cannotConstructWithWrongInvariants() {
    ImmutableSillyValidatedConstructedValue.of(10, true);
  }

  @Test
  public void canConstructWithCorrectInvariants() {
    ImmutableSillyValidatedConstructedValue.of(-10, true);
    ImmutableSillyValidatedConstructedValue.of(10, false);
    ImmutableSillyValidatedConstructedValue.of(-10, false);
  }
}
