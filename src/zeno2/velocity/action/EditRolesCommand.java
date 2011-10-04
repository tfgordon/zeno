package zeno2.velocity.action;


import java.util.*;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.velocity.context.Context;
import org.apache.velocity.app.tools.*;
import org.apache.velocity.servlet.VelocityServlet;

import zeno2.kernel.*;

/**
 *  Handles zeno journal roles
 *
 *@author     <a href="mailto:eckehard.gross@ais.fraunhofer.de">Eckehard Gross</a>
 *@version    2.0.2, 201-11-14
 */
public class EditRolesCommand extends Command {
	
	public final String present = "forum/journal/editJournalRoles.vm";
	public final String select = "forum/journal/selectForRole.vm";
	public final String jnselect = "forum/journal/selectForReplace.vm";
	public final String struct	= "forum/journal/editJournalXStruct.vm";
	public final String edit	= "forum/journal/editJournal.vm";
	
	public EditRolesCommand(HttpServletRequest req, HttpServletResponse resp) {
		super(req, resp);
	}
	
	public String exec(Context ctx) throws Exception {

		String id = request.getParameter("id");
		String view = request.getParameter("view");
		String mode = request.getParameter("mode");
		int idNr = 0;
		ctx.put("mode", view);
		
		try {
			idNr = Integer.parseInt(id);
		}
		catch (Exception e) {
			idNr = 0;
		}
		
		Factory factory = (Factory) ctx.get(Constants.FACTORY_KEY);
		Journal me =null;
		try {
			me = (Journal) factory.loadResource(idNr);
			ctx.put("journal", me);
			ctx.put("id", id);
			String styleSheet = me.getStyleSheetUrl();
			if ((styleSheet == null) || !styleSheet.startsWith("ss")) {
				styleSheet = "ss1";
			}
			ctx.put("zcss", "/zeno/css/"+styleSheet+".css");
		} catch (Exception e) {
			return showMessage(ctx, "error.no_such_journal");
		}
		
		
		if (mode.equals("present")) {
			return preparePresent(factory, me, ctx);
		} 
			
		else if (mode.equals("update")) {
			
			if (request.getParameter("ok") != null) {
				String role = request.getParameter("role");
				String[] idsToRemove = request.getParameterValues("remove");
				boolean recursively = role.equals(request.getParameter("recursively"));
				removePrincipalsFromRole(me, idsToRemove, role, recursively);				
				String idToAdd = request.getParameter("add");
				String cid = request.getParameter("cid");
				
				if ("<".equals(idToAdd)) {
					String[] ids = new String[1];
					ids[0] = cid;
					addPrincipalsToRole(me, ids, role, recursively);
					return preparePresent(factory, me, ctx);
				}
				
				Iterator principalsToSelect = 
						getPrincipalsToSelect(factory, idToAdd, cid);
				if (principalsToSelect != null) {
					ctx.put("principals", principalsToSelect);
					ctx.put("mode", "prinselect");
					ctx.put("role", role);
					if (recursively)
						ctx.put("recursively", role);
					return select;
				} else {
					return preparePresent(factory, me, ctx);
				}
			} else 
				return struct;
		}
		
		else if (mode.equals("select")) {
			
			if (request.getParameter("ok") != null) {
				String role = request.getParameter("role");
				String[] prinselected = request.getParameterValues("prinselected");
				boolean recursively = role.equals(request.getParameter("recursively"));
				addPrincipalsToRole(me, prinselected, role, recursively);
			}
			return preparePresent(factory, me, ctx);
		} 
		
		else if (mode.equals("replace")) {
			
			if (request.getParameter("ok") != null) {
				String use = request.getParameter("use");
				boolean recursively = "all".equals(request.getParameter("recursively"));
				List journalsToSelect =
					getJournalsToSelect(factory, use);
				if (journalsToSelect != null) {
					ctx.put("journals", journalsToSelect);
					ctx.put("mode", "jnselect");
					if (recursively)
						ctx.put("recursively", "all");
					return jnselect;
				} else {
					return preparePresent(factory, me, ctx);
				}
			} else
				return struct;
		}
		
		else if (mode.equals("jnselect")) {
			
			if (request.getParameter("ok") != null) {
				String jnselected = request.getParameter("jnselected");
				boolean recursively = "all".equals(request.getParameter("recursively"));
				replaceRoleDefinition(factory, me, jnselected, recursively);
			}
			return preparePresent(factory, me, ctx);
		}
		 
		else 	
			return struct;
			
	}
	
	
	private String preparePresent(Factory factory, Journal me, Context ctx) {
		ctx.put("readers", getPrincipalsInRole(factory, me, "reader"));
		ctx.put("writers", getPrincipalsInRole(factory, me, "writer"));
		ctx.put("editors", getPrincipalsInRole(factory, me, "editor"));
		ctx.put("add", "");
		ctx.put("use", "");
		return present;
	}
	
