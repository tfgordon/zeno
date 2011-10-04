package zeno2.db;

import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import zeno2.kernel.Attachment;
import zeno2.kernel.NotFoundException;
import zeno2.kernel.ZenoException;
import zeno2.db.ArticleImpl;

public class AttachmentOnDiskImpl implements Attachment {

    //
    // some constants for convenience
    //
    final private static String ZENO_ATTACHMENT_BASE_PROPERTYNAME = "zenoAttachmentBase";
    final private static String ZENO_ATTACHMENT_BASE_DEFAULT  = ".";
    
    int         id        = 0;
    int         articleId = 0;
    String      name      = "";
    String      mimeType  = "";
    File        file      = null;
    FactoryImpl factory;
    boolean     modified  = false;

    private boolean     attachmentInStream;
    private InputStream inputStream       = null;
    private String      baseDirectory    = "";
    private File        attachment       = null;

    private static Logger log;

    /** create a new attachment */
    public AttachmentOnDiskImpl(FactoryImpl factory,
				Integer articleId,
				String name,
				String mimeType,
				File file) {

	log = Logger.getLogger(AttachmentOnDiskImpl.class.getName());
	
	this.factory = factory;
	this.baseDirectory = getBaseDirectory(factory);
	this.articleId = articleId.intValue();
	this.name = name;
	this.mimeType = mimeType;
	this.file = file;
	this.inputStream = null;
	this.attachmentInStream = false;

    }


    public AttachmentOnDiskImpl(FactoryImpl factory,
				Integer     articleId,
				String      name,
				String      mimeType,
				InputStream inputStream) {

	log = Logger.getLogger(AttachmentOnDiskImpl.class.getName());
	
	this.factory = factory;
	this.baseDirectory = getBaseDirectory(factory);
	this.articleId = articleId.intValue();
	this.name = name;
	this.mimeType = mimeType;
	this.file = null;
	this.inputStream = inputStream;
	this.attachmentInStream = true;
    }


    /** load an existing attachment */
    
    //heg throw exception ?????
    public AttachmentOnDiskImpl(FactoryImpl factory, ResultSet rs) {
	try {

	    log = Logger.getLogger(AttachmentOnDiskImpl.class.getName());

	    this.factory = factory;
	    this.baseDirectory = getBaseDirectory(factory);
	    this.id = rs.getInt("id");
	    this.articleId = rs.getInt("article");
	    this.name = rs.getString("name");
	    this.mimeType = rs.getString("mime_type");
	    this.attachment = new File(baseDirectory, String.valueOf(this.id));
	    this.attachmentInStream = false;

	} catch (java.sql.SQLException e) {
	    log.error("SQL Exception in constructor");
	    factory.reportError("Attachment.creator", e);
	}
    }


    /**
     * Returns the base directory for attachments.
     * 
     * @return the value of the Property <code>zenoAttachmentBase</code>. Default is 
     * the current working directory.
     */
    private static String getBaseDirectory(FactoryImpl factory) {
	
	return(factory.getMonitor().getProperty(ZENO_ATTACHMENT_BASE_PROPERTYNAME, 
						ZENO_ATTACHMENT_BASE_DEFAULT));
    }
    
    
    /**
     * Tests whether the entry exists in the database
     * @return true if file (attachment) already exists, false otherwise
     *
     */
    protected boolean exists() throws ZenoException {

	StringBuffer buf = new StringBuffer();
	buf.append(" where article =");
	buf.append(DBClient.format(this.articleId));
	buf.append(" and name = ");
	buf.append(DBClient.format(this.name));
	int count = factory.dbclient.count("attachment", buf.toString());
	return (count > 0);
    }

    /*
    protected boolean exists() throws ZenoException {
	boolean bReturnValue;
	
	if (attachment != null)
	    bReturnValue = attachment.exists();
	else
	    bReturnValue = false;
	
	return bReturnValue;
    }
    */    


