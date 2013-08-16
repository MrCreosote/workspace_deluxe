package us.kbase.shock.client;

import java.io.IOException;
import java.util.Map;

import us.kbase.auth.AuthUser;
import us.kbase.auth.TokenExpiredException;
import us.kbase.shock.client.exceptions.ShockHttpException;
import us.kbase.shock.client.exceptions.ShockNodeDeletedException;
import us.kbase.shock.client.exceptions.UnvalidatedEmailException;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * <p>Represents a shock node <b>at the time the node was retrieved from shock</b>.
 * Later updates to the node, including updates made via this class' methods,
 * will not be reflected in the instance. To update the local representation
 * of the node {@link us.kbase.shock.client.BasicShockClient#getNode(ShockNodeId)
 * getNode()} must be called again.</p>
 * 
 * <p>This class is never instantiated manually.</p>
 * 
 * Note that mutating the return value of the {@link #getAttributes()} method
 * will alter this object's internal representation of the attributes.
 * 
 * @author gaprice@lbl.gov
 *
 */
@JsonIgnoreProperties({"relatives", "type", "indexes", "tags", "linkages"})
public class ShockNode extends ShockData {

	private Map<String, Object> attributes;
	@JsonProperty("file")
	private ShockFileInformation file;
	private ShockNodeId id;
	private ShockVersionStamp version;
	@JsonIgnore
	private BasicShockClient client;
	@JsonIgnore
	private boolean deleted = false;
	
	private ShockNode(){}
	
	//MUST add a client after object deserialization or many of the methods
	//below will fail
	void addClient(BasicShockClient client) {
		this.client = client;
	}
	
	private void checkDeleted() throws ShockNodeDeletedException {
		if (deleted) {
			throw new ShockNodeDeletedException();
		}
	}

	/**
	 * Get the shock node's attributes. Note mutating the attributes will
	 * mutate the internal representation of the attributes in this object.
	 * @return the shock node's attributes.
	 * @throws ShockNodeDeletedException if the {@link #delete()} method has been 
	 * previously called.
	 */
	public Map<String, Object> getAttributes() throws ShockNodeDeletedException {
		checkDeleted();
		return attributes;
	}

	/** 
	 * Proxy for {@link BasicShockClient#deleteNode(ShockNodeId) deleteNode()}. 
	 * Deletes this node on the server. All methods will throw an
	 * exception after this method is called.
	 * @throws ShockHttpException if the shock server couldn't delete the node.
	 * @throws IOException if an IO problem occurs.
	 * @throws ExpiredTokenException if the client's token has expired.
	 * @throws ShockNodeDeletedException if this method has already been 
	 * called on this object.
	 */
	public void delete() throws ShockHttpException, IOException,
			TokenExpiredException, ShockNodeDeletedException {
		client.deleteNode(getId());
		client = null; //remove ref to client
		deleted = true;
	}
	
	/**
	 * Proxy for {@link BasicShockClient#getACLs(ShockNodeId) getACLs()}.
	 * Returns all the access control lists (ACLs) for the node.
	 * @return the ACLs.
	 * @throws ShockHttpException if the shock server cannot retrieve the ACLs.
	 * @throws IOException if an IO problem occurs.
	 * @throws ExpiredTokenException if the client's token has expired.
	 * @throws ShockNodeDeletedException if the {@link #delete()} method has been 
	 * previously called.
	 */
	@JsonIgnore
	public ShockACL getACLs() throws ShockHttpException, IOException,
			TokenExpiredException, ShockNodeDeletedException {
		return client.getACLs(getId());
	}
	
	/**
	 * Proxy for {@link BasicShockClient#getACLs(ShockNodeId, ShockACLType)
	 * getACLs()}.
	 * Returns the requested access control list (ACL) for the node.
	 * @param acl the type of ACL to retrive from shock.
	 * @return an ACL.
	 * @throws ShockHttpException if the shock server cannot retrieve the ACL.
	 * @throws IOException if an IO problem occurs.
	 * @throws TokenExpiredException if the client's token has expired.
	 * @throws ShockNodeDeletedException if the {@link #delete()} method has been 
	 * previously called.
	 */
	@JsonIgnore
	public ShockACL getACLs(ShockACLType acl) throws ShockHttpException,
			IOException, TokenExpiredException, ShockNodeDeletedException {
		return client.getACLs(getId(), acl);
	}
	
	/**
	 * Proxy for {@link BasicShockClient#setNodeReadable(ShockNodeId, AuthUser)
	 * setNodeReadable()}.
	 * Adds the user to the node's read access control list (ACL).
	 * @param user the user to which read permissions shall be granted.
	 * @throws ShockHttpException if the read ACL could not be modified.
	 * @throws IOException if an IO problem occurs.
	 * @throws TokenExpiredException if the client's token has expired.
	 * @throws ShockNodeDeletedException if the {@link #delete()} method has been 
	 * previously called.
	 * @throws UnvalidatedEmailException if the <code>user</code>'s email
	 * address is unvalidated.
	 */
	@JsonIgnore
	public void setReadable(AuthUser user) throws ShockHttpException,
			IOException, TokenExpiredException, ShockNodeDeletedException,
			UnvalidatedEmailException {
		client.setNodeReadable(getId(), user);
	}
	
	/**
	 * Proxy for {@link BasicShockClient#setNodeWorldReadable(ShockNodeId)
	 * setNodeWorldReadable()}.
	 * Removes all users from the node's read access control list (ACL), thus
	 * making the node world readable.
	 * @throws ShockHttpException if the read ACL could not be modified.
	 * @throws IOException if an IO problem occurs.
	 * @throws TokenExpiredException if the client's token has expired.
	 * @throws ShockNodeDeletedException if the {@link #delete()} method has been 
	 * previously called.
	 */
	@JsonIgnore
	public void setWorldReadable() throws ShockHttpException,
			IOException, TokenExpiredException, ShockNodeDeletedException {
		client.setNodeWorldReadable(getId());
	}
	
	/**
	 * Proxy for {@link BasicShockClient#getFile(ShockNodeId) getFile()}.
	 * Gets the file stored at this shock node.
	 * @return the shock node's file.
	 * @throws ShockHttpException if the file could not be retrieved from shock.
	 * @throws IOException if an IO problem occurs.
	 * @throws TokenExpiredException if the client's token has expired.
	 * @throws ShockNodeDeletedException if the {@link #delete()} method has been 
	 * previously called.
	 */
	@JsonIgnore
	public byte[] getFile() throws ShockHttpException, IOException,
			TokenExpiredException, ShockNodeDeletedException {
		return client.getFile(getId());
	}
	
	/**
	 * Get information about the file stored at this node.
	 * @return file information.
	 * @throws ShockNodeDeletedException if the {@link #delete()} method has been 
	 * previously called.
	 */
	@JsonIgnore
	public ShockFileInformation getFileInformation() throws 
			ShockNodeDeletedException {
		checkDeleted();
		return file;
	}
	
	/**
	 * Get the id of this node.
	 * @return this node's id.
	 * @throws ShockNodeDeletedException if the {@link #delete()} method has been 
	 * previously called.
	 */
	public ShockNodeId getId() throws ShockNodeDeletedException {
		checkDeleted();
		return id;
	}
	
	/**
	 * Get the version of this node.
	 * @return this node's current version.
	 * @throws ShockNodeDeletedException if the {@link #delete()} method has been 
	 * previously called.
	 */
	public ShockVersionStamp getVersion() throws ShockNodeDeletedException {
		checkDeleted();
		return version;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "ShockNode [attributes=" + attributes + ", file=" + file
				+ ", id=" + id + ", version=" + version + ", deleted=" +
				deleted + "]";
	}
}