	private static List getPrincipalsInRole(Factory factory, Journal journal, String role) {
		List principals = new ArrayList();
		try {
			Iterator it = journal.getRoleDefinition(role).iterator();
			while(it.hasNext()) {
				Principal principal = factory.loadPrincipal((String)it.next());
				principals.add(principal);
			}
		} catch (ZenoException e) {
			System.out.println(e);
		}
		return principals;
		
	}	
		
	private void removePrincipalsFromRole(Journal journal, String[] ids, String role) {
		if (ids == null)
			return;
		for (int i = 0; i < ids.length; i++) {
			try {
				journal.deletePrincipalFromRole(ids[i], role);
			} catch (Exception e) {
				System.out.println(e);
			}
		}
	}
	
	private void removePrincipalsFromRole(Journal journal, 
											String[] ids, 
											String role, 
											boolean recursively) {
		
		removePrincipalsFromRole(journal, ids, role);
		
		if (recursively) {
			try {
				Iterator it = ((Journal)journal).getSubjournals().iterator();
				while(it.hasNext()) {
					Journal subjournal = (Journal)it.next();
					removePrincipalsFromRole(subjournal, ids, role, recursively);
				}
			} catch (ZenoException e) {}
		}
		
	}
	
	
	
	private void addPrincipalsToRole(Journal journal, String id, String role) {
		
		if (id == null || id.equals(""))
			return;
		try {
			journal.addPrincipalToRole(id, role);
		} catch (Exception e) {
			System.out.println(e);
		}
	}
	
	
	private void addPrincipalsToRole(Journal journal, String[] ids, String role) {
	
		if (ids == null)
			return;
		for(int i=0; i<ids.length; i++) {
			try {
				journal.addPrincipalToRole(ids[i], role);
			} catch (Exception e) {
				System.out.println(e);
			}
		}
	}
	
	private void addPrincipalsToRole(Journal journal, 
											String[] ids, 
											String role, 
											boolean recursively) {
		
		addPrincipalsToRole(journal, ids, role);
		
		if (recursively) {
			try {
				Iterator it = ((Journal)journal).getSubjournals().iterator();
				while(it.hasNext()) {
					Journal subjournal = (Journal)it.next();
					addPrincipalsToRole(subjournal, ids, role, recursively);
				}
			}catch (ZenoException e) {}
		}
	}
	
	private void replaceRoleDefinition(Factory factory, 
										Journal journal, 
										String id, 
										boolean recursively) {
		try {
			int intid = Integer.parseInt(id);
			Journal source = (Journal)factory.loadResource(intid);
			journal.replaceRoleDefinition(source, null, recursively);
		} catch(Exception e) {
			System.out.println(e);
		}
	}
	
	
	private Iterator getPrincipalsToSelect(Factory factory, 
											String name,
											String community) {
		
		if (name == null || name.equals(""))
			return null;
		try {
			Iterator it = factory.searchPrincipals(community, name, "");
			if (it.hasNext())
				return it;
			else
				return null;
		} catch(ZenoException e) {
			System.out.println(e);
			return null;
		}
	}
	
	private List getJournalsToSelect(Factory factory, String name) {
		
		if (name == null || name.equals(""))
			return Collections.EMPTY_LIST;
		try {
			name = "%" + name + "%";
			return factory.searchJournals(name);
		} catch(ZenoException e) {
			System.out.println(e);
			return Collections.EMPTY_LIST;
		}	
	}
	

	
}