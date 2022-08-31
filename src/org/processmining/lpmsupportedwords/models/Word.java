package org.processmining.lpmsupportedwords.models;

public class Word {
	public short[] word;
	public int support;
	
	public Word(short[] word, int support) {
		this.word = word;
		this.support = support;
	}
}