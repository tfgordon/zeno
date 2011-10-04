package zeno2.servlets.extcmdaction;

/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999 The Apache Software Foundation.  All rights
 * reserved.
 *
 */
import com.oreilly.servlet.MultipartRequest;

// Java Stuff
import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
//import javax.servlet.ServletInputStream;

import zeno2.kernel.Article;
import zeno2.kernel.Attachment;
import zeno2.kernel.Factory;
import zeno2.kernel.Journal;
import zeno2.kernel.NoPermissionException;
import zeno2.kernel.NotFoundException;
import zeno2.kernel.ZenoException;
//import zeno2.kernel.;
import zeno2.servlets.ZenoExternalCommandServlet;

import zeno2.util.ZenoUtilities;
import zeno2.velocity.util.Tools;

/**
 *  Base class external zeno commands
 *
 *@author     <a href="mailto:lothar.oppor@ais.fhg.de">Lothar Oppor</a>
 *@created    2002-05-27
 *@version    $zenoVersion $
 */
public class AddAttachmentCommand extends Command {

	String artId = "";


	/**
	 *  Constructor
	 *
	 *@param  fact   Description of Parameter
	 *@param  multi  Description of Parameter
	 */
	public AddAttachmentCommand(MultipartRequest multi, Factory fact) {
		super(multi, fact);
		System.out.println("AddAttachmentCommand.exec.fact=" + fact);
	}


	/**
	 *  Implemented by classes that extends this class
	 *
	 *@return    the name of the template to execute
	 */
	public Iterator exec() {
		System.out.println("AddAttachmentCommand.exec: start");
		List result = new ArrayList();
		String id = getMultiParam("id");
		System.out.println("AddAttachmentCommand.exec.id=" + id);
		if (id.equals("null")) {
			result.add(ZenoExternalCommandServlet.ERRORS[11]);
			result.add("id");
			return result.iterator();
		}
		int idNr = (new Tools()).toInt(id);
		try {
			Article art = (Article) factory.loadResource(idNr);
			System.out.println("AddAttachmentCommand.exec.art=" + art);
			uploadAttachment(art, result);
		}
		catch (ClassCastException e) {
			result.add(ZenoExternalCommandServlet.ERRORS[12]);
			result.add("id -->" + e.toString());
			System.out.println("AddAttachmentCommand.exec.error=" + e);
		}
		catch (NotFoundException e) {
			result.add(ZenoExternalCommandServlet.ERRORS[13]);
			result.add("id -->" + e.toString());
			System.out.println("AddAttachmentCommand.exec.error=" + e);
		}
		catch (NoPermissionException e) {
			result.add(ZenoExternalCommandServlet.ERRORS[6]);
			result.add("id -->" + e.toString());
			System.out.println("AddAttachmentCommand.exec.error=" + e);
		}
		catch (Exception e) {
			result.add(ZenoExternalCommandServlet.ERRORS[7]);
			result.add("id -->" + e.toString());
			System.out.println("AddAttachmentCommand.exec.error=" + e);
		}
		return result.iterator();
	}


	/**
	 *  Description of the Method
	 *
	 *@param  art  Description of Parameter
	 *@param  mes  Description of Parameter
	 */
	private void uploadAttachment(Article art, List mes) {
		try {
			int fnr = 1;
			String fileName = multi.getFilesystemName("file");
			while ((fileName != null) && !fileName.equals("")) {
				String type = multi.getContentType("file");
				System.out.print("AddAttachmentCommand.uploadAttachment.fileName="
						 + fileName + ", type=" + type);
				// ** supposed bug in com.oreilly.servlet.MultipartRequest;
				int p = type.indexOf(";");
				if (p > 0) {
					type = type.substring(0, p);
				}
				// ** end of bug prevent
				System.out.println(",(no semicolon) type=" + type);
				File f = multi.getFile("file");
				if (f != null) {
					Attachment newAttachment =
						art.addAttachment(fileName, type, f);
					mes.add(Integer.toString(newAttachment.getId()));
					System.out.println("AddAttachmentCommand.exec.got_file:" + fileName);
				}
				else {
					mes.add(ZenoExternalCommandServlet.ERRORS[14]);
					System.out.println("AddAttachmentCommand.exec.error***="
							 + ZenoExternalCommandServlet.ERRORS[14]);
				}
				fileName = multi.getFilesystemName("file" + Integer.toString(++fnr));
			}
			//mes.add(ZenoExternalCommandServlet.ERRORS[0]);
		}
		catch (Exception e) {
			mes.add(ZenoExternalCommandServlet.ERRORS[7]);
			mes.add("file -->" + e.toString());
			System.out.println("AddAttachmentCommand.exec.error=" + e);
		}
	}
}

