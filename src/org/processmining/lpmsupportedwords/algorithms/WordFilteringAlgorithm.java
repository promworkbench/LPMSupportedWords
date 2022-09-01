package org.processmining.lpmsupportedwords.algorithms;

import java.util.ArrayList;
import java.util.List;

import org.deckfour.xes.model.XAttributeDiscrete;
import org.deckfour.xes.model.XLog;
import org.processmining.lpmsupportedwords.models.ShortLog;
import org.processmining.lpmsupportedwords.models.Word;
import org.processmining.lpmsupportedwords.parameters.DiscoverContextRichLPMsParameters;

public class WordFilteringAlgorithm {

	/**
	 * Reads words from the first log calculating support for each word in the
	 * second log, and returns a log that contains only the relevant words.
	 * 
	 * @param words
	 * @param log
	 * @param parameters
	 * @return
	 */
	public static XLog filterWords(XLog words, XLog log, DiscoverContextRichLPMsParameters parameters) {
		ShortLog shortLog = new ShortLog(log, parameters.getClassifier());
		ShortLog shortWords = new ShortLog(words, shortLog);
		List<Word> wordsList = new ArrayList<>();
		for (short[] word : shortWords.getLog()) {
			wordsList.add(new Word(word, MineSupportedWordsAlgorithm.countPositiveWitnesses(word, shortLog.getLog())));
		}
		wordsList = filterIrrelevantSubwords(wordsList, parameters.getSupportDelta());
		return MineSupportedWordsAlgorithm.wordsToLog(wordsList, shortLog, null);
	}

	/**
	 * Reads the words from a log with support attributes on traces, and returns a
	 * log that contains only the relevant words.
	 * 
	 * @param words
	 * @param parameters
	 * @return
	 */
	public static XLog filterWords(XLog words, DiscoverContextRichLPMsParameters parameters) {
		ShortLog shortWords = new ShortLog(words, parameters.getClassifier());
		List<Word> wordsList = new ArrayList<>();
		short[][] shortLog = shortWords.getLog();

		for (int i = 0; i < shortLog.length; i++) {
			short[] word = shortLog[i];
			int support = (int) ((XAttributeDiscrete) words.get(i).getAttributes().get("support")).getValue();
			wordsList.add(new Word(word, support));
		}
		wordsList = filterIrrelevantSubwords(wordsList, parameters.getSupportDelta());
		return MineSupportedWordsAlgorithm.wordsToLog(wordsList, shortWords, null);
	}

	/**
	 * Returns the words from {@code wordSet} that are relevant w.r.t.
	 * {@code minSup}.
	 * 
	 * @param wordSet
	 * @param minSup
	 * @return
	 */
	public static List<Word> filterIrrelevantSubwords(List<Word> wordSet, int minSup) {
		List<Word> filteredWords = new ArrayList<>();
		for (Word word : wordSet) {
			if (isRelevantWord(word, wordSet, minSup)) {
				filteredWords.add(word);
			}
		}
		return filteredWords;
	}

	/**
	 * Checks if {@code potentialSubword} is a relevant word in {@code words}. A
	 * word is relevant iff: 1. it is at least more than one event and; 2. it is not
	 * a subword of any word in the given set, or it has at least {@code minSup}
	 * more support than any of the words it is a subword of.
	 * 
	 * @param potentiallyRelevantWord
	 * @param wordSet
	 * @param minSup
	 * @return
	 */
	private static boolean isRelevantWord(Word potentiallyRelevantWord, List<Word> wordSet, int minSup) {
		int supportThreshold = potentiallyRelevantWord.support - minSup;
		for (Word word : wordSet) {
			if (supportThreshold < word.support && isSubword(potentiallyRelevantWord.word, word.word)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Checks if {@code potentialSubword} is a subword of {@code word}. A word W is
	 * a subword of word W' if W is strictly smaller than W', and W' contains all
	 * events of W in the same order as they appear in W.
	 * 
	 * @param potentialSubword
	 * @param word
	 * @return {@code true} iff {@code potentialSubword} is a subword of
	 *         {@code word}.
	 */
	private static boolean isSubword(short[] potentialSubword, short[] word) {
		if (potentialSubword.length >= word.length) {
			return false;
		}
		int i = 0;
		for (short event : word) {
			if (potentialSubword[i] == event) {
				i++;
			}
			if (i == potentialSubword.length) {
				return true;
			}
		}
		return false;
	}
}
