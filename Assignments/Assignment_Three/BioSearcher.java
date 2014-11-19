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


public class BioSearcher {
  public static void main(String[] args) throws FileNotFoundException, IOException, ParseException, InvalidTokenOffsetsException {
    // Get an array of the bios
    String[] classBios = splitBios();
    StandardAnalyzer analyzer = new StandardAnalyzer(Version.LUCENE_40);

    // Create an index
    Directory theIndex = makeIndex(classBios, analyzer);

    // Get a query from the user, search, and print results
    while (true) {
      Query userQuery = runQuery(analyzer);
      runSearchDisplayResults(theIndex, userQuery, analyzer);
    }
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

    // Add content and a title
    doc.add(new TextField("content", content, Field.Store.YES));

    String theTitle = "Bio_" + theIndex;
    // use a string field for title because we don't want it tokenized
    doc.add(new StringField("title", theTitle, Field.Store.YES));
    w.addDocument(doc);
  }

  // This method generates a query
  private static Query runQuery(StandardAnalyzer theAnalyzer) throws ParseException {
    Scanner console = new Scanner(System.in);
    System.out.print("Search on: ");
    String querystr = console.nextLine();
    // System.out.println(querystr);

    Query q = new QueryParser(Version.LUCENE_40, "content", theAnalyzer).parse(querystr);
    System.out.println(q.toString());
    return q;
  }

  // This method runs the search and displays the results
  private static void runSearchDisplayResults(Directory theIndex, Query q, StandardAnalyzer theAnalyzer) throws InvalidTokenOffsetsException, IOException {
    int hitsPerPage = 10;
    IndexReader reader = DirectoryReader.open(theIndex);
    IndexSearcher searcher = new IndexSearcher(reader);
    TopScoreDocCollector collector = TopScoreDocCollector.create(hitsPerPage, true);
    searcher.search(q, collector);
    ScoreDoc[] hits = collector.topDocs().scoreDocs;

    System.out.println("Found " + hits.length + " matches.");

    ////////////////
    ////////////////
    // The phrase is return with HTML bold tags (<B></B>)
    SimpleHTMLFormatter htmlFormatter = new SimpleHTMLFormatter();
    // float aFloat = 100;
    // SpanGradientFormatter htmlFormatter = new SpanGradientFormatter(aFloat, "#0000ff", "#0000ff", "#0000ff", "#0000ff");
    Highlighter highlighter = new Highlighter(htmlFormatter, new QueryScorer(q));
    ////////////////
    ////////////////

    for (int i = 0; i < hits.length; ++i) {
      int docId = hits[i].doc;
      Document d = searcher.doc(docId);

      // Highlighter highlighter = new Highlighter(new QueryScorer(q));
      // highlighter.setTextFragmenter(new NullFragmenter());
      // Highlighter highlighter = new Highlighter(scorer);
      // highlighter.setTextFragmenter(fragmenter);
      // String[] fragments = highlighter.getBestFragments(stream, fieldContents, 5);

      // String text = highlighter.getBestFragment(theAnalyzer, "content", "assignment");

      // System.out.println(text);

      // String text = "The quick brown fox jumps over the lazy dog";
      // TermQuery query = new TermQuery(new Term("mike", "vanger"));
      // Scorer scorer = new QueryScorer(query);
      // Highlighter highlighter = new Highlighter(scorer);
      // TokenStream tokenStream = new SimpleAnalyzer().tokenStream("field", new StringReader(text));
      // System.out.println(highlighter.getBestFragment(tokenStream, text));

      String text = d.get("content");

      TokenStream tokenStream = TokenSources.getAnyTokenStream(searcher.getIndexReader(), docId, "content", theAnalyzer);
      TextFragment[] frag = highlighter.getBestTextFragments(tokenStream, text, false, 10);
      for (int j = 0; j < frag.length; j++) {
        if ((frag[j] != null) && (frag[j].getScore() > 0)) {
          System.out.println(d.get("title") + ":\n" + frag[j].toString());
        }
      }

      // System.out.println(d.get("title") + ":\n" + d.get("content"));
      System.out.println();
    }

    // reader can only be closed when there is no need to access the documents any more.
    reader.close();
  }

  public void readingIndex(Directory theIndex, int docNbr) {
    IndexReader reader = DirectoryReader.open(theIndex);

    Document doc = reader.document(docNbr);
    System.out.println("Processing file: "+doc.get("id"));

    Terms termVector = reader.getTermVector(docNbr, "contents");
    TermsEnum itr = termVector.iterator(null);
    BytesRef term = null;

    while ((term = itr.next()) != null) {
        String termText = term.utf8ToString();
        long termFreq = itr.totalTermFreq();   //FIXME: this only return frequency in this doc
        long docCount = itr.docFreq();   //FIXME: docCount = 1 in all cases

        System.out.println("term: "+termText+", termFreq = "+termFreq+", docCount = "+docCount);
    }

    reader.close();
}
}