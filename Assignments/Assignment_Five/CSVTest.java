import java.util.*;
import au.com.bytecode.opencsv.CSVReader;
import java.io.FileNotFoundException;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.util.Version;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.FieldInfo.IndexOptions;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.DocsEnum;
import org.apache.lucene.search.DocIdSetIterator;

public class CSVTest {
  public static void main(String[] args) throws FileNotFoundException, IOException {

    // Use a standard analyzer
    StandardAnalyzer analyzer = new StandardAnalyzer(Version.LUCENE_40);

    // Make a RAMDirectory and an IndexWriter
    Directory index = new RAMDirectory();
    IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_40, analyzer);

    IndexWriter w = new IndexWriter(index, config);

    // Read CSV and add each row to the index
    String csvFilename = "countries_and_capitals.csv";
    CSVReader csvReader = new CSVReader(new FileReader(csvFilename));
    String[] row = null;
    while((row = csvReader.readNext()) != null) {
      addDoc(w, row[0], row[1], row[2], row[3], row[4]);
      // System.out.println(row[0] + " # " + row[1] + " #  " + row[2] + " #  " + row[3] + " #  " + row[4]);
    }

    w.close();
    csvReader.close();

    displayBoolQuery(index);
    displayFuzzyQuery(index);
    displayPhraseQuery(index);

