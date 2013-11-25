package edu.cmu.lti.deiis.hw5.annotators;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import abner.Tagger;
import edu.cmu.lti.qalab.types.NounPhrase;
import edu.cmu.lti.qalab.types.Question;
import edu.cmu.lti.qalab.types.Token;
import edu.cmu.lti.qalab.utils.Utils;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;

public class QuestionTypeAnnotator extends JCasAnnotator_ImplBase {

	private Set<String> questionTags;
	private Set<String> nounTags;
	private Tagger tagger;

	public void initialize(UimaContext context)
			throws ResourceInitializationException {
		super.initialize(context);

		this.questionTags = new HashSet<String>();
		// Wh-determiner
		this.questionTags.add("WDT");
		// Wh-pronoun
		this.questionTags.add("WP");
		// Possessive wh-pronoun
		this.questionTags.add("WP$");
		// Wh-adverb
		this.questionTags.add("WRB");

		this.nounTags = new HashSet<String>();
		// Noun singular or mass
		this.nounTags.add("NN");
		// Noun plural
		this.nounTags.add("NNS");
		// Proper noun singular
		this.nounTags.add("NNP");
		// Proper noun plural
		this.nounTags.add("NNPS");
		
		this.tagger=new Tagger(Tagger.BIOCREATIVE);
	}

	@Override
	public void process(JCas jCas) throws AnalysisEngineProcessException {
		ArrayList<Question> questionList = Utils
				.getQuestionListFromTestDocCAS(jCas);
		
		
		
		for (Question question : questionList) {
			
			ArrayList<NounPhrase> nps=Utils.getNounPhraseListFromQuestionList(question);
			ArrayList<Token> questionTokens = Utils
					.getTokenListFromQuestion(question);

			StringBuffer highlight = new StringBuffer();
			StringBuffer posString=new StringBuffer();
			boolean beginHighlight = false;
			boolean beginNounPhrase = false;
			for (Token token : questionTokens) {
				if (this.questionTags.contains(token.getPos())) {
					beginHighlight = true;
				}		
				if (this.nounTags.contains(token.getPos())) {
					beginNounPhrase=true;
				}
				if (beginNounPhrase && !this.nounTags.contains(token.getPos()))
				{
					beginNounPhrase=false;
					beginHighlight =false;
				}
				if (beginHighlight) {
					highlight.append(token.getText()+" ");
					posString.append(token.getPos()+" ");
				}
			}
			System.out.println(highlight);
			System.out.println(posString);
		}

	}

}
