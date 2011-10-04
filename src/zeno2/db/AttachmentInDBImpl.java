package zeno2.db;

import java.io.File;
import java.io.FileInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.sql.Blob;
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

public class AttachmentInDBImpl implements Attachment {
	private int id = 0;
        private int articleId = 0;
	private String name = "";
	private String mimeType = "";
	private Blob blob = null;
	private File file = null;
	private FactoryImpl factory;
	private boolean modified = false;

        private static Logger log;
        private InputStream inputStream = null;
        private boolean attachmentInStream;;

	/** create a new attachment */
	public AttachmentInDBImpl(
			FactoryImpl factory,
			Integer articleId,
			String name,
			String mimeType,
			File file) {
	        
	        log = Logger.getLogger(AttachmentInDBImpl.class.getName());
		
		this.factory = factory;
		this.articleId = articleId.intValue();
		this.name = name;
		this.mimeType = mimeType;
		this.file = file;
		this.inputStream = null;
		this.attachmentInStream = false;
	}


        public AttachmentInDBImpl(FactoryImpl factory,
				  Integer     articleId,
				  String      name,
				  String      mimeType,
				  InputStream inputStream) {
	    
	        log = Logger.getLogger(AttachmentInDBImpl.class.getName());

		this.factory = factory;
		this.articleId = articleId.intValue();
		this.name = name;
		this.mimeType = mimeType;
		this.file = null;
		this.inputStream = inputStream;
		this.attachmentInStream = true;
	}



	/** load an existing attachment */

	//heg throw exception ?????
	public AttachmentInDBImpl(FactoryImpl factory, ResultSet rs) {
		try {

		        log = Logger.getLogger(AttachmentInDBImpl.class.getName());

			this.factory = factory;
			this.id = rs.getInt("id");
			this.articleId = rs.getInt("article");
			this.name = rs.getString("name");
			this.mimeType = rs.getString("mime_type");
			this.blob = rs.getBlob("contents");
			this.attachmentInStream = false;
		} catch (java.sql.SQLException e) {
		        log.error("SQL Exception in constructor");
			factory.reportError("Attachment.creator", e);
		}
	}

        protected boolean exists() throws ZenoException {

		  StringBuffer buf = new StringBuffer();
		  buf.append(" where article =");
		  buf.append(DBClient.format(this.articleId));
		  buf.append(" and name = ");
		  buf.append(DBClient.format(this.name));
		  int count = factory.dbclient.count("attachment", buf.toString());
		  return (count > 0);
	  }

        public void create() throws ZenoException {
	      
	          if (exists()) {
		          log.error("Name is already in use");
			  throw new ZenoException("name in use");
		  }
		  try {
			  String request =
				  "insert into attachment (article, name, mime_type, contents)"
					  + " values(?, ?, ?, ?)";
			  Connection con = factory.dbclient.getConnection();
			  PreparedStatement pdstm = con.prepareStatement(request);
			  pdstm.setInt(1, this.articleId);
			  pdstm.setString(2, this.name);
			  pdstm.setString(3, this.mimeType);
			  
			  if (attachmentInStream && inputStream != null) {
			      ByteArrayOutputStream aos = new ByteArrayOutputStream();
			      int c = 0;
			      while (inputStream.read() != -1)
				  c++;
			      
			      inputStream.reset();
			      //pdstm.setBytes(4, aos.toByteArray());
			      pdstm.setBinaryStream(4, inputStream, c);
			  } else if (file != null && !attachmentInStream) {
				  int size = (int) file.length();
				  FileInputStream fin = new FileInputStream(file);
				  pdstm.setBinaryStream(4, fin, size);
			  } else {
				  pdstm.setBytes(4, new byte[0]);
			  }
			  pdstm.executeUpdate();
			  this.file = null;
			  this.inputStream = null;

			  request = "select last_insert_id() from attachment ";
			  ResultSet rs = factory.dbclient.executeQuery(request);
			  if (rs.next()) {
				  this.id = rs.getInt(1);
			  }
		  } catch (java.sql.SQLException e) {
		          log.error("SQL Exception while creating attachment");
			  factory.reportError("Attachment.create", e);
			  throw new ZenoException("DB Error");
		  } catch (java.io.FileNotFoundException e) {
		          log.error("IO Exception while creationg attachment");
			  factory.reportError("Attachment.create", e);
			  throw new ZenoException("NoSuchFile " + file.getName());
		  } catch (java.io.IOException e) {
		          log.error("IO Exception while reading from stream");
			  factory.reportError("Attachment.create", e);
			  throw new ZenoException("Exception while reading from stream");
		  }
	  }

	  private Blob getBlob() throws ZenoException {
		  if (blob != null)
			  return blob;
		  try {
			  StringBuffer buf = new StringBuffer();
			  buf.append("select contents from attachment ");
			  buf.append(" where article = ");
			  buf.append(DBClient.format(this.articleId));
			  buf.append(" and id = ");
			  buf.append(DBClient.format(this.id));

			  ResultSet rs = factory.dbclient.executeQuery(buf.toString());
			  if (rs.next())
				  this.blob = rs.getBlob("contents");
			  else {
			          log.error("Missing blob");
				  factory.reportError("Attachment.getBlob missing blob");
				  throw new ZenoException("DatabaseException");
			  }
			  return blob;
		  } catch (java.sql.SQLException e) {
		          log.error("SQL Exception in getBlob");
			  factory.reportError("Attachment.getBlob", e);
			  throw new ZenoException("DatabaseException");
		  }
	  }

	  public int getId() {
		  return id;
	  }

	  public int getArticleId() {
		  return articleId;
	  }

