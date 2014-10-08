homework
  write a page about yourself, background, why taking this course

7 or 8 homeworks, no exams
  graded based on results
  ie any language or library

information retrieval
  inverted index
    postings lists
  lucene (apache)
    java
    pylucene
    solr

regex
  implicit concatenation
    "hello"
  or
    "H|h"
  kleene star
    zero or more
    *

    + // one or more (similar to *)

  "(H|h)ello*"
    matches hell, Hell, Hello, hello, helloooo etc

  \d => digit
  \w => a-zA-Z0-9_
  [] => or (ie [Hh]ello is like (H|h)ello)
    [0-9]
  . => wildcard

python re module
  pattern = re.compile('string') // creates a regex object
  pattern.search("another string") // searches the string for the pattern
  pattern.match("another string") // searches from the start only