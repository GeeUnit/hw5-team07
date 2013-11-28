package edu.cmu.lti.deiis.hw5.annotators;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.solr.common.SolrInputDocument;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;

import edu.cmu.lti.qalab.solrutils.SolrUtils;
import edu.cmu.lti.qalab.types.Dependency;
import edu.cmu.lti.qalab.types.NER;
import edu.cmu.lti.qalab.types.NounPhrase;
import edu.cmu.lti.qalab.types.Sentence;
import edu.cmu.lti.qalab.types.Synonym;
import edu.cmu.lti.qalab.types.TestDocument;
import edu.cmu.lti.qalab.utils.Utils;


public class SentenceCombiner extends JCasAnnotator_ImplBase {

	private static int K_SENTENCES = 3;

	// This class will merge all the annotations in batches of N sentences.
	@Override
	public void process(JCas jCas) throws AnalysisEngineProcessException {

		TestDocument testDoc = Utils.getTestDocumentFromCAS(jCas);
		
		try {
			String id = testDoc.getId();

			ArrayList<Sentence> sentenceList = Utils.fromFSListToCollection(
					testDoc.getSentenceList(), Sentence.class);
			
			List<ArrayList<NounPhrase>> newNPLists=new ArrayList<ArrayList<NounPhrase>>();
			List<ArrayList<NER>> newNERLists=new ArrayList<ArrayList<NER>>();
		
			
			for (int i = 0; i < sentenceList.size()-K_SENTENCES; i++) {
				
				Sentence sent = sentenceList.get(i);

				FSList fsNounList = sent.getPhraseList();
				ArrayList<NounPhrase> nounPhrases =new ArrayList<NounPhrase>();

				if(fsNounList!=null)
				{
					nounPhrases = Utils
							.fromFSListToCollection(fsNounList, NounPhrase.class);
				}
				
				FSList fsNEList = sent.getNerList();
				ArrayList<NER> NEs=new ArrayList<NER>();
				if(fsNEList!=null)
				{
					NEs=Utils.fromFSListToCollection(fsNEList, NER.class);
				}
				
				for (int j=i+1; j<i+K_SENTENCES;j++)
				{
					Sentence ctxSent=sentenceList.get(j);
					FSList ctxNounList = ctxSent.getPhraseList();
								
					if(ctxNounList!=null)
					{
						ArrayList<NounPhrase> contextNPs=Utils.fromFSListToCollection(ctxNounList, NounPhrase.class);
						Set<NounPhrase> uniqueNPs=new HashSet<NounPhrase>(contextNPs);
						for(NounPhrase np:uniqueNPs)
						{
							nounPhrases.add(np);
						}
					}
					
					FSList ctxNER=ctxSent.getNerList();
					if(ctxNER!=null)
					{
						ArrayList<NER> contextNER=Utils.fromFSListToCollection(ctxNER, NER.class);
						Set<NER> uniqueNER=new HashSet<NER>(contextNER);
						for(NER ner:uniqueNER)
						{
							NEs.add(ner);
						}
					}		
				}	

				newNPLists.add(nounPhrases);
				newNERLists.add(NEs);
			}

			ArrayList<Sentence> newSentenceList=new ArrayList<Sentence>();
			
			for(int i=0; i<sentenceList.size()-K_SENTENCES;i++)
			{
				Sentence sent=sentenceList.get(i);

				sent.setNerList(Utils.fromCollectionToFSList(jCas,newNERLists.get(i)));
				sent.setPhraseList(Utils.fromCollectionToFSList(jCas, newNPLists.get(i)));
				newSentenceList.add(sent);
			}
			
			FSList sentenceWindows=Utils.createSentenceList(jCas, newSentenceList);
			testDoc.setSentenceList(sentenceWindows);
			testDoc.addToIndexes();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}