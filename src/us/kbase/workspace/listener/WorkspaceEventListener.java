package us.kbase.workspace.listener;

/** A listener for workspace events.
 * @author gaprice@lbl.gov
 * 
 * @see WorkspaceEventListenerFactory
 *
 */
public interface WorkspaceEventListener {
	
	/** Notification that a workspace was created.
	 * @param id the workspace ID.
	 */
	void createWorkspace(final long id);
	
	/** Notification that a workspace was cloned.
	 * @param id the workspace ID.
	 */
	void cloneWorkspace(final long id);

	/** Notification that a workspace's meta data was altered.
	 * @param id the id of the workspace.
	 */
	void setWorkspaceMetadata(long id);
	
	//TODO NOW add more events & test

}