package com.amazonaws.sandbox.s3.multipart.lowlevel;

import java.io.File;
import java.util.List;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.PropertiesFileCredentialsProvider;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.retry.PredefinedRetryPolicies;
import com.amazonaws.sandbox.iam.IAMSandbox;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.AbortMultipartUploadRequest;
import com.amazonaws.services.s3.model.ListMultipartUploadsRequest;
import com.amazonaws.services.s3.model.MultipartUpload;
import com.amazonaws.services.s3.model.MultipartUploadListing;

public class S3AbortMultipartUploads {
	
	public static void main(String[] args) {
		S3AbortMultipartUploads abortUploads = new S3AbortMultipartUploads("tasadoratest");
		abortUploads.abortAllUploads();
	}
	
	private String bucketName;
	private AmazonS3 s3;

	public S3AbortMultipartUploads(String bucketName) {
		super();
		this.bucketName = bucketName;
		startS3ClientConnection();
	}
	
	
	public void abortAllUploads() {
		// Find all in-progress multipart uploads.
        ListMultipartUploadsRequest allMultipartUploadsRequest = new ListMultipartUploadsRequest(bucketName);
        MultipartUploadListing multipartUploadListing = s3.listMultipartUploads(allMultipartUploadsRequest);

        List<MultipartUpload> uploads = multipartUploadListing.getMultipartUploads();
        System.out.println("Before deletions, " + uploads.size() + " multipart uploads in progress.");

        // Abort each upload.
        for (MultipartUpload u : uploads) {
            System.out.println("Upload in progress: Key = \"" + u.getKey() + "\", id = " + u.getUploadId());    
            s3.abortMultipartUpload(new AbortMultipartUploadRequest(bucketName, u.getKey(), u.getUploadId()));
            System.out.println("Upload deleted: Key = \"" + u.getKey() + "\", id = " + u.getUploadId());
        }

        // Verify that all in-progress multipart uploads have been aborted.
        multipartUploadListing = s3.listMultipartUploads(allMultipartUploadsRequest);
        uploads = multipartUploadListing.getMultipartUploads();
        System.out.println("After aborting uploads, " + uploads.size() + " multipart uploads in progress.");
		
	}
	
	private void startS3ClientConnection() {
		File credentialsFilePath = new File(IAMSandbox.class.getClassLoader().getResource("tasadora-test-credentials.properties").getFile());
		PropertiesFileCredentialsProvider propertiesFileCredentialsProvider = new PropertiesFileCredentialsProvider(credentialsFilePath.getPath());
		
		ClientConfiguration clientConfiguration=new ClientConfiguration();
		clientConfiguration.setMaxConnections(10);
		clientConfiguration.setProtocol(Protocol.HTTPS);
		// Change the default setting of 3 retry attempts to 5
		clientConfiguration.setRetryPolicy(PredefinedRetryPolicies.getDefaultRetryPolicyWithCustomMaxRetries(5));
		
		EndpointConfiguration endpointConfiguration =
				new AwsClientBuilder.EndpointConfiguration("https://s3-eu-west-1.amazonaws.com","eu-west-1");
		
		this.s3 = AmazonS3ClientBuilder.standard()
				      .withCredentials(propertiesFileCredentialsProvider)
				      .withEndpointConfiguration(endpointConfiguration)
				      .withClientConfiguration(clientConfiguration)
				      .enablePathStyleAccess()
				      .build();
	}

}
