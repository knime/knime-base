# Commits

**Fix wrong usage of DefaultStatusMessage.mkError**

 * The mkError method uses the first parameter as format string. Some 
 * methods call it with an error message as first and only parameter. This 
 * produce problems if the error message contain a &quot;%&quot; character, e.g. from 
 * a filename. 

[6338761615e31fe](https://bitbucket.org/knime/knime-base/commits/6338761615e31fe) Sascha Wolke *2020-11-18 15:06:56*

**AP-15271: Add support for user defined file names**

 * Added support column selection for output file names 
 * Added support for custom user defined output file name pattern 
 * Update node description for new elements added to UI 
 * AP-15271 (Support option to allow user to specify the output file name for the Binary Objects to Files (Labs) node) 

[f91a7a469c48f2a](https://bitbucket.org/knime/knime-base/commits/f91a7a469c48f2a) Ayaz Ali Qureshi *2020-11-17 13:57:15*

**AP-15269: Implement removing of binary column from output**

 * Provide option to user for removing binary object column from output 
 * AP-15269 (Support removal of the binary object column for the Binary Objects to Files (Labs) node) 

[08a47fae11bf880](https://bitbucket.org/knime/knime-base/commits/08a47fae11bf880) Ayaz Ali Qureshi *2020-11-05 15:55:01*

**AP-15479: Changed package structure**

 * AP-15479 (Move utility filehandling nodes to filehandling.utility) 

[89679c35674703f](https://bitbucket.org/knime/knime-base/commits/89679c35674703f) Mark Ortmann *2020-11-05 15:26:13*

**AP-15479: Moved all filehandling utility nodes to corresponding plugin**

 * AP-15479 (Move utility filehandling nodes to filehandling.utility) 

[2fd5c498cfb51c6](https://bitbucket.org/knime/knime-base/commits/2fd5c498cfb51c6) Mark Ortmann *2020-11-05 15:26:13*

**AP-15479: Code cleanup**

 * AP-15479 (Move utility filehandling nodes to filehandling.utility) 

[1e77db8984c8fd0](https://bitbucket.org/knime/knime-base/commits/1e77db8984c8fd0) Mark Ortmann *2020-11-05 15:26:13*

**AP-15268: Implement execute logic Binary Objects to Files node**

 * Implement execution logic for Binary Objects to Files Node, currently hard coding filename generation with File_idx.dat pattern 
 * AP-15268 (Implement execute of the Binary Objects to Files (Labs) node) 

[6a7a196d3c727b7](https://bitbucket.org/knime/knime-base/commits/6a7a196d3c727b7) Ayaz Ali Qureshi *2020-11-03 09:30:04*

**AP-15367: Implemented basic version of Path to String**

 * AP-15367 (Path to String Node) 

[41c593231766005](https://bitbucket.org/knime/knime-base/commits/41c593231766005) Mark Ortmann *2020-10-23 08:04:51*

**AP-13479: Auto-guessing warning and missing value handling**

 * AP-13479 (File Meta Info (new filehandling)) 

[3b846b6474b9839](https://bitbucket.org/knime/knime-base/commits/3b846b6474b9839) Mark Ortmann *2020-10-23 08:04:51*

**Added missing LICENSE.txt to utility.nodes source build**


[8a9e1c5cf23b5a1](https://bitbucket.org/knime/knime-base/commits/8a9e1c5cf23b5a1) Mark Ortmann *2020-10-23 08:04:21*

**AP-15265: Implement configure of the Binary Objects to Files**

 * - Add autoGuess and validate functionalities in the NodeModel 
 * AP-15265 Implement configure of the Binary Objects to Files (Labs) 

[1d20eef8268f38d](https://bitbucket.org/knime/knime-base/commits/1d20eef8268f38d) Ayaz Ali Qureshi *2020-10-23 07:16:48*

**AP-15264: Implement basic dialog for Binary Objects to Files**

 * - Also implement the load and save functionality 
 * - Updated the node description to match UI 
 * AP-15264 (Implement basic dialog for Binary Objects to Files (Labs)) 

[308f7d4188596a6](https://bitbucket.org/knime/knime-base/commits/308f7d4188596a6) Ayaz Ali Qureshi *2020-10-16 07:41:23*

**AP-15263: Setup Basic Structure for Binary Objects to Files Node**

 * Setup basic directory structure for Binary Objects to Files Node 
 * AP-15263 (Create Binary Objects to Files skeleton (new filehandling)) 

[71ce5d0de512e37](https://bitbucket.org/knime/knime-base/commits/71ce5d0de512e37) Ayaz Ali Qureshi *2020-10-13 12:21:26*

**AP-13479: Added new plugins for filehandling utility nodes**

 * AP-13479 (File Meta Info (new filehandling)) 

[aadb0ee04d4e0d8](https://bitbucket.org/knime/knime-base/commits/aadb0ee04d4e0d8) Mark Ortmann *2020-09-17 14:08:45*

**AP-13479: Implemented File Meta Info node**

 * AP-13479 (File Meta Info (new filehandling)) 

[99db5758769fe9f](https://bitbucket.org/knime/knime-base/commits/99db5758769fe9f) Mark Ortmann *2020-09-17 14:08:45*

