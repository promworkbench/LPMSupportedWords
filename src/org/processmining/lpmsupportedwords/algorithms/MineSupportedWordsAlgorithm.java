package org.processmining.lpmsupportedwords.algorithms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.factory.XFactory;
import org.deckfour.xes.factory.XFactoryRegistry;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.lpmsupportedwords.models.ShortLog;
import org.processmining.lpmsupportedwords.models.Word;
import org.processmining.lpmsupportedwords.parameters.MineSupportedWordsParameters;

import com.google.common.primitives.Shorts;

public class MineSupportedWordsAlgorithm {
	/**
	 * Creates a log with traces that correspond to supported words in the input
	 * log. Also adds a "support" attribute to each trace.
	 * 
	 * @param context
	 * @param log
	 * @param parameters
	 * @return
	 */
	public static XLog mineSupportedWords(PluginContext context, XLog log, MineSupportedWordsParameters parameters) {
		ShortLog shortLog = new ShortLog(log, parameters.getClassifier());
		int[] eventCounter = shortLog.getEventCounts();
		boolean[] frequentEvents = new boolean[eventCounter.length];
		for (short i = 0; i < eventCounter.length; i++) {
			frequentEvents[i] = (eventCounter[i] >= parameters.getSupportThreshold());
		}
		System.out.println("[MSW]: frequent events selected");

		context.getProgress().setMaximum(frequentEvents.length);
		List<Word> wordSet = new ArrayList<>();
		for (short i = 0; i < frequentEvents.length; i++) {
			if (frequentEvents[i]) {
				frequentEvents[i] = false;
				Word word = new Word(new short[] { i }, eventCounter[i]);
				mineRecurse(shortLog.getLog(), parameters.getSupportThreshold(), frequentEvents, word, wordSet);
				frequentEvents[i] = true;
				context.getProgress().inc();
			}
		}
		System.out.println("[MSW]: mission complete");

		XLog output = wordsToLog(wordSet, shortLog, XFactoryRegistry.instance().currentDefault());
		String outputName = "Supported words of " + XConceptExtension.instance().extractName(log);
		XConceptExtension.instance().assignName(output, outputName);
		context.getFutureResult(0).setLabel(outputName);
		return output;
	}

	/**
	 * Transforms a list of Words into an XLog with support attributes.
	 * 
	 * @param words
	 * @param shortLog
	 * @param factory
	 * @return
	 */
	public static XLog wordsToLog(List<Word> words, ShortLog shortLog, XFactory factory) {
		if (factory == null) {
			factory = XFactoryRegistry.instance().currentDefault();
		}
		XLog newLog = factory.createLog();
		newLog.getClassifiers().add(shortLog.getClassifier());
		// containing a trace for each word
		for (Word word : words) {
			newLog.add(wordToTrace(word, shortLog, factory));
		}
		XConceptExtension.instance().assignName(newLog, "Log from Word List");
		return newLog;
	}

	/**
	 * Transforms a Word into an XTrace with support attribute.
	 * 
	 * @param word
	 * @param shortLog
	 * @param factory
	 * @return
	 */
	private static XTrace wordToTrace(Word word, ShortLog shortLog, XFactory factory) {
		XTrace newTrace = factory.createTrace();
		XEvent[] eventArray = shortLog.getEventArray();
		for (short activity : word.word) {
			newTrace.add(eventArray[activity]);
		}
		XConceptExtension.instance().assignName(newTrace, Arrays.toString(word.word));
		newTrace.getAttributes().put("support", factory.createAttributeDiscrete("support", word.support, null));
		return newTrace;
	}

	/**
	 * Part of mineSupportedWords().
	 * 
	 * @param log
	 * @param minSup
	 * @param freqEvents
	 * @param curWord
	 * @param wordSet
	 */
	private static void mineRecurse(short[][] log, int minSup, boolean[] freqEvents, Word curWord, List<Word> wordSet) {
		wordSet.add(curWord);

		for (short i = 0; i < freqEvents.length; i++) {
			if (freqEvents[i]) {
				freqEvents[i] = false;
				Word nextWord = new Word(new short[curWord.word.length + 1], 0);
				System.arraycopy(curWord.word, 0, nextWord.word, 0, curWord.word.length);
				nextWord.word[nextWord.word.length - 1] = i;
				nextWord.support = countPositiveWitnesses(nextWord.word, log);
				//System.out.println("[MSW]: Trying " + Arrays.toString(nextWord) + " and bigger...");
				if (nextWord.support >= minSup) {
					mineRecurse(log, minSup, freqEvents, nextWord, wordSet);
				}
				freqEvents[i] = true;
			}
		}
	}

	/**
	 * Counts how often the given word occurs in the given log. Ignores events that
	 * do not occur in word.
	 * 
	 * @param word
	 * @param log
	 * @return
	 */
	public static int countPositiveWitnesses(short[] word, short[][] log) {
		int witnessCount = 0;
		for (short[] trace : log) {
			int witnessIndex = 0;
			for (short event : trace) {
				// Check if the event fits the word
				if (word[witnessIndex] == event) {
					witnessIndex++;
				} else if (!Shorts.contains(word, event)) {
					/*
					 * I didn't project the log on curWord, so this case where the event doesn't
					 * occur in curWord can just be ignored.
					 */
				} else {
					// Didn't match & didn't skip event = start over
					witnessIndex = 0;
					// could cause a match on first event though...
					if (word[witnessIndex] == event) {
						witnessIndex++;
					}
				}
				// Event checked, do we have a complete witness now?
				if (witnessIndex == word.length) {
					witnessCount++;
					witnessIndex = 0;
				}
			}
		}
		return witnessCount;
	}

}
