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
@JsonPropertyOrder({ "awsRegion", "eventName", "eventSource", "eventTime", "eventVersion", "requestParameters",
		"responseElements", "s3", "userIdentity" })
public class Record {

	@JsonProperty("awsRegion")
	private String awsRegion;
	@JsonProperty("eventName")
	private String eventName;
	@JsonProperty("eventSource")
	private String eventSource;
	@JsonProperty("eventTime")
	private String eventTime;
	@JsonProperty("eventVersion")
	private String eventVersion;
	@JsonProperty("requestParameters")
	private RequestParameters requestParameters;
	@JsonProperty("responseElements")
	private ResponseElements responseElements;
	@JsonProperty("s3")
	private S3 s3;
	@JsonProperty("userIdentity")
	private UserIdentity userIdentity;
	@JsonIgnore
	private Map<String, java.lang.Object> additionalProperties = new HashMap<String, java.lang.Object>();

	@JsonProperty("awsRegion")
	public String getAwsRegion() {
		return awsRegion;
	}

	@JsonProperty("awsRegion")
	public void setAwsRegion(String awsRegion) {
		this.awsRegion = awsRegion;
	}

	@JsonProperty("eventName")
	public String getEventName() {
		return eventName;
	}

	@JsonProperty("eventName")
	public void setEventName(String eventName) {
		this.eventName = eventName;
	}

	@JsonProperty("eventSource")
	public String getEventSource() {
		return eventSource;
	}

	@JsonProperty("eventSource")
	public void setEventSource(String eventSource) {
		this.eventSource = eventSource;
	}

	@JsonProperty("eventTime")
	public String getEventTime() {
		return eventTime;
	}

	@JsonProperty("eventTime")
	public void setEventTime(String eventTime) {
		this.eventTime = eventTime;
	}

	@JsonProperty("eventVersion")
	public String getEventVersion() {
		return eventVersion;
	}

	@JsonProperty("eventVersion")
	public void setEventVersion(String eventVersion) {
		this.eventVersion = eventVersion;
	}

	@JsonProperty("requestParameters")
	public RequestParameters getRequestParameters() {
		return requestParameters;
	}

	@JsonProperty("requestParameters")
	public void setRequestParameters(RequestParameters requestParameters) {
		this.requestParameters = requestParameters;
	}

	@JsonProperty("responseElements")
	public ResponseElements getResponseElements() {
		return responseElements;
	}

	@JsonProperty("responseElements")
	public void setResponseElements(ResponseElements responseElements) {
		this.responseElements = responseElements;
	}

	@JsonProperty("s3")
	public S3 getS3() {
		return s3;
	}

	@JsonProperty("s3")
	public void setS3(S3 s3) {
		this.s3 = s3;
	}

	@JsonProperty("userIdentity")
	public UserIdentity getUserIdentity() {
		return userIdentity;
	}

	@JsonProperty("userIdentity")
	public void setUserIdentity(UserIdentity userIdentity) {
		this.userIdentity = userIdentity;
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