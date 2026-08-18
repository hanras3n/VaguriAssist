package com.vaguriassist;

public class HighlightEntry {
	public String word;
	public int color;

	public HighlightEntry() {
		this.word = "";
		this.color = 0xFFFF55;
	}

	public HighlightEntry(String word, int color) {
		this.word = word;
		this.color = color;
	}
}
