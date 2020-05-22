package com.amazonaws.sandbox.cloudfront;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Properties;
import java.util.UUID;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.auth.PropertiesFileCredentialsProvider;
import com.amazonaws.monitoring.ApiCallAttemptMonitoringEvent;
import com.amazonaws.monitoring.MonitoringEvent;
import com.amazonaws.monitoring.MonitoringListener;
import com.amazonaws.services.cloudfront.AmazonCloudFront;
import com.amazonaws.services.cloudfront.AmazonCloudFrontClientBuilder;
import com.amazonaws.services.cloudfront.model.CreateInvalidationRequest;
import com.amazonaws.services.cloudfront.model.CreateInvalidationResult;
import com.amazonaws.services.cloudfront.model.GetDistributionRequest;
import com.amazonaws.services.cloudfront.model.GetDistributionResult;
import com.amazonaws.services.cloudfront.model.GetInvalidationRequest;
import com.amazonaws.services.cloudfront.model.GetInvalidationResult;
import com.amazonaws.services.cloudfront.model.InvalidationBatch;
import com.amazonaws.services.cloudfront.model.Paths;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import com.amazonaws.services.securitytoken.model.Credentials;
import com.amazonaws.services.securitytoken.model.GetSessionTokenRequest;
import com.amazonaws.services.securitytoken.model.GetSessionTokenResult;

public class CloudFrontApp {

	private static final String AWS_TOKEN_DIR   = ".awsToken";
	private static final String FILE_TOKEN_NAME = "aws-service-user-access-token.properties";
	private static final String DISTR_ID = "E35ICIHCD970XR";

	public static void main(String[] args) {
		new CloudFrontApp().queryDistribution();
		//System.out.println(new CloudFrontApp().invalidateCacheDistributionCheckProgressTokenCredentials());
		// System.out.println(new CloudFrontApp().invalidateCacheDistributionBasicCredentials());
		// System.out.println(new CloudFrontApp().invalidateCacheDistributionCheckProgressStandardCredentials());
	}

