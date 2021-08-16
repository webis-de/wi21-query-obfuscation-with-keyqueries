package de.webis.keyqueries.anserini;

public interface HumanInTheLoopAware<T> {
  public void setHumanInTheLoop(HumanInTheLoopReranker<T> humanInTheLoop);
}
