package zeno2.kernel;

/** Zeno 2 external Link class */

public interface XLink {

	/** Gets the type of the external link. */

	public String getType();

	/** Gets the name of the external link. */

	public String getName();
	
	/** Changes the  name of the external link */

	public void setName(String name);

	/** Gets the source resource of the external link. */

	public ZenoResource getSource() throws ZenoException;

	public int getSourceId();

	/** Gets the reference of the external link. */

	public String getReference();

	/** Saves changes. Raises an exception if the user has no permission
	or the transaction could not be completed.  */

	public void save() throws ZenoException;
}