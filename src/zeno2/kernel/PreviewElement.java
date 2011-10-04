package zeno2.kernel;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

/** Zeno2 PreviewElement. */

public class PreviewElement {
		int level;
		String operation;
		ZenoResource resource;
		String result;
		List editors;
		List subPreview;
	
	public PreviewElement(ZenoResource resource) {
		this.operation = "";
		this.resource = resource;
	}
	
	public PreviewElement(String operation, ZenoResource resource) {
		this.operation = operation;
		this.resource = resource;
	}
	
	/*
	public PreviewElement(ZenoResource resource, int level) {
		this.operation = "";
		this.resource = resource;
		this.level = level;
	}
	
	public PreviewElement(String operation, ZenoResource resource, int level) {
		this.operation = operation;
		this.resource = resource;
		this.level = level;
	}
	
	public int getLevel() {
		return level;
	}
	
	*/
	

	public String getOperation() {
		return operation;
	} 
	
		
	public ZenoResource getResource() {
		return resource;
	}
	
	public String getResult() {
		return result;
	} 
		
	public List getEditors() {
		if (editors == null)
			return Collections.EMPTY_LIST;
		else
			return editors;
	}
	
	public List getSubPreview() {
		if (subPreview == null)
			return Collections.EMPTY_LIST;
		else
			return subPreview;
	}
	
	public int subPreviewSize() {
		return subPreview.size();
	}
	
	public void add(PreviewElement element) {
		if (subPreview == null)
			subPreview = new ArrayList();
		subPreview.add(element);
	}
		
	public void setLevel(int level) {
		this.level = level;
	}
	
		
	public void setOperation(String operation) {
		this.operation = operation;
	}
	
		
	public void setResource(ZenoResource resource) {
		this.resource = resource;
	}
	
	public void setResult(String result) {
		this.result = result;
	}
		
		
	public void setEditors(List editors) {
		this.editors = editors;
	}
		
		
		
}