package edu.cmu.lti.deiis.hw5.annotators;

import java.util.ArrayList;

import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;

import edu.cmu.lti.qalab.types.Sentence;
import edu.cmu.lti.qalab.types.TestDocument;
import edu.cmu.lti.qalab.utils.Utils;


public class SentenceCombiner extends JCasAnnotator_ImplBase {

	private static int K_SENTENCES = 3;

	// This class will merge all the annotations in batches of N sentences.
	@Override
	public void process(JCas jCas) throws AnalysisEngineProcessException {

		System.out.println("******Entered into process of Combining");
		TestDocument testDoc = Utils.getTestDocumentFromCAS(jCas);
		// String id = srcDoc.getId();
		ArrayList<Sentence> sentenceList = new ArrayList<Sentence>();
		try {
			// String lines[] = docText.split("[\\n]");
			FSList sentList = testDoc.getSentenceList();

			int i = 0;

			while (true) {

				i++;
				Sentence sentence = null;

				try {
					sentence = (Sentence) sentList.getNthElement(i);
				} catch (Exception e) {
					break;
				}

				String sentText = sentence.getText().trim();
				// System.out.println("Processing sentence "+i+"\t"+sentText);
				if (sentText.equals("")) {
					continue;
				}

				int endIndex=0;
				for (int j = i+1; j < i + K_SENTENCES-1; j++) {
					try {
						Sentence windowSentence=(Sentence) sentList.getNthElement(j);					
						sentText+=(windowSentence.getText()+" ");
						endIndex=windowSentence.getEnd();
						
					} catch (Exception e) {
						break;
					}
				}

				sentence.setText(sentText);
				sentence.setEnd(endIndex);;
				sentence.addToIndexes();
//				sentenceList.add(sentence);


			}
			System.out.println(i);

			// System.out.println("Difference between size of (SourceDocument - FilteredDocument): "+(docText.length()-filteredText.length()));

//			FSList modifiedSentList = Utils.createSentenceList(jCas,
//					sentenceList);
			// annotation.setId(id);
//			testDoc.setSentenceList(modifiedSentList);
//			testDoc.addToIndexes();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}