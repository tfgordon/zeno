package zeno2.kernel;

import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.Iterator;
import zeno2.kernel.Article;
import zeno2.kernel.Journal;
import zeno2.kernel.Topic;



public class ArticleCollection {
	Journal journal;
	Topic topic;
	List articles; 
	
	
	public ArticleCollection() {
		this.articles = new ArrayList();
	}
	
	
	public Journal getJournal() {
		return this.journal;
	}
	
	public void setJournal(Journal journal) {
		this.journal = journal;
	}
	
	public Topic getTopic() {
		return this.topic;
	}
	
	public void setTopic(Topic topic) {
		this.topic = topic;
	}
	
	public List getArticles() {
		return this.articles;
	}
	
	public void add(Article article) {
		articles.add(article);
	}
	
	public static List groupArticlesByJournal(List articles) throws ZenoException {
		//articles must be sorted by journal
		
		List collections = new ArrayList();
		Iterator it = articles.iterator();
		int ljid = 0;
		ArticleCollection collection = null;
		while(it.hasNext()) {
			Article cart = (Article)it.next();
			//heg
			int cjid = ((Article)cart).getParentId();
			if (ljid != cjid) {
				try {
					Journal journal = (Journal)cart.getParent();
					collection = new ArticleCollection();
					collection.setJournal(journal);
					collections.add(collection);
					ljid = cjid;
				} catch(ZenoException e) {
					collection = null;
				}
			}
			if (collection != null) collection.add(cart);
		}
		return collections;
	} 
	
	public static List groupArticlesByTopic(List articles) throws ZenoException {
		//articles must be sorted by topic
		
		List collections = new ArrayList();
		Iterator it = articles.iterator();
		int lpid = -1;
		ArticleCollection collection = null;
		while(it.hasNext()) {
			Article cart = (Article)it.next();
			//heg
			int cpid = ((Article)cart).getTopicId();
			if (lpid != cpid) {
				collection = new ArticleCollection();
				collections.add(collection);
				lpid = cpid;
			}
			if (cart instanceof Topic)
				collection.setTopic((Topic)cart);
			else
			 	collection.add(cart);
		}
		return collections;
	}
	
	public static List addTopics(List artCollections) throws ZenoException {
		List topics = new ArrayList();
		Iterator cit = artCollections.iterator();
		while(cit.hasNext()) {
			ArticleCollection artcoll = (ArticleCollection)cit.next();
			if (artcoll.getTopic() == null) {
				List articles = artcoll.getArticles();
				if (!articles.isEmpty()) {
					Topic topic = ((Article)articles.get(0)).getTopic();
					artcoll.setTopic(topic);
					topics.add(topic);
				}
			}
		}
		return topics;
	}
	
	public static List addAll(List artCollections) {
		List result = new ArrayList();
		Iterator cit = artCollections.iterator();
		while(cit.hasNext()) {
			ArticleCollection artcoll = (ArticleCollection)cit.next();
			Topic topic = artcoll.getTopic();
			if (topic != null) 
				result.add(topic);
			result.addAll(artcoll.getArticles());
		}
		return result;
	}
	
	public static List addAll2(List artCollections) {
		List result = new ArrayList();
		List bound = new ArrayList();
		result.add(bound);
		List free = new ArrayList();
		result.add(free);
		Iterator cit = artCollections.iterator();
		while(cit.hasNext()) {
			ArticleCollection artcoll = (ArticleCollection)cit.next();
			Topic topic = artcoll.getTopic();
			if (topic != null) {
				bound.add(topic);
				bound.addAll(artcoll.getArticles());
			} else
			 free.addAll(artcoll.getArticles());                                                      
		}
		return result;
	}
	
	public static List addAll3(List artCollections, List topics) {
		List result = new ArrayList();
		List bound = new ArrayList();
		result.add(bound);
		List free = new ArrayList();
		result.add(free);
		Iterator cit = artCollections.iterator();
		while(cit.hasNext()) {
			ArticleCollection artcoll = (ArticleCollection)cit.next();
			Topic topic = artcoll.getTopic();
			if (topic != null) {
				List artgroup = new ArrayList();
				bound.add(artgroup);
				if (topics.contains(topic))
					artgroup.add(Boolean.FALSE);
				else
					artgroup.add(Boolean.TRUE);
				artgroup.add(topic);
				artgroup.addAll(artcoll.getArticles());
			} else
				free.addAll(artcoll.getArticles());                                                      
		}
		return result;
	}
	
	public static void sort(List artCollections, String mode) {
		if ("byJournal".equals(mode))
			Collections.sort(artCollections, new JournalComparator());
		if ("byTopic".equals(mode))
			Collections.sort(artCollections, new TopicComparator());
	}
	
	
	private static int stringCompare(String a1, String a2) {
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
	
	static class JournalComparator implements Comparator {
			
		public int compare (Object o1, Object o2) {
			int result = 0;
			if (o1 instanceof ArticleCollection  
					&& o2 instanceof ArticleCollection) {
				ArticleCollection ac1 = (ArticleCollection) o1;
				ArticleCollection ac2 = (ArticleCollection) o2;
				try {
					String title1 = ac1.getJournal().getTitle();
					String title2 = ac2.getJournal().getTitle();
					result = stringCompare(title1, title2);
				} catch(ZenoException e) {}
			} 
			return result;
		}
		
	}
	
	static class TopicComparator implements Comparator {
		
		public int compare (Object o1, Object o2) {
			int result = 0;
			if (o1 instanceof ArticleCollection  
					&& o2 instanceof ArticleCollection) {
				ArticleCollection ac1 = (ArticleCollection) o1;
				ArticleCollection ac2 = (ArticleCollection) o2;
				Topic top1 = ac1.getTopic();
				Topic top2 = ac2.getTopic();
				if (top1 == top2) return 0;
				if (top1 == null) return 1;
				if (top2 == null) return -1;
				try {
					int rank1 = top1.getRank();
					int rank2 = top2.getRank();
					if (rank1 == rank2) {
						String title1 = top1.getTitle();
						String title2 = top2.getTitle();
						return stringCompare(title1, title2);
					} else
						return rank1 - rank2;
				} catch(ZenoException e) {}
			} 
			return result;
		}
		
	}
	
	
}