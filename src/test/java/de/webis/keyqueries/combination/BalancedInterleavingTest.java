package de.webis.keyqueries.combination;


import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.lucene.search.ScoreDoc;
import org.junit.Assert;
import org.junit.Test;

public class BalancedInterleavingTest {

  @Test
  public void checkInterleavingForTwoDisjointRankings() {
    List<List<String>> rankings = Arrays.asList(
      Arrays.asList("A", "B", "C"),
      Arrays.asList("D", "E", "F")
    );

    List<String> expected = Arrays.asList("A", "D", "B", "E", "C", "F");
    List<String> actual = Interleaving.balancedInterleaving(rankings);

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void checkInterleavingForTwoDisjointRankingsWithScoreDocs() {
    List<List<ScoreDoc>> rankings = Arrays.asList(
      Arrays.asList(doc(1), doc(2), doc(3)),
      Arrays.asList(doc(4), doc(5), doc(6))
    );

    List<String> expected = Arrays.asList("1 (score:6.0)", 
      "4 (score:5.0)", "2 (score:4.0)", "5 (score:3.0)",
      "3 (score:2.0)", "6 (score:1.0)"
    );
    List<String> actual = balancedInterleaving(rankings);

    Assert.assertEquals(expected, actual);
  }
  
  @Test
  public void checkInterleavingForTwoDisjointRankingsInverse() {
    List<List<String>> rankings = Arrays.asList(
      Arrays.asList("D", "E", "F"),
      Arrays.asList("A", "B", "C")
    );

    List<String> expected = Arrays.asList("D", "A", "E", "B", "F", "C");
    List<String> actual = Interleaving.balancedInterleaving(rankings);

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void checkInterleavingForTwoRankings() {
    List<List<String>> rankings = Arrays.asList(
      Arrays.asList("A", "B", "C", "D"),
      Arrays.asList("B", "E", "A", "F")
    );

    List<String> expected = Arrays.asList("A", "B", "E", "C", "D", "F");
    List<String> actual = Interleaving.balancedInterleaving(rankings);

    Assert.assertEquals(expected, actual);
  }
  
  @Test
  public void checkInterleavingForTwoRankingsWithScoreDocs() {
    List<List<ScoreDoc>> rankings = Arrays.asList(
      Arrays.asList(doc(1), doc(2), doc(3), doc(4)),
      Arrays.asList(doc(2), doc(5), doc(1), doc(6))
    );

    List<String> expected = Arrays.asList("1 (score:6.0)",
      "2 (score:5.0)", "5 (score:4.0)", "3 (score:3.0)",
      "4 (score:2.0)", "6 (score:1.0)");
    List<String> actual = balancedInterleaving(rankings);

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void checkInterleavingForTwoDRankingsInverse() {
    List<List<String>> rankings = Arrays.asList(
      Arrays.asList("B", "E", "A", "F"),
      Arrays.asList("A", "B", "C", "D")
    );

    List<String> expected = Arrays.asList("B", "A", "E", "C", "F", "D");
    List<String> actual = Interleaving.balancedInterleaving(rankings);

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void checkInterleavingForMultipleDisjointRankings() {
    List<List<String>> rankings = Arrays.asList(
      Arrays.asList("A", "B", "C"),
      Arrays.asList("D", "E", "F"),
      Arrays.asList("G", "H", "I"),
      Arrays.asList("J", "K", "L")
    );

    List<String> expected = Arrays.asList("A", "D", "G", "J", "B", "E", "H", "K", "C", "F", "I", "L");
    List<String> actual = Interleaving.balancedInterleaving(rankings);

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void checkInterleavingForMultipleDisjointRankingsInverse() {
    List<List<String>> rankings = Arrays.asList(
      Arrays.asList("J", "K", "L"),
      Arrays.asList("G", "H", "I"),
      Arrays.asList("D", "E", "F"),
      Arrays.asList("A", "B", "C")
    );

    List<String> expected = Arrays.asList("J", "G", "D", "A", "K", "H", "E", "B", "L", "I", "F", "C");
    List<String> actual = Interleaving.balancedInterleaving(rankings);

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void checkInterleavingForMultipleRankings() {
    List<List<String>> rankings = Arrays.asList(
      Arrays.asList("A", "B", "C"),
      Arrays.asList("B", "E", "F"),
      Arrays.asList("G", "A", "I"),
      Arrays.asList("J", "K", "G")
    );

    List<String> expected = Arrays.asList("A", "B", "G", "J", "E", "K", "C", "F", "I");
    List<String> actual = Interleaving.balancedInterleaving(rankings);

    Assert.assertEquals(expected, actual);
  }
  
  @Test
  public void checkInterleavingForMultipleRankingsWithScoreDocs() {
    List<List<ScoreDoc>> rankings = Arrays.asList(
      Arrays.asList(doc(1), doc(2), doc(3)),
      Arrays.asList(doc(2), doc(5), doc(6)),
      Arrays.asList(doc(7), doc(1), doc(9)),
      Arrays.asList(doc(10), doc(11), doc(7))
    );

    List<String> expected = Arrays.asList("1 (score:9.0)", "2 (score:8.0)",
      "7 (score:7.0)", "10 (score:6.0)", "5 (score:5.0)", "11 (score:4.0)",
      "3 (score:3.0)", "6 (score:2.0)", "9 (score:1.0)"
    );
    List<String> actual = balancedInterleaving(rankings);

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void checkInterleavingForMultipleRankingsInverse() {
    List<List<String>> rankings = Arrays.asList(
      Arrays.asList("J", "K", "G"),
      Arrays.asList("G", "A", "I"),
      Arrays.asList("B", "E", "F"),
      Arrays.asList("A", "B", "C")
    );

    List<String> expected = Arrays.asList("J", "G", "B", "A", "K", "E", "I", "F", "C");
    List<String> actual = Interleaving.balancedInterleaving(rankings);

    Assert.assertEquals(expected, actual);
  }
  

  private static List<String> balancedInterleaving(List<List<ScoreDoc>> rankings) {
    return Interleaving.balancedInterleaving1(rankings)
      .stream().map(i -> i.doc +" (score:" + i.score + ")")
      .collect(Collectors.toList());
  }
  
  private static ScoreDoc doc(int docId) {
    return new ScoreDoc(docId, 0f);
  }
}
