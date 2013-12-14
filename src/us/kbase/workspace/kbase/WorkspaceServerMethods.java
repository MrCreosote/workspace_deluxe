package us.kbase.workspace.kbase;

import static us.kbase.common.utils.ServiceUtils.checkAddlArgs;
import static us.kbase.workspace.kbase.KBaseIdentifierFactory.processWorkspaceIdentifier;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import us.kbase.common.service.Tuple11;
import us.kbase.common.service.Tuple8;
import us.kbase.typedobj.core.TypeDefId;
import us.kbase.typedobj.exceptions.BadJsonSchemaDocumentException;
import us.kbase.typedobj.exceptions.InstanceValidationException;
import us.kbase.typedobj.exceptions.TypeStorageException;
import us.kbase.typedobj.exceptions.TypedObjectValidationException;
import us.kbase.workspace.CreateWorkspaceParams;
import us.kbase.workspace.ObjectSaveData;
import us.kbase.workspace.SaveObjectsParams;
import us.kbase.workspace.database.ObjectIDNoWSNoVer;
import us.kbase.workspace.database.ObjectInformation;
import us.kbase.workspace.database.Permission;
import us.kbase.workspace.database.Provenance;
import us.kbase.workspace.database.WorkspaceIdentifier;
import us.kbase.workspace.database.WorkspaceInformation;
import us.kbase.workspace.database.WorkspaceUser;
import us.kbase.workspace.database.exceptions.CorruptWorkspaceDBException;
import us.kbase.workspace.database.exceptions.NoSuchObjectException;
import us.kbase.workspace.database.exceptions.NoSuchWorkspaceException;
import us.kbase.workspace.database.exceptions.PreExistingWorkspaceException;
import us.kbase.workspace.database.exceptions.WorkspaceCommunicationException;
import us.kbase.workspace.exceptions.WorkspaceAuthorizationException;
import us.kbase.workspace.workspaces.WorkspaceSaveObject;
import us.kbase.workspace.workspaces.Workspaces;

public class WorkspaceServerMethods {
	
	final Workspaces ws;
	final ArgUtils au = new ArgUtils();
	
	public WorkspaceServerMethods(final Workspaces ws) {
		this.ws = ws;
	}
	
	public Tuple8<Long, String, String, String, Long, String, String, String> createWorkspace(
			final CreateWorkspaceParams params, final WorkspaceUser user)
			throws PreExistingWorkspaceException,
			WorkspaceCommunicationException, CorruptWorkspaceDBException {
		checkAddlArgs(params.getAdditionalProperties(), params.getClass());
		Permission p = au.getGlobalWSPerm(params.getGlobalread());
		final WorkspaceInformation meta = ws.createWorkspace(user,
				params.getWorkspace(), p.equals(Permission.READ),
				params.getDescription());
		return au.wsInfoToTuple(meta);
	}
	

	public List<Tuple11<Long, String, String, String, Long, String, Long, String, String, Long, Map<String, String>>> saveObjects(
			final SaveObjectsParams params, final WorkspaceUser user)
			throws ParseException, WorkspaceCommunicationException,
			WorkspaceAuthorizationException, NoSuchObjectException,
			CorruptWorkspaceDBException, NoSuchWorkspaceException,
			TypedObjectValidationException, TypeStorageException,
			BadJsonSchemaDocumentException, InstanceValidationException {

		checkAddlArgs(params.getAdditionalProperties(), params.getClass());
		final WorkspaceIdentifier wsi = processWorkspaceIdentifier(
				params.getWorkspace(), params.getId());
		final List<WorkspaceSaveObject> woc = new ArrayList<WorkspaceSaveObject>();
		int count = 1;
		if (params.getObjects().isEmpty()) {
			throw new IllegalArgumentException("No data provided");
		}
		for (ObjectSaveData d: params.getObjects()) {
			checkAddlArgs(d.getAdditionalProperties(), d.getClass());
			ObjectIDNoWSNoVer oi = null;
			if (d.getName() != null || d.getObjid() != null) {
				 oi = ObjectIDNoWSNoVer.create(d.getName(), d.getObjid());
			}
			String errprefix = "Object ";
			if (oi == null) {
				errprefix += count;
			} else {
				errprefix += count + ", " + oi.getIdentifierString() + ",";
			}
			if (d.getData() == null) {
				throw new IllegalArgumentException(errprefix + " has no data");
			}
			TypeDefId t;
			try {
				t = TypeDefId.fromTypeString(d.getType());
			} catch (IllegalArgumentException iae) {
				throw new IllegalArgumentException(errprefix + " type error: "
						+ iae.getLocalizedMessage(), iae);
			}
			final Provenance p = au.processProvenance(user,
					d.getProvenance());
			final boolean hidden = au.longToBoolean(d.getHidden());
			try {
				if (oi == null) {
					woc.add(new WorkspaceSaveObject(d.getData().asJsonNode(),
							t, d.getMeta(), p, hidden));
				} else {
					woc.add(new WorkspaceSaveObject(oi,
							d.getData().asJsonNode(), t, d.getMeta(), p,
							hidden));
				}
			} catch (IllegalArgumentException iae) {
				throw new IllegalArgumentException(errprefix + " save error: "
						+ iae.getLocalizedMessage(), iae);
			}
			count++;
		}
		params.setObjects(null); // garbage collect the objects, although
		// just passing a pointer around so no biggie
		// setting params = null won't help since the method caller still has a ref
		
		final List<ObjectInformation> meta = ws.saveObjects(user, wsi, woc); 
		return au.objInfoToTuple(meta);
	}

}
