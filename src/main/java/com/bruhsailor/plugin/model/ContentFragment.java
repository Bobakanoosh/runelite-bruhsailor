package com.bruhsailor.plugin.model;

import java.util.Map;

public class ContentFragment
{
	public String text;
	public Formatting formatting;

	public static class Formatting
	{
		public Boolean bold;
		public Double fontSize;
		public Color color;
	}

	public static class Color
	{
		public double r;
		public double g;
		public double b;
	}
}
