package edu.cmu.lti.deiis.hw5.answer_ranking;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.resource.ResourceInitializationException;

import edu.cmu.lti.qalab.types.Answer;
import edu.cmu.lti.qalab.types.Question;
import edu.cmu.lti.qalab.types.QuestionAnswerSet;
import edu.cmu.lti.qalab.utils.Utils;

public abstract class AbstractPruner extends JCasAnnotator_ImplBase {

	protected Set<String> nounTags;
	protected Set<String> questionTags;
	
	public void initialize(UimaContext context)
			throws ResourceInitializationException {
		super.initialize(context);
		this.nounTags=new HashSet<String>();
		this.nounTags.add("NN");
		this.nounTags.add("NNS");
		this.nounTags.add("NNP");
		this.nounTags.add("NNPS");
		this.nounTags.add("PRP");
		this.nounTags.add("PRP$");
		
		this.questionTags=new HashSet<String>();
		this.questionTags.add("WDT");
		this.questionTags.add("WP");
		this.questionTags.add("WP$");
		this.questionTags.add("WRB");
	}

	@Override
	public void process(JCas aJCas) throws AnalysisEngineProcessException {
		ArrayList<QuestionAnswerSet> qaSets=Utils.getQuestionAnswerSetFromTestDocCAS(aJCas);
		
		for(QuestionAnswerSet qaSet: qaSets)
		{
				Question q=qaSet.getQuestion();
				FSList answerFS=qaSet.getAnswerList();
				ArrayList<Answer> answerChoices=Utils.fromFSListToCollection(answerFS, Answer.class);
				
				ArrayList<Answer> prunedAnswers=prune(q,answerChoices);
				
				FSList newAnswerChoices=Utils.fromCollectionToFSList(aJCas, prunedAnswers);
				qaSet.setAnswerList(newAnswerChoices);
				
				qaSet.addToIndexes();
		}
	}

	public abstract ArrayList<Answer> prune(Question question,
			ArrayList<Answer> answers);
}
