package us.kbase.typedobj.core;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import us.kbase.typedobj.core.ReportUtil.IdForValidation;
import us.kbase.typedobj.core.validatorconfig.WsIdRefValidationBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jsonschema.report.LogLevel;
import com.github.fge.jsonschema.report.ProcessingMessage;
import com.github.fge.jsonschema.report.ProcessingReport;


/**
 * The report generated when a typed object instance is validated.  If the type definition indicates
 * that fields are ID references, those ID references can be extracted.
 *
 */
public class TypedObjectValidationReport {

	private final static int EXPECTED_NUMBER_OF_IDS = 25;
	
	
	protected ProcessingReport processingReport;
	
	private final AbsoluteTypeDefId validationTypeDefId;
	
	private List<IdReference> idReferences;
	
	private boolean idRefListIsBuilt;
	
	
	public TypedObjectValidationReport(ProcessingReport processingReport, AbsoluteTypeDefId validationTypeDefId) {
		this.processingReport=processingReport;
		this.idRefListIsBuilt=false;
		this.validationTypeDefId=validationTypeDefId;
	}
	
	
	/**
	 * Get the absolute ID of the typedef that was used to validate the instance
	 */
	public AbsoluteTypeDefId getValidationTypeDefId() {
		return validationTypeDefId;
	}
	
	/**
	 * @return boolean true if the instance is valid, false otherwise
	 */
	public boolean isInstanceValid() {
		return processingReport.isSuccess();
	}
	
	/**
	 * Iterate over all items in the report and count the errors.
	 * @return n_errors
	 */
	public int getErrorCount() {
		if(isInstanceValid()) { return 0; }
		Iterator<ProcessingMessage> mssgs = processingReport.iterator();
		int n_errors=0;
		while(mssgs.hasNext()) {
			ProcessingMessage pm = mssgs.next();
			if(pm.getLogLevel().equals(LogLevel.ERROR)) {
				n_errors++;
			}
		}
		return n_errors;
	}
	
	/**
	 * Iterate over all items in the report and return the error messages.
	 * @return n_errors
	 */
	public String [] getErrorMessages() {
		if(isInstanceValid()) { return new String[0]; }
		
		Iterator<ProcessingMessage> mssgs = processingReport.iterator();
		ArrayList <String> errMssgs = new ArrayList<String>();
		
		while(mssgs.hasNext()) {
			ProcessingMessage pm = mssgs.next();
			if(pm.getLogLevel().equals(LogLevel.ERROR)) {
				errMssgs.add(pm.getMessage());
			}
		}
		return errMssgs.toArray(new String[errMssgs.size()]);
	}
	
	
	
	/**
	 * This method returns the raw report generated by the json schema, useful in some cases if
	 * you need to dig down into the guts of keywords or to investigate why something failed.
	 */
	public ProcessingReport getRawProcessingReport() {
		return processingReport;
	}
	
	
	
	/**
	 * Returns the full information about each ID Reference, e.g. info on where in the instance it was located
	 * @return
	 */
	public List<IdReference> getListOfIdReferenceObjects() {
		if(!idRefListIsBuilt) {
			buildIdList();
		}
		return this.idReferences;
	}
	
	/**
	 * Return a list of all fields that were flagged as ID References
	 * @return
	 */	
	public String [] getListOfIdReferences() {
		if(!idRefListIsBuilt) {
			buildIdList();
		}
		String [] idRefOnly = new String[idReferences.size()];
		for(int i=0; i<idReferences.size(); i++) {
			idRefOnly[i] = idReferences.get(i).getIdReference();
		}
		return idRefOnly;
	}
	
	
	/**
	 * Set the absolute ID References as specified in the absoluteIdRefMapping (original ids
	 * are keys, replacement absolute IDs are values)
	 */
	public void setAbsoluteIdReferences(Map<String,String> absoluteIdRefMapping) {
		//@todo implement
	}
	
	
	@Override
	public String toString() {
		// temp hack, just return what the processing report says
		//@TODO make nicer string version of TypedObjectValidationReport
		return processingReport.toString();
	}
	
	
	
	/**
	 * given the internal processing report, compute the list of IDs
	 */
	protected void buildIdList() {
		Iterator<ProcessingMessage> mssgs = processingReport.iterator();
		this.idReferences = new ArrayList<IdReference>(EXPECTED_NUMBER_OF_IDS);
		while(mssgs.hasNext()) {
			ProcessingMessage m = mssgs.next();
			if( m.getMessage().compareTo(WsIdRefValidationBuilder.keyword) != 0 ) {
				continue;
			}
			String id = m.asJson().get("id").asText();
			JsonNode typeNames = m.asJson().get("type");
			ArrayList<TypeDefId> typesList = new ArrayList<TypeDefId>(typeNames.size());
			for(int k=0; k<typeNames.size(); k++) {
				String fullName = typeNames.get(k).asText();
				typesList.add(new TypeDefId(new TypeDefName(fullName)));
			}
			//@todo reconstruct path to the node
			IdReference idRef = new IdReference("unknown",id,typesList);
			idReferences.add(idRef);
			System.out.println(idRef);
		}
		idRefListIsBuilt=true;
	}
	
	
	
	
	
	
	
	
	
}