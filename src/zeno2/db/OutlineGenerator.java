package zeno2.db;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import zeno2.kernel.Article;
import zeno2.kernel.ArticleCollection;
import zeno2.kernel.Link;
import zeno2.kernel.OutlineNode;
import zeno2.kernel.Topic;
import zeno2.kernel.ZenoCollection;
import zeno2.kernel.ZenoResource;
import zeno2.kernel.ZenoException;


public class OutlineGenerator {
	FactoryImpl factory;
	ZenoCollection collection;
	int id = 0;
	boolean extended = true;
	Comparator comparator;
	
	public OutlineGenerator(FactoryImpl factory, ZenoCollection collection, boolean extended) {
		this.factory = factory;
		this.collection = collection;
		this.id = collection.getId();
		this.extended = extended;
	}
	

		/** Returns an ordered list of OutlineNodes, with the article of the given id as the
	 source of the link in each node. At each level of the tree, the links are sorted by
	 label and source alias.  Since journals are arbitrary graphs, not trees, duplicates
	 or cycles are broken by expanding only a single occurrence of each node. The
	 other occurrences appear in the tree, but are not expanded.
	 The linkLabel parameter can be used to include articles which can be reached only via
	 links with the given label,
	 where each child node in the tree must be the source of a link
	 targeting the parent node.  If the linkLabel parameter is null or the
	 empty string, all links are used to generate the tree. */
	 
	 
	 
	 public List getOutline(List articles, String label, int direction)
			throws ZenoException {
		
		Hashtable mainLinkTable = new Hashtable();
		collectLinks(mainLinkTable, label, direction);
		List outline; 
		if (extended)
			outline = getXOutline(mainLinkTable, articles);
		else
		 	outline = getOutline(mainLinkTable, articles);
		 return outline;
	}
	
	public List getFullOutline(List artCollections, String label, int direction)
			throws ZenoException {
		
		extended = true;
		Hashtable mainLinkTable = new Hashtable();
		collectLinks(mainLinkTable, label, direction);
		List articles = ArticleCollection.addAll(artCollections);
		return getFullOutline(mainLinkTable, articles);
	}
	 
	 public OutlineNode getOutline (int resourceId, String linkLabel) 
			throws ZenoException
	{	
		List articles = new ArrayList();
		Article art = (Article)factory.loadResource(resourceId);	
		articles.add(art);
		List nodes = getOutline(articles, linkLabel, -1);
		return (OutlineNode)nodes.get(0);
	}
	
	

	/** collects all inlinks or outlinks of the journal in linkTable
	if direction is < 0 or > 0 respectively */
	
	
	public void collectLinks(Hashtable linkTable, String linkLabel, int direction) 
			throws ZenoException {
		
		StringBuffer buf = new StringBuffer();
		buf.append("select label, source_alias, target_alias, source, target, flag from link, resource");
		if (direction < 0)
			buf.append(" where target=id");
		else
			buf.append(" where source=id");
		if (collection instanceof Topic)
			buf.append(" and part=");
		else
			buf.append(" and parent=");
		buf.append(DBClient.format(this.id));
		
		if (!"".equals(linkLabel)) {
			buf.append(" and label = ");
			buf.append(DBClient.format(linkLabel));
		}
		buf.append(" and source_mark='false' and target_mark='false'");
		List allLinks = LinkImpl.getLinksWhere(factory, buf.toString());
		Iterator it = allLinks.iterator();
		while(it.hasNext()) {
			LinkImpl link = (LinkImpl)it.next();
			Integer keyId = 
				new Integer((direction < 0) ? link.getTargetId() : link.getSourceId());
			List links = (List)linkTable.get(keyId);
			if (links == null) {
				links = new ArrayList();
				linkTable.put(keyId, links);
			}
			links.add(link);
		}
	}


