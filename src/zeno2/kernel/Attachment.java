package zeno2.kernel;

import java.io.File;
import java.io.InputStream;

/**
 * Zeno 2 Attachments.
 */

public interface Attachment {
	
	/** Gets the id of this attachment. */

	public int getId();

	/** Gets the id of the article of this attachment. */

	public int getArticleId();

	/** Gets the name of the attachment. */

	public String getName() throws ZenoException;
	
	/** Sets the name of the attachment. */

	public void setName(String name) throws ZenoException;

	/** Computes the size of the attachment, in bytes. */

	public long size() throws ZenoException;

	/** Gets the MIME type of the attachment. */

	public String getMimeType() throws ZenoException;

	/** Sets the MIME type of the attachment. */

	public void setMimeType(String mimeType) throws ZenoException;

	/** Returns the contents of the attachment. */

	public InputStream getContents() throws ZenoException;

	/** Sets the content of attachment. */

	public void setContents(File contents) throws ZenoException;

	/** Saves changes to the persistent store.  Raises an exception if
	the user does not have permission or the transaction could not be completed. */

	public void save() throws ZenoException;

	public void create() throws ZenoException;

	public void setContents(byte[] contents) throws ZenoException;


}
