package com.amazonaws.sandbox.s3.multipart.lowlevel;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.PropertiesFileCredentialsProvider;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.event.ProgressEvent;
import com.amazonaws.event.ProgressListener;
import com.amazonaws.regions.Regions;
import com.amazonaws.retry.PredefinedRetryPolicies;
import com.amazonaws.sandbox.iam.IAMSandbox;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.CompleteMultipartUploadResult;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.ObjectTagging;
import com.amazonaws.services.s3.model.PartETag;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.Tag;
import com.amazonaws.services.s3.model.UploadPartRequest;
import com.amazonaws.services.s3.model.UploadPartResult;

import br.com.ujr.utils.TimeTracker;

public class S3LowLevelSendMultiPartFile {
	
	public static void main(String[] args) throws Exception {
		//createFileOnDisk();
		//uploadFile();
		
		S3LowLevelSendMultiPartFile s = new S3LowLevelSendMultiPartFile();
		s.multipartUploadsFile();
	}

    static int THREAD_POOL_SIZE = 5;
    static Semaphore SEMAPHORE  = new Semaphore(THREAD_POOL_SIZE);
    
    S3UploadTracker s3UploadTracker = new S3UploadTracker();
	
	private void multipartUploadsFile() {
        try {
        	final String bucket = "tasadora-test";
            final String key    = "request/tasacion-file-multipartedUploaded.pdf";
        	//File file           = new File("/Users/ualter/Temp/fichero.txt");
            String fileName     = "/Users/ualter/Temp/529-2712-1-PB.pdf";
            File file           = new File(fileName);
        	long contentLength  = file.length();
        	long partSize       = 10 * 1024 * 1024;
        	
        	s3UploadTracker.setBucketName(bucket);
        	s3UploadTracker.setKey(key);
        	s3UploadTracker.setFile(fileName);
        	
        	TimeTracker timeTracker = TimeTracker.getInstance();
        	
        	File credentialsFilePath = new File(IAMSandbox.class.getClassLoader().getResource("tasadora-test-credentials.properties").getFile());
    		PropertiesFileCredentialsProvider propertiesFileCredentialsProvider = new PropertiesFileCredentialsProvider(credentialsFilePath.getPath());
    		
    		ClientConfiguration clientConfiguration=new ClientConfiguration();
    		clientConfiguration.setMaxConnections(THREAD_POOL_SIZE);
    		// Change the default setting of 3 retry attempts to 5
    		clientConfiguration.setRetryPolicy(PredefinedRetryPolicies.getDefaultRetryPolicyWithCustomMaxRetries(5));
    		
    		
    		EndpointConfiguration endpointConfiguration =
					new AwsClientBuilder.EndpointConfiguration("https://tasadora-test.s3-eu-west-1.amazonaws.com","eu-west-1");
    		
    		final AmazonS3 s3 = AmazonS3ClientBuilder.standard()
    				      .withCredentials(propertiesFileCredentialsProvider)
    				      //.withEndpointConfiguration(endpointConfiguration)
    				      .withRegion(Regions.EU_WEST_1)
    				      .withClientConfiguration(clientConfiguration)
    				      .enablePathStyleAccess()
    				      .build();
    		
    		List<PartETag> partETags = new ArrayList<PartETag>();
    		
    		UploadRequest uploadRequest;
    		
    		// Initiate the multipart upload.
            InitiateMultipartUploadRequest initRequest  = new InitiateMultipartUploadRequest(bucket, key);
            InitiateMultipartUploadResult  initResponse = s3.initiateMultipartUpload(initRequest);
            
            System.out.println("*** Starting the Upload: " + initResponse.getUploadId());
            timeTracker.startTracking("Upload File S3");
            
            s3UploadTracker.setUploadId(initResponse.getUploadId());
            
            boolean startListen = false;
            // Upload the file parts.
            long filePosition = 0;
            for (int i = 1; filePosition < contentLength; i++) {
            	
            	// Because the last part could be less than partSize MB, adjust the part size as needed.
                partSize = Math.min(partSize, (contentLength - filePosition));
                
                /*
                // Create the request to upload a part.
        		UploadPartRequest uploadRequest = new UploadPartRequest()
        		        .withBucketName(bucket)
        		        .withKey(key)
        		        .withUploadId(initResponse.getUploadId())
        		        .withPartNumber(i)
        		        .withFileOffset(filePosition)
        		        .withFile(file)
        		        .withPartSize(partSize);
        		
        		// Upload the part and add the response's ETag to our list.
        		UploadPartResult uploadResult = s3.uploadPart(uploadRequest);
        		partETags.add(uploadResult.getPartETag());
        		*/
                
            	System.out.print("Acquiring lock... " + "(" + SEMAPHORE.availablePermits());
            	if ( SEMAPHORE.availablePermits() == 0 ) {
            		System.out.println(") ===> All busy");
            	} else {
            		System.out.println(")");
            	}
            	SEMAPHORE.acquire();
            	System.out.println("Got the permit!");
            	
                uploadRequest = new UploadRequest(bucket, key, file, s3, partSize, filePosition, i, partETags, initResponse, this.s3UploadTracker);
                Thread t = new Thread(uploadRequest);
                t.setName("***** Upload-" + i);
                t.setDaemon(true);
                t.start();
                
                filePosition += partSize;
            }
            
            System.out.println("\nWaiting the remaining Threads to Finalize its job");
            while ( SEMAPHORE.availablePermits() < THREAD_POOL_SIZE );
            System.out.println("All done!");
            
            // Complete the multipart upload.
            CompleteMultipartUploadRequest compRequest = new CompleteMultipartUploadRequest(bucket, key, initResponse.getUploadId(), partETags);
            CompleteMultipartUploadResult resultUpload = s3.completeMultipartUpload(compRequest);
            System.out.println(resultUpload.getKey() + " uploaded!\n");
			System.out.println(timeTracker.endTracking("Upload File S3"));
			
			this.s3UploadTracker.setFinalizedSuccessfully(true);
			this.s3UploadTracker.printSnapShot();
			
		} catch (AmazonServiceException e) {
			e.printStackTrace();
		} catch (AmazonClientException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
        
        System.out.println("\n\nEND");
	}

	private void oneShotUploadFile() throws IOException {
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
	
	private static class UploadRequest implements Runnable {
		
		private String bucket;
		private String key;
		private File file;
		private AmazonS3 s3;
		private long partSize;
		private long filePosition;
		private int sequence;
		private List<PartETag> partETags;
		private InitiateMultipartUploadResult initResponse;
		private S3UploadTracker s3UploadTracker;
		
		public UploadRequest(String bucket, String key, File file, AmazonS3 s3, long partSize, long filePosition, 
				int sequence, List<PartETag> partETags,
				InitiateMultipartUploadResult initResponse, S3UploadTracker s3UploadTracker) {
			super();
			this.bucket = bucket;
			this.key = key;
			this.file = file;
			this.s3 = s3;
			this.partSize = partSize;
			this.filePosition = filePosition;
			this.sequence = sequence;
			this.partETags = partETags;
			this.initResponse = initResponse;
			this.s3UploadTracker = s3UploadTracker;
		}


		@Override
		public void run() {
			
			System.out.println(Thread.currentThread().getName() + "...: FilePosition:" + filePosition + ", PartSize:" + partSize);
			
			UploadPartRequest uploadRequest = new UploadPartRequest()
			        .withBucketName(bucket)
			        .withKey(key)
			        .withUploadId(initResponse.getUploadId())
			        .withPartNumber(sequence)
			        .withFileOffset(filePosition)
			        .withFile(file)
			        .withPartSize(partSize);
			
			
			ProgressListener  progressListener = new ProgressListener() {
				@Override
				public void progressChanged(ProgressEvent progressEvent) {
					//progressEvent.getEventType()
				}
			};
			uploadRequest.withGeneralProgressListener(progressListener);
			
			
			// Upload the part and add the response's ETag to our list.
			UploadPartResult uploadResult = s3.uploadPart(uploadRequest);
			PartETag partETag = uploadResult.getPartETag();
			partETags.add(partETag);
			
			s3UploadTracker.addPartETag(partETag);
			s3UploadTracker.setFilePosition(filePosition);
			s3UploadTracker.printSnapShot();
			
			StringBuffer sb = new StringBuffer();
			sb.append("\n");
			sb.append(" ***** ").append(Thread.currentThread().getName()).append("\n");
			sb.append("            ").append("FilePosition...:").append(filePosition).append("\n");
			sb.append("            ").append("PartSize.......:").append(partSize).append("\n");
			sb.append("            ").append("ETag...........:").append(uploadResult.getPartETag().getETag()).append("\n");
			sb.append("            ").append("PartNumber.....:").append(uploadResult.getPartETag().getPartNumber()).append("\n");
			System.out.println(sb.toString());
			
			System.out.println("Releasing lock..." + "(" + Thread.currentThread().getName() + ")");
			SEMAPHORE.release();
			System.out.println("Available Semaphore permits now: " + SEMAPHORE.availablePermits());
			
		}
		
	}

}
