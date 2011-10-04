package zeno2.kernel;

import java.util.Date;
import java.util.List;

/**
 * Interface to resources in a Zeno 2 Journal.
 * Every implementation class must have a constructor method
 * taking an resource id, as a string, and a second parameter
 * for the factory. The constructor should not call the loadHeader method. 
 * Rather, the resource header should be loaded lazily, on demand. 
 * Modifications by setters require a save to be made persistent.
 * if not stated otherwise explicitly.  An exception is
 * raised if the user does not have permission to execute 
 * some method.
 * @author T. Gordon 
 */

public interface ZenoResource {

	/** Tests whether the resource with this id exists in the persistent 
	store. Resources marked for deletion still exist. */

	public boolean exists() throws ZenoException;

	/** Loads all the properties of the resource from the persistent
	store in a single transaction. */

	public void loadProperties() throws ZenoException;

	/** Returns the identifier of the resource. */

	public int getId();

	/** Returns the full URL for the default view of this resource. For example:
	"http://zeno.gmd.de/zeno2/view/<id>" */

	public String getURL();

	/** Returns the "display name" of the resource.  Permissions:
	Ordinarily only readers may view the title.  There are
	two exceptions:  A user who is a reader in a journal may
	view the titles and ids of all ancestors of the journal and
	also the titles of all subjournals. */

	public String getTitle() throws ZenoException;

	/** Sets "display name" of the resource. */

	public void setTitle(String title) throws ZenoException;

	/** Gets the note of this resource. */

	public String getNote() throws ZenoException;

	/** Sets the note of this resource. */

	public void setNote(String note) throws ZenoException;

	/** Computes the size of the note in KB. */

	public int noteSize() throws ZenoException;

	/** Returns the rank of the resource.  The rank property 
	provides a way to (partially) order resources in a collection.  */

	public int getRank() throws ZenoException;

	/** Sets the rank of the resource. */

	public void setRank(int rank) throws ZenoException;

	/** Gets the id of the principal who created the resource. This is set
	automatically by Zeno. */

	public String getCreator() throws ZenoException;

	/** Get the creation date of a resource. This is set automatically
	when the resource is created. */

	public Date getCreationDate() throws ZenoException;

	/** Gets the identifier of the principal who last modified the resource. 
	 This is set automatically by Zeno when changes are made. */

	public String getModifier() throws ZenoException;

	/** Gets the last modification date. This is set automatically
	by Zeno. */

	public Date getModificationDate() throws ZenoException;

	/** Gets the parent collection of this resource.  Returns
	null if this resource is a root collection. */

	public ZenoCollection getParent() throws ZenoException;
	
	/** Gets the Idenfifier of the parent collection of this resource.  
	Returns 0 if this resource is a root collection. */

	public int getParentId();

	/** Checks whether this resource has a parent. */

	public boolean hasParent() throws ZenoException;

	/** Gets the journal of this resource.  Returns null if this
	resource is not in a journal. */

	public Journal getJournal() throws ZenoException;

	/** Returns a list of PathElement objects, one for each of this
	resource's ancestors, ordered from "oldest" to "youngest". */

	public List getPath() throws ZenoException;

	/** Test whether this resource has been marked for deletion. */

	public boolean getMarkedForDeletion() throws ZenoException;

	/** Marks or unmarks this resource to be deleted. Obsolete */

	public void setMarkedForDeletion(boolean markedForDeletion)
		throws ZenoException;
		
	/** Marks this resource and its descendants to be deleted.
	A Resource is only marked, if all descendants could be marked.
	Returns true if the resource is already marked or was marked. */
	  
	public boolean markForDeletion() throws ZenoException;	
	
	/** Unmarks this resource without descendants. 
	A Resource is only unmarked if its parent is not marked
	Returns true if the resource is not marked or was unmarked. */
		
	public boolean unmarkForDeletion() throws ZenoException;	

	/** Saves the state of this resource back to the persistent store. */

	public void save() throws ZenoException;

	/** Returns true iff the resource is locked.  Locked resource
	may be modified only by an editor. */
	//obsolete
	public boolean locked() throws ZenoException;
	
	/** Returns true iff the resource is closed.  closed resource
	may be modified only by an editor. */

	public boolean isClosed() throws ZenoException;
	
	/** Returns a combination of the letters  d = marked for deletion,
	 * l = locked, h = unpublished encoding the status of the resource */
	
	public String getStatus() throws ZenoException;

	/** Sets the lock of this resource.  Only an editor may set the lock. */
	// obsolete  
	public void setLock(boolean locked) throws ZenoException;
	
	/** Sets the closed attribute of the resource to true. 
	Writers must not modifiy a closed resource. */
		
	public void close() throws ZenoException;
	
	/** Sets the closed attribute of the resource to false. */
	
	public void open() throws ZenoException;
	

	/** Moves this resource to the given collection.  No need to call save()
	to make this change persistent. */

	public void move(ZenoCollection collection) throws ZenoException;

	/** Copies this resource to the given collection and returns the copy. */

	public ZenoResource copy(ZenoCollection collection, boolean withLabels)
		throws ZenoException;

	/** Gets the specified link. */

	public Link getLink(String label, int targetid) throws ZenoException;

	/** Adds a link.  Adds a link from this resource to another resource, if allowed. 
	 This change is made persistent, without having to call the save method. */

	public void addLink(
		String sourceAlias,
		String label,
		int targetId,
		String targetAlias)
		throws ZenoException;

	/** Deletes a link. This change is made persistent without having to
	call the save() method. */

	public void deleteLink(String label, int targetid) throws ZenoException;

	/** Gets the links for this resource. Each item
	 in the list is an instance of the Link class.  Directions are < 0 for links from
	another resource to this resource, 0 for all links involving this resource and > 0
	for links from this resource to another. */

	public List getLinks(int direction) throws ZenoException;

	/** Gets the link labels for this resource. Uses the direction parameter to
	select which links to include.  Directions are < 0 for links from
	another resource to this resource, 0 for all links involving this resource and > 0
	for links from this resource to another.  */

	public List getLinkLabelsUsed(int direction) throws ZenoException;

	/** Returns the links from this resource to another with the given label. Each item in the
	set is an instance of the Link class.  */

	public List getLinksWithLabel(int direction, String label)
		throws ZenoException;
		
	/** Gets the specified external link. */

	public XLink getXLink(String type, String reference) throws ZenoException;

	/** Adds an external link.  Adds a link from this resource to a external resource, if allowed. 
	 This change is made persistent, without having to call the save method. */

	public void addXLink(String type, String reference, String name)
		throws ZenoException;

	/** Deletes an external link. This change is made persistent without having to
	call the save() method. */

	public void deleteXLink(String type, String reference) throws ZenoException;

	/** Gets the external links of the specified type for this resource. 
	Each item in the list is an instance of the XLink class.  */

	public List getXLinks(String type) throws ZenoException;

	/** Gets the keys of user-defined properties */
	
	public List getPropertyKeys() throws ZenoException;

	/** Gets the user-defined property with the specified name. Returns null
	if there is no such property*/

	public String getProperty(String propertyName) throws ZenoException;

	/** Creates or updates the property with the user-defined property
	with the given name. This change is made persistent, without having
	to call the save method. */

	public void setProperty(String propertyName, String propertyValue)
		throws ZenoException;

	/** Removes the user-defined property with the specified name.*/

	public void removeProperty(String propertyName) throws ZenoException;

}