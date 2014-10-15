The program BioSearcher.java takes an input file (classbios.txt), breaks it into chunks for each individual bio, creates a Lucene index, adds those bios to the index, gets a query from the user, searches for that query, and displays the results. It uses the StandardAnalyzer. Make sure that "classbios.txt" is in the same directory as the Java program.

Results are displayed as phrases, with the actual query term enclosed in HTML bold tags. Each result is preceded by the "title" of the bio. This title is of the form "Bio_1", "Bio_2", etc.

The title and the content were the only two fields included. There is not enough information in the original document to consistently deduce authors or any other obvious field.

When compiling, be sure to have included these jars in your CLASSPATH:
lucene-core-4.10.1.jar
lucene-queryparser-4.10.1.jar
lucene-analyzers-common-4.10.1.jar
lucene-highlighter-4.10.1.jar
lucene-memory-4.10.1.jar

Three creative queries:
A URL:
  Input: http://www.google.com
  Parsed query: http:// content:www.google.com

An e-mail address:
  Input: michaelvanger2014@u.northwestern.edu
  Parsed query: content:michaelvanger2014 content:u.northwestern.edu

A date:
  Input: 10/15/14
  Parsed query: content:10 content:/15/ content:14

  A note: This apparently found two matches, but also an exception. Enclosing the input with quotations gives this for a parsed query: content:"10 15 14". This has no matches and no exception.