    // getNaiveBayesStuff(index);
  }

  // This method adds a doc to the index
  private static void addDoc(IndexWriter w, String continent, String country, String capital, String country_text, String capital_text) throws IOException {
    // Instantiate a document
    Document doc = new Document();

    // Create the field type for the country text and capital text
    FieldType fieldType = new FieldType();
    fieldType.setStoreTermVectors(true);
    fieldType.setStoreTermVectorPositions(true);
    fieldType.setIndexed(true);
    fieldType.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS);
    fieldType.setStored(true);

    // use a string field for continent, country, capital because we don't want it tokenized
    doc.add(new StringField("continent", continent, Field.Store.YES));
    doc.add(new StringField("country", country, Field.Store.YES));
    doc.add(new StringField("capital", capital, Field.Store.YES));

    // Add text
    doc.add(new Field("country_text", country_text, fieldType));
    doc.add(new Field("capital_text", capital_text, fieldType));

    w.addDocument(doc);
  }

  private static void displayBoolQuery(Directory theIndex) throws IOException {

    IndexReader reader = DirectoryReader.open(theIndex);
    IndexSearcher searcher = new IndexSearcher(reader);

    Query query1 = new TermQuery(new Term("capital_text", "greek"));
    Query query2 = new TermQuery(new Term("capital_text", "roman"));
    Query query3 = new TermQuery(new Term("capital_text", "persian"));

    BooleanQuery booleanQuery = new BooleanQuery();
    booleanQuery.add(query1, BooleanClause.Occur.MUST);
    booleanQuery.add(query2, BooleanClause.Occur.MUST);
    booleanQuery.add(query3, BooleanClause.Occur.MUST_NOT);
    TopScoreDocCollector collector = TopScoreDocCollector.create(194, true);
    searcher.search(booleanQuery, collector);
    ScoreDoc[] hits = collector.topDocs().scoreDocs;

    System.out.println("Number of hits: " + hits.length);

    for (int i = 0; i < hits.length; ++i) {
      int docId = hits[i].doc;
      Document d = searcher.doc(docId);

      String capital_name = d.get("capital");
      System.out.println(capital_name);
    }

    System.out.println();
  }

  private static void displayFuzzyQuery(Directory theIndex) throws IOException {

    IndexReader reader = DirectoryReader.open(theIndex);
    IndexSearcher searcher = new IndexSearcher(reader);

    FuzzyQuery fuzzyQuery = new FuzzyQuery(new Term("capital_text", "shakespeare"));

    TopScoreDocCollector collector = TopScoreDocCollector.create(194, true);
    searcher.search(fuzzyQuery, collector);
    ScoreDoc[] hits = collector.topDocs().scoreDocs;

    System.out.println("Number of hits: " + hits.length);

    for (int i = 0; i < hits.length; ++i) {
      int docId = hits[i].doc;
      Document d = searcher.doc(docId);

      String capital_name = d.get("capital");
      System.out.println(capital_name);
    }

    System.out.println();
  }

  private static void displayPhraseQuery(Directory theIndex) throws IOException {

    IndexReader reader = DirectoryReader.open(theIndex);
    IndexSearcher searcher = new IndexSearcher(reader);

    PhraseQuery phraseQuery = new PhraseQuery();
    // This phrase query returns Baku
    // The phrase query "situated below sea level" returns Amsterdam
    phraseQuery.add(new Term("capital_text", "located"));
    phraseQuery.add(new Term("capital_text", "below"));
    phraseQuery.add(new Term("capital_text", "sea"));
    phraseQuery.add(new Term("capital_text", "level"));
    phraseQuery.setSlop(10);

    TopScoreDocCollector collector = TopScoreDocCollector.create(194, true);
    searcher.search(phraseQuery, collector);
    ScoreDoc[] hits = collector.topDocs().scoreDocs;

    System.out.println("Number of hits: " + hits.length);

    for (int i = 0; i < hits.length; ++i) {
      int docId = hits[i].doc;
      Document d = searcher.doc(docId);

      String capital_name = d.get("capital");
      System.out.println(capital_name);
    }

    System.out.println();
  }

  private static void getNaiveBayesStuff(Directory theIndex) throws IOException {

    IndexReader reader = DirectoryReader.open(theIndex);
    /*
    /////
    // Get unique terms (51,493)
    Fields fields = MultiFields.getFields(reader);
    Terms terms = fields.terms("capital_text");
    TermsEnum iterator = terms.iterator(null);
    BytesRef byteRef = null;
    int i = 0;

    while((byteRef = iterator.next()) != null) {
      String term = byteRef.utf8ToString();
      i++;
    }
    System.out.println(i);
    /////
    */

    Map<String, Integer> tf = new HashMap<String, Integer>();

    IndexSearcher searcher = new IndexSearcher(reader);

    Query query1 = new TermQuery(new Term("continent", "North America"));

    BooleanQuery booleanQuery = new BooleanQuery();
    booleanQuery.add(query1, BooleanClause.Occur.MUST);

    TopScoreDocCollector collector = TopScoreDocCollector.create(194, true);
    searcher.search(booleanQuery, collector);
    ScoreDoc[] hits = collector.topDocs().scoreDocs;

    // System.out.println(hits.length);

    // Build the term vector for all continental capitals
    for (int i = 0; i < hits.length; ++i) {
      int docId = hits[i].doc;
      Terms terms = reader.getTermVector(docId, "capital_text");

      if (terms != null && terms.size() > 0) {
        TermsEnum termsEnum = terms.iterator(null); // access the terms for this field
        BytesRef term = null;
        while ((term = termsEnum.next()) != null) { // explore the terms for this field
          DocsEnum docsEnum = termsEnum.docs(null, null); // enumerate through documents, in this case only one
          int docIdEnum;
          while ((docIdEnum = docsEnum.nextDoc()) != DocIdSetIterator.NO_MORE_DOCS) {
            // System.out.println(term.utf8ToString() + " " + docIdEnum + " " + docsEnum.freq()); //get the term frequency in the document
            Integer value = (Integer) tf.get(term.utf8ToString());
            if (value != null) {
              int currentFreq = (Integer) tf.get(term.utf8ToString());
              tf.put(term.utf8ToString(), new Integer(currentFreq + docsEnum.freq()));
            } else {
              tf.put(term.utf8ToString(), new Integer(docsEnum.freq()));
            }
          }
        }
      }
    }

    // Loop through and get the total number of tokens
    Integer asian_total = 0;
    for (Integer value : tf.values()) {
      asian_total += value;
    }

    Map<String, Double> mutual_information = new HashMap<String, Double>();
    for (Map.Entry<String, Integer> entry : tf.entrySet()) {
      String key = entry.getKey();
      Integer value = entry.getValue();
      Double new_value = (Double) ((value.intValue() + 1) / ((double) (asian_total.intValue() + 51493)));
      mutual_information.put(key, new_value);
    }

    // Sort hashmap by values
    List mutual_information_list = new LinkedList(mutual_information.entrySet());
    Collections.sort(mutual_information_list, new Comparator() {
      public int compare(Object o1, Object o2) {
        return ((Comparable) ((Map.Entry) (o1)).getValue()).compareTo(((Map.Entry) (o2)).getValue());
      }
    });

    Map sorted_mutual_information_map = new LinkedHashMap();
    for (Iterator it = mutual_information_list.iterator(); it.hasNext();) {
      Map.Entry entry = (Map.Entry)it.next();
      sorted_mutual_information_map.put(entry.getKey(), entry.getValue());
    }

    // System.out.println(sorted_mutual_information_map);
  }
}