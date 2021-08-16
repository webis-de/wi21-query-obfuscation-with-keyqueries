package de.webis.crypsor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

public class UserStudyCsvGeneratorTest {
	@Test
	public void testThatJudgedDocsAreRetrieved() {
		List<String> input = new ArrayList<>(Arrays.asList(
			"french lick resort and casino,service spa indoor outdoor activitie individual schedule treatment visual staff,1",
			"french lick resort and casino,event space recreational site golf court japenese player,-1",
			"french lick resort and casino,frenchlicksprings springscasinograndsstaff,-1"		
		));
		
		Set<Set<String>> expected = new HashSet<>(Arrays.asList(
			new HashSet<>(Arrays.asList("service", "spa", "indoor", "outdoor", "activitie", "individual", "schedule", "treatment", "visual", "staff")),
			new HashSet<>(Arrays.asList("event", "space", "recreational", "site", "golf", "court", "japenese", "player")),
			new HashSet<>(Arrays.asList("frenchlicksprings", "springscasinograndsstaff"))
		));
		
		Set<Set<String>> actual = UserStudyCsvGenerator.alreadyJudgedQueries(input);
		
		Assert.assertEquals(expected, actual);
	}
	
	@Test
	public void testThatUngudgedDocsAreNotRetrieved() {
		List<String> input = new ArrayList<>(Arrays.asList(
			"french lick resort and casino,service spa indoor outdoor activitie individual schedule treatment visual staff,1",
			"french lick resort and casino,event space recreational site golf court japenese player,",
			"french lick resort and casino,frenchlicksprings springscasinograndsstaff,-1"		
		));
		
		Set<Set<String>> expected = new HashSet<>(Arrays.asList(
			new HashSet<>(Arrays.asList("service", "spa", "indoor", "outdoor", "activitie", "individual", "schedule", "treatment", "visual", "staff")),
			new HashSet<>(Arrays.asList("frenchlicksprings", "springscasinograndsstaff"))
		));
		
		Set<Set<String>> actual = UserStudyCsvGenerator.alreadyJudgedQueries(input);
		
		Assert.assertEquals(expected, actual);
	}
}
