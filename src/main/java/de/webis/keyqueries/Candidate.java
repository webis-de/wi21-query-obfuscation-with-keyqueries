package de.webis.keyqueries;

public class Candidate {
	String query;
	boolean isKeyquery;
	
	public Candidate(String query, KeyQueryChecker kq) {
		this.query = query;
		if(kq.isKeyQuery(query)) {
			this.isKeyquery = true;
		} else {
			this.isKeyquery = false;
		}
	}
	@Override
	public String toString() {
		return query +"=" +isKeyquery;
	}
}
