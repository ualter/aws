package com.amazonaws.sandbox.iam;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.auth.PropertiesFileCredentialsProvider;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.Region;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import com.amazonaws.services.securitytoken.model.AssumeRoleRequest;
import com.amazonaws.services.securitytoken.model.AssumeRoleResult;
import com.amazonaws.services.securitytoken.model.Credentials;
import com.amazonaws.services.securitytoken.model.GetSessionTokenRequest;
import com.amazonaws.services.securitytoken.model.GetSessionTokenResult;

public class IAMSandbox {
	
	public static void main(String[] args) throws Exception {
		System.out.println("TOKEN....:");
		System.out.println(
				//testSTSToken("arn:aws:iam::933272457605:role/role-tasadora", 3600)
				testSTSToken("arn:aws:iam::933272457605:role/role-tasadora", new Integer(3600))
		);
		System.out.println("\nOK\n");
	}
	
	
	private static String testSTSToken(String roleArn, Integer timeInSeconds) throws FileNotFoundException, IOException {
		File fileCredentials = new File(IAMSandbox.class.getClassLoader().getResource("tasadora-alpha-credentials.properties").getFile());
				
		AWSCredentials longTermCredentials = new PropertiesCredentials(fileCredentials);
		AWSSecurityTokenService stsClient = AWSSecurityTokenServiceClientBuilder.standard()
											.withCredentials(new AWSStaticCredentialsProvider(longTermCredentials))
											.withRegion(Region.EU_Ireland.name())
											.build();
		AssumeRoleRequest assumeRequest = new AssumeRoleRequest()
				.withRoleArn(roleArn)
	            .withDurationSeconds(timeInSeconds)
	            .withRoleSessionName("ujr");
		
		AssumeRoleResult assumeResult = stsClient.assumeRole(assumeRequest);
		
		
		BasicSessionCredentials temporaryCredentials = new BasicSessionCredentials(
																	               assumeResult.getCredentials().getAccessKeyId(),
																	               assumeResult.getCredentials().getSecretAccessKey(),
																	               assumeResult.getCredentials().getSessionToken());
		return temporaryCredentials.getSessionToken();
		
	}
	
	
	public static BasicSessionCredentials getTasadorasToken(String user, String loginCitrixTasadora, String passwordCitrixTasadora) throws FileNotFoundException, IOException {
		// The parameters here: loginCitrixTasadora and passwordCitrixTasadora do not have any utility!!!
		// It is just a "rehearsal" that to delivery a Token for a Tasadora, this Tasadora must prove its identity to the Token Generator (Gestor Tasaciones)
		
		PropertiesFileCredentialsProvider awsCredentials = buildCredentials(user);
		
		
		AWSSecurityTokenService stsClient = AWSSecurityTokenServiceClientBuilder.standard()
											.withRegion(Regions.US_WEST_2.name())
											.withCredentials(awsCredentials)
											.build();
		
		AssumeRoleRequest assumeRoleRequest = new AssumeRoleRequest()
				.withRoleArn("arn:aws:iam::933272457605:role/role-tasadora")
				.withDurationSeconds(new Integer(3600))
	            //.withDurationSeconds(3600)
	            .withRoleSessionName("tasadoras");
		stsClient.assumeRole(assumeRoleRequest);
		
		
		GetSessionTokenRequest getSessionTokenRequest = new GetSessionTokenRequest();
		getSessionTokenRequest.setDurationSeconds(new Integer(3600));
		GetSessionTokenResult sessionTokenResult = stsClient.getSessionToken(getSessionTokenRequest);
		Credentials credentials = sessionTokenResult.getCredentials();
		
		BasicSessionCredentials temporaryCredentials = new BasicSessionCredentials(credentials.getAccessKeyId(),
				                                                                   credentials.getSecretAccessKey(),
				                                                                   credentials.getSessionToken());
		return temporaryCredentials;
	}
	
	
	private static PropertiesFileCredentialsProvider buildCredentials(String usersName) {
		File credentialsFilePath = new 
				File(IAMSandbox.class.getClassLoader().getResource(usersName + "-credentials.properties").getFile());
		PropertiesFileCredentialsProvider propertiesFileCredentialsProvider = new PropertiesFileCredentialsProvider(credentialsFilePath.getPath());
		return propertiesFileCredentialsProvider;
	}
	
	@SuppressWarnings("unused")
	public void completeCircuitForScreenShotPurposes() {
		
		
		File                              credentialsFilePath = new File("/path/file/tasadora-alpha-credentials.properties");
		PropertiesFileCredentialsProvider awsCredentials      = new PropertiesFileCredentialsProvider(credentialsFilePath.getPath());
		AWSSecurityTokenService           stsClient           = AWSSecurityTokenServiceClientBuilder.standard()
																.withRegion(Regions.US_WEST_2.name())
																.withCredentials(awsCredentials)
																.build();
		
		
		AssumeRoleRequest assumeRoleRequest = new AssumeRoleRequest()
				.withRoleArn("arn:aws:iam::933272457605:role/role-tasadora")
				.withDurationSeconds(new Integer(3600))
	            .withRoleSessionName("tasadoras");
		stsClient.assumeRole(assumeRoleRequest);
		
		
		GetSessionTokenRequest getSessionTokenRequest = new GetSessionTokenRequest();
		GetSessionTokenResult  sessionTokenResult     = stsClient.getSessionToken(getSessionTokenRequest);
		Credentials            credentials            = sessionTokenResult.getCredentials();
		
		BasicSessionCredentials temporaryCredentials = new BasicSessionCredentials(credentials.getAccessKeyId(),
				                                                                   credentials.getSecretAccessKey(),
				                                                                   credentials.getSessionToken());
		
		
		
		
		BasicSessionCredentials myToken = new BasicSessionCredentials(temporaryCredentials.getAWSAccessKeyId(),
																	  temporaryCredentials.getAWSSecretKey(),
																	  temporaryCredentials.getSessionToken());
		
		EndpointConfiguration endpointConfiguration 
									= new AwsClientBuilder.EndpointConfiguration("http://bancsabadell.aws.s3.proxy.com","us-west-2");
		AmazonS3 s3 = AmazonS3ClientBuilder.standard()
		                                   .withCredentials(new AWSStaticCredentialsProvider(myToken))
								           .withEndpointConfiguration(endpointConfiguration)
								           .enablePathStyleAccess()
								           .build();
		//s3.getObject(...)
		//s3.putObject(...)
		
		
	}

}