    /**
     * Creates a new entry in the <code>attachment-table</code>.
     * The id of this new entry is taken as the filename for the attachment.
     *
     * @throws ZenoException
     */
    public void create() throws ZenoException {
	
	//
	// check wheter the attachment already exists.
	//
	if (exists()) {
	    log.warn("This article has already an attachment with the name " + this.name);
	    throw new ZenoException("name in use");
	}
	
	try {

	    //
	    // Preprare the SQL Statement
	    // 
	    String request =
		"insert into attachment (article, name, mime_type)"
		+ " values(?, ?, ?)";
	    Connection con = factory.dbclient.getConnection();
	    PreparedStatement pdstm = con.prepareStatement(request);
	    pdstm.setInt(1, this.articleId);
	    pdstm.setString(2, this.name);
	    pdstm.setString(3, this.mimeType);
	    
	    //
	    // and insert a new row into the attachment-table
	    //
	    pdstm.executeUpdate();

	    //
	    // retrieve the id of the new row, which will be 
	    // used as the filename for the attachment
	    //
	    request = "select last_insert_id() from attachment ";
	    ResultSet rs = factory.dbclient.executeQuery(request);

	    //
	    // if the resultset is not empty
	    //
	    if (rs.next()) {

		// 
		// grab the id
		//
		this.id = rs.getInt(1);

		attachment = new File(this.baseDirectory, String.valueOf(this.id));

		if (attachmentInStream) {
		    if (this.inputStream != null)
			copyFile(inputStream, attachment);

		} else {
		    if (this.file != null)
			copyFile(file, attachment);
		    
		}
		this.file = null;
		this.inputStream = null;

	    } else {
		
		//
		// otherwise throw an exception
		//
		log.error("can't get id of the new attachment.");
		throw new ZenoException("can't get attachment id");
	    }

	    
	} catch (java.sql.SQLException e) {
	    
	    log.error("SQL Exception while creating an attachment.");
	    factory.reportError("Attachment.create", e);
	    throw new ZenoException("DB Error");
	}
    }
    
    
    /**
     * Returns the File object, which represents the attachment
     *
     * @return an instance of class <code>File</code>
     * @trows  ZenoException, if there is no attachment
     */
    private File getAttachment() throws ZenoException {
	
	//
	// if attachment is already initialized, return it
	//
	if (this.attachment != null)
	    return this.attachment;
	
	//
	// check if id equals 0 (which means no attachment created so far)
	//
	if (this.id == 0) {
	    log.error("There is no attachment.");
	    throw new ZenoException("no attachment created");

	//
	// otherwise instantiate a new File object, and return it
	// 
	} else
	    return (new File(baseDirectory, String.valueOf(this.id)));
	
    }
    

    /**
     * Returns the id of this attachment
     *
     * @return id
     */
    public int getId() {

	return id;
    }
    
    
    /*
     * Returns the id of the associated article
     *
     * @return articleId
     */
    public int getArticleId() {
	
	return articleId;
    }

    
    /*
     * Returns the name of this attachment
     *
     * @return name
     */
    public String getName() throws ZenoException {
	
	factory.checkPermission("Attachment.getName", this);
	return name;
    }

    
    /**
     * Sets the name of this attachment. 
     * 
     * @param the new name of the attachment
     */
    public void setName(String name) throws ZenoException {
	
	factory.checkPermission("Attachment.setName", this);
	this.name = name;
	this.modified = true;
    }

    
    /**
     * Returns the size of the attachment in bytes.
     *
     * @return size
     */
    public long size() throws ZenoException {

	factory.checkPermission("Attachment.size", this);
	
	if (this.attachment != null)
	    return attachment.length();

	else
	    return 0;

    }

    
    /**
     * Returns the mime-type of the attachment
     *
     * @return mime-type
     */
    public String getMimeType() throws ZenoException {

	factory.checkPermission("Attachment.getMimeType", this);
	return this.mimeType;
    }
    
    
    /**
     * Sets the mime-type for the attachment
     *
     * @param mimeType
     */
    public void setMimeType(String mimeType) throws ZenoException {

	factory.checkPermission("Attachment.setMimeType", this);
	this.mimeType = mimeType;
	this.modified = true;
    }

    
    
