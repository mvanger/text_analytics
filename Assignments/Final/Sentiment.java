import java.util.Properties;
import au.com.bytecode.opencsv.CSVReader;
import java.io.FileNotFoundException;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.*;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations;
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations.AnnotatedTree;
import edu.stanford.nlp.neural.rnn.RNNCoreAnnotations;

public class Sentiment {
  public static void main(String[] args) throws FileNotFoundException, IOException {
    String[] sentimentText = new String[]{"Very Negative","Negative", "Neutral", "Positive", "Very Positive"};
    Properties props = new Properties();
    props.setProperty("annotators", "tokenize, ssplit, parse, sentiment");
    StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
    int mainSentiment = 0;

    CSVReader reader = new CSVReader(new FileReader("play_all.csv"));
    String[] nextLine;

    PrintWriter writer = new PrintWriter("results2.csv", "UTF-8");
    int i = 1;
    while ((nextLine = reader.readNext()) != null) {
      System.out.println(i);
      if (nextLine[5] != null && nextLine[5].length() > 0) {
        int longest = 0;
        Annotation annotation = pipeline.process(nextLine[5]);
        for (CoreMap sentence : annotation.get(CoreAnnotations.SentencesAnnotation.class)) {
          Tree tree = sentence.get(SentimentCoreAnnotations.AnnotatedTree.class);
          int sentiment = RNNCoreAnnotations.getPredictedClass(tree);
          String partText = sentence.toString();
          if (partText.length() > longest) {
            mainSentiment = sentiment;
            longest = partText.length();
          }
        }
        writer.println(mainSentiment + "," + sentimentText[mainSentiment]);
      }
      i++;
    }

    writer.close();

  }
}