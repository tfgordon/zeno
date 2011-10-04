package zeno2.kernel;

import java.util.ArrayList;
import java.util.List;

/** Zeno2 OutlineNode. */

public class OutlineNode {
	String label;
	String position;
	String id;
	ZenoResource resource;
	String alias;
	String mark;
	boolean duplicate;
	boolean duplicated;
	String originalPosition;
	List children;


	public OutlineNode(String label, String id, String alias) {
		this.label = label;
		this.id = id;
		this.alias = alias;
		this.mark = "";
		this.duplicate = false;
		this.children = new ArrayList();
	}

	public OutlineNode(Link link, boolean source) {
		this.label = (source ? "" : "-") + link.getLabel();
		this.id = Integer.toString(source ? link.getSourceId() : link.getTargetId());
		this.alias = source ? link.getSourceAlias() : link.getTargetAlias();
		this.mark = "";
		this.duplicate = false;
		this.children = new ArrayList();
	}

	public String getLabel() {
		return this.label;
	}

	public String getId() {
		return this.id;
	}
	
	public String getPosition() {
		return position;
	}
	
	public void setPosition(String position) {
		this.position = position;
	}
	
	public String getOriginalPosition() {
		return originalPosition;
	}
	
	public void setOriginalPosition(String position) {
		this.originalPosition = position;
	}
	
	public ZenoResource getResource() {
		return resource;
	}
	
	public void setResource(ZenoResource resource) {
		this.resource = resource;
	}

	public String getAlias() {
		return this.alias;
	}
	
	public String getMark() {
		return this.mark;
	}

	public boolean getIsDuplicate() {
		return this.duplicate;
	}

	public void setDuplicate(boolean duplicate) {
		this.duplicate = duplicate;
	}
	
	public boolean isDuplicated() {
		return this.duplicated;
	}

	public void setDuplicated(boolean duplicated) {
		this.duplicated = duplicated;
	}

	public List getChildren() {
		return this.children;
	}
	
	public void add(OutlineNode child) {
		children.add(child);
	}
	
	public void genMark(Article root) throws ZenoException {
		if (resource == null || root == null)
			;
		else if (resource instanceof Journal)
			mark = "jl";
		else if (resource.getParentId() != root.getParentId())
			mark = "jc";
		else if (((Article)resource).getTopicId() != ((Article)root).getTopicId())
			mark = "tc";
	}

	public String toString() {
		String result = position + "  " + mark + " " + label + "  " + id + " " + alias;
		if (duplicated)
			result = result + " repeated";
		if (duplicate)
			result = result + "  repetition " + originalPosition;
		//if (resource != null)
		//	result = result + "  " + resource.toString();
		return result;
	}

}