/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package zeno2.james.transport.matchers;


import org.apache.mailet.Mail;
import org.apache.mailet.GenericMatcher;
import org.apache.mailet.MailAddress;
import org.apache.mailet.MailetContext;

import org.apache.log4j.Logger;

import zeno2.db.ZenoMail;
import zeno2.james.util.ZenoMailReply;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import java.util.Collection;
import java.util.Iterator;
import java.util.Vector;
import java.util.Date;

/**
 * @version 1.0.0, 07/09/2002
 * @author Ahmet Ocakli <ahmet.ocakli@ais.fhg.de>
 */
public class RecipientIsZenoJournal extends GenericMatcher {

    private static Logger        log;
    private        ZenoMail zenoMail;

    public RecipientIsZenoJournal() {

	log = Logger.getLogger(RecipientIsZenoJournal.class.getName());
	zenoMail = new ZenoMail();
    }

    /**
     * Matches each recipient one by one through matchRecipient(MailAddress
     * recipient) method.  Handles splitting the recipients Collection
     * as appropriate.
     *
     * @param mail - the message and routing information to determine whether to match
     * @return Collection the Collection of MailAddress objects that have been matched
     */

    public final Collection match(Mail mail) throws MessagingException {
        Collection   matching = new Vector();

	zenoMail.login();

	if (zenoMail.zenoMailIsConfigured()) {
	    
	    StringBuffer errorBuffer = new StringBuffer();
	    
	    for (Iterator i = mail.getRecipients().iterator(); i.hasNext(); ) {
		MailAddress rec = (MailAddress) i.next();
		if (matchRecipient(rec, errorBuffer)) {
		    matching.add(rec);
		}
	    }

	    if (0 != errorBuffer.length() && zenoMail.isZenoUser(mail.getSender().toString().toLowerCase())) {

		Collection errors = new Vector();
		errors.add(mail.getSender());

		MimeMessage reply;

		if (null != (reply = ZenoMailReply.createReply(getMailetContext().getPostmaster(),
							       mail.getMessage(),
							       errorBuffer))) {
		    getMailetContext().sendMail(getMailetContext().getPostmaster(),
						errors, reply);
		}

	    }
	    
	}

	zenoMail.logout();
	return matching;
    }

    
    public boolean matchRecipient(MailAddress recipient, StringBuffer errorBuffer) {
	
	boolean journalExists = false;	
        MailetContext mailetContext = getMailetContext();

	if (mailetContext.isLocalServer(recipient.getHost().toLowerCase())) {
	    
	    if (zenoMail.isZenoAlias(recipient.getUser().toLowerCase())) {
		
		journalExists = true;
		System.out.println("ok, journal exists");
	    } else {
		System.out.println("no such journal: " + recipient.getUser().toLowerCase() );
		errorBuffer.append("** no such journal: " + recipient.getUser().toLowerCase() + " **\n");

	    }

	}
	return journalExists;
	
    }
}
