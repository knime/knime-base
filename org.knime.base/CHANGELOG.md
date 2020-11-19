# Commits

**AP-15538: Fix preview disabling logic in remote context**

 * AP-15538 (Preview is not correctly disabled in remote job view) 
 * https://knime-com.atlassian.net/browse/AP-15538 

[7dc9b478b0bc39a](https://bitbucket.org/knime/knime-base/commits/7dc9b478b0bc39a) Simon Schmid *2020-11-10 12:51:08*

**AP-13428: Pull hasDialog implementation into abstract factory**

 * AP-13428 (Parquet Reader (new file handling)) 

[f90ba193c87b279](https://bitbucket.org/knime/knime-base/commits/f90ba193c87b279) Adrian Nembach *2020-11-10 11:23:54*

**AP-14860: Changed settings name to usernameVarName and passwordVarName**

 * AP-14860 (Variable to Credential) 

[6a8428ff3d32988](https://bitbucket.org/knime/knime-base/commits/6a8428ff3d32988) Tobias Koetter *2020-11-09 09:34:20*

**AP-14860: New node implemented**

 * AP-14860 (Variable to Credential) 

[cd0b4726b1c0f4d](https://bitbucket.org/knime/knime-base/commits/cd0b4726b1c0f4d) Tobias Koetter *2020-11-08 15:10:08*

**Rename progress methods of the Read interface**

 * #getEstimatedSizeInBytes -&gt; #getMaxProgress 
 * #readBytes -&gt; #getProgress 

[3fd6da6961708e5](https://bitbucket.org/knime/knime-base/commits/3fd6da6961708e5) Simon Schmid *2020-11-06 09:01:52*

**AP-15479: Moved all filehandling utility nodes to corresponding plugin**

 * AP-15479 (Move utility filehandling nodes to filehandling.utility) 

[2fd5c498cfb51c6](https://bitbucket.org/knime/knime-base/commits/2fd5c498cfb51c6) Mark Ortmann *2020-11-05 15:26:13*

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

**AP-15025: Node description polishing**

 * AP-15025 (Revise Create File/Folder Variables (Labs)) 

[b23acc096467600](https://bitbucket.org/knime/knime-base/commits/b23acc096467600) Mark Ortmann *2020-11-04 11:48:11*

**AP-13721: Fixed auto-guessing bug**

 * AP-13721 (String to Path node) 

[a3a35c0a3995135](https://bitbucket.org/knime/knime-base/commits/a3a35c0a3995135) Mark Ortmann *2020-11-04 10:56:58*

**AP-13433: Renamed Copy/Moves Files to Transfer Files**

 * AP-13433 (Copy / move files no table input (new file handling)) 

[529b69c5e169838](https://bitbucket.org/knime/knime-base/commits/529b69c5e169838) Mark Ortmann *2020-11-04 10:56:58*

**AP-15025: Fixed typo**

 * AP-15025 (Revise Create File/Folder Variables (Labs)) 

[34bbcb90adc892c](https://bitbucket.org/knime/knime-base/commits/34bbcb90adc892c) Mark Ortmann *2020-11-04 10:56:58*

**Remove column filter when accessing DataRows**

 * We need to output the entire DataRow and not only the column we used 
 * for comparison. Came up during fast table testing. 

[a1cbcaf57c7320e](https://bitbucket.org/knime/knime-base/commits/a1cbcaf57c7320e) Christian Dietz *2020-11-04 10:03:43*

**AP-15025: Fixed backwards compatbility issues**

 * AP-15025 (Revise Create File/Folder Variables (Labs)) 

[5de86479012076e](https://bitbucket.org/knime/knime-base/commits/5de86479012076e) Mark Ortmann *2020-11-03 14:37:13*

**AP-15489: Fix copy constructor to include number format settings**

 * AP-15489 (Simple File Reader: Changing the decimal separator doesn&#39;t 
 * update the preview properly) 

[fbbeba1070df470](https://bitbucket.org/knime/knime-base/commits/fbbeba1070df470) Adrian Nembach *2020-11-03 13:27:57*

**AP-15288: Fixed bug file extension replacement**

 * AP-15288 (Folder selection should also add empty folders to the created 
 * archive) 

[7698fb2c2fe0299](https://bitbucket.org/knime/knime-base/commits/7698fb2c2fe0299) Mark Ortmann *2020-11-03 10:26:19*

**AP-15378: Fixed UI**

 * AP-15378 (Show error rather than disable option to include parent dir 
 * compress/copy move node) 

[e2896e35a6f0bbc](https://bitbucket.org/knime/knime-base/commits/e2896e35a6f0bbc) Mark Ortmann *2020-11-03 10:22:10*

**AP-15025: Adapted Create Directory to the new UI**

 * AP-15025 (Revise Create File/Folder Variables (Labs)) 

[6ccd44ef9eb838f](https://bitbucket.org/knime/knime-base/commits/6ccd44ef9eb838f) Mark Ortmann *2020-11-02 17:01:37*

**AP-15025: Adapted Create File/Folders Variables to the new UI**

 * AP-15025 (Revise Create File/Folder Variables (Labs)) 

[238e72b73554bb8](https://bitbucket.org/knime/knime-base/commits/238e72b73554bb8) Mark Ortmann *2020-11-02 17:01:37*

**AP-15025: Adapted Create Temp Dir to the new UI**

 * AP-15025 (Revise Create File/Folder Variables (Labs)) 

[22596e9e872f670](https://bitbucket.org/knime/knime-base/commits/22596e9e872f670) Mark Ortmann *2020-11-02 17:01:37*

**AP-15025: Implemented new JTable to allow users to create FSLocations**

 * AP-15025 (Revise Create File/Folder Variables (Labs)) 

[0b1eb3f74c44559](https://bitbucket.org/knime/knime-base/commits/0b1eb3f74c44559) Mark Ortmann *2020-11-02 17:01:37*

**AP-14489: Add preview to all tabs in CSV and Simple File Reader**

 * AP-14489 (Add Preview to all tabs in TRF based reader dialogs) 

[340224d1382ccf5](https://bitbucket.org/knime/knime-base/commits/340224d1382ccf5) Adrian Nembach *2020-11-02 16:34:59*

**AP-15382: Adapted copy move node description**

 * AP-15382 (Copy/Move files ensure that empty folder are being copied when 
 * Folder is selected) 

[bedf6739138abf1](https://bitbucket.org/knime/knime-base/commits/bedf6739138abf1) Lars Schweikardt *2020-11-02 15:25:21*

**AP-15364: context extractor node: "" instead of null for jobID**

 * AP-15364: Empty String Flow Variable crashes Python Serialization 
 * Extract Context Properties: inconsistent output of context.job.id when 
 * running on AP 

[b1ab5bbc7f9c00a](https://bitbucket.org/knime/knime-base/commits/b1ab5bbc7f9c00a) Bernd Wiswedel *2020-10-30 15:31:27*

**AP-13456: Changes to node facing API**

 * AP-13456 (Implement table spec transformation in dialog) 

[68ede12833a0445](https://bitbucket.org/knime/knime-base/commits/68ede12833a0445) Adrian Nembach *2020-10-30 14:59:18*

**AP-13456: Adapt CSV and Simple File Reader to TRF changes**

 * AP-13456 (Implement table spec transformation in dialog) 

[1c22f26859035cb](https://bitbucket.org/knime/knime-base/commits/1c22f26859035cb) Adrian Nembach *2020-10-30 14:59:18*

**AP-15140: Changed ISE to IAE**

 * AP-15140 (Support FSLocation and Collection FlowVariables in 
 * Variable Loop End) 

[657aaf77a694d96](https://bitbucket.org/knime/knime-base/commits/657aaf77a694d96) Mark Ortmann *2020-10-30 14:02:09*

**AP-15140: Changed input port from optional to required**

 * AP-15140 (Support FSLocation and Collection FlowVariables in 
 * Variable Loop End) 

[e8369200b76e21e](https://bitbucket.org/knime/knime-base/commits/e8369200b76e21e) Mark Ortmann *2020-10-30 12:20:59*

**AP-15378: Added new swingworker to copy move node**

 * Replaced the old swing worker with the new one in the copy move node. 
 * AP-15378 (Show error rather than disable option to include parent dir 
 * compress/copy move node) 

[f661e78f7217a38](https://bitbucket.org/knime/knime-base/commits/f661e78f7217a38) Lars Schweikardt *2020-10-30 11:33:23*

**AP-15378: Added new swingworker to compress node**

 * Replaced the old swing worker with the new one in the compress node. 
 * AP-15378 (Show error rather than disable option to include parent dir 
 * compress/copy move node) 

[ac3bb1118dbec34](https://bitbucket.org/knime/knime-base/commits/ac3bb1118dbec34) Lars Schweikardt *2020-10-30 11:33:23*

**AP-15378: Adapted includeSourceFolderSwingWorker**

 * Adapted the includeSourceFolderSwingWorker so that it returns a 
 * statusmessage instead of a boolean. 
 * AP-15378 (Show error rather than disable option to include parent dir 
 * compress/copy move node) 

[1b3c20dd4269eb3](https://bitbucket.org/knime/knime-base/commits/1b3c20dd4269eb3) Lars Schweikardt *2020-10-30 11:33:23*

**AP-15382: Changed Files in Folder desc copy move**

 * Changed the description of the files in folder option of the copy move 
 * node to make it more clear that not only the file get copied but also 
 * the necessary folder structure. 
 * AP-15382 (Copy/Move files ensure that empty folder are being copied when 
 * Folder is selected) 

[8ee576afa715f09](https://bitbucket.org/knime/knime-base/commits/8ee576afa715f09) Lars Schweikardt *2020-10-30 11:32:03*

**AP-15382: Replaced copied column with dir colum**

 * Replaced the copied boolean column with a new column called directory 
 * which indicates whether the copied path is a file or directory 
 * Ap-15382 (Copy/Move files ensure that empty folder are being copied when 
 * Folder is selected) 

[8ec05dc52656818](https://bitbucket.org/knime/knime-base/commits/8ec05dc52656818) Lars Schweikardt *2020-10-30 11:32:03*

**AP-15382: Fixed delete source files folders bug**

 * Fixed a bug in the copy move node which caused an ArrayIndexOutOfBound 
 * exception and a bug in this option which lead to that the node will not 
 * fail of the fail option is checked. 
 * AP-15382 (Copy/Move files ensure that empty folder are being copied when 
 * Folder is selected) 

[03c969f18737d87](https://bitbucket.org/knime/knime-base/commits/03c969f18737d87) Lars Schweikardt *2020-10-30 11:32:03*

**AP-13784: Added error when creating empty archives**

 * AP-13784 (Compress Files no table input (new file handling)) 

[097e0c4c1a8d26c](https://bitbucket.org/knime/knime-base/commits/097e0c4c1a8d26c) Mark Ortmann *2020-10-30 10:47:02*

**AP-13783: Decompress Files (Labs) now also lists folders**

 * AP-13783 (De-compress Files (new file handling)) 

[9e37c1ccd680ace](https://bitbucket.org/knime/knime-base/commits/9e37c1ccd680ace) Mark Ortmann *2020-10-29 17:05:30*

**Renamed settings Compress/Decompress**


[7001532e513e640](https://bitbucket.org/knime/knime-base/commits/7001532e513e640) Mark Ortmann *2020-10-29 17:05:30*

**AP-15288: Added functionality to include empty folders to archives**

 * AP-15288 (Folder selection should also add empty folders to the created archive) 

[5a9d283b6002ef8](https://bitbucket.org/knime/knime-base/commits/5a9d283b6002ef8) Mark Ortmann *2020-10-29 17:05:30*

**AP-13784: Removed AR compression**

 * AP-13784 (Compress Files no table input (new file handling)) 

[34d46715d607f9e](https://bitbucket.org/knime/knime-base/commits/34d46715d607f9e) Mark Ortmann *2020-10-29 17:05:30*

**AP-15382: Copy complete folder Copy Move**

 * Added the option to completely copy a folder with all its files and 
 * folders (including empty folders). As well as a new option to let the 
 * node fail if something goes wrong during the deletion process. 
 * AP-15382 (Copy/Move files ensure that empty folder are being copied when 
 * Folder is selected) 

[b83a7ede260b218](https://bitbucket.org/knime/knime-base/commits/b83a7ede260b218) Lars Schweikardt *2020-10-28 08:15:55*

**AP-15382: Remove flow variable, renamed delete opt.**

 * Removed pushing of the flow variables and renamed the fail on deletion 
 * option to Fail on unsuccessful deletion and also changed the order of 
 * the flow variables in the flow variable tab. 
 * AP-15382 (Copy/Move files ensure that empty folder are being copied when 
 * Folder is selected) 

[87e8f2299143499](https://bitbucket.org/knime/knime-base/commits/87e8f2299143499) Lars Schweikardt *2020-10-28 08:15:55*

**AP-15140: Minor code enhancements**

 * AP-15140 (Support FSLocation and Collection FlowVariables in 
 * Variable Loop End) 

[55b12a8430f5bfb](https://bitbucket.org/knime/knime-base/commits/55b12a8430f5bfb) Mark Ortmann *2020-10-26 16:54:16*

**AP-15140: Added variable loop end node**

 * AP-15140 (Support FSLocation and Collection FlowVariables in 
 * Variable Loop End) 

[ece58b56f82d192](https://bitbucket.org/knime/knime-base/commits/ece58b56f82d192) Mark Ortmann *2020-10-26 16:10:59*

**AP-15140: Made VariableToCelLConverters closable**

 * AP-15140 (Support FSLocation and Collection FlowVariables in 
 * Variable Loop End) 

[b290bc7cef8c899](https://bitbucket.org/knime/knime-base/commits/b290bc7cef8c899) Mark Ortmann *2020-10-26 16:10:59*

**AP-15140: Changed no variable selected from error to warning**

 * AP-15140 (Support FSLocation and Collection FlowVariables in 
 * Variable Loop End) 

[9a1027db72bbb15](https://bitbucket.org/knime/knime-base/commits/9a1027db72bbb15) Mark Ortmann *2020-10-26 16:10:59*

**AP-15134: Fixed Table Row To Variable Loop Start**

 * AP-15134 (Support Paths and ListCells in Table to Variable Loop Start) 

[969a5b1d5d01f64](https://bitbucket.org/knime/knime-base/commits/969a5b1d5d01f64) Mark Ortmann *2020-10-26 16:09:50*

**AP-15289: Fixed dialog**

 * AP-15289 (Allow hierarchy flattening Compress Compress Files/Folder (Labs)) 

[bbf58770af8bf22](https://bitbucket.org/knime/knime-base/commits/bbf58770af8bf22) Mark Ortmann *2020-10-26 10:09:51*

**AP-15134: Implement Variable Loop Start node**

 * AP-15134 (Support Paths and ListCells in Table to Variable Loop Start) 

[987447321361956](https://bitbucket.org/knime/knime-base/commits/987447321361956) Mark Ortmann *2020-10-23 11:35:45*

**AP-13721: Auto-guessing, improved validation, icons and node doc**

 * AP-13721 (String to Path node) 

[f038cfb6383d4a0](https://bitbucket.org/knime/knime-base/commits/f038cfb6383d4a0) Mark Ortmann *2020-10-23 08:04:51*

**AP-15289: Adapted hierarchy flattening status message and its location**

 * AP-15289 (Allow hierarchy flattening Compress Compress Files/Folder (Labs)) 

[28d80a8b2fb94f9](https://bitbucket.org/knime/knime-base/commits/28d80a8b2fb94f9) Mark Ortmann *2020-10-22 13:06:44*

**AP-15289: Fixed racing condition in IncludeParentFolderSwingWorker**

 * AP-15289 (Allow hierarchy flattening Compress Compress Files/Folder (Labs)) 

[c8d6a0b0bafe397](https://bitbucket.org/knime/knime-base/commits/c8d6a0b0bafe397) Mark Ortmann *2020-10-19 11:31:00*

**AP-15289: Renamed StatusMessageUtils and added NO_OP consumer**

 * AP-15289 (Allow hierarchy flattening Compress Compress Files/Folder (Labs)) 

[f4c4fb0a8049d3b](https://bitbucket.org/knime/knime-base/commits/f4c4fb0a8049d3b) Mark Ortmann *2020-10-19 11:30:59*

**AP-15289: Added flatten hierarchy option to PathReliativizer**

 * AP-15289 (Allow hierarchy flattening Compress Compress Files/Folder (Labs)) 

[e8dfaeb01bf531b](https://bitbucket.org/knime/knime-base/commits/e8dfaeb01bf531b) Mark Ortmann *2020-10-19 11:30:59*

**AP-15289: Added option for hierarchy flattening to the dialog**

 * AP-15289 (Allow hierarchy flattening Compress Compress Files/Folder (Labs)) 

[e226cab32b699d6](https://bitbucket.org/knime/knime-base/commits/e226cab32b699d6) Mark Ortmann *2020-10-19 11:30:59*

**AP-15135: Removed unused import**

 * AP-15135: (Create Paths uses Reader instead of WriterFileChooser) 

[7960bda20b9d098](https://bitbucket.org/knime/knime-base/commits/7960bda20b9d098) Mark Ortmann *2020-10-16 14:53:28*

**AP-13721: Added missing warnings invocation**

 * AP-13721 (String to Path node) 

[5745e8a09b106ba](https://bitbucket.org/knime/knime-base/commits/5745e8a09b106ba) Mark Ortmann *2020-10-16 10:13:02*

**AP-15328: ColumnSelection for TransposeTableNodeModel**


[79f61fbe007a9bc](https://bitbucket.org/knime/knime-base/commits/79f61fbe007a9bc) Christian Dietz *2020-10-16 07:48:02*

**AP-14945: Fix incorrect checking of modifier keys**

 * In the past, when determining whether modifier keys are pressed 
 * when entering a value via keyboard presses in the Table 
 * Creator node, we would compare the modifier key mask of the 
 * current keyboard event with a virtual key code. This is 
 * semantically incorrect. 
 * AP-14945 (bug\AP-14945-shift-key-doesnt-work-in-table-) 

[4c9fc77839748e7](https://bitbucket.org/knime/knime-base/commits/4c9fc77839748e7) Benjamin Moser *2020-10-15 15:15:27*

**AP-15025: Revised UI, configure behavior and name of Create Paths (labs)**

 * node 
 * AP-15025 (Revise Create Paths (Labs)) 

[14fd7864c4dab9b](https://bitbucket.org/knime/knime-base/commits/14fd7864c4dab9b) Mark Ortmann *2020-10-15 12:37:02*

**AP-14897: Adapted API-filters**

 * AP-14897 (API to create FastTables in Nodes) 

[0659849ec405407](https://bitbucket.org/knime/knime-base/commits/0659849ec405407) Mark Ortmann *2020-10-14 13:27:36*

**AP-14872: Refactor TRF to allow for spec transformations in the dialog**

 * AP-14872 (Support table spec transformations in the TRF backend) 

[5d9ceeb71a06aed](https://bitbucket.org/knime/knime-base/commits/5d9ceeb71a06aed) Adrian Nembach *2020-10-14 13:08:52*

**AP-15341: Fixed broken links for nodes converting cell to variable and vice versa**

 * AP-15341 (Fix table row/column to variable and vice versa java doc) 

[140103038817bc0](https://bitbucket.org/knime/knime-base/commits/140103038817bc0) Mark Ortmann *2020-10-13 13:18:41*

**AP-13433: Minor code cleanup**

 * AP-13433 (Copy / move files no table input (new file handling)) 

[45c5a84ea71ae68](https://bitbucket.org/knime/knime-base/commits/45c5a84ea71ae68) Mark Ortmann *2020-10-13 12:52:47*

**AP-15186: Changed FileCopier copy routine**

 * Changed the copy method in the FileCopier from Files.copy(Path, 
 * outputStream) to Files.copy(path, path) to avoid unnecessary streaming 
 * through the jvm in case of using the very same filesystem for source and 
 * destination. 
 * AP-15186 (Avoid downloading data in Copy/Move Files/Folders) 

[9cd3554a208d53f](https://bitbucket.org/knime/knime-base/commits/9cd3554a208d53f) Lars Schweikardt *2020-10-12 12:05:11*

**AP-15299: UI improvements and config name adaptations**

 * AP-15299 (Add Column FIlter to Table Row to Variable (Labs)) 

[1ecb75fd9194fda](https://bitbucket.org/knime/knime-base/commits/1ecb75fd9194fda) Mark Ortmann *2020-10-12 11:22:40*

**AP-15306: Changed configure and execution behavior Table Row to Variable (Labs)**

 * The node now fails for missing cells that have no associated default value 
 * AP-15306 (Revise default handling in Table Row to Variable (Labs)) 

[13de3042224cf84](https://bitbucket.org/knime/knime-base/commits/13de3042224cf84) Mark Ortmann *2020-10-12 11:22:39*

**AP-15299: Added column filter to table row to variable (labs)**

 * AP-15299 (Add Column FIlter to Table Row to Variable (Labs)) 

[c6246b4399e928d](https://bitbucket.org/knime/knime-base/commits/c6246b4399e928d) Mark Ortmann *2020-10-12 11:20:52*

**AP-15088: Fixed issues copyMove StatusMessageRep**

 * Fixed several issues with the copyMove StatusMessageReporter like that 
 * the reporter should report an error if we cannot access files from the 
 * source path as well as proper message in case the source path is empty. 
 * AP-15088 (Implement StatusMessageReporter for Copy/Move Files no table 
 * input) 

[1fd07e3d0000a78](https://bitbucket.org/knime/knime-base/commits/1fd07e3d0000a78) Lars Schweikardt *2020-10-12 08:27:01*

**AP-13433: Fixed include Parent folder bug copy move**

 * If a folder above the root was selected the node did not offer the 
 * include parent folder option (which is now renamed to Included selected 
 * source folder) which it now does. 
 * AP-13433 (Copy / move files no table input (new file handling)) 

[16920635b5d53cf](https://bitbucket.org/knime/knime-base/commits/16920635b5d53cf) Lars Schweikardt *2020-10-12 08:27:01*

**AP-15088: Fixed bug in copyMove statusReporter**

 * Fixed a bug in the status message reporter of the copy/move files node 
 * which was responsible that the ReadPathAccessor was closed to early 
 * which lead to an inconsistent behaviour in the way the node reports 
 * StatusMessages. 
 * AP-15088 (Implement StatusMessageReporter for Copy/Move Files no table 
 * input) 

[ab509d38664732f](https://bitbucket.org/knime/knime-base/commits/ab509d38664732f) Lars Schweikardt *2020-10-12 08:27:00*

**Corrected Column Appender's node description**


[ab561d5eb935781](https://bitbucket.org/knime/knime-base/commits/ab561d5eb935781) Mark Ortmann *2020-10-12 08:13:34*

**AP-15313: Don't set BaseFileSystemProvider.m_fileSystem to null**

 * Also, improve exception handling in Create Temp Dir (Labs) node, when deleting 
 * temp dirs in the background. 
 * AP-15313 (Resetting connector node together with Create Temp Dir node sometimes results in NPE) 

[5e0a8c34c8f28c4](https://bitbucket.org/knime/knime-base/commits/5e0a8c34c8f28c4) Bjoern Lohrmann *2020-10-09 06:52:06*

**AP-13784: Dialog and config changes**

 * - &quot;Include parent&quot; is now called &quot;Include selected source folder&quot; and 
 * moved below the file choosers (the config and node description is 
 * changed accordingly) 
 * - The file extension error is now displayed in the status of the 
 * destination file chooser 
 * AP-13784 (Compress Files no table input (new file handling)) 

[5baff11588fba6d](https://bitbucket.org/knime/knime-base/commits/5baff11588fba6d) Adrian Nembach *2020-10-08 11:17:08*

**Fixed NPE in deprecated code**

 * (Came up during testing when bulk-instantiating all possible Nodes) 
 * Error message was: 
 * Could not instantiate org.knime.base.node.mine.sota.SotaNodeFactory: 
 * OutPortType[0] must not be null! 

[b611f10a9142da1](https://bitbucket.org/knime/knime-base/commits/b611f10a9142da1) Bernd Wiswedel *2020-10-07 09:52:48*

**AP-15136: Decompress fixed folder bug**

 * Also removed the 
 * functionality that if the missing folder option is unchecked and a 
 * non existent folder is used the 
 * StatusMessageReporter gives an error as well if the node gets executed. 
 * t 
 * AP-15136 (Implement StatusMessageReporter for De-compress Files no table 
 * input) 

[c4468243c329b8a](https://bitbucket.org/knime/knime-base/commits/c4468243c329b8a) Lars Schweikardt *2020-10-05 12:28:22*

**AP-13433: Fixed copy/move desc and folder bug**

 * Fixed the node description to match the functionality and fixed the bug 
 * that the node always creates folders which are non existent even if the 
 * create missing folders option is unchecked. Also removed the 
 * functionalty that if the missing folder option is unchecked and a single 
 * non existent folder is added to an existing path the 
 * StatusMessageReporter gives an error as well if the node gets executed. 
 * AP-13433 (Copy / move files no table input (new file handling)) 

[30278b41d3c904e](https://bitbucket.org/knime/knime-base/commits/30278b41d3c904e) Lars Schweikardt *2020-10-05 07:29:00*

**AP-15135: Use custom file chooser for Create Paths**

 * AP-15135: (Create Paths uses Reader instead of WriterFileChooser) 

[325b77aaf4a6083](https://bitbucket.org/knime/knime-base/commits/325b77aaf4a6083) Adrian Nembach *2020-10-02 08:36:32*

**AP-13783: Make Compress node insensitive to file extension case**

 * AP-13783 (Compress Files no table input (new file handling)) 

[e5dea1b0cafc738](https://bitbucket.org/knime/knime-base/commits/e5dea1b0cafc738) Adrian Nembach *2020-09-30 08:39:05*

**AP-13783: Make Decompress node dialog insensitive to file extension case**

 * AP-13783 (De-compress Files (new file handling)) 

[5e3818d90a25f56](https://bitbucket.org/knime/knime-base/commits/5e3818d90a25f56) Adrian Nembach *2020-09-30 08:38:36*

**DEVOPS-483: Update build.properties to include LICENSE file in bin & src jars.**

 * DEVOPS-483 (Add LICENSE.txt files to binary builds of all our plug-ins) 

[2b2a4ed5e650567](https://bitbucket.org/knime/knime-base/commits/2b2a4ed5e650567) Sebastian Gerau *2020-09-28 22:53:18*

**AP-15134: Decompress Status message reporter**

 * AP-15134 (Implement StatusMessageReporter for De-compress Files no table 
 * input) 

[4e41020c622ff8f](https://bitbucket.org/knime/knime-base/commits/4e41020c622ff8f) Lars Schweikardt *2020-09-25 09:19:08*

**AP-15041: added job id to extract context node**

 * AP-15041 (Extract Context Properties node should provide job ID on server) 

[f57b231f5becd05](https://bitbucket.org/knime/knime-base/commits/f57b231f5becd05) Moritz Heine *2020-09-24 13:06:08*

**AP-15088: CopyMove Status message reporter**

 * AP-15088 (Implement StatusMessageReporter for Copy/Move Files no table 
 * input) 

[f703757ec163c64](https://bitbucket.org/knime/knime-base/commits/f703757ec163c64) Lars Schweikardt *2020-09-24 10:33:44*

**AP-13784: Added Compress Files/Folder (Labs) node**

 * AP-13784 (Compress Files no table input (new file handling)) 

[0c80d7c0f5e26d6](https://bitbucket.org/knime/knime-base/commits/0c80d7c0f5e26d6) Timmo Waller-Ehrat *2020-09-23 11:28:41*

**AP-13783: Added Decompress Files (Labs) node**

 * AP-13783 (De-compress Files (new file handling)) 

[3a4dac765387519](https://bitbucket.org/knime/knime-base/commits/3a4dac765387519) Timmo Waller-Ehrat *2020-09-23 11:22:51*

**AP-13433: Fixed bug copying between diff. FS**

 * Fixed a bug which prevented to copy files between different Filesystems. 
 * AP-13433 (Copy / move files no table input (new file handling)) 

[3369cc9d75209d5](https://bitbucket.org/knime/knime-base/commits/3369cc9d75209d5) Lars Schweikardt *2020-09-22 09:54:28*

**AP-13433: Implemented shared classes for filehandling util**

 * Implemented a swing worker which check if the include parent folder 
 * can be checked based on a entered path in the filechooser. This 
 * will be used at least in the copy move file and uncompress node. Also 
 * added a Path manipulator interface as well as a new class which 
 * implements this interface to relativzize a file path based on several 
 * options. 
 * AP-13433 (Copy / move files no table input (new file handling)) 

[bb93a108d472bb1](https://bitbucket.org/knime/knime-base/commits/bb93a108d472bb1) Lars Schweikardt *2020-09-21 12:24:45*

**AP-13433: Implementation of the Copy Move Files Node**

 * AP-13433 (Copy / move files no table input (new file handling)) 

[b4317d3a8efed1c](https://bitbucket.org/knime/knime-base/commits/b4317d3a8efed1c) Lars Schweikardt *2020-09-21 12:24:45*

**AP-15142: Show warning for already taken column name only in configure**

 * AP-15142 (String to Path remove column name already taken warning) 

[1b725560e2302a4](https://bitbucket.org/knime/knime-base/commits/1b725560e2302a4) Timmo Waller-Ehrat *2020-09-21 08:30:24*

**AP-15047: Reduced visibilities and minor refactoring**

 * AP-15047 (Support Paths in Table to Variable and vice versa) 

[79f63a6982f54af](https://bitbucket.org/knime/knime-base/commits/79f63a6982f54af) Mark Ortmann *2020-09-18 10:31:37*

**AP-15047: Reverted d16977 and a1039b**

 * AP-15047 (Support Paths in Table to Variable and vice versa) 

[0868db69134bcb0](https://bitbucket.org/knime/knime-base/commits/0868db69134bcb0) Mark Ortmann *2020-09-18 10:31:37*

**AP-15047: Ported changes to the new versions of the nodes**

 * Contains the fix for AP-15132 (Table Row to Variable does not omit 
 * missing cells for columns without default value) 
 * AP-15047 (Support Paths in Table to Variable and vice versa) 

[ddf6239ecd73062](https://bitbucket.org/knime/knime-base/commits/ddf6239ecd73062) Mark Ortmann *2020-09-18 09:38:52*

**AP-15047: Deprecated VariableAndDataCellPair/Utility**

 * AP-15047 (Support Paths in Table to Variable and vice versa) 

[a1039b2005382ec](https://bitbucket.org/knime/knime-base/commits/a1039b2005382ec) Mark Ortmann *2020-09-17 15:18:11*

**AP-15047: Added flowvar/cell converters and tests**

 * AP-15047 (Support Paths in Table to Variable and vice versa) 

[ecd0939e0dee5be](https://bitbucket.org/knime/knime-base/commits/ecd0939e0dee5be) Mark Ortmann *2020-09-17 15:18:10*

**AP-15047: Path support for nodes converting variables to cells and vice**

 * versa 
 * Contains the fix for AP-15132 (Table Row to Variable does not omit 
 * missing cells for columns without default value) 
 * AP-15047 (Support Paths in Table to Variable and vice versa) 

[d169777ec4ab3f2](https://bitbucket.org/knime/knime-base/commits/d169777ec4ab3f2) Mark Ortmann *2020-09-17 15:18:10*

**AP-15117: Added meta data to column spec**

 * AP-15117 (String To Path Node Model not creating meta data) 

[2388d59f6bb703b](https://bitbucket.org/knime/knime-base/commits/2388d59f6bb703b) Mark Ortmann *2020-09-15 11:32:47*

**AP-15060: Instantiate JSpinner in createFilePanel**

 * This method won&#39;t be called in batch mode and thus no exception will be 
 * thrown. Methods accessing the spinner have been adjusted to avoid 
 * possible NPEs. 
 * AP-15060 (SimpleFileReader does not work on WindowsServer) 

[17d41cabf696858](https://bitbucket.org/knime/knime-base/commits/17d41cabf696858) Adrian Nembach *2020-08-31 19:21:41*

**AP-13721: Fixed a bug where the dialog accesses the wrong spec index**

 * AP-13721 (String to Path node) 

[6e8078ef7e35f21](https://bitbucket.org/knime/knime-base/commits/6e8078ef7e35f21) Mark Ortmann *2020-08-25 08:23:20*

**AP-13727: Changed tmp to temp**

 * AP-13727 (Create Temp Dir node) 

[fa1f5ba4cb20482](https://bitbucket.org/knime/knime-base/commits/fa1f5ba4cb20482) Mark Ortmann *2020-08-20 14:37:13*

**AP-14912: Remove Custom URL FS from Create Paths (Labs) node**

 * The node behaves incorrectly if the Custom/KNIME URL file system is 
 * selected. I created a follow-up ticket (AP-15012) to add it again once 
 * we have resolved the issues. 
 * AP-14912 (Custom URL does not disable mode selection) 

[60d66c9daf84566](https://bitbucket.org/knime/knime-base/commits/60d66c9daf84566) Adrian Nembach *2020-08-18 10:00:07*

**AP-14912: Remove Custom/KNIME URL file system from List Files/Folders**

 * It was accidentally added with AP-13432 
 * AP-14912 (Custom URL does not disable mode selection) 

[a300499918546a5](https://bitbucket.org/knime/knime-base/commits/a300499918546a5) Adrian Nembach *2020-08-17 16:30:02*

**AP-14912: Properly update filter mode enabled status**

 * The logic controlling the enabled status of the filter modes is moved 
 * from the dialog component into the settings model, where it belongs. 
 * AP-14912 (Custom URL does not disable mode selection) 

[086546c08698442](https://bitbucket.org/knime/knime-base/commits/086546c08698442) Adrian Nembach *2020-08-17 13:48:13*

**AP-13721: Add API filters for streaming related warnings**

 * AP-13721 (String to Path node) 

[e2529d411761d9d](https://bitbucket.org/knime/knime-base/commits/e2529d411761d9d) Adrian Nembach *2020-08-14 15:52:07*

**AP-13721: Changed port order and minor code cleanup**

 * AP-13721 (String to Path node) 

[d1402049b414b1d](https://bitbucket.org/knime/knime-base/commits/d1402049b414b1d) Mark Ortmann *2020-08-14 15:52:07*

**AP-13432: Correct number of default file systems in node description**

 * AP-13432 (Delete files no table input (new file handling)) 

[6a345537c557463](https://bitbucket.org/knime/knime-base/commits/6a345537c557463) Adrian Nembach *2020-08-14 11:09:40*

**AP-13748: Recursive loop starts to copy their 'loop' data**

 * AP-13748: Error when saving workflow with failure in Recursive Loop End 

[65ce8d54e6c9021](https://bitbucket.org/knime/knime-base/commits/65ce8d54e6c9021) Bernd Wiswedel *2020-08-14 09:44:15*

**AP-13432: Remove Custom/KNIME URL from node description**

 * AP-13432 (Delete files no table input (new file handling)) 

[e945b923d15751d](https://bitbucket.org/knime/knime-base/commits/e945b923d15751d) Adrian Nembach *2020-08-13 09:45:10*

**api filters fixed**


[2551fd495ce67a5](https://bitbucket.org/knime/knime-base/commits/2551fd495ce67a5) Bernd Wiswedel *2020-08-11 13:26:43*

**AP-13721: Adapted SettingsModelFileSystem constructor**

 * AP-13721 (String to Path node) 

[ecd60d663b36655](https://bitbucket.org/knime/knime-base/commits/ecd60d663b36655) Mark Ortmann *2020-08-11 13:19:49*

**AP-13721: Updated String to Path node**

 * Added option to fail on missing values and made the node streamable 
 * AP-13721 (String to Path node) 

[01c628ec35587e0](https://bitbucket.org/knime/knime-base/commits/01c628ec35587e0) Mark Ortmann *2020-08-10 15:51:01*

**AP-13432: Exclude Custom/KNIME URL for delete and create (temp) dir**

 * These nodes do not allow to use Custom/KNIME URL because the 
 * functionality they require is not available for this file system. 
 * AP-13432 (Delete files no table input (new file handling)) 

[9173ffe90d73239](https://bitbucket.org/knime/knime-base/commits/9173ffe90d73239) Adrian Nembach *2020-08-10 13:17:28*

**AP-13432: Added Delete Files node**

 * AP-13432 (Delete files no table input (new file handling)) 

[15c04bc6973ab72](https://bitbucket.org/knime/knime-base/commits/15c04bc6973ab72) Timmo Waller-Ehrat *2020-08-05 14:14:11*

**AP-13721: Added String to Path node**

 * AP-13721 (String to Path node) 

[9e2ffff3a389494](https://bitbucket.org/knime/knime-base/commits/9e2ffff3a389494) Timmo Waller-Ehrat *2020-08-05 08:35:17*

**AP-13434: Added Create Directory Node**

 * AP-13434 (Create directory (new file handling)) 

[4671a6f4fc5f979](https://bitbucket.org/knime/knime-base/commits/4671a6f4fc5f979) Timmo Waller-Ehrat *2020-07-31 14:29:20*

**AP-13434: Added Create Directory Node**

 * AP-13434 (Create directory (new file handling)) 

[7d708b05a40a045](https://bitbucket.org/knime/knime-base/commits/7d708b05a40a045) Timmo Waller-Ehrat *2020-07-31 12:38:08*

**Increased version range of knime-core**


[0b2e80a9d0c5147](https://bitbucket.org/knime/knime-base/commits/0b2e80a9d0c5147) Moritz Heine *2020-07-31 09:30:48*

**SRV-3056: use wrapped output stream that disconnects automatically**

 * SRV-3056 (Error while Executing Workflow that Reads and Writes a table within a loop on KNIME Server) 

[b009fd398c3eb20](https://bitbucket.org/knime/knime-base/commits/b009fd398c3eb20) Moritz Heine *2020-07-30 12:57:48*

**AP-14457: Don't show the table preview for invalid locations**

 * Until now the preview was just not updated in this case, leaving the 
 * previous preview in place even though we might be pointing to a 
 * completely different location now. 
 * AP-14457 (AbstractSettingsModelFileChooser: Persist settings when 
 * adding/removing the file system port) 

[19a72e59a8ec35b](https://bitbucket.org/knime/knime-base/commits/19a72e59a8ec35b) Adrian Nembach *2020-07-28 14:53:11*

**Revert "AP-14748: CSV Reader: #onClose in EDT thread"**

 * This reverts commit 1587db94cc6412cfa15d06620caaee265dc829a5. 
 * This is no longer required as the EDT queuing is done in the super class 

[bfcd55be227f0d8](https://bitbucket.org/knime/knime-base/commits/bfcd55be227f0d8) Bernd Wiswedel *2020-07-24 13:07:16*

**Version bump for 4.3 release**


[ed379d2e9e9684b](https://bitbucket.org/knime/knime-base/commits/ed379d2e9e9684b) Thorsten Meinl *2020-07-23 15:06:42*

**added sonarlint bindings**


[2f7758647791f24](https://bitbucket.org/knime/knime-base/commits/2f7758647791f24) Bernd Wiswedel *2020-07-16 19:32:48*

