package zeno2.kernel;

import java.io.File;
import java.io.InputStream;
import java.util.Date;
import java.util.List;

/**
 * Zeno 2 Articles
 * @author T. Gordon 
 */
public interface Article extends ZenoResource {
	
	public void finish(boolean anonymously) throws ZenoException;

	/** The author is NOT a synonym for the creator, to allow
	one user to add an article for another user. */

	/** Returns the id of the principal who is the author of this article. */

	public String getAuthor() throws ZenoException;

	/** Sets the id of the author of this article. Save to make persistent. */

	public void setAuthor(String principalId) throws ZenoException;

	/** Returns the topic, the resource belongs to, or null */
	
	public Topic getTopic() throws ZenoException;

	/** Returns the identifier of the topic, the resource belongs to, or 0 */
	
	public int getTopicId();

	/** Returns true if this article is a topic of the journal. */ 

	public boolean isTopic() throws ZenoException;
	
	/** Sets the isTopic property of this article. */
	//obsolete
	public void setIsTopic(boolean isTopic) throws ZenoException;
	
	 /** Transforms an article into a topic; no operation for a topic;
	throws a NoPermissionException unless the user is an editor 
	or the creator of the article */
	
	public Topic transformArticle() throws ZenoException;
	
	/** Gets a comma separated list of keywords for this article. */

	public String getKeywords() throws ZenoException;

	/** Sets the keywords for this article.  They should be separated by
	commas. Save to make persistent. */

	public void setKeywords(String keywords) throws ZenoException;

	/** Gets the expiration date of this article. */

	public Date getExpirationDate() throws ZenoException;

	/** Sets the expiration date of this article. Save to make persistent. */

	public void setExpirationDate(Date expirationDate) throws ZenoException;

	/** If the article describes a task or event, the begin and end date properties
	can be used to record the date or period of the event or task.  Use the beginDate
	property to record the date when a task is scheduled to start and the endDate property
	to record when it should be completed, i.e. the due date.  If an event has no
	duration, use the startDate to record the date and leave the endDate null.  If a
	task has no scheduled starting time, leave the beginDate null.
	Returns the date when the task shall begin or the event is scheduled to start.
	May be null.  */

	public Date getBeginDate() throws ZenoException;

	/** Sets the date when the event or task begins. Save to make persistent. */

	public void setBeginDate(Date beginDate) throws ZenoException;

	/** Returns the date when the task shall be completed (i.e. due date) or  
	the event is finished.  May be null.  */

	public Date getEndDate() throws ZenoException;

	/** Sets the date when the event described will occur or the task described is due. 
	Save to make persistent. */

	public void setEndDate(Date endDate) throws ZenoException;

	/** Gets the label of this article.  Labels are used to indicate the type
	of the article.  The ArticleLabels property of the Journal class can be used to 
	provide a set of recommended labels. */

	public String getLabel() throws ZenoException;

	/** Sets the label of the article. Save to make persistent. */

	public void setLabel(String label) throws ZenoException;

	/** Gets the qualifier of this article.  These qualifiers are labels
	reserved for use by editors.  The qualifiers property
	of the Journal class can be used to provide a set of recommended
	qualifiers. */

	public String getQualifier() throws ZenoException;

	/** Sets the editor label of this article. Save to make persistent. */

	public void setQualifier(String label) throws ZenoException;

	/** Checks whether the article is published. */

	public boolean getPublished() throws ZenoException;

	/** Sets the published property of this article. Save to make persistent. */

	public void setPublished(boolean published) throws ZenoException;

	/** Returns the URL of the CSS file for this article's journal. */

	public String getStyleSheetUrl() throws ZenoException;

	/** Gets the attachment of this article with the specified id.  */

	public Attachment getAttachment(int id) throws ZenoException;

	/** Gets the attachment of this article with the specified name.  */

	public Attachment getAttachment(String name) throws ZenoException;

	/** Gets the attachments of this article.  */

	public List getAttachments() throws ZenoException;

	/** Adds an attachment to this article.  This is made persistent immediately
	without having to use the save method. */

	public Attachment addAttachment(String name, String mimeType, File file)
		throws ZenoException;

	public Attachment addAttachment(String name, String mimeType, InputStream inputStream)
		throws ZenoException;

	/** Deletes an attachment.  This is done immediately, without having to 
	use the save method. */

	public void deleteAttachment(int id) throws ZenoException;
	
	
	/** Deletes an attachment.  This is done immediately, without having to 
	use the save method. obsolete */

	public void deleteAttachment(String name) throws ZenoException;

	/** Renames an attachment.  This is done immediately, without having to 
	use the save method. obsolete*/

	public void renameAttachment(String oldname, String newname)
		throws ZenoException;

	/** Generates an event stating that the user has read this article. */

	public void generateReadEvent() throws ZenoException;
	
	/** Increments the read count of the article and returns the new value */
	
	public int incrementReadCount() throws ZenoException;
	
	/** Creates a new article and links it to this article 
	using label and default alias; if this article belongs 
	to a topic, the new article is added to this topic;
	if the journal or topic is closed a NoPermissionException
	is thrown for writers. */
	
	public Article createReply(String linkLabel) throws ZenoException;


	/** Returns a list of articles in the parent journal, ordered by rank and then title, 
	which have a link targeting this article. */

	public List getReplies() throws ZenoException;

	//public void finished() throws ZenoException;

}