    /**
     * Returns an InputStream object (contents of the attachment) to
     * the caller
     *
     * @return InputStream
     */
    public InputStream getContents() throws ZenoException {

	factory.checkPermission("Attachment.getContents", this);

	try {

	    if (attachment == null) {
		log.error("No attachment associated with this article");
		throw new ZenoException("no attachment assigned so far");

	    } else {
		return (new FileInputStream(attachment));
		
	    }
	    
	} catch (java.io.FileNotFoundException e) {
	    
	    log.error("can't access attachment " + attachment.getAbsolutePath() + ". File not found.");
	    factory.reportError("Attachment.getContents", e);
	    throw new ZenoException("FileNotFoundException");
	    
	}
    }
    

    /**
     * Sets the contents of the attachment
     *
     * @param contents File
     */
    public void setContents(File contents) throws ZenoException {

	factory.checkPermission("Attachment.setContents", this);
	this.file = contents;
	this.modified = true;
	
    }
    

    /**
     * Sets the contents of the attachment
     *
     * @param contents byte[]
     */
    public void setContents(byte[] contents) throws ZenoException {
	//executed immediately

	try {

	    factory.checkPermission("Attachment.setContents", this);
	    
	    //
	    // check whether the attachment is already on disk
	    // if so, remove it
	    //
	    if (attachment.exists())
		attachment.delete();
	    
	    //
	    // open a new outputstream
	    //
	    FileOutputStream fout = new FileOutputStream(attachment);
	    
	    //
            // and write the file
	    //
	    fout.write(contents);
	    fout.close();

	} catch (java.io.IOException e) {
	    log.error("can't access attachment " + attachment.getAbsolutePath() + ". IO Exception");
	    factory.reportError("Attachment.setContents", e);
	    throw new ZenoException("IOException");
	    
	}
    }
    

    
    /**
     * Saves/Updates all informations about the attachment
     * into the database / disk
     */
    public void save() throws ZenoException {
	// permission already checked by set methods
		if (modified) {
			try {
				if (this.file != null) {
			   	String request =
						"update attachment set name = ? , mime_type = ?"
						+ " where article = ? and id = ?";
					Connection con = factory.dbclient.getConnection();
					PreparedStatement pdstm = con.prepareStatement(request);
					int size = (int) file.length();
					FileInputStream fin = new FileInputStream(file);
					pdstm.setString(1, this.name);
					pdstm.setString(2, this.mimeType);
					pdstm.setInt(3, this.articleId);
					pdstm.setInt(4, this.id);
					pdstm.executeUpdate();
					copyFile(file, attachment);
					this.file = null;
					this.modified = false;
			    
				} else {
			    // no need to update the content
					StringBuffer buf = new StringBuffer();
					buf.append("update attachment");
					buf.append(" set name=");
					buf.append(DBClient.format(this.name));
					buf.append(", mime_type=");
					buf.append(DBClient.format(this.mimeType));
					buf.append(" where article=");
					buf.append(DBClient.format(this.articleId));
					buf.append(" and id=");
					buf.append(DBClient.format(this.id));
					Connection con = factory.dbclient.getConnection();
					Statement stmt = con.createStatement();
					stmt.executeUpdate(buf.toString());
					this.modified = false;
				}
				ArticleImpl art = (ArticleImpl)factory.loadResource(this.articleId);
				art.modified();
			} catch (java.sql.SQLException e) {
				log.error("SQL Exception while updating the database");
				factory.reportError("Attachment.save", e);
				throw new ZenoException("DataBaseException");

			} catch (java.io.FileNotFoundException e) {
				log.error("can't find specified file " + file.getAbsolutePath());
				factory.reportError("Attachment.create", e);
				throw new NotFoundException("NoSuchFile " + file.getName());
			}
		}
	}

