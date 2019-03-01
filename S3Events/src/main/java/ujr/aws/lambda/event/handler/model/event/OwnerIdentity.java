package ujr.aws.lambda.event.handler.model.event;

import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "principalId" })
public class OwnerIdentity {

	@JsonProperty("principalId")
	private String principalId;
	@JsonIgnore
	private Map<String, java.lang.Object> additionalProperties = new HashMap<String, java.lang.Object>();

	@JsonProperty("principalId")
	public String getPrincipalId() {
		return principalId;
	}

	@JsonProperty("principalId")
	public void setPrincipalId(String principalId) {
		this.principalId = principalId;
	}

	@JsonAnyGetter
	public Map<String, java.lang.Object> getAdditionalProperties() {
		return this.additionalProperties;
	}

	@JsonAnySetter
	public void setAdditionalProperty(String name, java.lang.Object value) {
		this.additionalProperties.put(name, value);
	}

}