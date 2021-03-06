/*
 * Copyright © 2020 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package io.cdap.plugin.spark;

import com.google.common.base.Strings;
import io.cdap.cdap.api.annotation.Description;
import io.cdap.cdap.api.annotation.Macro;
import io.cdap.cdap.api.annotation.Name;
import io.cdap.cdap.api.data.schema.Schema;
import io.cdap.cdap.api.dataset.lib.KeyValue;
import io.cdap.cdap.api.plugin.PluginConfig;
import io.cdap.cdap.etl.api.FailureCollector;
import io.cdap.plugin.common.KeyValueListParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

/**
 * Config class for HierarchyToRelational.
 */
public class HierarchyToRelationalConfig extends PluginConfig {

  private static final String PARENT_FIELD = "parentField";
  private static final String CHILD_FIELD = "childField";
  private static final String PARENT_CHILD_MAPPING_FIELD = "parentChildMappingField";
  private static final String LEVEL_FIELD = "levelField";
  private static final String LEVEL_FIELD_DEFAULT_VALUE = "Level";
  private static final String TOP_FIELD = "topField";
  private static final String TOP_FIELD_DEFAULT_VALUE = "Top";
  private static final String BOTTOM_FIELD = "bottomField";
  private static final String BOTTOM_FIELD_DEFAULT_VALUE = "Bottom";
  private static final String TRUE_VALUE_FIELD = "trueValueField";
  private static final String TRUE_VALUE_FIELD_DEFAULT_VALUE = "Y";
  private static final String FALSE_VALUE_FIELD = "falseValueField";
  private static final String FALSE_VALUE_FIELD_DEFAULT_VALUE = "N";
  private static final String MAX_DEPTH_FIELD = "maxDepthField";
  private static final String MAX_DEPTH_FIELD_DEFAULT_VALUE = "50";


  @Name(PARENT_FIELD)
  @Description("Specifies the field from the input schema that should be used as the parent in the " +
    "hierarchical model. Should always contain a single, non-null root element in the hierarchy.")
  @Macro
  public String parentField;

  @Name(CHILD_FIELD)
  @Description("Specifies the field from the input schema that should be used as the child in the hierarchical " +
    "model.")
  @Macro
  public String childField;

  @Name(PARENT_CHILD_MAPPING_FIELD)
  @Description("Specifies parent child field mapping for fields that require swapping parent fields with tree/branch" +
    " root fields. ")
  @Macro
  public String parentChildMappingField;

  @Name(LEVEL_FIELD)
  @Description("The name of the field that should contain the Yes level in the hierarchy starting at a particular " +
    "node in the tree. The level is calculated as a distance of a node to a particular parent node in the tree.")
  @Macro
  @Nullable
  public String levelField;

  @Name(TOP_FIELD)
  @Description("The name of the field that determines whether a node is the root element or the top-most element" +
    " in the hierarchy. The input data should always contain a single non-null root node. For that node, this" +
    " field is true, while it is marked false for all other nodes in the hierarchy.")
  @Macro
  @Nullable
  public String topField;

  @Name(BOTTOM_FIELD)
  @Description("The name of the field that determines whether a node is a leaf element or the bottom-most " +
    "element in the hierarchy. The input data can contain multiple leaf nodes.")
  @Macro
  @Nullable
  public String bottomField;

  @Name(TRUE_VALUE_FIELD)
  @Description("The value that denotes truth in the Top and Bottom fields.")
  @Macro
  @Nullable
  public String trueValueField;

  @Name(FALSE_VALUE_FIELD)
  @Description("The value that denotes false in the Top and Bottom fields")
  @Macro
  @Nullable
  public String falseValueField;

  @Name(MAX_DEPTH_FIELD)
  @Description("The maximum depth upto which the data should be flattened. If a node is reached at a deeper" +
    " level, an error should be thrown.")
  @Macro
  @Nullable
  public String maxDepthField;

  public boolean requiredFieldsContainMacro() {
    return containsMacro(PARENT_FIELD) || containsMacro(CHILD_FIELD) || containsMacro(PARENT_CHILD_MAPPING_FIELD)
      || containsMacro(LEVEL_FIELD) || containsMacro(TOP_FIELD) || containsMacro(LEVEL_FIELD) ||
      containsMacro(BOTTOM_FIELD);
  }

