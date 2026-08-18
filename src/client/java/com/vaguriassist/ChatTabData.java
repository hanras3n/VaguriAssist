package com.vaguriassist;

import java.util.ArrayList;
import java.util.List;

public class ChatTabData {
	public String name;
	public int color;
	public boolean hideFromMain;
	public List<ChatFilterData> filters;

	public ChatTabData() {
		this.name = "";
		this.color = 0xFFFFFF;
		this.hideFromMain = false;
		this.filters = new ArrayList<>();
	}

	public ChatTabData(String name, int color) {
		this.name = name;
		this.color = color;
		this.hideFromMain = false;
		this.filters = new ArrayList<>();
	}

	public void addFilter(String pattern, String type) {
		ChatFilterData filter = new ChatFilterData();
		filter.pattern = pattern;
		filter.type = type;
		filters.add(filter);
	}

	public static class ChatFilterData {
		public String pattern;
		public String type; // "contains", "starts_with", "regex"

		public ChatFilterData() {
			this.pattern = "";
			this.type = "contains";
		}
	}
}
