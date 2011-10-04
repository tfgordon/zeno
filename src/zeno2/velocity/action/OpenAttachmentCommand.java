package zeno2.velocity.action;

/*
 *
 *
 *
 *
 */
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.velocity.context.Context;
import org.apache.velocity.servlet.VelocityServlet;

import zeno2.kernel.Article;
import zeno2.kernel.Attachment;
import zeno2.kernel.Constants;
import zeno2.kernel.Factory;
import zeno2.velocity.util.Errors;

/**
 *  Handles zeno journal views
 *
 *@author     <a href="mailto:lothar.oppor@ais.fraunhofer.de">Lothar Oppor</a>
 *@version    2.0.2, 2001-09-07
 */
public class OpenAttachmentCommand extends Command {

	/**
	 *  Description of the Field
	 *
	 *@param  req   Description of Parameter
	 *@param  resp  Description of Parameter
	 *@since
	 */
	public OpenAttachmentCommand(HttpServletRequest req, HttpServletResponse resp) {
		super(req, resp);
	}


	/**
	 *  Compact, if the resource is a journal and compact parameter is true
	 *  Otherwise mark the resource for deleted. The handeled resource has to be
	 *  saved!!!
	 *
	 *@param  ctx            Velocity Context
	 *@return                the appropriate template file name
	 *@exception  Exception  Description of Exception
	 *@since
	 */
	public String exec(Context ctx) throws Exception {

		HttpSession sess = request.getSession();
		String id = request.getParameter("id");
		String attachmentName = request.getParameter("attachment");
//System.out.println("OpenAttachmentCommand.exec.id=" + id
//+ ", attachment=" + attachmentName);
		Factory factory = (Factory) sess.getAttribute(Constants.FACTORY_KEY);
		int idNr = 0;
		try {
			idNr = Integer.parseInt(id);
		}
		catch (Exception e) {
			System.out.println("OpenAttachmentCommand.exec.id=" + id + ":"
					 + e.getMessage());
			Errors err = new Errors();
			err.addError("error.invalid_id");
			return EditArticleCommand.templates.getProperty("print");
		}
		try {
			Attachment attachment =
					((Article) factory.loadResource(idNr)).getAttachment(attachmentName);
			String mimeType = attachment.getMimeType();
			response.setContentType(mimeType);
			// ************* does not work *****************
			//response.setHeader("Content-Disposition"
			//	,"attachment;filename="+attachmentName);
			BufferedOutputStream out = new BufferedOutputStream(response.getOutputStream());
			BufferedInputStream in = new BufferedInputStream(attachment.getContents());

			byte buffer[] = new byte[2048];
			int n = -1;
			while ((n = in.read(buffer)) > 0) {
				out.write(buffer, 0, n);
				out.flush();
			}
			in.close();
			return null;
		}
		catch (Exception e) {
			System.out.println("OpenAttachmentCommand.exec.error:"
					 + e.getMessage());
			Errors err = new Errors();
			err.addError("error.no_open_attachmentd");
			return EditArticleCommand.templates.getProperty("print");
		}

	}
}

