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
package org.immutables.generate.internal.processing;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import javax.annotation.Nullable;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.SimpleAnnotationValueVisitor6;
import org.immutables.annotation.GenerateDefault;
import org.immutables.annotation.GenerateGetters;
import org.immutables.annotation.GenerateImmutable;
import org.immutables.annotation.GenerateMarshaler;
import org.immutables.annotation.GenerateRepository;

/**
 * DISCLAIMER: ALL THIS LOGIC IS A PIECE OF CRAP THAT ACCUMULATED OVER TIME.
 * QUALITY OF RESULTED GENERATED CLASSES ALWAYS WAS HIGHEST PRIORITY
 * BUT MODIFIABILITY SUFFERS, SO NEW VERSION WILL REIMPLEMENT IT FROM SCRATCH.
 */
public abstract class GenerateType extends TypeIntrospectionBase {

  private SegmentedName segmentedName;

  public SegmentedName getSegmentedName() {
    return segmentedName;
  }

  public void setSegmentedName(SegmentedName segmentedName) {
    this.segmentedName = segmentedName;
  }

  public String getSimpleName() {
    return segmentedName.simpleName;
  }

  private boolean emptyNesting;

  public boolean isEmptyNesting() {
    return emptyNesting;
  }

  public void setEmptyNesting(boolean emptyNesting) {
    this.emptyNesting = emptyNesting;
  }

  public String getImmutableReferenceName() {
    return "Immutable" + (isHasNestingParent() ? getName() : getSimpleName());
  }

  /**
   * Something less than half of 255 parameter limit in java methods (not counting 2-slot double
   * and long parameters).
   */
  private static final int SOME_RANDOM_LIMIT = 100;

  private GenerateType nestingParent;

  public void setNestingParent(GenerateType nestingParent) {
    this.nestingParent = nestingParent;
  }

  private List<GenerateType> nestedChildren;

  public void setNestedChildren(List<GenerateType> nestedChildren) {
    this.nestedChildren = nestedChildren;
    for (GenerateType child : nestedChildren) {
      child.setNestingParent(this);
    }
  }

  public List<GenerateType> getNestedChildren() {
    return nestedChildren;
  }

  public boolean isHasNestingParent() {
    return nestingParent != null;
  }

  public boolean isHasNestedChildren() {
    return nestedChildren != null;
  }

  @Nullable
  private String validationMethodName;

  @Nullable
  public String getValidationMethodName() {
    return validationMethodName;
  }

  public boolean isIface() {
    return internalTypeElement().getKind() == ElementKind.INTERFACE;
  }

  public String getInheritsKeyword() {
    return isIface() ? "implements" : "extends";
  }

  public <T extends Annotation> T getAnnotationFromThisOrEnclosingElement(Class<T> annotationType) {
    T annotation = internalTypeElement().getAnnotation(annotationType);
    if (annotation == null && nestingParent != null) {
      annotation = nestingParent.internalTypeElement().getAnnotation(annotationType);
    }
    return annotation;
  }

  public boolean isGenerateGetters() {
    return internalTypeElement().getAnnotation(GenerateGetters.class) != null;
  }

  public void setValidationMethodName(@Nullable String validationMethodName) {
    this.validationMethodName = validationMethodName;
  }

  public String getPackageName() {
    return segmentedName.packageName;
  }

  public String getName() {
    return segmentedName.referenceClassName;
  }

  public String getDefName() {
    return isHasNestingParent() ? segmentedName.simpleName : ("Immutable" + segmentedName.simpleName);
  }

  public String getAccessPrefix() {
    return internalTypeElement().getModifiers().contains(Modifier.PUBLIC)
        ? "public "
        : "";
  }

  public boolean isGenerateOrdinalValue() {
    return isOrdinalValue();
  }

  public boolean isUseConstructorOnly() {
    return isUseConstructor() && !isUseBuilder();
  }

  private GenerateImmutable immutableProperties;

  private GenerateImmutable getGenerataeImmutableProperties() {
    if (immutableProperties == null) {
      immutableProperties = internalTypeElement().getAnnotation(GenerateImmutable.class);
    }
    return immutableProperties;
  }

  public boolean isUseCopyMethods() {
    return getGenerataeImmutableProperties().withers()
        && getImplementedAttributes().size() > 0 && getImplementedAttributes().size() < SOME_RANDOM_LIMIT;
  }

  public boolean isUseSingleton() {
    return getGenerataeImmutableProperties().singleton();
  }

  public boolean isUseInterned() {
    return getGenerataeImmutableProperties().intern();
  }

  public boolean isUsePrehashed() {
    return isUseInterned()
        || isGenerateOrdinalValue()
        || getGenerataeImmutableProperties().prehash();
  }