	  public String getName() throws ZenoException {
		  factory.checkPermission("Attachment.getName", this);
		  return name;
	  }

	  public void setName(String name) throws ZenoException {
		  factory.checkPermission("Attachment.setName", this);
		  this.name = name;
		  this.modified = true;
	  }

	  public long size() throws ZenoException {
		  factory.checkPermission("Attachment.size", this);
		  try {
			  if (blob == null) {
				  getBlob();
			  }
			  return blob.length();
		  } catch (java.sql.SQLException e) {
		          log.error("SQL Exception in size");
			  factory.reportError("Attachment.size", e);
			  throw new ZenoException("DatabaseException");
		  }
	  }

	  public String getMimeType() throws ZenoException {
		  factory.checkPermission("Attachment.getMimeType", this);
		  return this.mimeType;
	  }

	  public void setMimeType(String mimeType) throws ZenoException {
		  factory.checkPermission("Attachment.setMimeType", this);
		  this.mimeType = mimeType;
		  this.modified = true;
	  }

	  public InputStream getContents() throws ZenoException {
		  factory.checkPermission("Attachment.getContents", this);
		  try {
			  if (blob == null) {
				  getBlob();
			  }
			  return blob.getBinaryStream();
		   } catch (java.sql.SQLException e) {
		          log.error("SQL Exception in getContents");
			  factory.reportError("Attachment.getContents", e);
			  throw new ZenoException("DataBaseException");
		  }
	  }

	  public void setContents(File contents) throws ZenoException {
		  factory.checkPermission("Attachment.setContents", this);
		  this.file = contents;
		  this.modified = true;

	}

	public void setContents(byte[] contents) throws ZenoException {
		//executed immediately
		try {
			factory.checkPermission("Attachment.setContents", this);
			String request =
				"update attachment set contents = ? , mime_type = ?"
					+ " where article = ? and id = ?";
			Connection con = factory.dbclient.getConnection();
			PreparedStatement pdstm = con.prepareStatement(request);
			pdstm.setBytes(1, contents);
			pdstm.setString(2, this.mimeType);
			pdstm.setInt(3, this.articleId);
			pdstm.setInt(4, this.id);
			pdstm.executeUpdate();
			this.blob = null;
		} catch (java.sql.SQLException e) {
		        log.error("SQL Exception in setContents");
			factory.reportError("Attachment.setContents", e);
			throw new ZenoException("DB Error");
		}
	}
	
	public void save() throws ZenoException {
		// permission already checked by set methods
		if (modified) {
			try {
				if (this.file != null) {
					String request =
						"update attachment set name = ? , contents = ? , mime_type = ?"
							+ " where article = ? and id = ?";
					Connection con = factory.dbclient.getConnection();
					PreparedStatement pdstm = con.prepareStatement(request);
					int size = (int) file.length();
					FileInputStream fin = new FileInputStream(file);
					pdstm.setString(1, this.name);
					pdstm.setBinaryStream(2, fin, size);
					pdstm.setString(3, this.mimeType);
					pdstm.setInt(4, this.articleId);
					pdstm.setInt(5, this.id);
					pdstm.executeUpdate();
					this.blob = null;
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
			        log.error("SQL Exception in save");
				factory.reportError("Attachment.save", e);
				throw new ZenoException("DataBaseException");
			} catch (java.io.FileNotFoundException e) {
			        log.error("File Not Found Exception in create");
				factory.reportError("Attachment.create", e);
				throw new NotFoundException("NoSuchFile " + file.getName());
			}
		}
	}

	
        public static void copyAttachments(
		FactoryImpl factory,
		List attachments,
		Hashtable newids)
		throws ZenoException {

		String request =
			"insert into attachment (article, name, mime_type, contents)"
				+ " values(?, ?, ?, ?)";
		PreparedStatement pdstm;
		try {
			Connection con = factory.dbclient.getConnection();
			pdstm = con.prepareStatement(request);
		} catch (java.sql.SQLException e) {
		        log.error("SQL Exception in copyAttachments");
			factory.reportError("Attachment.copyAttachments", e);
			throw new ZenoException("DB error");
		}

		Iterator it = attachments.iterator();
		while (it.hasNext()) {
		        AttachmentInDBImpl atch = (AttachmentInDBImpl) it.next();
		        Integer oldArticle = new Integer(atch.getArticleId());
			Integer newArticle = (Integer) newids.get(oldArticle);
			if (newArticle != null) {
				try {
					pdstm.clearParameters();
					pdstm.setInt(1, newArticle.intValue());
					pdstm.setString(2, atch.getName());
					pdstm.setString(3, atch.getMimeType());
					pdstm.setBlob(4, atch.getBlob());

					pdstm.executeUpdate();
				} catch (java.sql.SQLException e) {
					// transfer as far as possible
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
			
		} catch (java.sql.SQLException e) {
		        //to do: rollback
		        log.error("SQL Exception in deleteAttachment");
		        factory.reportError("AttachmentInDBImpl.deleteAttachment", e);
			throw new ZenoException("DatabaseException");
		}
		
	};


        public static void deleteAttachments(FactoryImpl factory,
					     Integer     articleId) throws ZenoException {

	        try {
		        StringBuffer delbuf = new StringBuffer();
			delbuf.append("DELETE FROM attachment WHERE article = ");
			delbuf.append(DBClient.format(articleId.intValue()));
			
			factory.dbclient.executeUpdate(delbuf.toString());
	    
		} catch (java.sql.SQLException e) {
		        log.error("SQL Exception in deleteAttachments");
		        factory.reportError("AttachmentInDBImpl.deleteAttachments", e);
			throw new ZenoException("DatabaseException");

		}
	};

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

}