	protected void collectLinksBut(Hashtable linkTable, String linkLabel) 
			throws ZenoException {
	
		StringBuffer buf = new StringBuffer();
		buf.append("select distinct label, source_alias, target_alias, source,target, flag from link, resource");
		buf.append(" where (target=id or source =id) ");
		if (collection instanceof Topic)
			buf.append(" and part=");
		else
			buf.append(" and parent=");
		buf.append(DBClient.format(this.id));
		if (linkLabel != null && !linkLabel.equals("")) {
			buf.append(" and label<>");
			buf.append(DBClient.format(linkLabel));
		}
		//heg
		buf.append(" and source_mark='false' and target_mark='false'");
		List alllinks = LinkImpl.getLinksWhere(factory, buf.toString());
		Iterator it = alllinks.iterator();
		while(it.hasNext()) {
			LinkImpl link = (LinkImpl)it.next();
			Integer targetId = new Integer(link.getTargetId());
			List links = (List)linkTable.get(targetId);
			if (links == null) {
				links = new ArrayList();
				linkTable.put(targetId, links);
			}
			links.add(link);
			Integer sourceId = new Integer(link.getSourceId());
			links = (List)linkTable.get(sourceId);
			if (links == null) {
				links = new ArrayList();
				linkTable.put(sourceId, links);
			}
			links.add(link);
		}
	} 
	
	public List getOutline(Hashtable mainLinkTable, List articles)
			throws ZenoException {
		
		comparator = new NodeComparator();
		List result = new ArrayList();
		Iterator artit = articles.iterator();
		List outlineLinks = new ArrayList();
		Hashtable visited = new Hashtable();
		int nr = 1;
		while (artit.hasNext()) {
			ArticleImpl art = (ArticleImpl)artit.next();
			OutlineNode artNode =  
				new OutlineNode("", Integer.toString(art.getId()), art.getAlias());
			artNode.setPosition(Integer.toString(nr++));
			artNode.setResource(art);
			result.add(artNode);
		}
		Iterator resit = result.iterator();
		while (resit.hasNext()) {
			OutlineNode artNode = (OutlineNode)resit.next();
			genChildren(mainLinkTable, artNode, visited, null);
		}
		return result;	
	}
	
	
	protected List getXOutline(Hashtable mainLinkTable, List articles)
			throws ZenoException {
		
		comparator = new NodeComparator2();
		List result = new ArrayList();
		Iterator artit = articles.iterator();
		List outlineLinks = new ArrayList();
		Hashtable resTable = new Hashtable();
		Hashtable visited = new Hashtable();
		int nr = 1;
		while (artit.hasNext()) {
			ArticleImpl art = (ArticleImpl)artit.next();
			OutlineNode artNode = 
				new OutlineNode("", Integer.toString(art.getId()), art.getAlias());
			artNode.setPosition(Integer.toString(nr++));
			artNode.setResource(art);
			collectOutlineLinks(mainLinkTable, art.id, outlineLinks);
			resTable.put(new Integer(art.getId()), art);
			result.add(artNode);
		}
		LinkImpl.fillLinks(factory, outlineLinks, resTable, false);
		Iterator resit = result.iterator();
		while (resit.hasNext()) {
			OutlineNode artNode = (OutlineNode)resit.next();
			Article root = (Article)artNode.getResource();
			genChildren(mainLinkTable, artNode, visited, root);
		}
		return result;	
	}
	
	public static Hashtable genResTable(List resList) {
		Hashtable table = new Hashtable();
		Iterator lit = resList.iterator();
		while(lit.hasNext()) {
			ArticleImpl art = (ArticleImpl)lit.next();
			table.put(new Integer(art.getId()), art);
		}
		return table;
	}
	
	public List getFullOutline(Hashtable mainLinkTable, List articles)
			throws ZenoException {
		
		comparator = new NodeComparator2();
		List result = new ArrayList();
		Iterator artit = articles.iterator();
		Hashtable resTable = genResTable(articles);
		Hashtable visited = new Hashtable();
		int nr = 1;
		while (artit.hasNext()) {
			ArticleImpl art = (ArticleImpl)artit.next();
			if (art instanceof Topic || 
					visited.get(new Integer(art.getId())) == null) {
				OutlineNode artNode = 
					new OutlineNode("", Integer.toString(art.getId()), art.getAlias());
				artNode.setPosition(Integer.toString(nr++));
				artNode.setResource(art);
				result.add(artNode);
				List outlineLinks = new ArrayList();
				collectOutlineLinks(mainLinkTable, art.id, outlineLinks);
				LinkImpl.fillLinks(factory, outlineLinks, resTable, false);
				genChildren(mainLinkTable, artNode, visited, art);
			}
		}
		return result;	
	}
	
	
	protected static void collectOutlineLinks
		(Hashtable linkTable, int id, List outlineLinks) {
	
		List links = (List)linkTable.get(new Integer(id));
		if (links != null) {
			Iterator it = links.iterator();
			while (it.hasNext()) {
				LinkImpl link = (LinkImpl) it.next();
				if (!outlineLinks.contains(link)) {
					outlineLinks.add(link);
					int otherId;
					if (link.getSourceId() == id) {
						otherId = link.getTargetId();
					} else {
						otherId = link.getSourceId();
					}
					collectOutlineLinks(linkTable, otherId, outlineLinks);
				}
			}
		}
	}
	
