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
@JsonPropertyOrder({ "configurationId", "bucket", "object", "s3SchemaVersion" })
public class S3 {

	@JsonProperty("configurationId")
	private String configurationId;
	@JsonProperty("bucket")
	private Bucket bucket;
	@JsonProperty("object")
	private ujr.aws.lambda.event.handler.model.event.Object object;
	@JsonProperty("s3SchemaVersion")
	private String s3SchemaVersion;
	@JsonIgnore
	private Map<String, java.lang.Object> additionalProperties = new HashMap<String, java.lang.Object>();

	@JsonProperty("configurationId")
	public String getConfigurationId() {
		return configurationId;
	}

	@JsonProperty("configurationId")
	public void setConfigurationId(String configurationId) {
		this.configurationId = configurationId;
	}

	@JsonProperty("bucket")
	public Bucket getBucket() {
		return bucket;
	}

	@JsonProperty("bucket")
	public void setBucket(Bucket bucket) {
		this.bucket = bucket;
	}

	@JsonProperty("object")
	public ujr.aws.lambda.event.handler.model.event.Object getObject() {
		return object;
	}

	@JsonProperty("object")
	public void setObject(ujr.aws.lambda.event.handler.model.event.Object object) {
		this.object = object;
	}

	@JsonProperty("s3SchemaVersion")
	public String getS3SchemaVersion() {
		return s3SchemaVersion;
	}

	@JsonProperty("s3SchemaVersion")
	public void setS3SchemaVersion(String s3SchemaVersion) {
		this.s3SchemaVersion = s3SchemaVersion;
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