  public void validate(FailureCollector collector) {
    if (requiredFieldsContainMacro()) {
      return;
    }
    if (parentField.equals(childField)) {
      collector.addFailure("Parent field is same as child field.", "Parent field needs to be different child field.")
        .withConfigProperty(PARENT_FIELD);
    }
    if (Strings.isNullOrEmpty(parentField)) {
      collector.addFailure("Parent field is null/empty.", "Please provide valid parent field.")
        .withConfigProperty(PARENT_FIELD);
    }
    if (!Strings.isNullOrEmpty(PARENT_CHILD_MAPPING_FIELD)) {
      Map<String, String> parentChildMapping = getParentChildMapping();
      if (parentChildMapping.containsKey(parentField) || parentChildMapping.containsValue(parentField)) {
        collector.addFailure("Parent key field found mapping.",
                             "Parent key field cannot be part of parent-> child mapping.")
          .withConfigProperty(PARENT_CHILD_MAPPING_FIELD);
      }
      if (parentChildMapping.containsKey(childField) || parentChildMapping.containsValue(childField)) {
        collector.addFailure("Child key field found mapping.",
                             "Child key field cannot be part of parent-> child mapping.")
          .withConfigProperty(PARENT_CHILD_MAPPING_FIELD);
      }
    }
    if (Strings.isNullOrEmpty(childField)) {
      collector.addFailure("Child field is null/empty.", "Please provide valid child field.")
        .withConfigProperty(CHILD_FIELD);
    }
    if (!Strings.isNullOrEmpty(maxDepthField)) {
      try {
        Integer.parseInt(maxDepthField);
      } catch (Exception e) {
        collector.addFailure("Invalid max depth field.", "Please provide positive integer as max depth field.")
          .withConfigProperty(CHILD_FIELD);
      }
    }
    collector.getOrThrowException();
  }

  public String getParentField() {
    return parentField;
  }

  public String getChildField() {
    return childField;
  }

  public String getLevelField() {
    if (Strings.isNullOrEmpty(levelField)) {
      return LEVEL_FIELD_DEFAULT_VALUE;
    }
    return levelField;
  }

  public String getTopField() {
    if (Strings.isNullOrEmpty(topField)) {
      return TOP_FIELD_DEFAULT_VALUE;
    }
    return topField;
  }

  public String getBottomField() {
    if (Strings.isNullOrEmpty(bottomField)) {
      return BOTTOM_FIELD_DEFAULT_VALUE;
    }
    return bottomField;
  }

  public String getTrueValueField() {
    if (Strings.isNullOrEmpty(trueValueField)) {
      return TRUE_VALUE_FIELD_DEFAULT_VALUE;
    }
    return trueValueField;
  }

  public String getFalseValueField() {
    if (Strings.isNullOrEmpty(falseValueField)) {
      return FALSE_VALUE_FIELD_DEFAULT_VALUE;
    }
    return falseValueField;
  }

  public String getMaxDepthField() {
    if (Strings.isNullOrEmpty(maxDepthField)) {
      return MAX_DEPTH_FIELD_DEFAULT_VALUE;
    }
    return maxDepthField;
  }

  public Map<String, String> getParentChildMapping() {
    Map<String, String> parentChildMap = new HashMap<>();
    if (Strings.isNullOrEmpty(parentChildMappingField)) {
      return parentChildMap;
    }
    KeyValueListParser keyValueListParser = new KeyValueListParser(";", "=");
    Iterable<KeyValue<String, String>> parsedParentChildMappingField = keyValueListParser
      .parse(parentChildMappingField);
    for (KeyValue<String, String> keyValuePair : parsedParentChildMappingField) {
      parentChildMap.put(keyValuePair.getKey(), keyValuePair.getValue());
    }
    return parentChildMap;
  }

  /**
   * Generate output schema including additional fields from plugin configuration
   *
   * @param inputSchema {@link Schema}
   */
  public Schema generateOutputSchema(Schema inputSchema) {
    if (inputSchema == null || inputSchema.getFields() == null) {
      throw new IllegalArgumentException("Input schema is required.");
    }
    List<Schema.Field> fields = new ArrayList<>();
    List<String> nonMappedFields = getNonMappedFields(inputSchema);
    for (Schema.Field field : inputSchema.getFields()) {
      if (nonMappedFields.contains(field.getName())) {
        Schema.Field updatedField = Schema.Field.of(field.getName(), field.getSchema().isNullable() ? field.getSchema()
          : Schema.nullableOf(field.getSchema()));
        fields.add(updatedField);
      } else {
        fields.add(field);
      }
    }
    fields.add(Schema.Field.of(getLevelField(), Schema.of(Schema.Type.INT)));
    fields.add(Schema.Field.of(getTopField(), Schema.of(Schema.Type.STRING)));
    fields.add(Schema.Field.of(getBottomField(), Schema.of(Schema.Type.STRING)));
    return Schema.recordOf("record", fields);
  }

  /**
   * Generates list of fields that are in input schema but are not mapped.
   *
   * @param inputSchema {@link Schema}
   * @return list of fields not included in parent->child mapping
   */
  public List<String> getNonMappedFields(Schema inputSchema) {
    List<Schema.Field> fields = inputSchema.getFields();
    Map<String, String> parentChildMapping = getParentChildMapping();
    return fields.stream().map(field -> field.getName())
      .filter(fieldName -> !(parentChildMapping.containsKey(fieldName) ||
        parentChildMapping.containsValue(fieldName) || fieldName.equals(parentField) || fieldName.equals(childField)))
      .collect(Collectors.toList());
  }
}
