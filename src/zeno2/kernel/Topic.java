package zeno2.kernel;

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Interface to Zeno 2 topics.
 * @author E. Gross 
 */

public interface Topic extends Article, ZenoCollection {
	
	/** Transforms a topic into a plain article which is returned 
	member articles of the topic become direct member articles of the journal
	throws a NoPermissionException unless the user is an editor or the
	creator of the topic*/
	
	public Article dissolve() throws ZenoException;
	
	/** Creates a copy of the topic as an article in the given collection;
	members of the topic are not copied.  */
	
	public Article copyAsArticle(ZenoCollection collection, boolean withLabels)
		throws ZenoException;
	
	/** The articles parameter denotes a set of (plain) articles 
	which are to be moved to this journal. Other resources in the set
	are silently ommitted.
	
	To move a resource means to remove it from the place where it is and 
	add it to a given collection (topic). All attributes and all links 
	connecting this resource with others (moved or not) persist.*/
	
	public void moveHereArticles(Set articles)
		throws ZenoException;
	
	/** The articles parameter denotes a set of (plain) articles 
	which are to be copied to this journal.Other resources in the set
	are silently ommitted.
	
	To copy a resource means to create a new resource derived from the 
	original and add it to the destination journal: all attributes but 
	the id are initialized from the original.
	
	The withLinks parameter specifies whether links pointing from copied
	nodes to other nodes are to be copied or not. To copy a link means to 
	create a new link that connects either the copies of the two endnodes 
	(in the destination journal) or the copy of one endnode with the other
	uncopied endnode. 
	
	Which endnodes are actually copied, depends on the permissions of the 
	requesting user. Resources are not copied (without throwing an exception) 
	if he has no permission.*/
	
	public void copyHereArticles(Set articles, boolean withLinks)
		throws ZenoException;
	
	/** Removes all articles with an identifier contained in the List ids  
	 * which belong to the topic and are marked for deletion. 
	 * If the List contains the topic identifier and the topic is
	 * marked for deletion the topic with all its members is removed */
		
	public void removeArticles(List ids)
		throws ZenoException;


}