	protected void collectOutlineLinks
		(Hashtable linkTable, ZenoResource res, List outlineLinks) {
		
		int id = ((ResourceImpl)res).id;
		List links = (List)linkTable.get(new Integer(id));
		if (links != null) {
			Iterator it = links.iterator();
			while (it.hasNext()) {
				LinkImpl link = (LinkImpl) it.next();
				if (!outlineLinks.contains(link)) {
					outlineLinks.add(link);
					int otherId;
					if (link.getSourceId() == id) {
						link.setSourceResource(res);
						otherId = link.getTargetId();
					} else {
						link.setTargetResource(res);
						otherId = link.getSourceId();
					}
					collectOutlineLinks(linkTable, otherId, outlineLinks);
				}
			}
		}
	}
	
	protected  OutlineNode genOutlineNode(Link link, int parentId) {
		String linkLabel, id, alias;
		ZenoResource res;
		if (link.getSourceId() == parentId) {
			linkLabel = link.getLabel();
			id = Integer.toString(link.getTargetId());
			alias = link.getTargetAlias();
			res = ((LinkImpl)link).getTargetResource();
		} else {
			linkLabel = "-" + link.getLabel();
			id = Integer.toString(link.getSourceId());
			alias = link.getSourceAlias();
			res = ((LinkImpl)link).getSourceResource();
		}
		OutlineNode node = new OutlineNode(linkLabel, id, alias);
		node.setResource(res);
		return node;
	}
	
	protected void genChildren
		(Hashtable linkTable, OutlineNode node, Hashtable visited, Article root)
			throws ZenoException {
		
		Integer nodeId = new Integer(node.getId());
		OutlineNode rnode = (OutlineNode)visited.get(nodeId);
		if (rnode != null) {
			node.setDuplicate(true);
			node.setOriginalPosition(rnode.getPosition());
			rnode.setDuplicated(true);
			return;
		}
		
		visited.put(nodeId, node);
		
		List links = (List)linkTable.get(nodeId);
		if (links != null) {
			Iterator it = links.iterator();
			while (it.hasNext()) {
				LinkImpl link1 = (LinkImpl) it.next();
				OutlineNode cnode = genOutlineNode(link1, nodeId.intValue());
				if (root != null)
					cnode.genMark(root);
				if (!extended || cnode.getResource() != null)
					node.add(cnode);
			}
			Collections.sort(node.getChildren() , comparator);
		}
		Iterator cit = node.getChildren().iterator();
		int nr = 1;
		while(cit.hasNext()) {
			OutlineNode cnode = (OutlineNode)cit.next();
			String newPosition = node.getPosition() + "." + nr++;
			cnode.setPosition(newPosition);
			genChildren(linkTable, cnode, visited, root);
		}
	}
		
		//obsolete
		public List getOutline0(List articles) throws ZenoException
	{
		List result = new ArrayList();
		NodeComparator nodeComparator = new NodeComparator();
		Hashtable mainLinkTable = new Hashtable();
		collectLinks(mainLinkTable, "", -1);
		Iterator artit = articles.iterator();
		int nr = 1;
		while (artit.hasNext()) {
			ArticleImpl art = (ArticleImpl)artit.next();
			OutlineNode artNode =
				new OutlineNode("", Integer.toString(art.getId()), art.getAlias());
			artNode.setPosition(Integer.toString(nr++));
			artNode.setResource(art);
			Integer artId = new Integer(art.getId());
			HashSet visited = new HashSet();
			List links = (List)mainLinkTable.get(artId);
			if (links != null) {
				Iterator it = links.iterator();
				while (it.hasNext()) {
					LinkImpl link = (LinkImpl) it.next();
					artNode.add(getNode(mainLinkTable, link, art.getId(), "", visited));
				}
				Collections.sort(artNode.getChildren(), nodeComparator);
			}
			result.add(artNode);
		}
		return result;
	}
	
