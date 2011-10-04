package zeno2.notification;

import javax.mail.*;
import javax.mail.internet.*;
import java.util.*;


public class MailWrapper {
    public void postMail( String recipients[ ], String subject, String message , String from, String smtpHost) throws MessagingException {
	boolean debug = false;
	
	//Set the host smtp address
	Properties props = new Properties();
	props.put("mail.smtp.host", smtpHost);
	
	// create some properties and get the default Session
	Session session = Session.getDefaultInstance(props, null);
	session.setDebug(debug);
	
	// create a message
	Message msg = new MimeMessage(session);
	
	// set the from and to address
	InternetAddress addressFrom = new InternetAddress(from);
	msg.setFrom(addressFrom);
	InternetAddress[] addressTo = new InternetAddress[recipients.length];
	for (int i = 0; i < recipients.length; i++)
	    {
		addressTo[i] = new InternetAddress(recipients[i]);
		System.out.println("MailWrapper: sending email to "+addressTo[i]);
	    }
	msg.setRecipients(Message.RecipientType.TO, addressTo);
	// Optional : You can also set your custom headers in the Email if you Want
	msg.addHeader("MyHeaderName", "myHeaderValue");
	// Setting the Subject and Content Type
	msg.setSubject(subject);
	msg.setContent(message, "text/plain");
	Transport.send(msg);
    }
   
}
