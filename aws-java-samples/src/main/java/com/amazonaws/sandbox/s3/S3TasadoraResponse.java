package com.amazonaws.sandbox.s3;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.auth.PropertiesFileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.sandbox.iam.IAMSandbox;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectTagging;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.Tag;

//@SuppressWarnings("unused")
public class S3TasadoraResponse {
	
	public static void main(String[] args) throws Exception {
		uploadResponseToS3("tasadora-alpha","tasadora-alpha","response/tasacion-" + UUID.randomUUID());
		
//		testPutFileDirectlyWithAccessPrivateKey("tasadora-alpha","response/tasacion-" + UUID.randomUUID());
	}
	
	
	private static void uploadResponseToS3(String user, String bucketName, String key)  throws Exception {
		// Generate TOKEN (Temporary Security Credentials)
		BasicSessionCredentials basicSessionCredentials = IAMSandbox.getTasadorasToken(user, "tasadora-login","tasador-password");
		
		System.out.println("\n******* TEMPORARY SECURITY CREDENTIALS to \"" + user + "\" *******");
		System.out.println("|");
		System.out.println("| AccessKeyID..:" + basicSessionCredentials.getAWSAccessKeyId());
		System.out.println("| SecretKey....:" + basicSessionCredentials.getAWSSecretKey());
		System.out.println("| Token........:" + basicSessionCredentials.getSessionToken());
		System.out.println("|");
		System.out.println("| Bucket name..:" + bucketName);
		System.out.println("| File.........:" + key);
		System.out.println("|");
		
		AmazonS3 s3 = AmazonS3ClientBuilder.standard()
				.withCredentials(new AWSStaticCredentialsProvider(basicSessionCredentials))
				.withRegion(Region.getRegion(Regions.US_WEST_2).getName())
				.build();
		
        // Upload File
        System.out.println("| Uploading a response to S3....");
        PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, key,  createSampleFile());
        // JAVA 8 --> List<Tag> tags = new ArrayList<Tag>();
        List tags = new ArrayList();
        tags.add(new Tag("TASADORA", "ALPHA"));
        tags.add(new Tag("TYPE", "RESPONSE"));
        putObjectRequest.setTagging(new ObjectTagging(tags));
        s3.putObject(putObjectRequest);
        System.out.println("| ---> Uploaded!");
        System.out.println("\n");
	}

	
	private static void testPutFileDirectlyWithAccessPrivateKey(String bucketName, String key) throws IOException {
		File credentialsFilePath = new 
				File(IAMSandbox.class.getClassLoader().getResource("credentials.properties").getFile());
		PropertiesFileCredentialsProvider propertiesFileCredentialsProvider = new PropertiesFileCredentialsProvider(credentialsFilePath.getPath());
		
		AmazonS3 s3 = AmazonS3ClientBuilder.standard()
				.withCredentials(propertiesFileCredentialsProvider)
				.withRegion(Region.getRegion(Regions.US_WEST_2).getName())
				.build();
		
		System.out.println("Uploading a response to S3....");
        PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, key,  createSampleFile());
        //JAVA 8 --> List<Tag> tags = new ArrayList<Tag>();
        List tags = new ArrayList();
        tags.add(new Tag("TASADORA", "ALPHA"));
        tags.add(new Tag("TYPE", "RESPONSE"));
        putObjectRequest.setTagging(new ObjectTagging(tags));
        s3.putObject(putObjectRequest);
        System.out.println("Uploaded!");
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
	

}
