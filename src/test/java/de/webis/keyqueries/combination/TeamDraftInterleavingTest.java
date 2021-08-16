package de.webis.keyqueries.combination;


import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.lucene.search.ScoreDoc;
import org.junit.Assert;
import org.junit.Test;

public class TeamDraftInterleavingTest {

  @Test
  public void checkInterleavingForTwoDisjointRankings() {
    List<List<String>> rankings = Arrays.asList(
      Arrays.asList("A", "B", "C"),
      Arrays.asList("D", "E", "F")
    );

    List<String> expected = Arrays.asList("A", "D", "B", "E", "C", "F");
    List<String> actual = Interleaving.teamDraftInterleaving(rankings);

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void checkInterleavingForTwoDisjointRankingsInverse() {
    List<List<String>> rankings = Arrays.asList(
      Arrays.asList("D", "E", "F"),
      Arrays.asList("A", "B", "C")
    );

    List<String> expected = Arrays.asList("D", "A", "E", "B", "F", "C");
    List<String> actual = Interleaving.teamDraftInterleaving(rankings);

    Assert.assertEquals(expected, actual);
  }
  
  @Test
  public void checkInterleavingForTwoDisjointRankingsInverseWithScoreDocs() {
    List<List<ScoreDoc>> rankings = Arrays.asList(
      Arrays.asList(doc(4), doc(5), doc(6)),
      Arrays.asList(doc(1), doc(2), doc(3))
    );

    List<String> expected = Arrays.asList("4 (score:6.0)", "1 (score:5.0)", "5 (score:4.0)", "2 (score:3.0)", "6 (score:2.0)", "3 (score:1.0)");
    List<String> actual = teamDraftInterleaving(rankings);

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void checkInterleavingForTwoRankings() {
    List<List<String>> rankings = Arrays.asList(
      Arrays.asList("A", "B", "C"),
      Arrays.asList("D", "B", "A")
    );

    List<String> expected = Arrays.asList("A", "D", "B", "C");
    List<String> actual = Interleaving.teamDraftInterleaving(rankings);

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void checkInterleavingForTwoDRankingsInverse() {
    List<List<String>> rankings = Arrays.asList(
      Arrays.asList("D", "B", "A"),
      Arrays.asList("A", "B", "C")
    );

    List<String> expected = Arrays.asList("D", "A", "B", "C");
    List<String> actual = Interleaving.teamDraftInterleaving(rankings);

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void checkInterleavingForTwoDRankingsInverseWithScoreDocs() {
    List<List<ScoreDoc>> rankings = Arrays.asList(
      Arrays.asList(doc(4), doc(2), doc(1)),
      Arrays.asList(doc(1), doc(2), doc(3))
    );

    List<String> expected = Arrays.asList("4 (score:4.0)", "1 (score:3.0)", "2 (score:2.0)", "3 (score:1.0)");
    List<String> actual = teamDraftInterleaving(rankings);

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
    List<String> actual = Interleaving.teamDraftInterleaving(rankings);

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
    List<String> actual = Interleaving.teamDraftInterleaving(rankings);

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

    List<String> expected = Arrays.asList("A", "B", "G", "J", "C", "E", "I", "K", "F");
    List<String> actual = Interleaving.teamDraftInterleaving(rankings);

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

    List<String> expected = Arrays.asList("J", "G", "B", "A", "K", "I", "E", "C", "F");
    List<String> actual = Interleaving.teamDraftInterleaving(rankings);

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void checkInterleavingForMultipleRankingsInverseWithScoreDocs() {
    List<List<ScoreDoc>> rankings = Arrays.asList(
      Arrays.asList(doc(10), doc(11), doc(7)),
      Arrays.asList(doc(7), doc(1), doc(9)),
      Arrays.asList(doc(2), doc(5), doc(6)),
      Arrays.asList(doc(1), doc(2), doc(3))
    );

    List<String> expected = Arrays.asList("10 (score:9.0)", "7 (score:8.0)", "2 (score:7.0)", "1 (score:6.0)", "11 (score:5.0)", "9 (score:4.0)", "5 (score:3.0)", "3 (score:2.0)", "6 (score:1.0)");
    List<String> actual = teamDraftInterleaving(rankings);

    Assert.assertEquals(expected, actual);
  }
  
  private static List<String> teamDraftInterleaving(List<List<ScoreDoc>> rankings) {
    return Interleaving.teamDraftInterleaving1(rankings)
      .stream().map(i -> i.doc +" (score:" + i.score + ")")
      .collect(Collectors.toList());
  }
  
  private static ScoreDoc doc(int docId) {
    return new ScoreDoc(docId, 0f);
  }
}