    /**
     * Copies/assigns the attachments of an article to another article
     *
     * @param factory FactoryImpl
     * @param attachments List
     * @param newids Hashtable
     */
    public static void copyAttachments(
	FactoryImpl factory,
	List attachments,
	Hashtable newids)
	throws ZenoException {
	
	String request =
	    "insert into attachment (article, name, mime_type)"
	    + " values(?, ?, ?)";
	PreparedStatement pdstm;
	try {
	    Connection con = factory.dbclient.getConnection();
	    pdstm = con.prepareStatement(request);

	} catch (java.sql.SQLException e) {
	    log.error("SQL Exception while copying attachments");
	    factory.reportError("Attachment.copyAttachments", e);
	    throw new ZenoException("DB error");

	}
	
	Iterator it = attachments.iterator();
	//
	// Iterate through all attachments
	//
	while (it.hasNext()) {
	    AttachmentOnDiskImpl atch = (AttachmentOnDiskImpl) it.next();
	    Integer oldArticle = new Integer(atch.articleId);
	    Integer newArticle = (Integer) newids.get(oldArticle);
	    if (newArticle != null) {
		try {
		    //
		    // Insert a new row into the attachment table
		    // 
		    pdstm.clearParameters();
		    pdstm.setInt(1, newArticle.intValue());
		    pdstm.setString(2, atch.name);
		    pdstm.setString(3, atch.mimeType);
		    
		    pdstm.executeUpdate();
		    
		    request = "select last_insert_id() from attachment ";
		    ResultSet rs = factory.dbclient.executeQuery(request);

		    //
		    // and determine id of the new row
		    //
		    if (rs.next()) {

			int newId = rs.getInt(1);
			//
			// create a new File object with the id as Filename
			//
			File newAttachment = new File(getBaseDirectory(factory), String.valueOf(newId));
			
			//
			// and copy the attachment
			//
			atch.copyFile(atch.getAttachment(), newAttachment);

		    } else {
			log.error("Insert of a new attachment failed");
			throw new ZenoException("copyAttachments: Insert of new attachment failed.");

		    }
		    
		} catch (java.sql.SQLException e) {
		    // transfer as far as possible
		    log.error("SQL Exception while copying attachments");
		    factory.reportError("Attachment.copyAttachments", e);

		}

	    }

	}

    }


    public static void deleteAttachment(FactoryImpl factory,
					Integer     articleId,
					Integer     attachmentId) throws ZenoException {


	try {
	    StringBuffer buf = new StringBuffer();
	    buf.append("delete from attachment where article=");
	    buf.append(DBClient.format(articleId.intValue()));
	    buf.append(" and id =");
	    buf.append(DBClient.format(attachmentId.intValue()));
	    
	    // to do: start transaction
	    factory.dbclient.executeUpdate(buf.toString());

	    File atch = new File(getBaseDirectory(factory), String.valueOf(attachmentId));

	    if (atch.exists())
		atch.delete();

	} catch (java.sql.SQLException e) {
	    //to do: rollback
	    log.error("SQL Exception while removing attachment from database");
	    factory.reportError("AttachmentOnDiskImpl.deleteAttachment", e);
	    throw new ZenoException("DatabaseException");
	}

    };


    public static void deleteAttachments(FactoryImpl factory,
					 Integer     articleId) throws ZenoException {

	try {
	    StringBuffer selbuf = new StringBuffer();
	    selbuf.append("SELECT id FROM attachment WHERE article = ");
	    selbuf.append(DBClient.format(articleId.intValue()));
	    
	    ResultSet rs = factory.dbclient.executeQuery(selbuf.toString());
	    
	    while (rs.next()) {
		
		File atch = new File(getBaseDirectory(factory), String.valueOf(rs.getInt(1)));
		
		if (atch.exists())
		    atch.delete();
	    }
	    
	    StringBuffer delbuf = new StringBuffer();
	    delbuf.append("DELETE FROM attachment WHERE article = ");
	    delbuf.append(DBClient.format(articleId.intValue()));
	    
	    factory.dbclient.executeUpdate(delbuf.toString());
	    
	} catch (java.sql.SQLException e) {
	    log.error("SQL Exception while removing the attachments of article with id " + String.valueOf(articleId));
	    factory.reportError("AttachmentOnDiskImpl.deleteAttachments", e);
	    throw new ZenoException("DatabaseException");
	}
    };


    
    /**
     * encloses the specified string with quotes
     *
     * @param s String
     * @returns the quoted string
     */
    protected String quote(String s) {
	return "\"" + s + "\"";
    }

    
    public void show(PrintWriter prw) throws ZenoException {
	if (prw == null)
	    prw = new PrintWriter(System.out, true);
	prw.println("<attachment");
	prw.println("    id= " + id);
	prw.println("    name=" + quote(name));
	prw.println("    mimeType=" + quote(mimeType));
	prw.println(">");
    }


