/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package zeno2.james.transport.mailets;

import org.apache.mailet.GenericMailet;
import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;
import org.apache.mailet.MailetException;

import java.util.Collection;
import java.util.Iterator;
import java.util.Vector;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import zeno2.db.ZenoMail;
import zeno2.james.util.ZenoMailAttachment;
import zeno2.james.util.ZenoMailReply;

import java.lang.StringBuffer;

import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.MessagingException;

import javax.mail.internet.MimeMessage;
import javax.mail.internet.InternetAddress;


/**
 * Receive  a Mail from JamesSpoolManager and takes care of delivery
 * the message to local inboxes.
 *
 * @version 1.0.0, 07/11/2002
 * @author Ahmet Ocakli <ahmet.ocakli@ais.fgh.de>
 */
public class ZenoDelivery extends GenericMailet {

    private static Logger log   = null;
    private int           level = 0;

    private ZenoMail      zenoMail;    
    private boolean       mailetIsConfigured;


    private class MailContent {
	private boolean   _hasHtmlPart;
	private String    _htmlContent;
	private String    _plainContent;
	private ArrayList _attachments;


	public MailContent() {
	    
	    _hasHtmlPart = false;
	    _htmlContent = "";
	    _plainContent = "";
	    _attachments = new ArrayList();

	}


	public void addPlainContent(String content) {
	    
	    _plainContent += content;
	}


	public void addHtmlContent(String content) {
	    
	    _hasHtmlPart = true;
	    _htmlContent+= content;
	}


	public void addAttachment(Part attachment) {

	    ZenoMailAttachment ma = new ZenoMailAttachment(attachment);
	    String filename = ma.getFilename();

	    if (!nameIsUnique(filename)) {
		int i;
		for (i = 0; !nameIsUnique(filename + "_Copy_" + i) ;i++);
		ma.setFilename(filename + "_Copy_" + i);
	    }

	    _attachments.add(ma);
	}


	public ArrayList getAttachments() {
	    
	    return _attachments;
	}


	public String getContent() {
	    
	    if (_plainContent.equals(""))
		return _htmlContent;
	    else
		return _plainContent;
	}


	private boolean nameIsUnique(String name) {
	    boolean returnValue = true;

	    for (Iterator i = _attachments.iterator(); i.hasNext(); ) {
		if (((ZenoMailAttachment) i.next()).getFilename().equals(name)) {
		    returnValue = false;
		    break;
		}
	    }

	    return returnValue;
	}

    }


    public ZenoDelivery() {

        log = Logger.getLogger(ZenoDelivery.class.getName());
	zenoMail = new ZenoMail();
    };


    public void service(Mail mail) throws MessagingException {

	String  journalAlias;
	boolean mailAlreadyProcessed;
        Collection errors = new Vector();
	MailContent mailContent = new MailContent();

	String sender = mail.getSender().toString().toLowerCase();
	StringBuffer userID = new StringBuffer();
	StringBuffer errorBuffer = new StringBuffer();

	mailAlreadyProcessed = false;

	zenoMail.login();

	for (Iterator recipients = mail.getRecipients().iterator(); recipients.hasNext(); ) {
	    MailAddress recipient = (MailAddress) recipients.next();


	    journalAlias = recipient.getUser().toLowerCase();

	    userID.delete(0, userID.length());

	    int foobar = zenoMail.checkUserPermission(sender, journalAlias, userID);
	    
	    if (zenoMail.PERMISSION_GRANTED == foobar) {

		if (!mailAlreadyProcessed) {
		    processMessage(mail.getMessage(), mailContent);
		    mailAlreadyProcessed = true;
		}
		
		System.out.println("permission granted for journal " + journalAlias + ": creating new article");
		zenoMail.createArticle(journalAlias, userID.toString(), mail.getMessage().getSubject(), mailContent.getContent(), mailContent.getAttachments());

	    } else if (zenoMail.PERMISSION_DENIED_INFORM_SENDER == foobar) {
		System.out.println("permission denied for journal " + journalAlias + ": inform sender");
		errorBuffer.append("** insufficient access permissons for journal " + journalAlias + " **\n");
		errors.add(mail.getSender());
		
	    } else {
		System.out.println("permission denied for journal " + journalAlias + ": ignore email");

	    }
	    
	}

	zenoMail.logout();


        if (!errors.isEmpty()) {
            //If there were errors, we need to send a message to the sender
            //  with the details

	    MimeMessage reply;

	    if (null != (reply = ZenoMailReply.createReply(getMailetContext().getPostmaster(),
								 mail.getMessage(),
								 errorBuffer))) {
		getMailetContext().sendMail(getMailetContext().getPostmaster(),
					    errors, reply);
	    }
        }
        //We always consume this message
        mail.setState(Mail.GHOST);
	
    }
    

    private void processMessage(Part message, MailContent mailContent) {
	
	try {

	    if (message.isMimeType("text/plain")) {
		mailContent.addPlainContent((String) message.getContent());
		
	    } else if (message.isMimeType("text/html")) {
		mailContent.addHtmlContent((String) message.getContent());
		
	    } else if (message.isMimeType("multipart/*")) {
		Multipart mp    = (Multipart) message.getContent();
                int       count = mp.getCount();
		
                level++;
                for (int i = 0; i < count; i++)
                    processMessage(mp.getBodyPart(i), mailContent);
                level--;
		
	    } else if (message.isMimeType("message/rfc822")) {
		level++;
		processMessage((Part) message.getContent(), mailContent);
		level--;

	    }

	    if (level != 0 && !message.isMimeType("multipart/*")) {
		String disp = message.getDisposition();
		if ((disp == null || disp.equalsIgnoreCase(message.ATTACHMENT) || disp.equalsIgnoreCase(message.INLINE)) && 
		    message.getFileName() != null)
		    mailContent.addAttachment(message);
	    }

	} catch (Exception e) {
	    e.printStackTrace();
	}

    }


    public void init() throws MailetException {

	ZenoMail.initialize(getInitParameter("zeno-war"));
	
    }

    
    public String getMailetInfo() {

        return "Zeno Delivery Mailet";
    }

}
