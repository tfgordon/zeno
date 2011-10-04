package zeno2.db;

import org.apache.log4j.Logger;

import java.io.PrintWriter;

import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.Iterator;
import java.lang.StringBuffer;


import zeno2.james.util.ZenoMailAttachment;
import zeno2.kernel.Monitor;
import zeno2.kernel.Factory;
import zeno2.kernel.Journal;
import zeno2.kernel.Article;
import zeno2.kernel.ZenoException;

import java.io.File;
import java.util.jar.JarFile;
import java.util.jar.JarEntry;

import java.io.IOException;

public class ZenoMail {

    private Logger   log;
    private Factory  factory;
    private Monitor  monitor;
    private boolean  zenoMailConfigured;
    private DBClient dbClient;
    private PrintWriter prw;

    private final static String MODERATOR_QUALIFIER = "By Mail";

    public final static int PERMISSION_DENIED_IGNORE_EMAIL = 0;
    public final static int PERMISSION_DENIED_INFORM_SENDER = 1;
    public final static int PERMISSION_GRANTED = 2;

    private final static String ZENO_WAR = "zeno.war";
    private final static String ZENO_PROPERTIES = "WEB-INF/conf/zeno.properties";
    private static String ZENO_WAR_BASE = "";

    public ZenoMail() {

	log = Logger.getLogger(ZenoMail.class.getName());

	prw = new PrintWriter(System.out, true);
	zenoMailConfigured = false;

    }


    public static void initialize(String zenowar) {
	
	ZENO_WAR_BASE = zenowar;
    }


    public void login() {
	
	zenoMailConfigured = false;

	try {
	    String mailUser;
	    String mailPassword;
	    File   zenoPropertiesFile;
	    monitor = new MonitorImpl();
	    monitor.setErrorWriter(prw);
	    
	    zenoPropertiesFile = new File(ZENO_WAR_BASE, ZENO_PROPERTIES);
	    
	    if (zenoPropertiesFile.exists()) {
		System.out.println("Login: using zeno.properties in filesystem");
		monitor.configure(zenoPropertiesFile.getAbsolutePath());

	    } else {
		System.out.println("Login: using zeno.properties in zeno.war");
		zenoPropertiesFile = new File(ZENO_WAR_BASE, ZENO_WAR);

		if (zenoPropertiesFile.exists()) {
		    JarFile zenowar = new JarFile(zenoPropertiesFile);
		    JarEntry zenoprop = zenowar.getJarEntry(ZENO_PROPERTIES);
		    
		    monitor.configure(zenowar.getInputStream(zenoprop));

		} else {
		    System.out.println("Error: can't find zeno.properties");
		    log.error("can't find zeno.properties.");

		}
	    }

	    mailUser = monitor.getProperty("zenoMailUser", "mailer");
	    mailPassword = monitor.getProperty("zenoMailPassword", "password");

	    factory = monitor.login(mailUser, mailPassword);
	    dbClient = ((FactoryImpl) factory).getDBClient();
	    zenoMailConfigured = true;
	    
	} catch (ZenoException e) {
	    log.error("can't login to ZENO");

	} catch (IOException e) {
	    log.error("IOException");

	}

    }


    public void logout() {

	zenoMailConfigured = false;
	dbClient = null;
	factory = null;
	monitor = null;
    }


    public boolean zenoMailIsConfigured() {
	
	return zenoMailConfigured;
    }


    public Factory getFactory() {

	return factory;
    }


    public int checkUserPermission(String user, String journalAlias, StringBuffer userID) {
	
	int returnValue = PERMISSION_DENIED_IGNORE_EMAIL;

	try {
	    
	    ResultSet rs = getUserIdentities(user);

	    if (null != rs && rs.next()) {
		Journal journal = getJournal(journalAlias);
		returnValue = PERMISSION_DENIED_INFORM_SENDER;

		if (null != journal) {
		    do {
			if (journal.hasRole(rs.getString(1), "editor") ||
			    journal.hasRole(rs.getString(1), "writer")) {
			    
			    userID.append(rs.getString(1));
			    returnValue = PERMISSION_GRANTED;
			    break;
			}
			    
		    } while (rs.next());

		}

	    }

	} catch (Exception e) {
	    e.printStackTrace();

	}

	return returnValue;
    }


    public void createArticle(String journalAlias,
			      String userID, 
			      String subject, 
			      String note, 
			      ArrayList attachments) {

	Journal journal = getJournal(journalAlias);

	if (null != journal) {
	    try {
		/*
		 * Due to a bug in Permission-Handling
		 * we create topics rather than articles for now.
		 *
		 */
		Article article = factory.createArticle(journal);
		//Article article = factory.createTopic(journal);
		
		article.setAuthor(userID);
		article.setTitle(subject);
		article.setNote(note);
		
		article.save();

		for (Iterator i = attachments.iterator(); i.hasNext(); ) {
		    ZenoMailAttachment mailAttachment = (ZenoMailAttachment) i.next();
		    article.addAttachment(mailAttachment.getFilename(), mailAttachment.getContentType(), mailAttachment.getInputStream());
		}
		
		try {
		    article.setQualifier(MODERATOR_QUALIFIER);
		    
		} catch (ZenoException e) {
		    e.printStackTrace();
		}
		
		article.save();
		
	    } catch (Exception e) {
		e.printStackTrace();
		
	    }
	    
	}   

    }


    private Journal getJournal(String alias) {

		Journal journal = null;
		String journalID = getJournalID(alias);
		int jid = Integer.valueOf(journalID).intValue();

		try {
	    	if (null != journalID) {
				//journal =  factory.getJournalByID(Integer.valueOf(journalID).intValue());
				journal = (Journal)factory.loadResource(jid);
	    	}
		} catch (ZenoException e) {
	    	e.printStackTrace();
		}
		return journal;
    }

	    
    public String getJournalID(String alias) {
	
	String zenoAlias = null;

	try {

	    ResultSet rs = dbClient.executeQuery("SELECT resource FROM property WHERE name = 'MailAlias' and lower(value) = '" + alias + "';");

	    if (rs.next()) {
		zenoAlias = rs.getString(1);

	    }

	} catch (SQLException e) {
	    log.error("Exception while checking for alias in database");

	}

	return zenoAlias;
    }


    private ResultSet getUserIdentities(String user) {
	ResultSet rs = null;

	try {
	    rs = dbClient.executeQuery("SELECT id FROM principal WHERE lower(email) = '" + user + "';");

	} catch (SQLException e) {
	    e.printStackTrace();

	}

	return rs;
    }
	    
    public boolean isZenoUser(String mailAddress) {

	boolean returnValue = false;
	ResultSet rs = getUserIdentities(mailAddress);

	try {
	    if (null != rs && rs.next()) {
		return true;
	    }
	} catch (SQLException e) {
	    e.printStackTrace();
	}

	return returnValue;
    }
    
    
    public boolean isZenoAlias(String alias) {
	
	if (getJournalID(alias) == null) {
	    return false;
	    
	} else {
	    return true;
	    
	}
    }
}
