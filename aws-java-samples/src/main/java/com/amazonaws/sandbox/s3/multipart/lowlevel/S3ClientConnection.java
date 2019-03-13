package com.amazonaws.sandbox.s3.multipart.lowlevel;

import java.io.File;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.PropertiesFileCredentialsProvider;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.retry.PredefinedRetryPolicies;
import com.amazonaws.sandbox.iam.IAMSandbox;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;

public class S3ClientConnection {
	
	
	public static AmazonS3 S3Client(String user) {
		File credentialsFilePath = new File(IAMSandbox.class.getClassLoader().getResource(user + "-credentials.properties").getFile());
		PropertiesFileCredentialsProvider propertiesFileCredentialsProvider = new PropertiesFileCredentialsProvider(credentialsFilePath.getPath());
		
		ClientConfiguration clientConfiguration=new ClientConfiguration();
		clientConfiguration.setMaxConnections(10);
		clientConfiguration.setProtocol(Protocol.HTTPS);
		// Change the default setting of 3 retry attempts to 5
		clientConfiguration.setRetryPolicy(PredefinedRetryPolicies.getDefaultRetryPolicyWithCustomMaxRetries(5));
		
		EndpointConfiguration endpointConfiguration =
				new AwsClientBuilder.EndpointConfiguration("https://s3-eu-west-1.amazonaws.com","eu-west-1");
		
		return AmazonS3ClientBuilder.standard()
				      .withCredentials(propertiesFileCredentialsProvider)
				      .withEndpointConfiguration(endpointConfiguration)
				      .withClientConfiguration(clientConfiguration)
				      .enablePathStyleAccess()
				      .build();
	}

}
