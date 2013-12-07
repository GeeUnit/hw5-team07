package edu.cmu.lti.deiis.hw5.answer_ranking;

import java.io.IOException;
import java.util.ArrayList;
import java.util.PriorityQueue;
import java.util.Set;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.resource.ResourceInitializationException;

import de.linguatools.disco.DISCO;
import edu.cmu.lti.qalab.types.Answer;
import edu.cmu.lti.qalab.types.NounPhrase;
import edu.cmu.lti.qalab.types.Question;
import edu.cmu.lti.qalab.types.Token;
import edu.cmu.lti.qalab.utils.Utils;

/**
 * Prunes documents based on a lack of second-order semantic similarity. It uses a combination of DISCO and the 2007 pubmed corpus
 * @author jeffgee@cmu.edu
 *
 */
public class CooccurrencePruner extends AbstractPruner {

	private DISCO disco;
	private static final String DIR = "corpus/en-PubMedOA-20070501";
	private static double THRESSHOLD=0.06D;

	public void initialize(UimaContext context) {
		try {
			super.initialize(context);
			disco = new DISCO(DIR, true);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ResourceInitializationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public ArrayList<Answer> prune(Question question, ArrayList<Answer> answers) {
		
		ArrayList<Token> tokens=Utils.getTokenListFromQuestion(question);
		
		
		ArrayList<Answer> prunedAnswers=new ArrayList<Answer>();

		ArrayList<NounPhrase> qNPs=Utils.getNounPhraseListFromQuestionList(question);
		
		for(Answer answer:answers)
		{
			
			/**
			 * Keep the None of the above answers
			 */
			String answerText=answer.getText().toLowerCase();
			if(answerText.contains("none")&&answerText.contains("above"))
			{
				prunedAnswers.add(answer);
				continue;
			}
			
			FSList npList=answer.getNounPhraseList();
			ArrayList<NounPhrase> nounPhrases=Utils.fromFSListToCollection(npList, NounPhrase.class);
			if(nounPhrases.size()>0){
				
				double maxSOS=0D;
				
				//for each of the nounPhrases in the answer
				for(NounPhrase np:nounPhrases)
				{
					String[] npTokens=np.getText().split(" ");
					String interestingToken=npTokens[npTokens.length-1];
					
					
					try {
						
						int count=disco.frequency(interestingToken);
						//if an answer is not found in the corpus, keep it 
						if(count>0)
						{
							for(NounPhrase qNP:qNPs)
							{
								String[] qNPTokens=qNP.getText().split(" ");
								String qToken=qNPTokens[qNPTokens.length-1];
								
								double sos=disco.secondOrderSimilarity(qToken, interestingToken);
							
								//if a word is found in the corpus, and it is semantically dissimilar, prune it.
								if(sos>maxSOS)
								{
									maxSOS=sos;
								}
							}
						}
						else
						{
							maxSOS=THRESSHOLD;
						}
						
				
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
				}
				
				if(maxSOS>=THRESSHOLD)
				{
					prunedAnswers.add(answer);
				}
				else
				{
					if(answer.getIsCorrect())
					{
						//show if a correct answer was pruned
						System.out.print("CORRECT ===========================>");
					}
					System.out.println("PRUNED BASED ON COOCURRENCE: "+answer.getText());

				}
				
			}
			else
			{
				prunedAnswers.add(answer);
			}
		}
		if(prunedAnswers.size()>0)
		{
			return prunedAnswers;
		}
		else
		{
			return answers;
		}
	}
	
	/** 
	 * 
	 * **/
	public Token getKeyToken(ArrayList<Token> tokens) {
		boolean beginNounPhrase = false;
		Token key=null;
		for (Token token : tokens) {
			if (this.nounTags.contains(token.getPos())&&!token.getText().equals("kind") && !token.getText().equals("type")) {
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
}
