package com.amazonaws.sandbox.s3;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.auth.PropertiesFileCredentialsProvider;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.regions.Regions;
import com.amazonaws.sandbox.iam.IAMSandbox;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectTagging;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.Tag;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import com.amazonaws.services.securitytoken.model.AssumeRoleRequest;
import com.amazonaws.services.securitytoken.model.AssumeRoleResult;

//@SuppressWarnings("unused")
public class S3Sandbox {
	
	public static void main(String[] args) throws Exception {
		//testSTSToken();
		//createBucketAndUploadFileAtS3("tasadora-a");
		//deleteFileAtS3("tasadora-a","request/tasacion-file-1");
		//uploadFile("owner","tasadora-beta","request/tasacion-" + UUID.randomUUID());
		//uploadFile("joe","tasadora-alpha","request/tasacion-" + UUID.randomUUID());
		//uploadFile("alice","tasadora-alpha","request/tasacion-" + UUID.randomUUID());
		//uploadFile("tasadora-beta","tasadora-beta","request/tasacion-" + UUID.randomUUID());
		//readFileAtS3("tasadora-beta","tasadora-beta","request/tasacion-8b53f6f1-2128-4615-8d2b-2370b2f8af71");
		
		//uploadFile("tasadora-gamma","tasadora-gamma","request/tasacion-" + UUID.randomUUID());
		//readFileAtS3("tasadora-gamma","tasadora-gamma","request/tasacion-c66b08cb-db97-4622-9eb6-57439a9022ab");
		
		readFileAtS3("tasadora-alpha","tasadora-alpha","request/tasacion-0833ce85-fe95-4777-8247-545324865258");
		//System.out.println("Ok");
	}
	
