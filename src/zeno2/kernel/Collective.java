package zeno2.kernel;

import java.util.List;

/**
 * Zeno 2 Collective.  
 */

public interface Collective extends Principal {
	
	/** Returns the membership criterion of the collective. */
	
	public String getMembershipCriterion() throws ZenoException;
	
	/** Sets the membership criterion of the collective */
	
	public void setMembershipCriterion(String criterion) 
		throws ZenoException;
	
}