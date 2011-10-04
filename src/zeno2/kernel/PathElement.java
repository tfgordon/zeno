package zeno2.kernel;

/** Zeno2 PathElement. */

public class PathElement {
	int id;
	String title;
	String zenoClass;

	public PathElement(int id, String title, String zenoClass) {
		this.id = id;
		this.title = title;
		this.zenoClass = zenoClass;
	}

	public int getId() {
		return this.id;
	}

	public String getTitle() {
		return this.title;
	}

	public String getZenoClass() {
		return this.zenoClass;
	}

	public String toString() {
		return "[" + id + "  " + title + "   " + zenoClass + "]";
	}
}