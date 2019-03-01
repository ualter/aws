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

public class S3ResumeLowLevelSendMultiPartFile {
	
	public static void main(String[] args) throws Exception {
		
		/*
		 
		 To resume (restart) a MultiPartUpload to S3 we will need those variables below, otherwise it will be impossible to restart it
		 
		 - (Bucket) Name of the Bucket
		 - (Key) The exact name of the object key (file name), must be the same
		 - (UploadId) The uploadId that was stopped/crashed
		 - (File) The local file to resume its upload
		 - (partSize) Block of size in MBbytes to be uploaded 
		   (technically it is not necessary to follow the same block size used before, 
		    it only need to be sure to start at the right byte, that is, the byte right after the last byte successfully uploaded already)
		 - (startFilePosition) That's the position in bytes of the file where the upload will be resumed, must be the byte right after the last one successfully uploaded before
		 - (List<PartETag>) The list of PartETag successfully already uploaded (made by the PartNumber and eTag)    
		 
		 */
		
		String bucket            = "tasadora-test";
        String key               = "request/tasacion-file-multipartedUploaded.pdf";
		String uploadId          = "Doy.yiSXWEQkvvv2ZuUx7p3mcMod6bVQUjLrqFeQif8Q6XJYWwJZpHwre8tma.e8uN3_V0K_mrVXjf80EHMbuTbcwjnetdQR0nPHQAfYdWg7zHvyj.eE9cN3BxfajNpl";
		String file              = "/Users/ualter/Temp/529-2712-1-PB.pdf";
		int    partSize          = 10;
		long   startFilePosition = 41943040 + (partSize * 1024 * 1024);
		
		List<PartETag> partETags = new ArrayList<PartETag>();
		partETags.add(new PartETag(1, "3beb7e9af8674013a48d78ba14b4b075"));
		partETags.add(new PartETag(2, "5bfdc19fa1fa7b7b347f2c13a500d14c"));
		partETags.add(new PartETag(3, "ef18c3c8c4b3162258a35c3bf424bc15"));
		partETags.add(new PartETag(4, "f31091770d8053754a02715dca601fa3"));
		partETags.add(new PartETag(5, "f31aa43f51fba0f8991eb3971a9c55b0"));
		
		
		S3ResumeLowLevelSendMultiPartFile s3ResumeUpload = new S3ResumeLowLevelSendMultiPartFile();
		s3ResumeUpload.multipartResumeUploadsFile(
				bucket,
				key,
				uploadId,
				file,
				partETags,
				6,
				startFilePosition,
				partSize);
	}

	static int THREAD_POOL_SIZE = 5;
	static Semaphore SEMAPHORE  = new Semaphore(THREAD_POOL_SIZE);
	
	S3UploadTracker s3UploadTracker = new S3UploadTracker();
	
	private void multipartResumeUploadsFile(String bucket, String key, 
			String uploadId, String fileName, List<PartETag> parETagsAlreadyUploaded,
			int startSequence, long startFilePosition, int partSizeMB) {
		
		s3UploadTracker.setBucketName(bucket);
    	s3UploadTracker.setKey(key);
    	s3UploadTracker.setFile(fileName);
		
        try {
			File file           = new File(fileName);
        	long contentLength  = file.length();
        	long partSize       = partSizeMB * 1024 * 1024;
        	
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
    		for(PartETag pTag : parETagsAlreadyUploaded) {
    			partETags.add(new PartETag(pTag.getPartNumber(), pTag.getETag()));
    			
    		}
    		
    		UploadRequest uploadRequest;
    		// Initiate the multipart upload.
            InitiateMultipartUploadRequest initRequest  = new InitiateMultipartUploadRequest(bucket, key);
            InitiateMultipartUploadResult  initResponse = s3.initiateMultipartUpload(initRequest);
            initResponse.setUploadId(uploadId);

            //initRequest.withGeneralProgressListener(progressListener)
            
            System.out.println("*** Re-starting the Upload: " + initResponse.getUploadId());
            timeTracker.startTracking("Upload File S3");
            
            s3UploadTracker.setUploadId(initResponse.getUploadId());
            
            boolean startListen = false;
            // Upload the file parts.
            long filePosition = startFilePosition;
            for (int i = startSequence; filePosition < contentLength; i++) {
            	
            	// Because the last part could be less than partSize MB, adjust the part size as needed.
                partSize = Math.min(partSize, (contentLength - filePosition));
                
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
