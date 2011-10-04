package zeno2.james.util;

import org.apache.mailet.MailAddress;
import java.lang.StringBuffer;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.InternetAddress;

public class ZenoMailReply {

    public static MimeMessage createReply(MailAddress sender, MimeMessage mimeMessage, StringBuffer reason) {
	
	StringBuffer error = new StringBuffer();
	
	error.append("Sorry, your e-mail was rejected. Please check the message below.\n\n");
	error.append("---------------------------------------------------------------\n\n");
	error.append(reason.toString());
	MimeMessage reply = null;
	
	try {
	    reply = (MimeMessage) mimeMessage.reply(false);
	    if (null != mimeMessage.getHeader("Return-Path")) {
		reply.setRecipient(MimeMessage.RecipientType.TO, new InternetAddress(mimeMessage.getHeader("Return-Path")[0]));
	    }
	    
	    reply.setFrom(sender.toInternetAddress());
	    
	    try {
		
		reply.setText(error.toString());
		
	    } catch (Exception e) {
		e.printStackTrace();
	    }
	} catch (Exception e) {
	    e.printStackTrace();
	}

	return reply;

    }
}
