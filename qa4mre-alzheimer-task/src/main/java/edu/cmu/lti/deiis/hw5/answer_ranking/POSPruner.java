package edu.cmu.lti.deiis.hw5.answer_ranking;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.resource.ResourceInitializationException;

import edu.cmu.lti.qalab.types.Answer;
import edu.cmu.lti.qalab.types.NounPhrase;
import edu.cmu.lti.qalab.types.Question;
import edu.cmu.lti.qalab.types.QuestionAnswerSet;
import edu.cmu.lti.qalab.types.Token;
import edu.cmu.lti.qalab.utils.Utils;

public class POSPruner extends AbstractPruner {

	public void initialize(UimaContext context)
			throws ResourceInitializationException {
		super.initialize(context);
	}

	@Override
	public ArrayList<Answer> prune(Question question, ArrayList<Answer> answers) {
		Set<String> questionPos=this.getQuestionPOS(question);
		
		ArrayList<Answer> prunedAnswerList=new ArrayList<Answer>();
		for (Answer answer : answers) {
			
			/**
			 * Keep the None of the above answers
			 */
			String answerText=answer.getText().toLowerCase();
			if(answerText.contains("none")&&answerText.contains("above"))
			{
				prunedAnswerList.add(answer);
				continue;
			}
			
			String expectedPos=this.getAnswerPOS(answer);
			
			if(questionPos.contains("*") || questionPos.contains(expectedPos))
			{
				prunedAnswerList.add(answer);
			}
			else
			{
				if(answer.getIsCorrect())
				{
					System.out.print("CORRECT >>>>>>>>>>>>>>>>>>>>>");
				}
				System.out.println("PRUNED BASED ON EXPECTED POS: "+expectedPos+"---->"+answer.getText());
			}
		}
		return prunedAnswerList;
	}

	/** 
	 * 
	 * **/
	public Token getKeyPOS(ArrayList<Token> tokens) {
		boolean beginNounPhrase = false;
		Token key=null;
		for (Token token : tokens) {
			if (this.nounTags.contains(token.getPos())&& !token.getText().equals("kind") && !token.getText().equals("type")) {
				beginNounPhrase = true;
				key=token;
			}
			if (beginNounPhrase && !this.nounTags.contains(token.getPos())) {
				beginNounPhrase = false;
				break;
			}
		}
		return key;
	}
	
	
	public List<Token> getQuestionPattern(Question question)
	{
		FSList tokensFS=question.getTokenList();
		ArrayList<Token> tokenList=Utils.fromFSListToCollection(tokensFS, Token.class);
		
		List<Token> questionTags=new ArrayList<Token>();
		
		boolean beginQuestion=false;

		for (Token token : tokenList) {
	
			if (this.questionTags.contains(token.getPos())) {
				if(beginQuestion==true)
				{
					questionTags.clear();
				}
				beginQuestion=true;
			}
			if (this.nounTags.contains(token.getPos())) {
				beginQuestion=false;
			}
			if (beginQuestion)
			{
				questionTags.add(token);
			}
		}
		return questionTags;
	}

	/**
	 * Return the most probable POS that the question is seeking
	 * @param question
	 * @return
	 */
	public Set<String> getQuestionPOS(Question question)
	{
		List<Token> questionTokens=this.getQuestionPattern(question);
		
		StringBuffer posChainBuffer=new StringBuffer();
		
		for(Token qt:questionTokens)
		{
			if(posChainBuffer.length()>0)
			{
				posChainBuffer.append("-");
			}
			posChainBuffer.append(qt.getPos());
		}
		String posChain=posChainBuffer.toString();
		
		HashSet<String> expectedPOS=new HashSet<String>();
		if(posChain.startsWith("WDT")||posChain.startsWith("WP"))
		{
			expectedPOS.add("NN");
			expectedPOS.add("NNS");
			expectedPOS.add("NNP");
			expectedPOS.add("NNPS");
			expectedPOS.add("SYM");
						
		}
		else if(posChain.startsWith("WRB-JJ"))
		{
			expectedPOS.add("CD");
		}
		else
		{
			expectedPOS.add("*");
		}
		return expectedPOS;
	}
	
	/**
	 * Returns the true POS tag of the answer.
	 * @param answer
	 * @return
	 */
	public String getAnswerPOS(Answer answer)
	{
		FSList tokensFS=answer.getTokenList();
		ArrayList<Token> tokenList=Utils.fromFSListToCollection(tokensFS, Token.class);
		
		FSList npFS=answer.getNounPhraseList();
		ArrayList<NounPhrase> npList=Utils.fromFSListToCollection(npFS, NounPhrase.class);
		
		Set<String> posTags=new HashSet<String>();
		
		for(Token token: tokenList)
		{
			posTags.add(token.getPos());
		}
//		if(posTags.contains("CD"))
//		{
//			return "CD";
//		}
		if(npList.size()>1)
		{
			return "NNS";
		}
		else
		{		
			String ansPos= tokenList.get(tokenList.size()-1).getPos();
			return ansPos;
		}
	}

	
}