	/**
	 * Simple Invalidation, using default credentials
	 * 
	 * To access the AWS Service, this method will use the default credential 
	 * provider chain to find the Access Key and Secret Key, which is:
     *  - Environment Variables: AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY;
     *  - Java System Properties: aws.accessKeyId and aws.secretKey;
     *  - The default credential profile files: typically located at ~/.aws/credentials;
     *  
	 * @return
	 */
	@SuppressWarnings("unused")
	private String invalidateCacheDistributionCheckProgressStandardCredentials() {
		AmazonCloudFront awsCloudFronClient = AmazonCloudFrontClientBuilder
				.standard()
				.withRegion("eu-central-1")
				.withMonitoringListener(new MonitoringListener() {
					@Override
					public void handleEvent(MonitoringEvent event) {
						if (event instanceof ApiCallAttemptMonitoringEvent) {
							ApiCallAttemptMonitoringEvent apiCallEvent = ((ApiCallAttemptMonitoringEvent) event);
							System.out.print("HTTP " + apiCallEvent.getHttpStatusCode());
						}
					}
				}).build();

		InvalidationBatch invalidationBatch = new InvalidationBatch();
		String uuid = UUID.randomUUID().toString().replace("-", "");
		invalidationBatch.setCallerReference(uuid);
		Paths paths = new Paths();
		paths.setItems(Arrays.asList(new String[] { "/*" }));
		paths.setQuantity(1);
		invalidationBatch.setPaths(paths);

		String distributionId = "E2ZCBHZCQBEB2W";
		CreateInvalidationRequest createInvalidationRequest = new CreateInvalidationRequest(distributionId,
				invalidationBatch);
		CreateInvalidationResult createInvalidationResult = awsCloudFronClient
				.createInvalidation(createInvalidationRequest);
		String status = createInvalidationResult.getInvalidation().getStatus();
		
		// The invalidation process finishes here, returning this Status.
        // If you receive a HTTP 201 InProgress, that's done the Invalidation.
        //
        // The rest of the code ahead is only as way (testing purposes) to monitoring the status (progress)
        // of the invalidation while is being occurring.
		while (status.equalsIgnoreCase("InProgress")) {
			System.out.println(" - " + status + "...");
			GetInvalidationRequest getInvalidationRequest = new GetInvalidationRequest(distributionId,
					createInvalidationResult.getInvalidation().getId());
			GetInvalidationResult getInvalidationResult = awsCloudFronClient.getInvalidation(getInvalidationRequest);
			status = getInvalidationResult.getInvalidation().getStatus();
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return " - " + status + "     <----- END";
	}

	/**
	 * Create a STS Token
	 */
	private BasicSessionCredentials createToken() {
		ClientConfiguration clientConfiguration = new ClientConfiguration();
		clientConfiguration.setProtocol(Protocol.HTTPS);

		PropertiesFileCredentialsProvider awsCredentials = getUserServiceCredentials("aws-service-user");
		
		AWSSecurityTokenService stsClient = AWSSecurityTokenServiceClientBuilder
										   .standard()
										   .withClientConfiguration(clientConfiguration)
										   .withCredentials(awsCredentials)
										   .build();

		GetSessionTokenRequest getSessionTokenRequest = new GetSessionTokenRequest();
		/*
		 * The duration, in seconds, that the credentials should remain valid.
		 * Acceptable durations user sessions range from: 
		 *   - 900 seconds (15 minutes); to
		 *   - 129,600 seconds (36 hours); with 
		 *   - 43,200 seconds (12 hours) as the default. 
		 * Sessions for AWS account owners are restricted to a maximum of 3,600
		 * seconds (one hour). If the duration is longer than one hour, the session for
		 * AWS account owners defaults to one hour.
		 */
		getSessionTokenRequest.setDurationSeconds(3600);
		GetSessionTokenResult sessionTokenResult = stsClient.getSessionToken(getSessionTokenRequest);
		Credentials credentials = sessionTokenResult.getCredentials();

		BasicSessionCredentials temporaryCredentials = new BasicSessionCredentials(credentials.getAccessKeyId(),
				credentials.getSecretAccessKey(), credentials.getSessionToken());
		System.out.println("\n******* TEMPORARY SECURITY CREDENTIALS *******");
		System.out.println("|");
		System.out.println("| AccessKeyID..:" + temporaryCredentials.getAWSAccessKeyId());
		System.out.println("| SecretKey....:" + temporaryCredentials.getAWSSecretKey());
		System.out.println(
				"| --- BEGIN TOKEN ---\n" + temporaryCredentials.getSessionToken() + "\n| --- END TOKEN ---\n");

		return temporaryCredentials;

	}

	/**
	 * Load from file {username}-credentials.properties
	 * @param usersName
	 * @return
	 */
	private PropertiesFileCredentialsProvider getUserServiceCredentials(String usersName) {
		File credentialsFilePath = new File(
				Thread.currentThread().getContextClassLoader().getResource(usersName + "-credentials.properties").getFile());
		PropertiesFileCredentialsProvider awsCredentials = new PropertiesFileCredentialsProvider(
				credentialsFilePath.getPath());
		return awsCredentials;
	}

	/**
	 * Persistent the STS Token
	 */
	private void persistTemporaryCredentials(BasicSessionCredentials temporaryCredentials) {
		String path = System.getProperty("user.home") + File.separator + AWS_TOKEN_DIR;
		File customDir = new File(path);
		if (!customDir.exists()) {
			customDir.mkdirs();
		}
		
		String time = null;
		PrintWriter pw = null;
		try {
			
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:SS");
	        LocalDateTime formatDateTime = LocalDateTime.now();
	        time = formatter.format(formatDateTime);
	        
	        pw = new PrintWriter(path + File.separator + FILE_TOKEN_NAME, "UTF-8");
			pw.write("time = " + time + "\n");
			pw.write("aws_access_key_id = " + temporaryCredentials.getAWSAccessKeyId() + "\n");
			pw.write("aws_secret_access_key = " + temporaryCredentials.getAWSSecretKey() + "\n");
			pw.write("token = " + temporaryCredentials.getSessionToken());
		} catch (FileNotFoundException | UnsupportedEncodingException e) {
			e.printStackTrace();
		} finally {
			if (pw != null) {
				pw.flush();
				pw.close();
			}
		}
		
		System.out.println("Created Token at...:" + time);
	}

	/**
	 * Invalidate the CloudFront Cache using a STS Token
	 */
	private String invalidateCacheDistributionCheckProgressTokenCredentials() {

		BasicSessionCredentials temporaryCredentials = this.loadToken();
		if (temporaryCredentials == null) {
			temporaryCredentials = this.createToken();
			this.persistTemporaryCredentials(temporaryCredentials);
		} 

		AmazonCloudFront awsCloudFronClient = AmazonCloudFrontClientBuilder.standard()
				.withCredentials(new AWSStaticCredentialsProvider(temporaryCredentials)).withRegion("eu-central-1")
				.withMonitoringListener(new MonitoringListener() {
					@Override
					public void handleEvent(MonitoringEvent event) {
						if (event instanceof ApiCallAttemptMonitoringEvent) {
							ApiCallAttemptMonitoringEvent apiCallEvent = ((ApiCallAttemptMonitoringEvent) event);
							System.out.print("HTTP " + apiCallEvent.getHttpStatusCode());
						}
					}
				}).build();

		InvalidationBatch invalidationBatch = new InvalidationBatch();
		String uuid = UUID.randomUUID().toString().replace("-", "");
		invalidationBatch.setCallerReference(uuid);
		Paths paths = new Paths();
		// Here you could specify an array of paths, files to invalidate (or the whole  thing using wildcards)
		paths.setItems(Arrays.asList(new String[] { "/*" }));
		paths.setQuantity(1);
		invalidationBatch.setPaths(paths);

		// You must know what CloudFront distribution is yours, its ID, and then manages it (can be queried too)
		String distributionId = "E2ZCBHZCQBEB2W";
		CreateInvalidationRequest createInvalidationRequest = new CreateInvalidationRequest(distributionId,
				invalidationBatch);
		CreateInvalidationResult createInvalidationResult = awsCloudFronClient
				.createInvalidation(createInvalidationRequest);
		String status = createInvalidationResult.getInvalidation().getStatus();

		// The invalidation process finishes here, returning this Status.
        // If you receive a HTTP 201 InProgress, that's done the Invalidation.
        //
        // The rest of the code ahead is only as way (testing purposes) to monitoring the status (progress)
        // of the invalidation while is being occurring.
		//
		while (status.equalsIgnoreCase("InProgress")) {
			System.out.println(" - " + status + "...");
			GetInvalidationRequest getInvalidationRequest = new GetInvalidationRequest(distributionId,
					createInvalidationResult.getInvalidation().getId());
			GetInvalidationResult getInvalidationResult = awsCloudFronClient.getInvalidation(getInvalidationRequest);
			status = getInvalidationResult.getInvalidation().getStatus();
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return " - " + status + "     <----- END";
	}

	/**
	 * Loading the STS Token (previously saved)
	 */
	private BasicSessionCredentials loadToken() {
		String filePath = System.getProperty("user.home") + File.separator + AWS_TOKEN_DIR + File.separator + FILE_TOKEN_NAME;
		File file = new File(filePath);
		if ( !file.exists() ) {
			return null;
		}
		
		Properties propertiesToken = new Properties();
		try ( InputStream inputStream = new FileInputStream(file) ) {
			propertiesToken.load(inputStream);
		} catch (IOException e) {
			e.printStackTrace();
     	}
		
		System.out.println("Loading Token From...: " + propertiesToken.getProperty("time"));
		
    	return new BasicSessionCredentials(propertiesToken.getProperty("aws_access_key_id"),
    			                           propertiesToken.getProperty("aws_secret_access_key"),
    			                           propertiesToken.getProperty("token"));
	}
	
	/**
	 * 
	 * Invalidate CloudFront Cache using the credentials explicitly (load from local file)
	 * 
	 * @return
	 */
	@SuppressWarnings("unused")
	private String invalidateCacheDistributionBasicCredentials() {
		PropertiesFileCredentialsProvider awsCredentials = getUserServiceCredentials("ecomm");
		BasicAWSCredentials awsCreds = new BasicAWSCredentials(awsCredentials.getCredentials().getAWSAccessKeyId(), 
				                                               awsCredentials.getCredentials().getAWSSecretKey());
		AmazonCloudFront awsCloudFronClient = AmazonCloudFrontClientBuilder
				                              .standard()
				                              .withCredentials(new AWSStaticCredentialsProvider(awsCreds))
				                              .withRegion("eu-central-1")
					                          .withMonitoringListener(new MonitoringListener() {
													@Override
													public void handleEvent(MonitoringEvent event) {
														if (event instanceof ApiCallAttemptMonitoringEvent ) {
															ApiCallAttemptMonitoringEvent apiCallEvent = 
																	((ApiCallAttemptMonitoringEvent) event);
															System.out.println("HTTP " + apiCallEvent.getHttpStatusCode());
														}
													}
											   })
				                              .build();
		
		InvalidationBatch invalidationBatch = new InvalidationBatch();
		String uuid = UUID.randomUUID().toString().replace("-", "");
		invalidationBatch.setCallerReference(uuid);
		Paths paths = new Paths();
		paths.setItems(Arrays.asList(new String[]{"/*"}));
		paths.setQuantity(1);
		invalidationBatch.setPaths(paths);
		
		CreateInvalidationRequest createInvalidationRequest = new CreateInvalidationRequest(DISTR_ID, invalidationBatch);
        CreateInvalidationResult  createInvalidationResult  = awsCloudFronClient.createInvalidation(createInvalidationRequest);
        return createInvalidationResult.getInvalidation().getStatus();
	}

	/**
	 * Just a simple request to get info of a CloudFront Distribution 
	 */
	@SuppressWarnings("unused")
	private void queryDistribution() {
		
		ClientConfiguration clientConfiguration = new ClientConfiguration();
        clientConfiguration.setProtocol(Protocol.HTTPS);
        
        PropertiesFileCredentialsProvider awsCredentials = getUserServiceCredentials("ecomm");
		BasicAWSCredentials awsCreds = new BasicAWSCredentials(awsCredentials.getCredentials().getAWSAccessKeyId(), 
				                                               awsCredentials.getCredentials().getAWSSecretKey());
		AmazonCloudFront awsCloudFronClient = AmazonCloudFrontClientBuilder
				                              .standard()
				                              .withCredentials(new AWSStaticCredentialsProvider(awsCreds))
				                              .withRegion("eu-central-1")
					                          .withMonitoringListener(new MonitoringListener() {
													@Override
													public void handleEvent(MonitoringEvent event) {
														if (event instanceof ApiCallAttemptMonitoringEvent ) {
															ApiCallAttemptMonitoringEvent apiCallEvent = 
																	((ApiCallAttemptMonitoringEvent) event);
															System.out.println("HTTP " + apiCallEvent.getHttpStatusCode());
														}
													}
											   })
				                              .build();

		GetDistributionRequest getDistributionRequest = new GetDistributionRequest();
		getDistributionRequest.setId(DISTR_ID);
		GetDistributionResult getDistributionResult = awsCloudFronClient.getDistribution(getDistributionRequest);
		System.out.println(getDistributionResult.getDistribution().getDomainName());
	}
	
	@SuppressWarnings("unused")
	private String invalidateCacheDistributionStandardCredentials() {

		AmazonCloudFront awsCloudFronClient = AmazonCloudFrontClientBuilder
				.standard()
				.withRegion("eu-central-1")
				.withMonitoringListener(new MonitoringListener() {
					@Override
					public void handleEvent(MonitoringEvent event) {
						if (event instanceof ApiCallAttemptMonitoringEvent) {
							ApiCallAttemptMonitoringEvent apiCallEvent = ((ApiCallAttemptMonitoringEvent) event);
							System.out.println("HTTP " + apiCallEvent.getHttpStatusCode());
						}
					}
				}).build();

		InvalidationBatch invalidationBatch = new InvalidationBatch();
		String uuid = UUID.randomUUID().toString().replace("-", "");
		invalidationBatch.setCallerReference(uuid);
		Paths paths = new Paths();
		paths.setItems(Arrays.asList(new String[] { "/*" }));
		paths.setQuantity(1);
		invalidationBatch.setPaths(paths);

		String distributionId = "E2ZCBHZCQBEB2W";
		CreateInvalidationRequest createInvalidationRequest = new CreateInvalidationRequest(distributionId,
				invalidationBatch);
		CreateInvalidationResult createInvalidationResult = awsCloudFronClient
				.createInvalidation(createInvalidationRequest);
		return createInvalidationResult.getInvalidation().getStatus();
	}

}