	@SuppressWarnings("deprecation")
	private static AmazonS3 buildAmazonS3Client(String user) {
		
		ClientConfiguration clientCfg = new ClientConfiguration();
	    clientCfg.setProtocol(Protocol.HTTP);
	    
	    //clientCfg.setProxyHost("cache.bancsabadell.com");
	    //clientCfg.setProxyPort(8080);
		//clientCfg.setProtocol(Protocol.HTTPS);
		
		AmazonS3 s3 = null;
		
		try {
			BasicSessionCredentials temporaryToken = IAMSandbox.getTasadorasToken(user, "UserName", "*******");
			System.out.println("\n******* TEMPORARY SECURITY CREDENTIALS to \"" + user + "\" *******");
			System.out.println("|");
			System.out.println("| AccessKeyID..:" + temporaryToken.getAWSAccessKeyId());
			System.out.println("| SecretKey....:" + temporaryToken.getAWSSecretKey());
			System.out.println("| --- BEGIN TOKEN ---\n" + temporaryToken.getSessionToken() + "\n| --- END TOKEN ---\n");
			
			
			EndpointConfiguration endpointConfiguration =
					//new AwsClientBuilder.EndpointConfiguration("http://34.222.229.179","us-west-2");
					//new AwsClientBuilder.EndpointConfiguration("http://ec2-52-38-15-125.us-west-2.compute.amazonaws.com","us-west-2");
					//new AwsClientBuilder.EndpointConfiguration("http://bancsabadells3.us-east-1.amazonaws.com","us-east-1");
					new AwsClientBuilder.EndpointConfiguration("http://bancsabadells3.us-west-2.amazonaws.com","us-west-2");
					//new AwsClientBuilder.EndpointConfiguration("http://s3.us-west-2.amazonaws.com","us-west-2");
			
			s3 = AmazonS3ClientBuilder.standard()
						   .withCredentials(new AWSStaticCredentialsProvider(temporaryToken))
						   .withEndpointConfiguration(endpointConfiguration)
						   .enablePathStyleAccess()
						   .withClientConfiguration(clientCfg)
						   //.withRegion(Regions.US_WEST_2)
						   /*.withRequestHandlers(new RequestHandler2() {
								@Override
							    public void beforeRequest(Request<?> request) {
									request.getHeaders().put("UALTER", "JUNIOR");
									request.getHeaders().put("Time", dateHeader());
							    }
							})*/
						   .build();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return s3;
	}
	
	private static String dateHeader() {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HHmm");
		return sdf.format(Calendar.getInstance().getTime());
	}
	
	private static void createBucketAndUploadFileAtS3(String bucketName)  throws Exception {
		//PropertiesFileCredentialsProvider propertiesFileCredentialsProvider = accountOwnerCredentials();
		AmazonS3 s3 = buildAmazonS3Client("ualter");

        //String bucketName = "my-first-s3-bucket-" + UUID.randomUUID();
        //String key = "MyObjectKey";
        String key = "request/tasacion-file-1";
        
        //Create Bucket
        System.out.println("Creating bucket " + bucketName + "\n");
        s3.createBucket(bucketName);
        
        //List
        System.out.println("Listing buckets");
        // JAVA 8
        //for (Bucket bucket : s3.listBuckets()) {
        //    System.out.println(" - " + bucket.getName());
        //}
        // JAVA 6
        Iterator it = s3.listBuckets().iterator();
        while ( it.hasNext() ) {
        	System.out.println(it.next());
        }
        
        // Upload File
        System.out.println("Uploading a new object to S3 from a file\n");
        PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, key, createSampleFile());
        
        // JAVA 8
        //List<Tag> tags = new ArrayList<Tag>();
        // JAVA 6
        List tags = new ArrayList();
        
        tags.add(new Tag("TASADORA", "ALPHA"));
        tags.add(new Tag("OTHER_TAG_INFO", "GOES_HERE"));
        putObjectRequest.setTagging(new ObjectTagging(tags));
        s3.putObject(putObjectRequest);
        
        //List
        System.out.println("Listing buckets");
        // JAVA 8
        //for (Bucket bucket : s3.listBuckets()) {
        //    System.out.println(" - " + bucket.getName());
        //}
        // JAVA 7
        it = s3.listBuckets().iterator();
        while ( it.hasNext() ) {
        	System.out.println(it.next());
        }
        
        // Donwload File
        System.out.println("Downloading an object");
        S3Object object = s3.getObject(new GetObjectRequest(bucketName, key));
        System.out.println("Content-Type: "  + object.getObjectMetadata().getContentType());
        displayTextInputStream(object.getObjectContent());
        
        // Clean
        //System.out.println("Deleting an object\n");
        //s3.deleteObject(bucketName, key);
        //System.out.println("Deleting bucket " + bucketName + "\n");
        //s3.deleteBucket(bucketName);
	}
	
