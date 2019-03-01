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
@JsonPropertyOrder({ "name", "ownerIdentity", "arn" })
public class Bucket {

	@JsonProperty("name")
	private String name;
	@JsonProperty("ownerIdentity")
	private OwnerIdentity ownerIdentity;
	@JsonProperty("arn")
	private String arn;
	@JsonIgnore
	private Map<String, java.lang.Object> additionalProperties = new HashMap<String, java.lang.Object>();

	@JsonProperty("name")
	public String getName() {
		return name;
	}

	@JsonProperty("name")
	public void setName(String name) {
		this.name = name;
	}

	@JsonProperty("ownerIdentity")
	public OwnerIdentity getOwnerIdentity() {
		return ownerIdentity;
	}

	@JsonProperty("ownerIdentity")
	public void setOwnerIdentity(OwnerIdentity ownerIdentity) {
		this.ownerIdentity = ownerIdentity;
	}

	@JsonProperty("arn")
	public String getArn() {
		return arn;
	}

	@JsonProperty("arn")
	public void setArn(String arn) {
		this.arn = arn;
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