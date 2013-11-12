package edu.cmu.lti.deiis.hw5.annotators;

import java.util.ArrayList;
import java.util.Iterator;

import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.jcas.tcas.Annotation;

import edu.cmu.lti.qalab.types.*;
import edu.cmu.lti.qalab.utils.Utils;


/**
 * NGramAnnotator detects NGrams (1, 2, 3) for question and answers. 
 * @author Laleh
 *
 */

public class NGramAnnotator extends JCasAnnotator_ImplBase {

  /**
   * @param N : keeps the size on N in NGrams
   *
   */
  @Override
  public void process(JCas aJCas){
    /**
     * considering n = 1,2,3 unigram, bigram, trigram
     */
    ArrayList<Sentence> sentList=Utils.getSentenceListFromTestDocCAS(aJCas);
    ArrayList<Question> quesList=Utils.getQuestionListFromTestDocCAS(aJCas);
    ArrayList<ArrayList<Answer>> ansList=Utils.getAnswerListFromTestDocCAS(aJCas);
    
    System.out.println("Number of Sentences:");
    System.out.println(sentList.size());
    
    for (int n = 1; n < 4; ++n) {
        try {
          //Iterator<Annotation> sentenceIter = aJCas.getAnnotationIndex(Sentence.type).iterator();
          //System.out.println("Number of Sentences in JCas:");
          //System.out.println(aJCas.getAnnotationIndex(Sentence.type).size());
          //System.out.println("Number of Ques in JCas:");
          //System.out.println(aJCas.getAnnotationIndex(Question.type).size());
          //System.out.println("Number of Ans in JCas:");
          //System.out.println(aJCas.getAnnotationIndex(Answer.type).size());
          //while (sentenceIter.hasNext()) {
            //Sentence sentence = (Sentence) sentenceIter.next();
          
          
          // Creating Ngram for sentences
          for(int s=0;s<sentList.size();s++){
            Sentence sentence=sentList.get(s);
            
            FSList tokens = sentence.getTokenList();
            ArrayList tokensArray =Utils.fromFSListToCollection(tokens, Token.class);
            
            int k = tokensArray.size() + 1 - n;

            for (int i = 0; i < k; ++i) {
              NGram annotatedNgram = new NGram(aJCas);
              annotatedNgram.setN(n);
              annotatedNgram.addToIndexes();
              annotatedNgram.setElementType(Token.class.getName());
              annotatedNgram.setOwner(sentence);

              ArrayList<Token> tokenList = new ArrayList<Token>();
              for (int j = 0; j < n; ++j) {
                tokenList.add((Token) tokensArray.get(i + j));
              }
              
              annotatedNgram.setBegin(tokenList.get(0).getBegin());
              annotatedNgram.setEnd(tokenList.get(n - 1).getEnd());
              FSArray annotatedTokens = new FSArray(aJCas, n);
              for (int j = 0; j < n; ++j) {
                annotatedTokens.set(j, tokenList.get(j));
              }
              
              annotatedNgram.setElements(annotatedTokens);
              
            }
          }
          System.out.format("%dgram for sentences is done.\n",n);
          
          
          // Creating Ngrams for questions
          for(int s=0;s<quesList.size();s++){
            Question question=quesList.get(s);
            
            FSList tokens = question.getTokenList();
            ArrayList tokensArray =Utils.fromFSListToCollection(tokens, Token.class);
            
            
            int k = tokensArray.size() + 1 - n;

            for (int i = 0; i < k; ++i) {
              NGram annotatedNgram = new NGram(aJCas);
              annotatedNgram.setN(n);
              annotatedNgram.addToIndexes();
              annotatedNgram.setElementType(Token.class.getName());
              annotatedNgram.setOwner(question);

              ArrayList<Token> tokenList = new ArrayList<Token>();
              for (int j = 0; j < n; ++j) {
                tokenList.add((Token) tokensArray.get(i + j));
              }
              
              annotatedNgram.setBegin(tokenList.get(0).getBegin());
              annotatedNgram.setEnd(tokenList.get(n - 1).getEnd());
              FSArray annotatedTokens = new FSArray(aJCas, n);
              for (int j = 0; j < n; ++j) {
                annotatedTokens.set(j, tokenList.get(j));
              }
              
              annotatedNgram.setElements(annotatedTokens);
              
            }
          }
          System.out.format("%dgram for questions is done.\n",n);
          
          
          
          // Creating Ngrams for answers
          Iterator<ArrayList<Answer>> ansChoiceListIter= ansList.iterator();
          while (ansChoiceListIter.hasNext()){
            ArrayList<Answer> ansChoiceList = ansChoiceListIter.next();
            for(int s=0;s<ansChoiceList.size();s++){
              Answer answer=ansChoiceList.get(s);
              
              FSList tokens = answer.getTokenList();
              ArrayList tokensArray =Utils.fromFSListToCollection(tokens, Token.class);
              
              
              int k = tokensArray.size() + 1 - n;
  
              for (int i = 0; i < k; ++i) {
                NGram annotatedNgram = new NGram(aJCas);
                annotatedNgram.setN(n);
                annotatedNgram.addToIndexes();
                annotatedNgram.setElementType(Token.class.getName());
                annotatedNgram.setOwner(answer);
  
                ArrayList<Token> tokenList = new ArrayList<Token>();
                for (int j = 0; j < n; ++j) {
                  tokenList.add((Token) tokensArray.get(i + j));
                }
                
                annotatedNgram.setBegin(tokenList.get(0).getBegin());
                annotatedNgram.setEnd(tokenList.get(n - 1).getEnd());
                FSArray annotatedTokens = new FSArray(aJCas, n);
                for (int j = 0; j < n; ++j) {
                  annotatedTokens.set(j, tokenList.get(j));
                }
                
                annotatedNgram.setElements(annotatedTokens);
                
              }
            }
          }
          System.out.format("%dgram for answers is done.\n",n);
          
                   
        } catch (Exception e) {
          e.printStackTrace();
        }
      
    }
  }

}
