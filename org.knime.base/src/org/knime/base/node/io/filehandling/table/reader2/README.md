# WebUI Reader Node Tutorial

This package provides a template for creating a WebUI adaptation of a reader node.
Follow the numbered steps below to implement your own reader node.

## Steps

1. Move this tutorial package to your desired location.
   - Also move the corresponding test package from `org.knime.base.tests`.
2. Rename all `Tutorial*` classes to match your reader name (e.g., `CSV*`).
3. Delete the dummy classes and replace with imports to your existing reader classes:
   - `TutorialMultiTableReadConfig` - replace with your `MultiTableReadConfig`
   - `TutorialTableReaderConfig` - replace with your `ReaderSpecificConfig`
   - `TutorialTableReader` - replace with your `TableReader`
4. Adjust type generics to match your `TableReader<C, T, V>` if needed:
   - `T` (external type): Replace `Class<?>` with your type if different (e.g., `DataType`)
   - `V` (value): Replace `String` with your value type if different (e.g., `DataValue`)
5. Complete the NodeFactory and ReaderSpecific implementation:
   - `createNodeDescription()` - set name, icon, port descriptions, node description, keywords, and version
   - `getReadAdapterFactory()` - return your ReadAdapterFactory instance
   - `getTypeHierarchy()` - return your ReadAdapterFactory's TYPE_HIERARCHY
   - `extractRowKey()` - implement if your V type requires special handling
   - `PRODUCTION_PATH_PROVIDER` - initialize from your ReadAdapterFactory
6. Configure file extensions and reader-specific parameters:
   - Set the file extensions in `SetTutorialExtensions`
   - Add reader-specific parameters to `TutorialReaderParameters`
   - Adjust or delete `TutorialReaderLayoutAdditions` if needed
7. Enable and complete the test classes:
   - `TutorialTransformationParametersStateProvidersTest`:
     - Remove `@Disabled`
     - Implement `writeFileWithIntegerAndStringColumn()` for your file format
     - Set the correct file extension in `getFileName()`
   - `TutorialTableReaderNodeParametersTest`:
     - Remove `@Disabled`
     - Create the `node_settings/TutorialTableReaderNodeParameters.xml` file
     - Rename the XML file to match your parameters class
8. Register the node in `plugin.xml` and deprecate the old node:
   - Register your new NodeFactory in `plugin.xml`
   - Deprecate the old node factory
   - If the old node had URL mappings (FileExtensionMapping), update them to point to the new node factory
9. Delete this README file.