  public boolean isGenerateMarshaled() {
    return (getAnnotationFromThisOrEnclosingElement(GenerateMarshaler.class) != null) || isGenerateDocument();
  }

  public boolean isGenerateDocument() {
    return internalTypeElement().getAnnotation(GenerateRepository.class) != null;
  }

  private Boolean hasAbstractBuilder;

  public boolean isHasAbstractBuilder() {
    if (hasAbstractBuilder == null) {
      boolean abstractBuilderDeclared = false;
      List<? extends Element> enclosedElements = internalTypeElement().getEnclosedElements();
      for (Element element : enclosedElements) {
        if (element.getKind() == ElementKind.CLASS) {
          if (element.getSimpleName().contentEquals("Builder")) {
            // We do not handle here if builder class is abstract static and not private
            // It's all to discretion compilation checking
            abstractBuilderDeclared = true;
            break;
          }
        }
      }

      hasAbstractBuilder = abstractBuilderDeclared;
    }
    return hasAbstractBuilder;
  }

  public String getDocumentName() {
    @Nullable
    GenerateRepository annotation = internalTypeElement().getAnnotation(GenerateRepository.class);
    if (annotation != null && !annotation.value().isEmpty()) {
      return annotation.value();
    }
    return inferDocumentCollectionName(getName());
  }

  private String inferDocumentCollectionName(String name) {
    char[] a = name.toCharArray();
    a[0] = Character.toLowerCase(a[0]);
    return String.valueOf(a);
  }

  private Set<String> importedMarshalledRoutines;

  public Set<String> getGenerateMarshaledImportRoutines() throws Exception {
    if (importedMarshalledRoutines == null) {
      Set<String> imports = Sets.newLinkedHashSet();

      for (GenerateAttribute a : filteredAttributes()) {
        imports.addAll(a.getMarshaledImportRoutines());
      }

      collectImportRoutines(imports);
      importedMarshalledRoutines = ImmutableSet.copyOf(imports);
    }

    return importedMarshalledRoutines;
  }

  private void collectImportRoutines(Set<String> imports) {
    Element element = internalTypeElement();
    for (;;) {
      imports.addAll(
          extractClassNamesFromMirrors(GenerateMarshaler.class,
              "importRoutines",
              element.getAnnotationMirrors()));

      Element enclosingElement = element.getEnclosingElement();
      if (enclosingElement == null || element instanceof PackageElement) {
        break;
      }
      element = enclosingElement;
    }
  }

  @Nullable
  public GenerateAttribute getIdAttribute() {
    for (GenerateAttribute attribute : getImplementedAttributes()) {
      if (attribute.getMarshaledName().equals("_id")) {
        return attribute;
      }
    }
    return null;
  }

  private Set<String> generateMarshaledTypes;

  public Set<String> getGenerateMarshaledTypes() throws Exception {
    if (generateMarshaledTypes == null) {
      Set<String> marshaledTypes = Sets.newLinkedHashSet();
      for (GenerateAttribute a : filteredAttributes()) {
        marshaledTypes.addAll(a.getSpecialMarshaledTypes());
      }
      generateMarshaledTypes = marshaledTypes;
    }
    return generateMarshaledTypes;
  }

  private List<String> extractClassNamesFromMirrors(
      Class<?> annotationType,
      String annotationValueName,
      List<? extends AnnotationMirror> annotationMirrors) {
    return extractedClassNamesFromAnnotationMirrors(annotationType.getName(),
        annotationValueName,
        annotationMirrors);
  }

  public static List<String> extractedClassNamesFromAnnotationMirrors(
      String annotationTypeName,
      String annotationValueName,
      List<? extends AnnotationMirror> annotationMirrors) {
    final List<String> collectClassNames = Lists.<String>newArrayList();

    for (AnnotationMirror annotationMirror : annotationMirrors) {
      if (annotationMirror.getAnnotationType().toString().equals(annotationTypeName)) {
        for (Entry<? extends ExecutableElement, ? extends AnnotationValue> e : annotationMirror.getElementValues()
            .entrySet()) {
          if (e.getKey().getSimpleName().contentEquals(annotationValueName)) {
            e.getValue().accept(new SimpleAnnotationValueVisitor6<Void, Void>() {
              @Override
              public Void visitArray(List<? extends AnnotationValue> vals, Void p) {
                for (AnnotationValue annotationValue : vals) {
                  annotationValue.accept(this, p);
                }
                return null;
              }

              @Override
              public Void visitType(TypeMirror t, Void p) {
                collectClassNames.add(t.toString());
                return null;
              }
            }, null);
          }
        }
      }
    }

    return ImmutableList.copyOf(collectClassNames);
  }

