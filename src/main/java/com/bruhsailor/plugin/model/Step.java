package com.bruhsailor.plugin.model;

import com.bruhsailor.plugin.StepId;
import java.util.List;

public class Step
{
	public List<ContentFragment> content;
	public List<NestedBlock> nestedContent;
	public Metadata metadata;

	// Populated after JSON parse by GuideRepository.
	public transient StepId id;

	public static class NestedBlock
	{
		public int level;
		public List<ContentFragment> content;
	}
}
