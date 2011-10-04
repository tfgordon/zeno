package zeno2.servlets.extcmdaction;

/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999 The Apache Software Foundation.  All rights
 * reserved.
 *
 */

// Java Stuff
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;

import zeno2.kernel.Article;
import zeno2.kernel.ArticleCollection;
import zeno2.kernel.Factory;
import zeno2.kernel.NotFoundException;
import zeno2.servlets.ZenoExternalCommandServlet;

import zeno2.util.ZenoUtilities;
import zeno2.velocity.util.Tools;

/**
 *  Base class for external zeno commands
 *
 *@author     <a href="mailto:lothar.oppor@ais.fhg.de">Lothar Oppor</a>
 *@created    2002-08-13
 *@version    $zenoVersion $
 */
public class GetArticleIdCommand extends Command {

	/**
	 *  Constructor
	 *
	 *@param  req   Description of Parameter
	 *@param  fact  Description of Parameter
	 */
	public GetArticleIdCommand(HttpServletRequest req, Factory fact) {
		super(req, fact);
	}


	/**
	 *  Implemented by classes that extends this class
	 *
	 *@return    the name of the template to execute
	 */
	public Iterator exec() {
		List result = new ArrayList();
		String title = getParam("title");
		//System.out.println("GetArticleIdCommand.exec.title=" + title);
		if (title.equals("null") || title.equals("")) {
			result.add(ZenoExternalCommandServlet.ERRORS[9]);
			return result.iterator();
		}
		if (title.equals("%")) {
			result.add(ZenoExternalCommandServlet.ERRORS[15] + "-->" + title);
			return result.iterator();
		}
		try {
			List objects = factory.searchArticles(null, title, null, null, null
					, null, null, null);
			System.out.println("GetArticleIdCommand.exec.objects=" + objects);
			if (objects.isEmpty()) {
				result.add(ZenoExternalCommandServlet.ERRORS[5] + "-->" + title);
			}
			else {
				for (int i = 0; i < objects.size(); i++) {
					Object o = objects.get(i);
					System.out.println("GetArticleIdCommand.exec.o=" + o);
					if (o instanceof ArticleCollection) {
						ArticleCollection articleCollection = (ArticleCollection) o;
						System.out.println("GetArticleIdCommand.exec.articleCollection=" + articleCollection);
						List arts = articleCollection.getArticles();
						for (int j = 0; j < arts.size(); j++) {
							Article art = (Article) arts.get(j);
							result.add(Integer.toString(art.getId())
									 + " " + art.getTitle());
						}
					}
				}
			}
		}
		catch (NotFoundException e) {
			result.add(ZenoExternalCommandServlet.ERRORS[16]);
			result.add("title -->" + e.toString());
		}
		catch (Exception e) {
			result.add(ZenoExternalCommandServlet.ERRORS[7]);
			result.add("title -->" + e.toString());
		}

		return result.iterator();
	}

}

