package edu.cmu.lti.deiis.hw5.answer_selection;

import java.util.ArrayList;
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

public class AnswerSelectionByKCandVoting extends JCasAnnotator_ImplBase {

  // Indicates if scoring methods should weight by candidate sentence relevance value if applicable
  private static final boolean WEIGHT_BY_RELEVANCE_VALUE = false;

  // The minimum score from voting/ranking an answer must have before it can be considered
  private static final double MIN_SCORE_THRESHOLD = 0.33;

  // The minimum score an answer needs to recieve a vote
  private static final double MIN_TO_VOTE_THRESHOLD = 0;

  int K_CANDIDATES = 5;

  private int docCount;
  private double sumAcc;
  private double sumCScore;
  
  @Override
  public void initialize(UimaContext context) throws ResourceInitializationException {
    super.initialize(context);
    K_CANDIDATES = (Integer) context.getConfigParameterValue("K_CANDIDATES");
    this.docCount=0;
    this.sumAcc=0D;
    this.sumCScore=0D;
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
        singleVote(candSent, hshAnswer);

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

    double accuracy=((matched * 100.0) / total);
    System.out.println("Correct: " + matched + "/" + total + "=" + accuracy
            + "%");
    // TO DO: Reader of this pipe line should read from xmi generated by
    // SimpleRunCPE
    double cAt1 = (((double) matched) / ((double) total) * unanswered + (double) matched)
            * (1.0 / total);
    System.out.println("c@1 score:" + cAt1);
    this.docCount++;
    this.sumAcc+=accuracy;
    this.sumCScore+=cAt1;

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

  /**
   * Populates hshAnswer with a vote for the CandidateAnswer with the highest score in the list.
   * 
   * @param candSent
   *          The CandidateSentence voting for a best answer
   * @param hshAnswer
   *          The map of answer text to current vote count
   */
  private void singleVote(CandidateSentence candSent, HashMap<String, Double> hshAnswer) {
    ArrayList<CandidateAnswer> candAnswerList = Utils.fromFSListToCollection(
            candSent.getCandAnswerList(), CandidateAnswer.class);
    String selectedAnswer = null;
    double maxScore = MIN_TO_VOTE_THRESHOLD;
    for (int j = 0; j < candAnswerList.size(); j++) {

      CandidateAnswer candAns = candAnswerList.get(j);
      String answer = candAns.getText();

      if(answer.toLowerCase().contains("none")&&answer.toLowerCase().contains("above"))
      {
    	  if(!hshAnswer.containsKey(answer))
    	  {
    		  hshAnswer.put(answer, 0D);
    	  }
    	  continue;
      }
      
      double totalScore = getCandidateAnswerScore(candAns);

      if (totalScore > maxScore) {
        maxScore = totalScore;
        selectedAnswer = answer;
      }
    }
    Double existingVal = hshAnswer.get(selectedAnswer);
    if (existingVal == null) {
      existingVal = new Double(0.0);
    }
    if(selectedAnswer != null) {
      if (WEIGHT_BY_RELEVANCE_VALUE)
        hshAnswer.put(selectedAnswer, existingVal + (candSent.getRelevanceScore() * 1.0));
      else
        hshAnswer.put(selectedAnswer, existingVal + 1.0);
    }
  }

  /**
   * Finds the Answer (key) with the highest value (votes/rank score) in hshAnswer
   * 
   * @param hshAnswer
   *          The map of Answer text to rank score/voting placement.
   * @return
   * @throws Exception
   */
  private String findBestChoice(HashMap<String, Double> hshAnswer) throws Exception {

    Iterator<String> it = hshAnswer.keySet().iterator();
    String bestAns = null;
    double maxScore = MIN_SCORE_THRESHOLD*this.K_CANDIDATES;
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
    // Assign answer to 'all of the above' answer if no other answer
    if (bestAns == null)
      bestAns = getNoneOfTheAbove(hshAnswer.keySet());

    return bestAns;
  }

  /**
   * Checks if one of the answers is 'all of the above'
   * 
   * @param set
   *          The set of answers
   * @return The 'All of the above' answer if found, else null.
   */
  private String getNoneOfTheAbove(Set<String> answerSet) {
    for (String answer : answerSet) {
      if (answer.toLowerCase().contains("none") && answer.toLowerCase().contains("above"))
      {
    	  
        return answer;
      }
    }

    return null;
  }

  /*
   * Gets the score for a candidate answer based on the corresponding candidate evidence.
   */
  private double getCandidateAnswerScore(CandidateAnswer candAns) {
    return candAns.getSimilarityScore() + candAns.getSynonymScore() + candAns.getPMIScore();
  }

  @Override
  public void destroy()
  {
	  System.out.println("AVERAGE c@1 Score: "+(this.sumCScore/this.docCount));
	  System.out.println("AVERAGE Accuracy: "+(this.sumAcc/this.docCount)+"%");
  }
  
}
