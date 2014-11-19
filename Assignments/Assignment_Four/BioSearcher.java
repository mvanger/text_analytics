import java.util.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Scanner;
import java.util.Arrays;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.util.Version;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import java.io.IOException;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.search.Query;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.search.highlight.TokenSources;
import org.apache.lucene.search.spans.SpanScorer;
import org.apache.lucene.analysis.CachingTokenFilter;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.NullFragmenter;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.highlight.TextFragment;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.search.highlight.SpanGradientFormatter;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.FieldInfo.IndexOptions;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.DocsEnum;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.DocIdSetIterator;

public class BioSearcher {
  public static void main(String[] args) throws FileNotFoundException, IOException, ParseException, InvalidTokenOffsetsException {
    // Get an array of the bios
    String[] classBios = splitBios();
    StandardAnalyzer analyzer = new StandardAnalyzer(Version.LUCENE_40);

    // Create an index
    Directory theIndex = makeIndex(classBios, analyzer);

    foo(theIndex);

  }

  // This method splits the bios into an array
  private static String[] splitBios() throws FileNotFoundException {
    String fileName = "classbios.txt";
    File inputFile = new File(fileName);
    Scanner in = new Scanner(inputFile);

    String fileText = "";

    // Get the class bios
    while (in.hasNextLine()) {
      fileText += " " + in.nextLine().trim();
    }

    in.close();

    // Splits string into array of strings for each individual bio
    String[] stringArray = fileText.split("\\d\\d\\s-+|-+\\sNEW\\sCOHORT\\s-+|-{21}");

    // First element is empty for some reason
    // Probably because the regex matches right away?
    for (int i = 1; i < stringArray.length; i++) {
      stringArray[i] = stringArray[i].trim();
    }

    return stringArray;
  }

  // This method creates an index
  private static Directory makeIndex(String[] docArray, StandardAnalyzer theAnalyzer) throws IOException {

    // Using RAMDirectory here
    // FSDirectory might work better for different projects
    Directory index = new RAMDirectory();
    IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_40, theAnalyzer);

    IndexWriter w = new IndexWriter(index, config);
    // Add each bio to the index
    // Start at 1 since the first element is an empty string
    for (int i = 1; i < docArray.length; i++) {
      addDoc(w, docArray[i], i);
    }
    w.close();