	private static void uploadFile(String user, String bucketName, String key)  throws Exception {
		
		//PropertiesFileCredentialsProvider propertiesFileCredentialsProvider = buildCredentials(user);
		AmazonS3 s3 = buildAmazonS3Client(user);
		
		System.out.println("\n*********** Upload to S3 as \"" + user + "\" user ***********");
        
        // Upload File
        System.out.println("| Uploading a new object to S3...");
        PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, key, createSampleFile());
        // JAVA 8
        //List<Tag> tags = new ArrayList<Tag>();
        // JAVA 6
        List tags = new ArrayList();
        tags.add(new Tag("USER", user));
        tags.add(new Tag("OTHER_TAG_INFO", "GOES_HERE"));
        putObjectRequest.setTagging(new ObjectTagging(tags));
        try {
			s3.putObject(putObjectRequest);
		    System.out.println("| File: " + key);
	        System.out.println("| ---> Uploaded!");
	        System.out.println("| ---> by \"" + user + "\"\n");
		} catch (Exception e) {
			System.out.println("| ------------------------------------> Ops... NOT OK!");
			System.out.println("| ---> Error:" + e.getMessage() + "\n");
			//e.printStackTrace();
		}
        
       
	}

	private static PropertiesFileCredentialsProvider accountOwnerCredentials() {
		return buildCredentials("owner");
	}

	private static PropertiesFileCredentialsProvider buildCredentials(String usersName) {
		File credentialsFilePath = new 
				File(IAMSandbox.class.getClassLoader().getResource(usersName + "-credentials.properties").getFile());
		PropertiesFileCredentialsProvider propertiesFileCredentialsProvider = new PropertiesFileCredentialsProvider(credentialsFilePath.getPath());
		return propertiesFileCredentialsProvider;
	}
	
	private static void deleteFileAtS3(String bucketName, String key)  throws Exception {
		PropertiesFileCredentialsProvider propertiesFileCredentialsProvider = accountOwnerCredentials();
		AmazonS3 s3 = buildAmazonS3Client("owner");

        //List
        System.out.println("Listing buckets");
        // JAVA 8 
        //for (Bucket bucket : s3.listBuckets()) {
        //    System.out.println(" - " + bucket.getName());
        //}
        // JAVA 6
        Iterator it = s3.listBuckets().iterator();
        while ( it.hasNext() ) {
        	System.out.println(it.next());
        }
        
        // Clean
        System.out.println("Deleting an object\n");
        s3.deleteObject(bucketName, key);
        
        System.out.println("Listing buckets");
        // JAVA 8 
        //for (Bucket bucket : s3.listBuckets()) {
        //   System.out.println(" - " + bucket.getName());
        //}
        // JAVA 6
        it = s3.listBuckets().iterator();
        while ( it.hasNext() ) {
        	System.out.println(it.next());
        }
	}
	
	private static void readFileAtS3(String user, String bucket, String key)  throws Exception {
		PropertiesFileCredentialsProvider propertiesFileCredentialsProvider = buildCredentials(user);
		AmazonS3 s3 = buildAmazonS3Client(user);
		
        S3Object object = s3.getObject(new GetObjectRequest(bucket, key));
        System.out.println("Content of " + key);
        displayTextInputStream(object.getObjectContent());
	}

	private static void testSTSToken() throws FileNotFoundException, IOException {
		File fileCredentials = new 
				File(IAMSandbox.class.getClassLoader().getResource("owner-credentials.properties").getFile());
				
		AWSCredentials longTermCredentials = new PropertiesCredentials(fileCredentials);
		AWSSecurityTokenService stsClient = AWSSecurityTokenServiceClientBuilder.standard()
				//.withCredentials(new AWSStaticCredentialsProvider(longTermCredentials))
				.withRegion(Regions.US_WEST_2.name())
				.build();
		
		AssumeRoleRequest assumeRequest = new AssumeRoleRequest()
				.withRoleArn("arn:aws:iam::933272457605:role/s3")
				.withDurationSeconds(new Integer(3600)) // JAVA 6
	            // JAVA 8 -->.withDurationSeconds(3600)
	            .withRoleSessionName("demo");
		
		AssumeRoleResult assumeResult = stsClient.assumeRole(assumeRequest);
		
		BasicSessionCredentials temporaryCredentials = new BasicSessionCredentials(
																	               assumeResult.getCredentials().getAccessKeyId(),
																	               assumeResult.getCredentials().getSecretAccessKey(),
																	               assumeResult.getCredentials().getSessionToken());
		System.out.println(temporaryCredentials.getSessionToken());
	}
	
	private static File createSampleFile() throws IOException {
        File file = File.createTempFile("aws-java-sdk-from-ualter", ".txt");
        file.deleteOnExit();

        Writer writer = new OutputStreamWriter(new FileOutputStream(file));
        writer.write("abcdefghijklmnopqrstuvwxyz\n");
        writer.write("01234567890112345678901234\n");
        writer.write("!@#$%^&*()-=[]{};':',.<>/?\n");
        writer.write("01234567890112345678901234\n");
        writer.write("abcdefghijklmnopqrstuvwxyz\n");
        writer.close();

        return file;
    }
	
	private static void displayTextInputStream(InputStream input) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(input));
        while (true) {
            String line = reader.readLine();
            if (line == null) break;

            System.out.println("    " + line);
        }
        System.out.println();
    }
	
	

}
