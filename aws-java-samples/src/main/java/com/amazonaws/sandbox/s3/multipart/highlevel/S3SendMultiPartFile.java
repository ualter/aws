package com.amazonaws.sandbox.s3.multipart.highlevel;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.PropertiesFileCredentialsProvider;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.client.builder.ExecutorFactory;
import com.amazonaws.regions.Regions;
import com.amazonaws.retry.PredefinedRetryPolicies;
import com.amazonaws.sandbox.iam.IAMSandbox;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectTagging;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.Tag;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import com.amazonaws.services.s3.transfer.Upload;

import br.com.ujr.utils.TimeTracker;

public class S3SendMultiPartFile {
	
	public static void main(String[] args) throws Exception {
		
		
		//createFileOnDisk();
		//System.exit(0);
		
		//uploadFile();
		multipartUploadsFile();
	}

	static int THREAD_POOL_SIZE = 10;
	
	private static void multipartUploadsFile() {
		TransferManager transferManager = null;
        try {
        	TimeTracker timeTracker = TimeTracker.getInstance();
        	
        	File credentialsFilePath = new File(IAMSandbox.class.getClassLoader().getResource("tasadora-test-credentials.properties").getFile());
    		PropertiesFileCredentialsProvider propertiesFileCredentialsProvider = new PropertiesFileCredentialsProvider(credentialsFilePath.getPath());
    		
    		ClientConfiguration clientConfiguration=new ClientConfiguration();
    		clientConfiguration.setMaxConnections(THREAD_POOL_SIZE);
    		clientConfiguration.setProtocol(Protocol.HTTP);
    		// Change the default setting of 3 retry attempts to 5
    		clientConfiguration.setRetryPolicy(PredefinedRetryPolicies.getDefaultRetryPolicyWithCustomMaxRetries(5));
    		
    		
    		EndpointConfiguration endpointConfiguration =
					new AwsClientBuilder.EndpointConfiguration("https://tasadora-test.s3-eu-west-1.amazonaws.com","eu-west-1");
    		
    		AmazonS3 s3 = AmazonS3ClientBuilder.standard()
    				      .withCredentials(propertiesFileCredentialsProvider)
    				      //.withEndpointConfiguration(endpointConfiguration)
    				      .withRegion(Regions.EU_WEST_1)
    				      .withClientConfiguration(clientConfiguration)
    				      .enablePathStyleAccess()
    				      .build();
    		
    		ExecutorFactory executorFactory = new ExecutorFactory() {
				@Override
				public ExecutorService newExecutor() {
					return Executors.newFixedThreadPool(THREAD_POOL_SIZE);
				}
			};
    		
    		transferManager = TransferManagerBuilder.standard()
    							  .withS3Client(s3)
    							  // Number of threads
    							  //.withExecutorFactory(() -> Executors.newFixedThreadPool(THREAD_POOL_SIZE));  (Sorry, only Java8, lambda)
    							  .withExecutorFactory(executorFactory)
    							  // Threshold in which multipart uploads will be performed
    							  .withMultipartUploadThreshold(((long)64*1024*1024))
    							  // Minimum part size for upload parts. Decreasing the minimum part size will cause multipart uploads to be split into a larger number of smaller parts. Setting this value too low can have a negative effect on transfer speeds since it will cause extra latency and network communication for each part.
    							  .withMinimumUploadPartSize(((long)16*1024*1024))
    							  .build();
    							  
    		
            String bucket = "tasadora-test";
            String key    = "request/tasacion-file-multipartedUploaded";
            File   file   = new File("/Users/ualter/Temp/fichero.txt");
            		
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucket, key, file);
            List tags = new ArrayList();
            tags.add(new Tag("PROJECT", "SOLUTION_ARCH"));
            putObjectRequest.setTagging(new ObjectTagging(tags));
            
            // To receive notifications when bytes are transferred, add a  
            // ProgressListener to your request.
            putObjectRequest.setGeneralProgressListener(new S3SendMultiParFileListener(file, bucket));
            
            // TransferManager processes all transfers asynchronously,
            // so this call returns immediately.
            timeTracker.startTracking("Upload File S3");
            Upload upload = transferManager.upload(putObjectRequest);
            System.out.println("*** Starting the Upload:");
			upload.waitForCompletion();
			System.out.println(timeTracker.endTracking("Upload File S3"));
			
		} catch (AmazonServiceException e) {
			e.printStackTrace();
		} catch (AmazonClientException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			if ( transferManager != null ) {
				transferManager.shutdownNow();
			}
		}
        
        System.out.println("\n\nEND");
	}
	
	private static void uploadFile() throws IOException {
		TimeTracker timeTracker = TimeTracker.getInstance();
		
		File credentialsFilePath = new File(IAMSandbox.class.getClassLoader().getResource("tasadora-test-credentials.properties").getFile());
		PropertiesFileCredentialsProvider propertiesFileCredentialsProvider = new PropertiesFileCredentialsProvider(credentialsFilePath.getPath());
		
		AmazonS3 s3 = AmazonS3ClientBuilder.standard()
				      .withCredentials(propertiesFileCredentialsProvider)
				      //.withEndpointConfiguration(endpointConfiguration)
				      .withRegion(Regions.EU_WEST_1)
				      .enablePathStyleAccess()
				      .build();
		
		/*
		Iterator it = s3.listBuckets().iterator();
		System.out.println("****** Buckets found:");
        while ( it.hasNext() ) {
        	Bucket bucket = (Bucket)it.next();
        	System.out.println( "- " + bucket.getName());
        }
        */
        
        String bucket = "tasadora-test";
        String key    = "request/tasacion-file";
        	
        File file = new File("/Users/ualter/Temp/fichero.txt");
        //File file = createSampleFile();
        PutObjectRequest putObjectRequest = new PutObjectRequest(bucket, key, file);
        List tags = new ArrayList();
        tags.add(new Tag("PROJECT", "SOLUTION_ARCH"));
        putObjectRequest.setTagging(new ObjectTagging(tags));
        
        timeTracker.startTracking("Upload File S3");
        System.out.println("*** Starting the Upload:");
        s3.putObject(putObjectRequest);
        System.out.println(timeTracker.endTracking("Upload File S3"));
        
        System.out.println("\n\nEND");
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
	
	private static void createFileOnDisk() throws Exception {
		File file = new File("/Users/ualter/Temp/fichero.txt");

        Writer writer = new OutputStreamWriter(new FileOutputStream(file));
        
        int i = 0;
        //while(i < 12000000) { // 1.5GB
        //while(i < 3000000) { // 405MB
        while(i < 1200000) { //	
	        writer.write("abcdefghijklmnopqrstuvwxyz\n");
	        writer.write("01234567890112345678901234\n");
	        writer.write("!@#$%^&*()-=[]{};':',.<>/?\n");
	        writer.write("01234567890112345678901234\n");
	        writer.write("abcdefghijklmnopqrstuvwxyz\n");
	        System.out.print(".");
	        i++;
        }
        System.out.println("\nEnd");
        writer.flush();
        writer.close();
	}

}