    return index;
  }

  // This method adds a doc to the index
  private static void addDoc(IndexWriter w, String content, int theIndex) throws IOException {
    Document doc = new Document();

    FieldType fieldType = new FieldType();
    fieldType.setStoreTermVectors(true);
    fieldType.setStoreTermVectorPositions(true);
    fieldType.setIndexed(true);
    fieldType.setIndexOptions(IndexOptions.DOCS_AND_FREQS);
    fieldType.setStored(true);
    // Add content and a title
    doc.add(new Field("content", content, fieldType));

    String theTitle = "Bio_" + theIndex;
    // use a string field for title because we don't want it tokenized
    doc.add(new StringField("title", theTitle, Field.Store.YES));
    w.addDocument(doc);
  }

  public static void foo(Directory theIndex) throws IOException, ParseException {

    IndexReader reader = DirectoryReader.open(theIndex);
    Map<String, Integer> tf = new HashMap<String, Integer>();
    Map<String, Integer> df = new HashMap<String, Integer>();

    for (int docID = 0; docID < reader.maxDoc(); docID++) {
      Terms terms = reader.getTermVector(docID, "content");
      // System.out.println(termFreqVector.getTerms() + " " + termFreqVector.getTermFrequencies());
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

            Integer df_value = (Integer) df.get(term.utf8ToString());
            if (df_value != null) {
              int currentFreq = (Integer) df.get(term.utf8ToString());
              df.put(term.utf8ToString(), new Integer(currentFreq + 1));
            } else {
              df.put(term.utf8ToString(), new Integer(1));
            }

          }
        }
      }
    }

    HashMap tf_idf = new HashMap();
    int n = reader.maxDoc();
    for (Map.Entry<String, Integer> entry : tf.entrySet()) {
      String key = entry.getKey();
      Integer tf_value = entry.getValue();
      Integer df_value = df.get(key);
      tf_idf.put(key, Math.log10(1 + tf_value) * Math.log10(n/df_value));
    }

    // Sort hashmap by values
    List list = new LinkedList(tf_idf.entrySet());
    Collections.sort(list, new Comparator() {
      public int compare(Object o1, Object o2) {
        return ((Comparable) ((Map.Entry) (o1)).getValue()).compareTo(((Map.Entry) (o2)).getValue());
      }
    });

    Map sorted_map = new LinkedHashMap();
    for (Iterator it = list.iterator(); it.hasNext();) {
      Map.Entry entry = (Map.Entry)it.next();
      sorted_map.put(entry.getKey(), entry.getValue());
    }
    // System.out.println(sorted_map);

    // Try some summarization

    Terms terms = reader.getTermVector(39, "content");
    Map<String, Integer> vanger = new HashMap<String, Integer>();
    if (terms != null && terms.size() > 0) {
      TermsEnum termsEnum = terms.iterator(null); // access the terms for this field
      BytesRef term = null;
      while ((term = termsEnum.next()) != null) { // explore the terms for this field
        DocsEnum docsEnum = termsEnum.docs(null, null); // enumerate through documents, in this case only one
        int docIdEnum;
        while ((docIdEnum = docsEnum.nextDoc()) != DocIdSetIterator.NO_MORE_DOCS) {
          // System.out.println(term.utf8ToString() + " " + docsEnum.freq());
          vanger.put(term.utf8ToString(), new Integer(docsEnum.freq()));
        }
      }
    }

    // Sort hashmap by values
    List vanger_list = new LinkedList(vanger.entrySet());
    Collections.sort(vanger_list, new Comparator() {
      public int compare(Object o1, Object o2) {
        return ((Comparable) ((Map.Entry) (o1)).getValue()).compareTo(((Map.Entry) (o2)).getValue());
      }
    });

    Map sorted_vanger_map = new LinkedHashMap();
    for (Iterator it = vanger_list.iterator(); it.hasNext();) {
      Map.Entry entry = (Map.Entry)it.next();
      sorted_vanger_map.put(entry.getKey(), entry.getValue());
    }
    // System.out.println(sorted_vanger_map); // This is the sorted TF matrix

    // Let's try the tf-idf now
    Map<String, Double> vanger_tf_idf = new HashMap<String, Double>();

    for (Map.Entry<String, Integer> entry : vanger.entrySet()) {
      String key = entry.getKey();
      vanger_tf_idf.put(key, (Double) tf_idf.get(key));
    }

    // Sort hashmap by values
    List vanger_tf_idf_list = new LinkedList(vanger_tf_idf.entrySet());
    Collections.sort(vanger_tf_idf_list, new Comparator() {
      public int compare(Object o1, Object o2) {
        return ((Comparable) ((Map.Entry) (o1)).getValue()).compareTo(((Map.Entry) (o2)).getValue());
      }
    });

    Map sorted_vanger_tf_idf_map = new LinkedHashMap();
    for (Iterator it = vanger_tf_idf_list.iterator(); it.hasNext();) {
      Map.Entry entry = (Map.Entry)it.next();
      sorted_vanger_tf_idf_map.put(entry.getKey(), entry.getValue());
    }

    System.out.println(sorted_vanger_tf_idf_map);

    reader.close();

  }
  // for (int i = 0; i < 10; i++) {
  //   int docNumber = hits.id(i);
  //   TermFreqVector[] termsV = ir.getTermFreqVectors(docNumber); //return an array of term frequency vectors for the specified document.
  //   for (int xy = 0; xy < termsV.length; xy++) { //loop over all terms-vectors in the current document
  //     String[] terms = termsV[xy].getTerms();
  //     for (int termsInArray = 0; termsInArray < terms.length; termsInArray++) {
  //       //toDo: count the occurrence of the terms
  //     }

  //   }
  // }
}