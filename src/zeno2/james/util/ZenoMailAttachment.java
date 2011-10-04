package zeno2.james.util;

import java.io.InputStream;

import javax.mail.Part;
import javax.mail.MessagingException;

public class ZenoMailAttachment {
    int    length;
    String filename;
    String mimetype;
    Part   attachment;

    public ZenoMailAttachment(Part attachment) {

	try {
	    filename = attachment.getFileName();
	    this.attachment = attachment;

	} catch (Exception e) {

	    e.printStackTrace();
	}
    }

    public int getLength() {

	return length;
    }

    public String getFilename() {

	return filename;
    }

    public void setFilename(String filename) {

	this.filename = filename;
    }

    public String getContentType() {

	String mimeType = "";

	try {
	    mimeType = attachment.getContentType();
	    if (mimeType != null) {
		int p = mimeType.indexOf(";");
		if (p > 0)
		    mimeType = mimeType.substring(0, p);

	    } else {
		mimeType = "application/x-unknown";

	    }

	} catch (MessagingException e) {
	    e.printStackTrace();
	}

	return mimeType;
    }

    public InputStream getInputStream() throws java.io.IOException, javax.mail.MessagingException {

	return attachment.getInputStream();
    }

}
