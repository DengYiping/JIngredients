
# JIngredients: A Tool to Detect Third-party Component Reuse in Java Software Release


The tool is an implementation of a method presented in the following paper.

> Takashi Ishio, Raula Gaikovina Kula, Tetsuya Kanda, Daniel M. German, Katsuro Inoue:
> Software Ingredients: Detection of Third-party Component Reuse in Java Software Release.
> In proceedings of MSR 2016, http://dx.doi.org/10.1145/2901739.2901773

The method generates a class-signature from identifiers (package/class/method/field names) in a class,
and then identifies "likely-reused" classes in a target jar file by comparing the class signatures with a signature database. 


## Dependencies

The tool is dependent on the following components.
  * ASM: Bytecode manipulation library (http://asm.ow2.org/)
  * SOBA: A simple wrapper of ASM (http://osdn.jp/projects/soba/)
  * GNU Trove: High performance collection library for Java (http://trove.starlight-systems.com/)

The source code is developed on JavaSE-1.8 and Eclipse, whereas it is compilable on JavaSE-1.7.
 

## Usage

Before executing the tool, a set of jar files is required as "existing components."
In our research, we have used a snapshot of the Maven repository.


### Listing Jar Files

At first, please create a list of jar files in a target directory.
"CreateValidJarList" is the main class to execute the step.

    java sarf.jingredients.CreateValidJarList components-dir output-jar-file-list.txt 2

  * The first argument components-dir specifies a directory including jar files.
  * The second argument output-file-name specifies a text file to be created.
  * The third argument specifies the number of threads used for the step.

A resultant text file will includes jar file names in the specified directory and its sub-directories.
While it is almost the same as "find" utility, a resultant list excludes corrupted jar files, jar files including corrupted class files, and jar files including no class files.
Since it checks the content of jar files, multiple threads should be used. 


### Database Construction

The second step constructs a signature database from the listed jar files.
Execute "CreateDB" as follows.

    java sarf.jingredients.CreateDB jar-file-list.txt db-name 2

  * The first argument jar-file-list.txt is the output of the previous step.
  * The second argument specifies a prefix of database files.  For example, if "dir/my-maven-snapshot" is specified, then the step generates files named "dir/my-maven-snapshot.txt", "dir/my-maven-snapshot.cname.bin", and so on.   
  * The third argument specifies the number of threads used for the step.  This step basically computes hash values for each class, a larger number of threads significantly is better.



### Analysis

Using a created database, the tool detects components in a target jar file.

    java sarf.jingredients.Analysis db-name output-file-prefix DEFAULT target.jar

  * The first argument specifies a database created by the previous step.  The tool also accepts multiple databases separated by ",".
  * The second argument specifies output file names.  
  * The third argument specifies a detection mode.  
    * "DEFAULT" executes an algorithm described in the paper as is.  
    * If "INSIDE" is specified, it excludes components that have the same Java packages as a target jar file from analysis.  If a target jar file is included in a database, this option enables to analyze its internal components.  The DEFAULT behavior reports the target jar file is an exact copy of the jar file in the database.    

The analysis step results in two text files named prefix-rank.txt and prefix-file.txt.
A "rank" file is a CSV including jar file names identified in the target jar file.
Each line indicates a component separated by commas.  Here is an example:

    576,1,C:\maven\ant\ant\1.6.5\jar.jar[576],org/apache/tools/ant/types/PropertySet;org/apache/...

  * The first column shows the number of classes that are detected as reuse.
  * The second column shows the number of jar files that are possible code origin.
  * The third column shows the jar file names and the numbers of classes in them.  In this example, 576 of 576 classes in ant-1.6.5 are included in a target jar file.   
  * The forth column includes a list of reused class names separated by ";".

The other output file includes a list of class files with their origins.  An example is the following.

    org/apache/tools/ant/types/Reference	C:\maven\ant\ant\1.6.5\jar.jar;	
    org/apache/tools/ant/types/RegularExpression	C:\maven\ant\ant\1.6.5\jar.jar;	

Those lines indicate that the classes are likely reused from ant-1.6.5.



## Example Dataset

An example dataset is available at:
http://sel.ist.osaka-u.ac.jp/people/ishio/dataset/MSR2016-Example.zip
The archive contains an example component database and 
an example zip file to try the tool.



## Limitations

Since the tool uses identifiers, obfuscated binaries are not analyzable.

