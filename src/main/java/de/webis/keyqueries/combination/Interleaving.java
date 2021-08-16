package de.webis.keyqueries.combination;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.lucene.search.ScoreDoc;

public class Interleaving {
  /**
   * Radlinski, Kurup, and Joachims. "How does clickthrough data reflect retrieval quality?" CIKM 2008.
   * Section 5.2. Team-Draft Interleaving Method
   * @param <T>
   * @param rankings
   * @return
   */
  public static <T> List<T> teamDraftInterleaving(List<List<T>> rankings) {
    List<T> ret = new ArrayList<>();
    int maxSize = maxSize(rankings);

    for (int j=0; j < maxSize; j++) {
      for (List<T> ranking : rankings) {
        for (int i = j; i < ranking.size(); i++) {
          if (!ret.contains(ranking.get(i))) {
            ret.add(ranking.get(i));
            break;
          }
        }
      }
    }

    return ret;
  }
  
  public static List<ScoreDoc> teamDraftInterleaving1(List<List<ScoreDoc>> rankings) {
    List<List<Pair<Integer, Integer>>> transformedRankings = fromRankingsOfScoreDocs(rankings);
    List<Pair<Integer, Integer>> ret = teamDraftInterleaving(transformedRankings);
    
    return toListOfScoreDocs(ret);
  }
  
  private static List<List<Pair<Integer, Integer>>> fromRankingsOfScoreDocs(List<List<ScoreDoc>> docs) {
    return docs.stream()
      .map(i -> fromScoreDocs(i))
      .collect(Collectors.toList());
  }
  
  private static List<ScoreDoc> toListOfScoreDocs(List<Pair<Integer, Integer>> docs) {
    List<ScoreDoc> ret = new ArrayList<>();
    int currentScore = docs.size();

    for(Pair<Integer, Integer> doc: docs) {
      ret.add(new ScoreDoc(doc.getKey(), (float)(currentScore--), doc.getRight()));
    }

    return ret;
  }
  
  private static List<Pair<Integer, Integer>> fromScoreDocs(List<ScoreDoc> docs) {
    return docs.stream()
      .map(i -> Pair.of(i.doc, i.shardIndex))
      .collect(Collectors.toList());
  }

  /**
   * Radlinski, Kurup, and Joachims. "How does clickthrough data reflect retrieval quality?" CIKM 2008.
   * Section 5.1. Balanced Interleaving Method
   * @param <T>
   * @param rankings
   * @return
   */
  public static <T> List<T> balancedInterleaving(List<List<T>> rankings) {
    List<T> ret = new ArrayList<>();
    int maxSize = maxSize(rankings);

    for (int i = 0; i < maxSize; i++) {
      for (List<T> ranking : rankings) {
        if (i < ranking.size() && !ret.contains(ranking.get(i))) {
          ret.add(ranking.get(i));
        }
      }
    }

    return ret;
  }
  
  public static List<ScoreDoc> balancedInterleaving1(List<List<ScoreDoc>> rankings) {
    List<List<Pair<Integer, Integer>>> transformedRankings = fromRankingsOfScoreDocs(rankings);
    List<Pair<Integer, Integer>> ret = balancedInterleaving(transformedRankings);
    
    return toListOfScoreDocs(ret);
  }

  private static <T> int maxSize(List<List<T>> lists) {
    if(lists.size() == 0) {
      return 0;
    }
    return lists
      .stream()
      .mapToInt(i -> i.size())
      .max().getAsInt();
  }
}