  public List<GenerateAttribute> getSettableAttributes() {
    return filteredAttributes()
        .filter(Predicates.or(
            GenerateAttributes.isGenerateAbstract(),
            GenerateAttributes.isGenerateDefault()))
        .toList();
  }

  public boolean isUseConstructor() {
    return !getConstructorArguments().isEmpty()
        || (!isUseBuilder() && !isUseSingleton() && getImplementedAttributes().isEmpty());
  }

  public List<GenerateAttribute> getConstructorArguments() {
    return filteredAttributes()
        .filter(Predicates.compose(Predicates.not(Predicates.equalTo(-1)), ToConstructorArgumentOrder.FUNCTION))
        .toSortedList(Ordering.natural().onResultOf(ToConstructorArgumentOrder.FUNCTION));
  }

  public List<GenerateAttribute> getConstructorOmited() {
    return filteredAttributes()
        .filter(Predicates.compose(Predicates.equalTo(-1), ToConstructorArgumentOrder.FUNCTION))
        .toList();
  }

  public List<GenerateAttribute> getAlignedAttributes() {
    return filteredAttributes()
        .filter(Predicates.compose(Predicates.not(Predicates.equalTo(-1)), ToAlignOrder.FUNCTION))
        .toSortedList(Ordering.natural().onResultOf(ToAlignOrder.FUNCTION));
  }

  private enum NonAuxiliary implements Predicate<GenerateAttribute> {
    PREDICATE;
    @Override
    public boolean apply(GenerateAttribute input) {
      return !input.isAuxiliary();
    }
  }

  private enum ToConstructorArgumentOrder implements Function<GenerateAttribute, Integer> {
    FUNCTION;

    @Override
    public Integer apply(GenerateAttribute input) {
      return input.getConstructorArgumentOrder();
    }
  }

  private enum ToAlignOrder implements Function<GenerateAttribute, Integer> {
    FUNCTION;

    @Override
    public Integer apply(GenerateAttribute input) {
      return input.getAlignOrder();
    }
  }

  public List<GenerateAttribute> getExcludableAttributes() {
    List<GenerateAttribute> excludables = Lists.newArrayList();
    for (GenerateAttribute attribute : filteredAttributes()) {
      if (attribute.isGenerateAbstract() && attribute.isContainerType()) {
        excludables.add(attribute);
      }
    }
    return excludables;
  }

  public List<GenerateAttribute> getLazyAttributes() {
    List<GenerateAttribute> lazyAttributes = Lists.newArrayList();
    for (GenerateAttribute attribute : filteredAttributes()) {
      if (attribute.isGenerateLazy()) {
        lazyAttributes.add(attribute);
      }
    }
    return lazyAttributes;
  }

  public List<GenerateAttribute> getAllAccessibleAttributes() {
    return ImmutableList.<GenerateAttribute>builder()
        .addAll(getImplementedAttributes())
        .addAll(getLazyAttributes())
        .build();
  }

  private List<GenerateAttribute> implementedAttributes;

  private FluentIterable<GenerateAttribute> filteredAttributes() {
    return FluentIterable.from(attributes());
  }

  @SuppressWarnings("unchecked")
  public List<GenerateAttribute> getImplementedAttributes() {
    if (implementedAttributes == null) {
      implementedAttributes = filteredAttributes()
          .filter(Predicates.or(
              GenerateAttributes.isGenerateAbstract(),
              GenerateAttributes.isGenerateDefault(),
              GenerateAttributes.isGenerateDerived()))
          .toList();
    }
    return implementedAttributes;
  }

  public List<GenerateAttribute> getEquivalenceAttributes() {
    return FluentIterable.from(getImplementedAttributes())
        .filter(NonAuxiliary.PREDICATE)
        .toList();
  }

  public List<GenerateAttribute> getHelperAttributes() {
    return filteredAttributes()
        .filter(Predicates.or(
            GenerateAttributes.isGenerateFunction(),
            GenerateAttributes.isGeneratePredicate()))
        .toList();
  }

  @Override
  protected TypeMirror internalTypeMirror() {
    return internalTypeElement().asType();
  }

  public abstract TypeElement internalTypeElement();

  public abstract List<GenerateAttribute> attributes();

  @GenerateDefault
  public boolean isGenerateModifiable() {
    return true;
  }

  @GenerateDefault
  public boolean isHashCodeDefined() {
    return false;
  }

  @GenerateDefault
  public boolean isEqualToDefined() {
    return false;
  }

  @GenerateDefault
  public boolean isToStringDefined() {
    return false;
  }

  @GenerateDefault
  public boolean isUseBuilder() {
    return true;
  }
}
