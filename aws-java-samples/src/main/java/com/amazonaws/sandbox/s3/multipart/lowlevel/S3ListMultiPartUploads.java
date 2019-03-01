package com.amazonaws.sandbox.s3.multipart.lowlevel;

import java.io.File;
import java.util.List;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.PropertiesFileCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.retry.PredefinedRetryPolicies;
import com.amazonaws.sandbox.iam.IAMSandbox;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ListMultipartUploadsRequest;
import com.amazonaws.services.s3.model.MultipartUpload;
import com.amazonaws.services.s3.model.MultipartUploadListing;

public class S3ListMultiPartUploads {
	
	public static void main(String[] args) {
		S3ListMultiPartUploads listUploads = new S3ListMultiPartUploads("tasadora-test", false);
		listUploads.startListening();
	}
	
	private String bucket;
	private AmazonS3 s3;
	private boolean endListening = false;
	private boolean listWhileInProcess = false;
	
	public S3ListMultiPartUploads(String bucket, boolean listWhileInProcess) {
		this.bucket = bucket;
		this.listWhileInProcess = listWhileInProcess;
	}
	
	public void startListening() {
		
		if ( s3 == null ) {
			this.startS3ClientConnection();
		}
		
		// Retrieve a list of all in-progress multipart uploads.
        ListMultipartUploadsRequest allMultipartUploadsRequest = new ListMultipartUploadsRequest(this.bucket);
        MultipartUploadListing multipartUploadListing = s3.listMultipartUploads(allMultipartUploadsRequest);
        
        List<MultipartUpload>  uploads = multipartUploadListing.getMultipartUploads();
        
        // Display information about all in-progress multipart uploads.
        System.out.println(uploads.size() + " multipart upload(s) in progress.");
        while( uploads.size() > 0 && !endListening) {
            for (MultipartUpload u : uploads) {
                //System.out.println("Upload in progress: Key = \"" + u.getKey() + "\", id = " + u.getUploadId());
            	System.out.println("Upload in progress: Key = \"" + u.getKey() + "\"" + ", UploadId:" + u.getUploadId());
            }
            
            if ( !this.listWhileInProcess ) {
            	break;
            }
            
            multipartUploadListing = s3.listMultipartUploads(allMultipartUploadsRequest);
            uploads = multipartUploadListing.getMultipartUploads();
		}
	}

	private void startS3ClientConnection() {
		File credentialsFilePath = new File(IAMSandbox.class.getClassLoader().getResource("tasadora-test-credentials.properties").getFile());
		PropertiesFileCredentialsProvider propertiesFileCredentialsProvider = new PropertiesFileCredentialsProvider(credentialsFilePath.getPath());
		
		ClientConfiguration clientConfiguration=new ClientConfiguration();
		clientConfiguration.setMaxConnections(10);
		// Change the default setting of 3 retry attempts to 5
		clientConfiguration.setRetryPolicy(PredefinedRetryPolicies.getDefaultRetryPolicyWithCustomMaxRetries(5));
		
		//EndpointConfiguration endpointConfiguration =
		//		new AwsClientBuilder.EndpointConfiguration("https://tasadora-test.s3-eu-west-1.amazonaws.com","eu-west-1");
		
		this.s3 = AmazonS3ClientBuilder.standard()
				      .withCredentials(propertiesFileCredentialsProvider)
				      //.withEndpointConfiguration(endpointConfiguration)
				      .withRegion(Regions.EU_WEST_1)
				      .withClientConfiguration(clientConfiguration)
				      .enablePathStyleAccess()
				      .build();
	}

}
