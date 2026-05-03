package com.bruhsailor.plugin.model;

import java.util.List;

public class GuideData
{
	public String title;
	public String updatedOn;
	public List<Chapter> chapters;

	public static class Chapter
	{
		public String title;
		public List<Section> sections;
	}

	public static class Section
	{
		public String title;
		public List<Step> steps;
	}
}
