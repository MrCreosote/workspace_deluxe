package us.kbase.workspace.database;

import us.kbase.common.service.KBaseObjectMapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class WorkspaceObjectData {
	
	private static final ObjectMapper MAPPER = new KBaseObjectMapper();

	private final JsonNode data;
	private final ObjectInfoUserMeta meta;
	private final Provenance prov;

	public WorkspaceObjectData(final JsonNode data,
			final ObjectInfoUserMeta meta, final Provenance prov) {
		if (data == null || meta == null || prov == null) {
			throw new IllegalArgumentException(
					"data, prov and meta cannot be null");
		}
		this.data = data;
		this.meta = meta;
		this.prov = prov;
	}

	public JsonNode getDataAsJsonNode() {
		return data;
	}
	
	public Object getData() {
		try {
			return MAPPER.treeToValue(data, Object.class);
		} catch (JsonProcessingException jpe) {
			//this should never happen
			throw new RuntimeException("something's dun broke", jpe);
		}
	}

	public ObjectInfoUserMeta getMeta() {
		return meta;
	}

	public Provenance getProvenance() {
		return prov;
	}

	@Override
	public String toString() {
		return "WorkspaceObjectData [data=" + data + ", meta=" + meta
				+ ", prov=" + prov + "]";
	}
}
