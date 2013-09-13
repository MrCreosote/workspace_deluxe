package us.kbase.workspace.database.mongo;

import java.util.Date;

import us.kbase.workspace.database.ObjectUserMetaData;
import us.kbase.workspace.database.WorkspaceUser;

public class MongoObjectUserMeta extends MongoObjectMeta implements
		ObjectUserMetaData {
	
	final private Object userMeta;
	
	public MongoObjectUserMeta(int id, String name, String typeString, Date createdDate,
			int version, WorkspaceUser creator, int workspaceid, String chksum,
			int size, Object userMeta) {
		super(id, name, typeString, createdDate, version, creator, workspaceid,
				chksum, size);
		//no error checking for now, add if needed
		this.userMeta = userMeta;
	}

	//meta is mutable
	@Override
	public Object getUserMetaData() {
		return userMeta;
	}
	
	@Override
	public String toString() {
		return "MongoObjectMeta [id=" + getObjectId() + ", name=" + 
				getObjectName() + ", type=" + getTypeString() + 
				", createdDate=" + getCreatedDate() + ", version="
				+ getVersion() + ", creator=" + getCreator() + ", workspaceId="
				+ getWorkspaceId() + ", chksum=" + getCheckSum() + 
				", userMeta=" + userMeta + "]";
	}

}
