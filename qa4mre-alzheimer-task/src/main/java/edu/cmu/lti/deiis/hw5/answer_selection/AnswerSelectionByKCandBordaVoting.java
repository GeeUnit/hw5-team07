package edu.cmu.lti.deiis.hw5.answer_selection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import edu.cmu.lti.qalab.types.Answer;
import edu.cmu.lti.qalab.types.CandidateAnswer;
import edu.cmu.lti.qalab.types.CandidateSentence;
import edu.cmu.lti.qalab.types.Question;
import edu.cmu.lti.qalab.types.QuestionAnswerSet;
import edu.cmu.lti.qalab.types.TestDocument;
import edu.cmu.lti.qalab.utils.Utils;

public class AnswerSelectionByKCandBordaVoting extends JCasAnnotator_ImplBase {
  
  // Number of votes each CandidateSentence receives with the Borda voting system
  private static final int BORDA_VOTES = 2;

  // Indicates if scoring methods should weight by candidate sentence relevance value if applicable
  private static final boolean WEIGHT_BY_RELEVANCE_VALUE = false;
  
  // The minimum score from voting/ranking an answer must have before it can be considered
  private static final double MIN_SCORE_THRESHOLD = 2;
  
  int K_CANDIDATES = 5;
  
  /**
   * In the Borda voting system, each CandidateSentence can vote for their top N answers with rank.
   * For example, if N = 3, a CandidateSentence would give its rank one answer three points, its
   * rank two answer two points, and a rank three answer one point.
   * 
   * @param candSent
   *          The CandidateSentence voting for a best answer
   * @param hshAnswer
   *          The map of answer text to current borda score
   */
  public void bordaVote(CandidateSentence candSent, HashMap<String, Double> hshAnswer) {
    ArrayList<CandidateAnswer> candAnswerList = Utils.fromFSListToCollection(
            candSent.getCandAnswerList(), CandidateAnswer.class);
    Collections.sort(candAnswerList, new scoreComparator());
    String currentAnswer = "";
    for (int j = 0; j < BORDA_VOTES; j++) {
      currentAnswer = candAnswerList.get(j).getText();

      Double existingVal = hshAnswer.get(currentAnswer);
      if (existingVal == null) {
        existingVal = new Double(0.0);
      }
      if (WEIGHT_BY_RELEVANCE_VALUE)
        hshAnswer.put(currentAnswer, existingVal
                + (candSent.getRelevanceScore() * (BORDA_VOTES - j)));
      else
        hshAnswer.put(currentAnswer, existingVal + (BORDA_VOTES - j));
    }

  }
  
  @Override
  public void initialize(UimaContext context) throws ResourceInitializationException {
    super.initialize(context);
    K_CANDIDATES = (Integer) context.getConfigParameterValue("K_CANDIDATES");
  }

  @Override
  public void process(JCas aJCas) throws AnalysisEngineProcessException {
    TestDocument testDoc = Utils.getTestDocumentFromCAS(aJCas);
    ArrayList<QuestionAnswerSet> qaSet = Utils.fromFSListToCollection(testDoc.getQaList(),
            QuestionAnswerSet.class);
    int matched = 0;
    int total = 0;
    int unanswered = 0;

    for (int i = 0; i < qaSet.size(); i++) {

      Question question = qaSet.get(i).getQuestion();
      System.out.println("Question: " + question.getText());

      ArrayList<Answer> choiceList = Utils.fromFSListToCollection(qaSet.get(i).getAnswerList(),
              Answer.class);
      String correct = getCorrectAnswer(choiceList);

      ArrayList<CandidateSentence> candSentList = Utils.fromFSListToCollection(qaSet.get(i)
              .getCandidateSentenceList(), CandidateSentence.class);

      int topK = Math.min(K_CANDIDATES, candSentList.size());

      HashMap<String, Double> hshAnswer = new HashMap<String, Double>();

      for (int c = 0; c < topK; c++) {

        CandidateSentence candSent = candSentList.get(c);

        // Vote for best answer according to CandidateSentence
        bordaVote(candSent, hshAnswer);

      }

      String bestChoice = null;
      try {
        bestChoice = findBestChoice(hshAnswer);

      } catch (Exception e) {
        e.printStackTrace();
      }
      System.out.println("Correct Choice: " + "\t" + correct);
      System.out.println("Best Choice: " + "\t" + bestChoice);

      if (bestChoice == null) {
        unanswered++;
      }
      if (bestChoice != null && correct.equals(bestChoice)) {
        matched++;

      }
      total++;
      System.out.println("================================================");

    }

    System.out.println("Correct: " + matched + "/" + total + "=" + ((matched * 100.0) / total)
            + "%");
    // TO DO: Reader of this pipe line should read from xmi generated by
    // SimpleRunCPE
    double cAt1 = (((double) matched) / ((double) total) * unanswered + (double) matched)
            * (1.0 / total);
    System.out.println("c@1 score:" + cAt1);

  }
  
  /**
   * Finds the Answer (key) with the highest value (votes/rank score) in hshAnswer
   * 
   * @param hshAnswer
   *          The map of Answer text to rank score/voting placement.
   * @return
   * @throws Exception
   */
  public String findBestChoice(HashMap<String, Double> hshAnswer) throws Exception {

    Iterator<String> it = hshAnswer.keySet().iterator();
    String bestAns = null;
    double maxScore = MIN_SCORE_THRESHOLD;
    System.out.println("Aggregated counts; ");
    while (it.hasNext()) {
      String key = it.next();
      Double val = hshAnswer.get(key);
      System.out.println(key + "\t" + key + "\t" + val);
      if (val > maxScore) {
        maxScore = val;
        bestAns = key;
      }

    }
    // All of the above logic
    if( bestAns == null) 
      bestAns = containsAllOfTheAbove(hshAnswer.keySet());
    
    return bestAns;
  }
  
  /**
   * Checks if one of the answers is 'all of the above' 
   * 
   * @param set The set of answers
   * @return The 'All of the above' answer if found, else null.
   */
  private String containsAllOfTheAbove(Set<String> answerSet) {
    for(String answer : answerSet) {
      if( answer.toLowerCase() == "all of the above" )
        return answer;
    }
    
    return null;
  }

  private String getCorrectAnswer(ArrayList<Answer> choiceList) {
    for (int j = 0; j < choiceList.size(); j++) {
      Answer answer = choiceList.get(j);
      if (answer.getIsCorrect()) {
        return answer.getText();
      }
    }

    return "";
  }
  
  /*
   * Gets the score for a candidate answer based on the corresponding candidate evidence.
   */
  private double getCandidateAnswerScore(CandidateAnswer candAns) {
    return candAns.getSimilarityScore() + candAns.getSynonymScore() + candAns.getPMIScore();
  }

  /*
   * Compares CandidateAnswers based on getCandidateAnswerScore()
   */
  private class scoreComparator implements Comparator<CandidateAnswer> {
    public int compare(CandidateAnswer a, CandidateAnswer b) {
      double aScore = getCandidateAnswerScore(a);
      double bScore = getCandidateAnswerScore(b);

      if (aScore > bScore)
        return 1;
      if (aScore < bScore)
        return -1;

      return 0;
    }
  }
}