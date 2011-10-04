package zeno2.db;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;
import zeno2.kernel.Collective;
import zeno2.kernel.ZenoException;
import zeno2.kernel.NoPermissionException;

public class DBCollectiveImpl extends DBPrincipalImpl 
										implements Collective {

	public DBCollectiveImpl(String id, PrincipalFactory factory) {
		super(id, factory);
	}
	
	public String getMembershipCriterion() throws ZenoException {
		String criterion = 
			factory.getProperty(this.id, "system", "membership");
		if (criterion == null) 
			criterion = "";
		return criterion;
	}
	
	public void setMembershipCriterion(String criterion) throws ZenoException {
		factory.setProperty(this.id, "system", "membership", criterion);
	}
	

}

