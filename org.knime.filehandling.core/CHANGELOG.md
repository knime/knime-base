# Commits

**Fix wrong usage of DefaultStatusMessage.mkError**

 * The mkError method uses the first parameter as format string. Some 
 * methods call it with an error message as first and only parameter. This 
 * produce problems if the error message contain a &quot;%&quot; character, e.g. from 
 * a filename. 

[6338761615e31fe](https://bitbucket.org/knime/knime-base/commits/6338761615e31fe) Sascha Wolke *2020-11-18 15:06:56*

**FSConnection: Log full exception when closing in background fails**


[a52efb060201219](https://bitbucket.org/knime/knime-base/commits/a52efb060201219) Bjoern Lohrmann *2020-11-17 13:06:14*

**BD-1079: Remove posix from supported file attribute views in BaseFileSystem**

 * Concrete file systems that support posix permissions should override this method 
 * and add &quot;posix&quot;. 
 * BD-1079 (Create Spark Context (Livy) (with File System Connection port)) 

[3e22413167708cb](https://bitbucket.org/knime/knime-base/commits/3e22413167708cb) Bjoern Lohrmann *2020-11-17 13:06:14*

**AP-15538: Fix preview disabling logic in remote context**

 * AP-15538 (Preview is not correctly disabled in remote job view) 
 * https://knime-com.atlassian.net/browse/AP-15538 

[7dc9b478b0bc39a](https://bitbucket.org/knime/knime-base/commits/7dc9b478b0bc39a) Simon Schmid *2020-11-10 12:51:08*

**AP-13428: Pull hasDialog implementation into abstract factory**

 * AP-13428 (Parquet Reader (new file handling)) 

[f90ba193c87b279](https://bitbucket.org/knime/knime-base/commits/f90ba193c87b279) Adrian Nembach *2020-11-10 11:23:54*

**AP-13428: Add API for traversing TreeTypeHierarchy**

 * Needed to create list types from a hierarchy of primitive data types. 
 * AP-13428 (Parquet Reader (new file handling)) 

[760dd05372bf54c](https://bitbucket.org/knime/knime-base/commits/760dd05372bf54c) Adrian Nembach *2020-11-10 11:23:54*

**AP-13428: Implement builder for TypedReaderTableSpec**

 * AP-13428 (Parquet Reader (new file handling)) 

[1c4e1ddd9c5bdcf](https://bitbucket.org/knime/knime-base/commits/1c4e1ddd9c5bdcf) Adrian Nembach *2020-11-10 11:23:54*

**BD-1078: export file selection dialog**

 * BD-1078 (Create Local Big Data Environment (with File System Connection port)) 

[47b68d94a7783dc](https://bitbucket.org/knime/knime-base/commits/47b68d94a7783dc) Sascha Wolke *2020-11-09 23:34:12*

**AP-14837: support whitespaces in path URI exporter**

 * getPath on the URI of the FSPath returns a decoded path. We must use the 
 * full URI constructor to avoid a second decode and possible exception on 
 * whitespaces in the path. 
 * AP-14837 (Path to URL framework) 

[c956e7db6e96ad8](https://bitbucket.org/knime/knime-base/commits/c956e7db6e96ad8) Sascha Wolke *2020-11-09 22:54:51*

**AP-15394: Adapt visibility of methods in AbstractTableReaderNoderFactory**

 * AP-15394 (Excel Reader: Allow selection of individual sheets) 
 * https://knime-com.atlassian.net/browse/AP-15394 

[bb5b6a11eb195be](https://bitbucket.org/knime/knime-base/commits/bb5b6a11eb195be) Simon Schmid *2020-11-09 16:36:53*

**AP-15394: Improve exception handling in table preview**

 * AP-15394 (Excel Reader: Allow selection of individual sheets) 
 * https://knime-com.atlassian.net/browse/AP-15394 

[162f06d6266947a](https://bitbucket.org/knime/knime-base/commits/162f06d6266947a) Simon Schmid *2020-11-09 16:36:53*

**Rename progress methods of the Read interface**

 * #getEstimatedSizeInBytes -&gt; #getMaxProgress 
 * #readBytes -&gt; #getProgress 

[3fd6da6961708e5](https://bitbucket.org/knime/knime-base/commits/3fd6da6961708e5) Simon Schmid *2020-11-06 09:01:52*

**AP-14837: fix ID of default URI exporter**

 * AP-14837 (Path to URL framework) 

[b79c7a5e6206e5b](https://bitbucket.org/knime/knime-base/commits/b79c7a5e6206e5b) Sascha Wolke *2020-11-05 14:47:19*

**Make getConnection() and canCreateConnection() public**


[a28fcc17e046a3e](https://bitbucket.org/knime/knime-base/commits/a28fcc17e046a3e) Tobias Koetter *2020-11-05 14:25:45*

**AP-15439: Implement UI improvements**

 * - Rename the &quot;Reset&quot; button to &quot;Reset all&quot; 
 * - Add space between buttons and table 
 * - Deactivate the column filter mode if a single file is read 
 * - Change the name of the &quot;Name&quot; column to &quot;New name&quot; 
 * - Properly clear validation status on reset 
 * - Highlight invalid names/specs via red border 
 * - Update node descriptions 
 * AP-15439 (Revise Table Transformation UI) 

[2cd1120a2682e03](https://bitbucket.org/knime/knime-base/commits/2cd1120a2682e03) Adrian Nembach *2020-11-05 08:59:27*

**AP-15439: Rename to TableTransformationPanel for conformity**

 * The thing displayed is a TableTransformation, so 
 * TableTransformationPanel seems to be a befitting name. 
 * AP-15439 (Revise Table Transformation UI) 

[3477b03a93d244b](https://bitbucket.org/knime/knime-base/commits/3477b03a93d244b) Adrian Nembach *2020-11-05 08:59:26*

**FHEXT-85: Add dbfs to fs-test.properties, exempt dbfs from mtime testcase**

 * Also, fix sonar issue in DefaultFSTestInitializer 
 * FHEXT-85 (Databricks File System Connector: Add browsability and testability) 

[4b9a045287c24d4](https://bitbucket.org/knime/knime-base/commits/4b9a045287c24d4) Alexander Bondaletov *2020-11-05 07:39:48*

**AP-14837: Add URI exporter framework and file URI exporter for local file system**

 * AP-14837 (Path to URL framework) 

[0cfa8a26c546cf4](https://bitbucket.org/knime/knime-base/commits/0cfa8a26c546cf4) Sascha Wolke *2020-11-04 13:09:19*

**FHEXT-40 Azure Blob: container name validation and related tests fixed**

 * FHEXT-40 (Azure Blob Storage Connector) 

[3e495168933f9af](https://bitbucket.org/knime/knime-base/commits/3e495168933f9af) Alexander Bondaletov *2020-11-03 23:20:25*

**Do not catch IOException in BasicLocalTestInitializer**


[672891b63f0782b](https://bitbucket.org/knime/knime-base/commits/672891b63f0782b) Sascha Wolke *2020-11-03 23:07:33*

**Fix extension-points and schemas in filehandling**

 * Add FSTestInitializerProvider extension-point to 
 * org.knime.filehandling.core.fs/plugin.xml 
 * (missed in 9fd6be1fb45e261602b7798280a7285d4476fdaa) 
 * Remove FSConnectionProvider extension-point from 
 * org.knime.filehandling.core.fs/plugin.xml 
 * (already part of org.knime.filehandling.core) 
 * Rename ...MountPointIDProvider.exsd schema to 
 * ...MountPointFileSystemAccess.exsd 
 * (missed in 3c1cb1abf62327a65fefacfdff863bd06278409c) 

[de429204cd57862](https://bitbucket.org/knime/knime-base/commits/de429204cd57862) Sascha Wolke *2020-11-03 23:05:54*

**AP-15339: Reserve one line of space for empty status messages**

 * The previous approach of settings the html style height to a minimum of 
 * 20px caused the message to be displaced in relation to the 
 * warning/error/info icon. 
 * AP-15339 (Fix broken error message for empty input file paths) 

[b6b0e40af821348](https://bitbucket.org/knime/knime-base/commits/b6b0e40af821348) Adrian Nembach *2020-11-03 16:15:07*

**AP-15339: Set a minimum size on the WordWrapJLabel**

 * This prevents the dialog from dancing around if the status message is 
 * empty. 
 * AP-15339 (Fix broken error message for empty input file paths) 

[5438e107c47feea](https://bitbucket.org/knime/knime-base/commits/5438e107c47feea) Adrian Nembach *2020-11-02 18:03:38*

**AP-15339: Fix error message for empty paths for FilterMode FILE**

 * In case of folder, files in folder or files and folders, an empty path 
 * is valid because it corresponds to the working directory of the file 
 * system. 
 * AP-15339 (Fix broken error message for empty input file paths) 

[3369c6c6558022e](https://bitbucket.org/knime/knime-base/commits/3369c6c6558022e) Adrian Nembach *2020-11-02 18:03:38*

**AP-14489: Support multiple previews in AbstractTableReaderNodeDialog**

 * AP-14489 (Add Preview to all tabs in TRF based reader dialogs) 

[85a3aec8337424c](https://bitbucket.org/knime/knime-base/commits/85a3aec8337424c) Adrian Nembach *2020-11-02 16:34:59*

**AP-14489: Fix rowkey bug in preview**

 * All preview iterators used the same RowKeyGeneratorContext which means 
 * that once the user scrolls back up, the row keys are still incremented. 
 * AP-14489 (Add Preview to all tabs in TRF based reader dialogs) 

[692a0a19d4b7ab8](https://bitbucket.org/knime/knime-base/commits/692a0a19d4b7ab8) Adrian Nembach *2020-11-02 16:34:58*

**AP-13456: Don't clear transformation table on duplicate names**

 * If a user provides duplicate column names via the transformation tab, 
 * then they should be able to correct their mistake. 
 * AP-13456 (Implement table spec transformation in dialog) 

[d374a31822e637e](https://bitbucket.org/knime/knime-base/commits/d374a31822e637e) Adrian Nembach *2020-11-02 09:11:07*

**AP-13456: Implement review feedback**

 * Mostly renaming and explanatory inline comments. 
 * AP-13456 (Implement table spec transformation in dialog) 

[15620990851ac1a](https://bitbucket.org/knime/knime-base/commits/15620990851ac1a) Adrian Nembach *2020-10-30 14:59:19*

**AP-13456: Changes to node facing API**

 * AP-13456 (Implement table spec transformation in dialog) 

[68ede12833a0445](https://bitbucket.org/knime/knime-base/commits/68ede12833a0445) Adrian Nembach *2020-10-30 14:59:18*

**AP-13456: Implement model part of dialog**

 * AP-13456 (Implement table spec transformation in dialog) 

[abc67f39d0fabb5](https://bitbucket.org/knime/knime-base/commits/abc67f39d0fabb5) Adrian Nembach *2020-10-30 14:59:17*

**AP-13456: Implement view part of dialog**

 * AP-13456 (Implement table spec transformation in dialog) 

[510955734c31dd1](https://bitbucket.org/knime/knime-base/commits/510955734c31dd1) Adrian Nembach *2020-10-30 14:59:17*

**AP-13456: Remove obsolete type mapping related classes**

 * Type mapping is now managed by the TransformationModel and 
 * Transformations respectively. 
 * AP-13456 (Implement table spec transformation in dialog) 

[cbaccd364fea382](https://bitbucket.org/knime/knime-base/commits/cbaccd364fea382) Adrian Nembach *2020-10-30 14:59:16*

**AP-13456: Internal TRF changes**

 * AP-13456 (Implement table spec transformation in dialog) 

[1cc0dc6c05d7ecd](https://bitbucket.org/knime/knime-base/commits/1cc0dc6c05d7ecd) Adrian Nembach *2020-10-30 14:59:16*

**AP-13456: Adapt DefaultTableSpecConfig**

 * AP-13456 (Implement table spec transformation in dialog) 

[8a3caac373e480f](https://bitbucket.org/knime/knime-base/commits/8a3caac373e480f) Adrian Nembach *2020-10-30 14:59:15*

**AP-13456: Introduce Transformations and ColumnFilterMode**

 * AP-13456 (Implement table spec transformation in dialog) 

[8610d49f1efa810](https://bitbucket.org/knime/knime-base/commits/8610d49f1efa810) Adrian Nembach *2020-10-30 14:58:50*

**AP-13456: Add convenience methods in GBCBuilder**

 * AP-13456 (Implement table spec transformation in dialog) 

[5c553efb507c997](https://bitbucket.org/knime/knime-base/commits/5c553efb507c997) Adrian Nembach *2020-10-30 14:58:49*

**AP-15288: Added functionality to include empty folders to archives**

 * AP-15288 (Folder selection should also add empty folders to the created archive) 

[5a9d283b6002ef8](https://bitbucket.org/knime/knime-base/commits/5a9d283b6002ef8) Mark Ortmann *2020-10-29 17:05:30*

**AP-15454: Paths are now always sorted lexicographically**

 * AP-15454 (Fixe Path sorting for PathAcessor and FSFiles) 

[783c9eb6d09bfdd](https://bitbucket.org/knime/knime-base/commits/783c9eb6d09bfdd) Mark Ortmann *2020-10-28 10:08:28*

**AP-15382: Added getFilesAndFolders to FSFiles**

 * Added a new method to the FSFiles class to retrieve paths from files and 
 * folder within a specified folder. 
 * AP-15382 (Copy/Move files ensure that empty folder are being copied when 
 * Folder is selected) 

[14710d0421384e4](https://bitbucket.org/knime/knime-base/commits/14710d0421384e4) Lars Schweikardt *2020-10-28 08:15:55*

**AP-14855: Added workflow icon when browsing Rest-FS**

 * AP-14855 (KNIME Server Connector (Labs)) 

[7fb555dd2bd8b36](https://bitbucket.org/knime/knime-base/commits/7fb555dd2bd8b36) Mark Ortmann *2020-10-27 13:49:05*

**AP-15140: Minor code enhancements**

 * AP-15140 (Support FSLocation and Collection FlowVariables in 
 * Variable Loop End) 

[55b12a8430f5bfb](https://bitbucket.org/knime/knime-base/commits/55b12a8430f5bfb) Mark Ortmann *2020-10-26 16:54:16*

**AP-15140: Added FSLocation cell factory manager**

 * AP-15140 (Support FSLocation and Collection FlowVariables in 
 * Variable Loop End) 

[52f49c8a8f43ffa](https://bitbucket.org/knime/knime-base/commits/52f49c8a8f43ffa) Mark Ortmann *2020-10-26 16:10:59*

**AP-14841: add HDFS wrapper wrapper to integration tests and to fs-test.properties**

 * filehandling: export local fs connection to use it in hdfs-wrapper-wrapper integration tests 
 * AP-14841 (Wrap NIO inside a Hadoop fs) 

[f22b1f33d6b5eb9](https://bitbucket.org/knime/knime-base/commits/f22b1f33d6b5eb9) Sascha Wolke *2020-10-23 21:54:36*

**Remove per Row CheckUtil calls on DefaultTypeMapper**


[5ebbcc4a44117c7](https://bitbucket.org/knime/knime-base/commits/5ebbcc4a44117c7) Christian Dietz *2020-10-23 14:58:53*

**Disable CheckUtils calls on cell level in ReaderFramework**


[da799272015a658](https://bitbucket.org/knime/knime-base/commits/da799272015a658) Christian Dietz *2020-10-23 14:41:30*

**AP-15289: Adapted hierarchy flattening status message and its location**

 * AP-15289 (Allow hierarchy flattening Compress Compress Files/Folder (Labs)) 

[28d80a8b2fb94f9](https://bitbucket.org/knime/knime-base/commits/28d80a8b2fb94f9) Mark Ortmann *2020-10-22 13:06:44*

**BaseFS: Fix issue with checking the target parent existence during copy/move**


[098f5369090d622](https://bitbucket.org/knime/knime-base/commits/098f5369090d622) Bjoern Lohrmann *2020-10-22 12:24:07*

**BaseFS: Add directory checks to BaseFileSystemProvider.newInputStream(), newOutputStream() and newByteChannel()**


[b7fefa40fae2750](https://bitbucket.org/knime/knime-base/commits/b7fefa40fae2750) Bjoern Lohrmann *2020-10-22 08:58:53*

**BaseFS: Throw DirectoryNotEmptyException when deleting non-empty directory in BaseFileSystemProvider.delete()**


[a2d0ba9710bf684](https://bitbucket.org/knime/knime-base/commits/a2d0ba9710bf684) Bjoern Lohrmann *2020-10-21 15:44:25*

**BaseFS: Provide base implementation for BaseFileSystemProvider.exists() using readFileAttributes()**


[1b803c1d7b06ba9](https://bitbucket.org/knime/knime-base/commits/1b803c1d7b06ba9) Bjoern Lohrmann *2020-10-21 15:43:40*

**BaseFS: BaseFileSystemProvider.move() and copy() throw DirectoryNotEmptyException if moving/copying to non-emtpy directory**

 * Also adds respective integration test cases. 

[3e241373df27cfe](https://bitbucket.org/knime/knime-base/commits/3e241373df27cfe) Bjoern Lohrmann *2020-10-21 15:42:41*

**BaseFS: provide default implementations for BaseFileSystem.getSchemeString() and getHostString()**


[f6ceb094e7b297c](https://bitbucket.org/knime/knime-base/commits/f6ceb094e7b297c) Bjoern Lohrmann *2020-10-21 15:40:57*

**AP-15289: Renamed StatusMessageUtils and added NO_OP consumer**

 * AP-15289 (Allow hierarchy flattening Compress Compress Files/Folder (Labs)) 

[f4c4fb0a8049d3b](https://bitbucket.org/knime/knime-base/commits/f4c4fb0a8049d3b) Mark Ortmann *2020-10-19 11:30:59*

**AP-15289: Moved StatusSwingSworker to status package**

 * AP-15289 (Allow hierarchy flattening Compress Compress Files/Folder (Labs)) 

[7995cec385b6715](https://bitbucket.org/knime/knime-base/commits/7995cec385b6715) Mark Ortmann *2020-10-19 11:30:59*

**AP-15289: Added missing sorting to FSFiles#getFilePathsFromFolder**

 * AP-15289 (Allow hierarchy flattening Compress Compress Files/Folder (Labs)) 

[16c0811831bd30d](https://bitbucket.org/knime/knime-base/commits/16c0811831bd30d) Mark Ortmann *2020-10-19 11:30:59*

**AP-14855: Added consumer to working directory chooser**

 * AP-14855 (KNIME Server Connector (Labs)) 

[b3785e58390b88d](https://bitbucket.org/knime/knime-base/commits/b3785e58390b88d) Mark Ortmann *2020-10-19 10:53:32*

**AP-15371: Fix percent-encoding issue in Custom URL file system**

 * AP-15371 (Reading from Custom URL fails when URL contains percent-encoded characters in query or fragment) 

[68a1c9be080342f](https://bitbucket.org/knime/knime-base/commits/68a1c9be080342f) Bjoern Lohrmann *2020-10-18 21:58:46*

**BaseFS: Add exists check when newOutputStream() is invoked with CREATE_NEW**

 * Also adds integration test. 

[5b6ed9ce1500061](https://bitbucket.org/knime/knime-base/commits/5b6ed9ce1500061) Bjoern Lohrmann *2020-10-15 20:06:02*

**AP-15088: Don't print prefix if the exception provides a message**

 * AP-15088 (Implement StatusMessageReporter for Copy/Move Files no table 
 * input) 

[5f82dc340dd680b](https://bitbucket.org/knime/knime-base/commits/5f82dc340dd680b) Adrian Nembach *2020-10-15 13:01:56*

**AP-15088: Added file/folder check Copy/Move**

 * Added an additional check if the folder option of the copy move is 
 * selected which checks if the specified path is a file or directory to be 
 * consistent with the other options. 
 * AP-15088 (Implement StatusMessageReporter for Copy/Move Files no table 
 * input) 

[01507d8842114c4](https://bitbucket.org/knime/knime-base/commits/01507d8842114c4) Lars Schweikardt *2020-10-14 15:03:59*

**FHEXT-40: Change default name of new folder created in JFileChooser**

 * Reason: Could not create Azure Blob Storage containers from JFileChooser 
 * due to container name restrictions. 
 * FHEXT-40 (Azure Blob Storage Connector) 

[84e333c6f75f35a](https://bitbucket.org/knime/knime-base/commits/84e333c6f75f35a) Bjoern Lohrmann *2020-10-14 13:26:00*

**AP-14850: Reduce sonar/eclipse warnings in BaseFileSystemProvider**

 * AP-14850: SKN Code Cleanup (2020-10) 

[48b9ffb99e51867](https://bitbucket.org/knime/knime-base/commits/48b9ffb99e51867) Bjoern Lohrmann *2020-10-14 13:26:00*

**BaseFS: Provide default implementation for move/moveInternal()**

 * Default implementation is emulating move by copy+delete. 
 * Commit also provides: 
 * FSFiles.isNonEmptyDirectory() to check whether a given path is a non-empty directory 
 * FSFiles.copyRecursively() to recursively copy a directory across providers (incl. integration tests) 
 * fixes bug in BlobStorePath.getPath() w.r.t. empty path 

[2bced7496c1434e](https://bitbucket.org/knime/knime-base/commits/2bced7496c1434e) Bjoern Lohrmann *2020-10-14 13:26:00*

**AP-14872: Refactor TRF to allow for spec transformations in the dialog**

 * AP-14872 (Support table spec transformations in the TRF backend) 

[5d9ceeb71a06aed](https://bitbucket.org/knime/knime-base/commits/5d9ceeb71a06aed) Adrian Nembach *2020-10-14 13:08:52*

**AP-14872: Expose ProductionPathProvider for use in Theobald reader**

 * AP-14872 (Support table spec transformations in the TRF backend) 

[4ee1e05aebd5de8](https://bitbucket.org/knime/knime-base/commits/4ee1e05aebd5de8) Adrian Nembach *2020-10-14 13:08:52*

**AP-15129: Improve javadoc**

 * AP-15129 (Add intended usage to the Java-Doc of the 
 * AbstractSettingsModelFileChooser) 

[571e03b1a07813e](https://bitbucket.org/knime/knime-base/commits/571e03b1a07813e) Adrian Nembach *2020-10-14 12:12:47*

**AP-15186: Added CheckedExBiFunction**

 * Added a CheckedExceptionBiFunction to the filehandling.core.util to make 
 * use of for instance in the copy move node. 
 * AP-15186 (Avoid downloading data in Copy/Move Files/Folders) 

[f0818ab9fb69f54](https://bitbucket.org/knime/knime-base/commits/f0818ab9fb69f54) Lars Schweikardt *2020-10-12 12:05:10*

**AP-15278: Always update the status reported by the swing worker**

 * It is ensured that the swing worker was not cancelled. 
 * AP-15278 (AbstractDialogComponentFileChooser doesn&#39;t always update the 
 * status message properly) 

[8adee71371aa1fa](https://bitbucket.org/knime/knime-base/commits/8adee71371aa1fa) Adrian Nembach *2020-10-09 12:15:33*

**AP-15278: Propagate active config changes**

 * This ensures that the status message of the file chooser is updated if 
 * e.g. the mountpoint changes. 
 * AP-15278 (AbstractDialogComponentFileChooser doesn&#39;t always update the 
 * status message properly) 

[3da64ee67e966a4](https://bitbucket.org/knime/knime-base/commits/3da64ee67e966a4) Adrian Nembach *2020-10-09 12:15:33*

**AP-15313: Don't set BaseFileSystemProvider.m_fileSystem to null**

 * Also, improve exception handling in Create Temp Dir (Labs) node, when deleting 
 * temp dirs in the background. 
 * AP-15313 (Resetting connector node together with Create Temp Dir node sometimes results in NPE) 

[5e0a8c34c8f28c4](https://bitbucket.org/knime/knime-base/commits/5e0a8c34c8f28c4) Bjoern Lohrmann *2020-10-09 06:52:06*

**AP-13784: Make DefaultWriterStatusMessageReporter public**

 * This is done to allow delegating to it from within StatusMessageReporter 
 * implementations that only do preliminary checks. 
 * AP-13784 (Compress Files no table input (new file handling)) 

[c71052f65843e98](https://bitbucket.org/knime/knime-base/commits/c71052f65843e98) Adrian Nembach *2020-10-08 11:17:08*

**BaseFS: Avoid fetching file attributes twice when exists() also fetches file attributes**


[821f76438b6258b](https://bitbucket.org/knime/knime-base/commits/821f76438b6258b) Bjoern Lohrmann *2020-10-07 22:16:42*

**AP-13433: Added new new-path utility method**

 * Added a new method to the StatusMessageNewPathUtils to indicate an error 
 * in case the user enters a invalid path and the create missing folders 
 * option is unchecked. 
 * AP-13433 (Copy / move files no table input (new file handling)) 

[9179e0992e0a4a8](https://bitbucket.org/knime/knime-base/commits/9179e0992e0a4a8) Lars Schweikardt *2020-10-05 07:28:20*

**AP-15135: Make file system chooser label customizable**

 * AP-15135: (Create Paths uses Reader instead of WriterFileChooser) 

[d58e6e03d7f6a58](https://bitbucket.org/knime/knime-base/commits/d58e6e03d7f6a58) Adrian Nembach *2020-10-02 08:36:32*

**DEVOPS-483: Update build.properties to include LICENSE file in bin & src jars.**

 * DEVOPS-483 (Add LICENSE.txt files to binary builds of all our plug-ins) 

[2b2a4ed5e650567](https://bitbucket.org/knime/knime-base/commits/2b2a4ed5e650567) Sebastian Gerau *2020-09-28 22:53:18*

**AP-15088: Extracted handling of new paths methods**

 * Extracted multiple used methods to handle new paths from the file 
 * chooser writer in a utility class to reduce redundancy. 
 * AP-15088 (Implement StatusMessageReporter for Copy/Move Files no table 
 * input) 

[f9b71851c39b8f0](https://bitbucket.org/knime/knime-base/commits/f9b71851c39b8f0) Lars Schweikardt *2020-09-24 10:33:30*

**AP-15088: Extracted methods into DefaultStatusMsg**

 * Extracte the warning, error, info methods into DefaultStatusMessage which creates the corresponding StatusMessage for the StatusMessageReporter to make use 
 * of them in other implementations of StatusMessageReporter. 
 * AP-15088 (Implement StatusMessageReporter for Copy/Move Files no table 
 * input) 

[640ffce89cb473f](https://bitbucket.org/knime/knime-base/commits/640ffce89cb473f) Lars Schweikardt *2020-09-24 10:33:19*

**AP-15088: Added getFilePathsFromFolder to FSFiles**

 * Added a new method to FSFiles which returns all file paths within a 
 * folder based on a source path. This method will be used in utility 
 * filehandling nodes and for reporting status messages. 
 * AP-15088 (Implement StatusMessageReporter for Copy/Move Files no table 
 * input) 

[6ce8c343dc1a054](https://bitbucket.org/knime/knime-base/commits/6ce8c343dc1a054) Lars Schweikardt *2020-09-24 08:34:39*

**AP-15088: Make updateComponent public**

 * Made the updateComponent Method of the 
 * AbstractDialogComponentFileChooser public to make use of it in the 
 * decompress and copy move files node. Its necessary to update the 
 * component to react to changes of the source file chooser model. 
 * AP-15088 (Implement StatusMessageReporter for Copy/Move Files no table 
 * input) 

[2f556fbd63c0a9a](https://bitbucket.org/knime/knime-base/commits/2f556fbd63c0a9a) Lars Schweikardt *2020-09-24 08:34:39*

**AP-13784: Add convenience methods to AbstractSettingsModelFileChooser**

 * Includes a convenience getter for the FilterMode and a method that 
 * extracts FSLocation from a NodeSettingsRO object without changing the 
 * state of the SettingsModel 
 * AP-13784 (Compress Files no table input (new file handling)) 

[8887e4ece174d95](https://bitbucket.org/knime/knime-base/commits/8887e4ece174d95) Adrian Nembach *2020-09-23 11:28:40*

**base-file-system-provider: use deep attributes cache invalidation in move**

 * The attributes cache must be recursive/deep invalidated in move operation to 
 * invalidate file attribute entries if we move directories. The 
 * integration move test already covers this (e.g. in the 
 * test_move_directory). 

[657219c807e1d5e](https://bitbucket.org/knime/knime-base/commits/657219c807e1d5e) Sascha Wolke *2020-09-21 07:36:05*

**FHEXT-63: Tighten type checks for PosixFileAttribute fetching**

 * FHEXT-63 (Drop broken PosixFileAttribute support from S3 file system) 

[14e548ca4377c0d](https://bitbucket.org/knime/knime-base/commits/14e548ca4377c0d) Bjoern Lohrmann *2020-09-15 10:05:21*

**FHEXT-48: Slightly improved error reporting when in file chooser when FSConnection could not be opened.**

 * FHEXT-48 (SSH Connector (Labs)) 

[41302f5ef71e8ce](https://bitbucket.org/knime/knime-base/commits/41302f5ef71e8ce) Bjoern Lohrmann *2020-09-04 09:57:42*

**AP-15070: Made status message computation customizable**

 * AP-15070 (Make StatusMessage computation customizable) 

[a04fe12132af29b](https://bitbucket.org/knime/knime-base/commits/a04fe12132af29b) Mark Ortmann *2020-09-03 11:47:27*

**AP-15053: Added missing update hook when opening the dialog**

 * AP-15053 (Create missing folders and overwrite settings not updated 
 * when opening dialogs) 

[bd52e38ebb19164](https://bitbucket.org/knime/knime-base/commits/bd52e38ebb19164) Mark Ortmann *2020-09-02 08:11:58*

**AP-14875: Fixed folders filter**

 * AP-14875 (Fix FilterOptions for folders in new DialogComponentFileChooser) 

[9ef619ab43bc853](https://bitbucket.org/knime/knime-base/commits/9ef619ab43bc853) Mark Ortmann *2020-08-24 21:59:59*

**AP-15017: Fix URIFileSystem.toBaseURI() issue**

 * Fixes bug that occured when the path string occurs before the 
 * actual path in the URI (e.g. when the path is &quot;/&quot;) 
 * AP-15017 (Custom/KNIME URL path implementation gives faulty FSLocation) 

[08af6a80524dfef](https://bitbucket.org/knime/knime-base/commits/08af6a80524dfef) Bjoern Lohrmann *2020-08-24 14:18:36*

**AP-15017: set FSLocation path to underlying URI in URIPath.toFSLocation()**

 * Also: 
 * adapts test cases to test the result of URIPath.toFSLocation() 
 * minor rewrite of some of the URI parsing to make it more readable 
 * AP-15017 (Custom/KNIME URL path implementation gives faulty FSLocation) 

[96e1cc4b2285f8d](https://bitbucket.org/knime/knime-base/commits/96e1cc4b2285f8d) Bjoern Lohrmann *2020-08-21 23:14:30*

**AP-14912: Validate FilterMode and file system compatibility**

 * This check ensures that the FILE filter mode is selectable if the 
 * Custom/KNIME URL file system is selectable. 
 * The provided fix is only temporary until AP-15009 is implemented. 
 * AP-14912 (Custom URL does not disable mode selection) 

[99895c50195c85f](https://bitbucket.org/knime/knime-base/commits/99895c50195c85f) Adrian Nembach *2020-08-17 16:40:19*

**AP-14912: Properly update filter mode enabled status**

 * The logic controlling the enabled status of the filter modes is moved 
 * from the dialog component into the settings model, where it belongs. 
 * AP-14912 (Custom URL does not disable mode selection) 

[086546c08698442](https://bitbucket.org/knime/knime-base/commits/086546c08698442) Adrian Nembach *2020-08-17 13:48:13*

**AP-13721: Adapted SettingsModelFileSystem constructor**

 * AP-13721 (String to Path node) 

[ecd60d663b36655](https://bitbucket.org/knime/knime-base/commits/ecd60d663b36655) Mark Ortmann *2020-08-11 13:19:49*

**AP-13432: Make displayed convenience file system configurable**

 * So far Custom/KNIME URL was excluded if File was not among the possible 
 * selection modes. This was enforced in the constructor of 
 * AbstractDialogComponentFileChooser which is a bad place for such a 
 * constraint because this kind of constrained heavily depends on the node 
 * functionality. Therefore this commit allows programmers to specify which 
 * convenience file systems should be available in the dialog by passing a 
 * set of FSCategory when creating the settings model for the file chooser. 
 * AP-13432 (Delete files no table input (new file handling)) 

[cb4c85d3932ab06](https://bitbucket.org/knime/knime-base/commits/cb4c85d3932ab06) Adrian Nembach *2020-08-10 13:16:21*

**AP-13721: Made getKeysForFSLocation public**

 * AP-13721 (String to Path node) 

[c661c2f7e078fa1](https://bitbucket.org/knime/knime-base/commits/c661c2f7e078fa1) Timmo Waller-Ehrat *2020-08-05 08:35:17*

**AP-13721: Removed constructor without FlowVariableModel**

 * AP-13721 (String to Path node) 

[c26763119cba46d](https://bitbucket.org/knime/knime-base/commits/c26763119cba46d) Timmo Waller-Ehrat *2020-08-05 08:35:17*

**AP-13721: Changed constructor to take FSLocationSpec as argument**

 * AP-13721 (String to Path node) 

[b34d60fcf161603](https://bitbucket.org/knime/knime-base/commits/b34d60fcf161603) Timmo Waller-Ehrat *2020-08-05 08:35:17*

**AP-13721: Export filesystemchooser package from filehandling.core**

 * AP-13721 (String to Path node) 

[aa907d3b7bce05f](https://bitbucket.org/knime/knime-base/commits/aa907d3b7bce05f) Timmo Waller-Ehrat *2020-08-05 08:35:17*

**AP-13721: Fixed bug in createLocation, so it also works for LOCAL FS**

 * The createLocation method checked, if the file system specifier is null 
 * and threw an exception if so. Because of this behavior the methode 
 * didn&#39;t work for local FS, because the file system specifier is always 
 * null in this case. Now it just passes null to FSLocation constructor 
 * instead of throwing an exception. 
 * AP-13721 (String to Path node) 

[53eadb83b30e187](https://bitbucket.org/knime/knime-base/commits/53eadb83b30e187) Timmo Waller-Ehrat *2020-08-05 08:35:17*

**AP-14793: Don't update status when the settings model is disabled**

 * AP-14793 (DialogComponentReader/WriterFileChooser: UI is not disabled 
 * when settings model is disabled) 

[0d7b700991b44ea](https://bitbucket.org/knime/knime-base/commits/0d7b700991b44ea) Adrian Nembach *2020-08-04 16:44:11*

**Increased version range of knime-core**


[0b2e80a9d0c5147](https://bitbucket.org/knime/knime-base/commits/0b2e80a9d0c5147) Moritz Heine *2020-07-31 09:30:48*

**SRV-3056: use wrapped output stream that disconnects automatically**

 * SRV-3056 (Error while Executing Workflow that Reads and Writes a table within a loop on KNIME Server) 

[b009fd398c3eb20](https://bitbucket.org/knime/knime-base/commits/b009fd398c3eb20) Moritz Heine *2020-07-30 12:57:48*

**AP-14793: Update enabled status on model change**

 * AP-14793 (DialogComponentReader/WriterFileChooser: UI is not disabled 
 * when settings model is disabled) 

[3090109749cc60e](https://bitbucket.org/knime/knime-base/commits/3090109749cc60e) Mark Ortmann *2020-07-28 15:16:41*

**AP-14457: Persist settings if fs port is added/removed**

 * This includes ensuring the correct behavior if the location is 
 * overwritten by a flow variable. 
 * AP-14457 (AbstractSettingsModelFileChooser: Persist settings when 
 * adding/removing the file system port) 

[be450f299122488](https://bitbucket.org/knime/knime-base/commits/be450f299122488) Adrian Nembach *2020-07-28 14:53:11*

**AP-14457: Clone settings for asynchronous work**

 * AP-14457 (AbstractSettingsModelFileChooser: Persist settings when 
 * adding/removing the file system port) 

[94b94b25052b8c2](https://bitbucket.org/knime/knime-base/commits/94b94b25052b8c2) Adrian Nembach *2020-07-28 14:53:11*

**AP-14457: Minor cleanups**

 * AP-14457 (AbstractSettingsModelFileChooser: Persist settings when 
 * adding/removing the file system port) 

[7ee00cb674545ac](https://bitbucket.org/knime/knime-base/commits/7ee00cb674545ac) Adrian Nembach *2020-07-28 14:53:11*

**AP-14457: Don't show the table preview for invalid locations**

 * Until now the preview was just not updated in this case, leaving the 
 * previous preview in place even though we might be pointing to a 
 * completely different location now. 
 * AP-14457 (AbstractSettingsModelFileChooser: Persist settings when 
 * adding/removing the file system port) 

[19a72e59a8ec35b](https://bitbucket.org/knime/knime-base/commits/19a72e59a8ec35b) Adrian Nembach *2020-07-28 14:53:11*

**AP-14457: Introduce isActive property on FileSystemSpecificConfigs**

 * AP-14457 (AbstractSettingsModelFileChooser: Persist settings when 
 * adding/removing the file system port) 

[0917c4654d2088a](https://bitbucket.org/knime/knime-base/commits/0917c4654d2088a) Adrian Nembach *2020-07-28 14:53:11*

**AP-14557: Transform knime://knime.workflow to knime://<current-mountpoint> when writing workflow to RelativeTo filesystem (in the AP)**

 * Note: Deploying workflows to the workflow data area this way makes little sense (and does not work properly), therefore this is unsupported. 
 * AP-14557 (Can&#39;t read a workflow written by the workflow writer if file system is relative to or mountpoint) 

[df141a037dc6dc7](https://bitbucket.org/knime/knime-base/commits/df141a037dc6dc7) Marc Bux *2020-07-27 13:53:23*

**Version bump for 4.3 release**


[ed379d2e9e9684b](https://bitbucket.org/knime/knime-base/commits/ed379d2e9e9684b) Thorsten Meinl *2020-07-23 15:06:42*

**BaseFS: Improve error message when tryting to create folder inside something that is not a folder**


[0984d93417758d8](https://bitbucket.org/knime/knime-base/commits/0984d93417758d8) Bjoern Lohrmann *2020-07-20 10:44:23*

**added sonarlint bindings**


[2f7758647791f24](https://bitbucket.org/knime/knime-base/commits/2f7758647791f24) Bernd Wiswedel *2020-07-16 19:32:48*

