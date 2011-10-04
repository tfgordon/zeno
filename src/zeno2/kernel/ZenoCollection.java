package zeno2.kernel;

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;


/**
 * Interface to Zeno 2 collections.
 * @author T. Gordon 
 */

public interface ZenoCollection extends ZenoResource {

	/** Returns an enumeration of the members of this collection which are not
	marked for deletion. Each member is a ZenoResource. The order of the
	members in this enumeration is unspecified. */

	public Iterator getMembers() throws ZenoException;

	/** Returns the members sorted by rank and then title, not including
	members marked for deletion. */

	public Iterator sortMembers() throws ZenoException;
	
	/** Removes a deletion mark from a resource and its descendants
	 * The resource is unmarked if its parent is not marked, 
	 * regardless whether all desendants  could be unmarkded
	 * Returns true if the reosurce is not marked or was unmarked*/
	
	public boolean unmarkForDeletion(boolean propagate) 
		throws ZenoException;

	/**Permanently removes all member resources of this collection (journal) 
	 that are marked for deletion and deletes all links whose endnodes are 
	 at least one of the removed resources. Recursively compacts member 
	 collections (subjournals). However, descending the tree structure (of 
	 nested journals) for compacting subjournals terminates where the user 
	 is not an editor, even if the user is an editor of some deeper 
	 journal.*/

	public void compact() throws ZenoException;

	/** Returns a set of all member resources which have been marked
	for deletion in this collection.  Does not list resources in member
	collections. */

	public Iterator getTrash() throws ZenoException;
	
	
	/** Returns the number of articles in the collection.
	Unpublished and deleted articles are NOT included in the count. */

	public int articleCount() throws ZenoException;
	
	/** Returns the number of deleted articles in the collection. */
	
	public int markedArticleCount() throws ZenoException;

	/** Returns an enumeration containing all article members of the collection,
	Only articles which are published and have not been marked for
	deletion are included. The order of the articles is unspecified. */

	public Iterator getArticles() throws ZenoException;

	/** Returns the n most recently created articles in a collection,
	sorted by creation date.  Only published and not marked for deletion
	articles are included. */
	
	public List getRecentArticles(int n) throws ZenoException;
	
	/** Returns articles created in this collection after date
	sorted by creation date. Only published and not marked for deletion
	topics are included. Uses date of last login if date is null */
	
	public List  getRecentArticles(Date date) throws ZenoException;
	
	/** Returns the n most recently after date modified articles in a collection,
	sorted by modification date. Only published and not marked for deletion
	articles are included. Uses date of last login if date is null.
	-1 for n means no restriction in number */
	
	public List getModifiedArticles(Date date, int n) throws ZenoException;
	
	/** Returns a List articles with BeginDate or EndDate property
	 belonging to the specified time intervall (including end points)
	 which are published and not marked for deletion.
	 The articles are ordered by beginDate, rank, title. */
	 
	public List getArticlesBetween(Date startDate, Date endDate) 
			throws ZenoException;
	
	/** Returns a list of ZenoEvent objects, ordered by date, for the
	creationDate, modificationDate, expirationDate, beginDate and endDate
	properties of published and not marked for deletion articles in this
	collection. This method simply lists the member articles' events
	chronologically, using temporal properties of the articles. It does
	not access a log file or protocol. */

	public Iterator getEventsDuring(Date startDate, Date endDate)
		throws ZenoException;

	/** Returns a list of all article members of the collection 
	which are unpublished but not marked for deletion. */

	public List getUnpublishedArticles() throws ZenoException;
	
	/** Returns a list of all articles with the specified properties in the
	 given collection. Null can be used as a wild card, matching all values. 
	 The parameters fromDate and toDate specify an intervall:
	 an article is selected if its creationDate is >= fromDate and <= endDate.
	 An unspecified date parameter is interpreted as "earliest date in the past" 
	 and "latest date in the future", respectively.The full text parameter 
	 searches notes, keywords, and titles. The full text parameter searches 
	 the values of "title", "note" and "keywords" (as if they were concatenated
	 to a single string). The full text parameter should be a white space 
	 separated list of words to search for. Only articles in which 
	 ALL the words occur are selected.  
	 
	 The order parameter can be used to specify the order in which the articles
	 are listed in the result.  This is a comma separated list of words from 
	 the following set: "author", "title", "creation_date", "label", "qualifier",
	 "rank".  Use the empty string if you do not want to specify an order. */

	public Iterator search(
		String authorId,
		String title,
		String articleLabel,
		String qualifier,
		Date fromDate,
		Date toDate,
		String fullText,
		String order)
			throws ZenoException;
			
	/** The resourceId parameter denotes an article of this collection
	which is selected as the entry point of the graph expansion along 
	the referential structure. The linkLabel parameter can be used to include 
	only resources which can be reached via links with the given label. 
	If the linkLabel parameter is null or the empty string, all links are 
	used to generate the representation of the graph.
	
	Returns an ordered list of outline nodes, with the resource of the 
	given id as the target of the link in each node. The nodes are sorted 
	by sourceAlias of their links. 
	An outline node is a triple (link, duplicate, childrenList).
	The childrenList is an ordered list of outline nodes. The nested 
	lists form an outline tree where the link of each child node targets 
	the source of the link of the parent node.
	
	The structure of the outline tree does not necessarily represent the 
	structure formed by the links. Since links can form arbitrary graphs 
	of resources, not only trees, duplicates or cycles are broken by 
	expanding only a single occurrence of each outline node. This 
	occurrence of the outline node (like any outline node that occurs 
	only once) has duplicate = false.  The other occurrences appear in 
	the outline tree, but are not expanded. They have duplicate = true, 
	which means: the source of the link in this outline node occurrence 
	is the target of a link in another occurrence of this outline node in 
	this tree. */

	public List getOutline(int articleId, String linkLabel) 
		throws ZenoException;


}