# WebUI Reader Node Tutorial

This package provides a template for creating a WebUI adaptation of a reader node.
Follow the numbered steps below to implement your own reader node.

## How to use this tutorial

- Some steps have corresponding `TODO (#N)` comments in the code (where N is the step number).
  Use your IDE's search to find all occurrences of `TODO (#N)` for the current step.
- After completing each step, verify that all `TODO (#N)` comments for that step have been resolved,
  then remove those TODO comments before proceeding to the next step.
- Once you reach step 6, your node is functional and can be tested.
  It might make sense to continue with the remaining steps before having implemented every last setting in step 6.

## Steps

1. Move this tutorial package to your desired location.
   - Also move the corresponding test package from `org.knime.base.tests`.
   - Option A: Use Eclipse's refactoring (right-click package → Refactor → Move/Rename) to handle imports automatically.
   - Option B: Use bash to copy files and adjust package statements:
     ```bash
     OLD_PKG=org.knime.base.node.io.filehandling.webui.reader2.tutorial && \
     NEW_PKG=your.new.package.path && \
     SRC_DEST=path/to/destination/src && \
     TEST_DEST=path/to/destination/test && \
     cp -r org.knime.base/src/org/knime/base/node/io/filehandling/webui/reader2/tutorial/* "$SRC_DEST" && \
     cp -r org.knime.base.tests/src/org/knime/base/node/io/filehandling/webui/reader2/tutorial/* "$TEST_DEST" && \
     find "$SRC_DEST" "$TEST_DEST" -name "*.java" -exec sed -i '' "s/$OLD_PKG/$NEW_PKG/g" {} \;
     ```
2. Rename all `Tutorial*` classes to match your reader name (e.g., `CSV*`).
   - Use this bash command from a common parent directory containing both your moved src and test tutorial packages
     (replace `YourReader`/`yourReader` with your prefix in PascalCase/camelCase, `Your Name` with your name, and adjust the paths):
     ```bash
     PREFIX=YourReader && \
     PREFIX_LOWER=yourReader && \
     AUTHOR="Your Name" && \
     SRC_PATH=path/to/your/moved/src/tutorial && \
     TEST_PATH=path/to/your/moved/test/tutorial && \
     find "$SRC_PATH" "$TEST_PATH" -name "Tutorial*.java" -exec bash -c 'mv "$0" "${0//Tutorial/$1}"' {} $PREFIX \; && \
     find "$SRC_PATH" "$TEST_PATH" -name "*.java" -exec sed -i '' -e "s/Tutorial/$PREFIX/g" -e "s/tutorial/$PREFIX_LOWER/g" -e "s/@author KNIME AG, Zurich, Switzerland/@author $AUTHOR/g" {} \;
     ```
   - Note: Adjust the `@author` tag appropriately for your organization.
3. Delete the dummy classes and replace with imports to your existing reader classes.
   Refer to your old NodeFactory to find which classes are used.
   - `DummyMultiTableReadConfig` - replace with your `MultiTableReadConfig`
   - `DummyTableReaderConfig` - replace with your `ReaderSpecificConfig`
   - `DummyTableReader` - replace with your `TableReader`
4. Adjust type generics to match your `TableReader<C, T, V>` if needed.
   Check your existing TableReader class to determine the correct types.
   - `T` (external type): Replace `Class<?>` with your type if different (e.g., `DataType`)
   - `V` (value): Replace `String` with your value type if different (e.g., `DataValue`)
5. Complete the NodeFactory and ReaderSpecific implementation.
   Refer to your old NodeFactory and existing reader infrastructure for the required values.
   - `createNodeDescription()` - set name, icon, port descriptions, node description, keywords, and version
   - `getReadAdapterFactory()` - return your ReadAdapterFactory instance
   - `getTypeHierarchy()` - return your ReadAdapterFactory's TYPE_HIERARCHY
   - `extractRowKey()` - implement if your V type requires special handling
   - `PRODUCTION_PATH_PROVIDER` - initialize from your ReadAdapterFactory
6. Configure file extensions and add reader-specific parameters:
   - Set the file extensions in `Set*Extensions` (inner class of your renamed NodeParameters)
   - For each new reader-specific parameter you need to add:
     1. **Nest it in a `*Parameters` class**: Group related parameters together in their own class
        - The tutorial includes two example classes starting with `My...` that demonstrate the recommended structure
        - These examples show how to organize parameters and define their layout
     2. **Add save logic**: Implement or extend the `saveToConfig()` method in your parameter class
        - This method should be called by the existing `saveToConfig()` in your main `ReaderParameters` class
     3. **Add validation logic**: Implement or extend the `validate()` method in your parameter class
        - This method should be called by the existing `validate()` in your main `ReaderParameters` class
     4. **If the parameter affects table spec detection**:
        - Add a `@ValueReference(<YourParameter>Ref.class)` annotation to the field
        - Register the Ref class in `*TransformationParametersStateProviders.initConfigIdTriggers()`
        - Add the field path to `TRIGGER_PATHS` in `*TransformationParametersStateProvidersTest`
          - Path format: `List.of("yourReaderParameters", "yourParamsGroup", "yourFieldName")`
          - Use field names without the "m_" prefix
7. Enable and complete the test classes:
   - `*TransformationParametersStateProvidersTest`:
     - Remove `@Disabled`
     - Implement `writeFileWithIntegerAndStringColumn()` for your file format
     - Set the correct file extension in `getFileName()`
   - `*TableReaderNodeParametersTest`:
     - Remove `@Disabled`
     - Create the `node_settings/*TableReaderNodeParameters.xml` file matching your parameters class name
8. Register the node in `plugin.xml` and deprecate the old node:
   - Register your new NodeFactory in `plugin.xml`
   - Deprecate the old node factory
   - If the old node had URL mappings (FileExtensionMapping), update them to point to the new node factory
9. Delete this README file.