	protected  OutlineNode getNode
			 (Hashtable linkTable, Link link, int parentId, String linkLabel, Set visited)
		throws ZenoException

	{
		Integer otherId = 
			new Integer((link.getSourceId() == parentId) ? link.getTargetId() : link.getSourceId());
		OutlineNode node = genOutlineNode(link, parentId);
	
		if (visited.contains(otherId)) {
			node.setDuplicate(true);
			return node;
		} else {
			visited.add(otherId);
			List links = (List)linkTable.get(otherId);
			if (links != null) {
				Iterator it = links.iterator();
				while (it.hasNext()) {
					LinkImpl link1 = (LinkImpl) it.next();
					if ("".equals(linkLabel) || link1.getLabel().equals(linkLabel))
						node.add(getNode(linkTable, link1, otherId.intValue(), linkLabel, visited));
					else
						node.add(genOutlineNode(link1, otherId.intValue()));
				}
				Collections.sort(node.getChildren() , new NodeComparator ());
			}
			return node;
		}
	}

	public List groupNodes(List nodes) {
		List result = new ArrayList();
		List nodegroup = null;
		int ltopid = -1;
		Iterator nit = nodes.iterator();
		while(nit.hasNext()) {
			OutlineNode node = (OutlineNode)nit.next();
			int ctopid = ((ArticleImpl)node.getResource()).getTopicId();
			if (ctopid != ltopid) {
				nodegroup = new ArrayList();
				result.add(nodegroup);
			}
			nodegroup.add(node);
		}
		return result;	
	}
	
	public List groupNodes2(List nodes) {
		List result = new ArrayList();
		List topgroups = new ArrayList();
		List freegroup = new ArrayList();
		result.add(topgroups);
		result.add(freegroup);
		List nodegroup = null;
		int ltopid = -1;
		Iterator nit = nodes.iterator();
		while(nit.hasNext()) {
			OutlineNode node = (OutlineNode)nit.next();
			int ctopid = ((ArticleImpl)node.getResource()).getTopicId();
			if (ctopid != ltopid) {
				nodegroup = new ArrayList();
				if (ctopid == 0)
					freegroup.add(nodegroup);
				else
					topgroups.add(nodegroup);
			}
			nodegroup.add(node);
		}
		return result;	
	}
	
	//-------------------------------------------------------
	
	
	class NodeComparator implements Comparator {
	
		public int compare (Object o1, Object o2) {
			int result = 0;
			if (o1 instanceof OutlineNode && o2 instanceof OutlineNode) {
				OutlineNode n1 = (OutlineNode) o1;
				OutlineNode n2 = (OutlineNode) o2;
				result = stringCompare(n1.getLabel(), n2.getLabel());
				if (result == 0) { 
					result = stringCompare(n1.getAlias(), n2.getAlias());
				}
			} 
			return result;
		}
	
	private final int stringCompare(String a1, String a2) {
			String s1 = a1.toLowerCase();
			String s2 = a2.toLowerCase();
			int len1 = s1.length();
			int len2 = s2.length();
			int n = Math.min(len1, len2);
			for (int i = 0; i < n; i++) {
				char c1 = s1.charAt(i);
				char c2 = s2.charAt(i);
				if (c1 != c2) { return c1 - c2; }
			}
			return len1 - len2;
		}
		
	}	

	class NodeComparator2 implements Comparator {
	
		public int compare (Object o1, Object o2) {
			int result = 0;
			if (o1 instanceof OutlineNode && o2 instanceof OutlineNode) {
				ZenoResource res1 = ((OutlineNode)o1).getResource();
				ZenoResource res2 = ((OutlineNode)o2).getResource();
				try {
					int rank1 = res1.getRank();
					int rank2 = res2.getRank();
					if (rank1 == rank2) 
						return res1.getId() - res2.getId();
					else 
						return rank1 - rank2;
				} catch(ZenoException e) {}
			}
			return result;
		}
	}
	
}