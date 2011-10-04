package zeno2.kernel;

/** Zeno 2 Link class */

public interface Link {

	/** Gets the label of the link. */

	public String getLabel();

	/** Gets the source alias of the link. */

	public String getSourceAlias();
	
	/** Gets the source alias of the link provided by the user. */

	public String getUserSourceAlias();

	/** Changes the source alias. Save to make persistent. */

	public void setSourceAlias(String title);

	/** Gets the target alias of the link. */

	public String getTargetAlias();
	
	/** Gets the target alias of the link provided by the user */

	public String getUserTargetAlias();

	/** Changes the target alias. Save to make persistent. */

	public void setTargetAlias(String title);

	/** Gets the source resource of the link. */

	public ZenoResource getSource() throws ZenoException;

	public int getSourceId();

	/** Gets the target resource of the link. */

	public ZenoResource getTarget() throws ZenoException;

	public int getTargetId();

	/** Saves changes. Raises an exception if the user has no permission
	or the transaction could not be completed.  */

	public void save() throws ZenoException;
}