    /**
     * Copies the <code>File</code> src to dest
     *
     * @param src File
     * @param dest File
     * @returns true on success, false otherwise
     */
    private boolean copyFile(File src, File dest) throws ZenoException {
	boolean bSuccess = false;
	
	//
	// test whether the source file is null
	//
	if (src == null) {
	    //factory.reportError("Attachment.copyFile", e);
	    throw new ZenoException("NullPointerException for source");

	// the same for the destination file
	} else if (dest == null) {
	    //factory.reportError("Attachment.copyFile", e);
	    throw new ZenoException("NullPointerException for destination");

	} else {
	    
	    //
	    // if the desitination already exists, delete it
	    //
	    if (dest.exists()) 
		dest.delete();

	    try {
		//
		// Open the source for reading, and the destination for 
		// writing
		//
		FileInputStream fin = new FileInputStream(src);
		FileOutputStream fout = new FileOutputStream(dest);
		
		int c;
		
		//
		// and write the contents of the source file into the
		// destination
		//
		while((c = fin.read()) != -1)
		    fout.write(c);
		
		fin.close();
		fout.close();
		
		bSuccess = true;

	    } catch (java.io.FileNotFoundException e) {
		log.error("can't find file " + src.getAbsolutePath());
		factory.reportError("Attachment.copyFile", e);
		throw new ZenoException("NoSuchFile " + src.getName());
		
	    } catch (java.io.IOException e) {
		log.error("can't access " + src.getAbsolutePath() + " and/or " + dest.getAbsolutePath() + ". IO Exception");
		factory.reportError("Attachment.copyFile", e);
		throw new ZenoException("NoSuchFile " + dest.getName());
		
	    }
	}
	
	return bSuccess;
    }


    /**
     * Copies the <code>File</code> src to dest
     *
     * @param src InputStream
     * @param dest File
     * @returns true on success, false otherwise
     */
    private boolean copyFile(InputStream fin, File dest) throws ZenoException {
        boolean bSuccess = false;

        //
        // test whether the source file is null
        //
        if (fin == null) {
            //factory.reportError("Attachment.copyFile", e);
            throw new ZenoException("NullPointerException for source");

	    // the same for the destination file
        } else if (dest == null) {
            //factory.reportError("Attachment.copyFile", e);
            throw new ZenoException("NullPointerException for destination");

        } else {

            //
            // if the desitination already exists, delete it
            //
            if (dest.exists())
                dest.delete();

            try {
                //
                // Open the source for reading, and the destination for
                // writing
                //
                FileOutputStream fout = new FileOutputStream(dest);

                int c;

                //
                // and write the contents of the source file into the
                // destination
                //
                while((c = fin.read()) != -1)
                    fout.write(c);

                fout.close();

                bSuccess = true;

		/*} catch (java.io.FileNotFoundException e) {
                log.error("can't find file " + src.getAbsolutePath());
                factory.reportError("Attachment.copyFile", e);
                throw new ZenoException("NoSuchFile " + src.getName());
	     */
            } catch (java.io.IOException e) {
                log.error("can't access " + dest.getAbsolutePath() + ". IO Exception");
                factory.reportError("Attachment.copyFile", e);
                throw new ZenoException("NoSuchFile " + dest.getName());

            }
        }

        return bSuccess;
    }

}
