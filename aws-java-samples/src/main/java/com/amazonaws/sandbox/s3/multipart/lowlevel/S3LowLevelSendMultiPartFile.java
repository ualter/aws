package com.amazonaws.sandbox.s3.multipart.lowlevel;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

import org.apache.commons.lang3.StringUtils;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.event.ProgressListener;
import com.amazonaws.services.s3.AmazonS3;
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
	
	private static final boolean LOG_THREAD_INFO    = false;
	private static final boolean LOG_THREAD_SUMMARY = false;
	private static final int     BLOCK_SIZE_MB      = 10;
	
	public static void main(String[] args) throws Exception {
		//startUploadFromScracth();
		resumeUpload();
	}

	private static void startUploadFromScracth() {
		String bucket   = "tasadoratest";
		String key      = "request/tasacion-file-multipartedUploaded.pdf";
		String fileName = "/Users/ualter/Temp/529-2712-1-PB.pdf";
		long   partSize = 10;
		
		S3LowLevelSendMultiPartFile s = new S3LowLevelSendMultiPartFile();
		s.multipartUploadsFile(bucket, key, fileName, partSize);
	}
	
	/**
	 
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
	private static void resumeUpload() {
		String bucket                            = "tasadoratest";
		String key                               = "request/tasacion-file-multipartedUploaded.pdf";
		String fileName                          = "/Users/ualter/Temp/529-2712-1-PB.pdf";
		int    partSize                          = 10;
		
		String uploadId                          = "3AQ7OmhbuzaGOTkLJsB2Pht_Uo2OJKyCouTNWjTCof5IOwFfpQupyzfNj7tuec8WrITOt22n.6PKEcMkSM5xXTA2RVRU2cCn9SetK4pqqcexOURWfTxun9sOYCwXxC2l";
		long   startFilePosition                 = 41943040 + (partSize * 1024 * 1024); // Add the part already uploaded with the Last Thread (filePosition + partSize)
		int    startSequence                     = 6;
		long   totalBytesAlreadyTransferedBefore = startFilePosition;
		
		List<PartETag> partETags = new ArrayList<PartETag>();
		partETags.add(new PartETag(1, "3beb7e9af8674013a48d78ba14b4b075"));
		partETags.add(new PartETag(2, "5bfdc19fa1fa7b7b347f2c13a500d14c"));
		partETags.add(new PartETag(3, "ef18c3c8c4b3162258a35c3bf424bc15"));
		partETags.add(new PartETag(4, "f31091770d8053754a02715dca601fa3"));
		partETags.add(new PartETag(5, "f31aa43f51fba0f8991eb3971a9c55b0"));
		
		S3LowLevelSendMultiPartFile s = new S3LowLevelSendMultiPartFile();
		s.multipartResumeUploadsFile(bucket, key, uploadId, fileName, partETags, startSequence, startFilePosition, partSize, true, true, totalBytesAlreadyTransferedBefore);
	}
	
	

    static int THREAD_POOL_SIZE = 5;
    static Semaphore SEMAPHORE  = new Semaphore(THREAD_POOL_SIZE);
    
    S3UploadTracker s3UploadTracker = new S3UploadTracker();
    S3LowLevelProgressListener progressListener = new S3LowLevelProgressListener();
    
    private void multipartUploadsFile(String bucket, String key, String fileName, long blockSize) {
    	this.multipartResumeUploadsFile(bucket, key, null, fileName, null, 1, 0, BLOCK_SIZE_MB, true, true, 0);
    }
    
    private void multipartResumeUploadsFile(String bucket, String key, 
			String uploadId, String fileName, List<PartETag> parETagsAlreadyUploaded,
			int startSequence, long startFilePosition, int blockSize, boolean incremental, boolean finalizeMutilPartUpload, long totalBytesAlreadyTransferedBefore) {
        try {
            File file           = new File(fileName);
        	long contentLength  = file.length();
        	long partSize       = blockSize * 1024 * 1024;
        	
        	s3UploadTracker.setBucketName(bucket);
        	s3UploadTracker.setKey(key);
        	s3UploadTracker.setFile(fileName);
        	s3UploadTracker.setPartSize(partSize);
        	
        	progressListener.setTotalFileSizeInBytes(file.length());
        	if ( parETagsAlreadyUploaded != null ) {
        		progressListener.setTotalBytesTransfered(totalBytesAlreadyTransferedBefore);
        	}
        	
        	AmazonS3 s3 = S3ClientConnection.S3Client("tasadora-test");
        	
        	TimeTracker timeTracker = TimeTracker.getInstance();        	
    		
        	List<PartETag> partETags = parETagsAlreadyUploaded;
        	if ( partETags == null ) {
        		partETags = new ArrayList<PartETag>();
        	}
    		UploadRequest uploadRequest;
    		// Initiate the multipart upload.
            InitiateMultipartUploadRequest initRequest  = new InitiateMultipartUploadRequest(bucket, key);
            InitiateMultipartUploadResult  initResponse = s3.initiateMultipartUpload(initRequest);
            if ( StringUtils.isNotBlank(uploadId) ) {
            	initResponse.setUploadId(uploadId);
            }
            
            
            String title = "Starting";
            if ( parETagsAlreadyUploaded != null ) {
            	title = "Resuming";
            }
            
            System.out.println("***************************************************");
            System.out.println("  " + title + " the Upload: " + initResponse.getUploadId());
            System.out.println("  File: " + fileName + " to " + bucket + "/" + key);
            System.out.println("***************************************************");
            timeTracker.startTracking("Upload File S3");
            
            s3UploadTracker.setUploadId(initResponse.getUploadId());
            
            boolean startListen = false;
            // Upload the file parts.
            long filePosition = startFilePosition;
            if ( incremental ) {
	            for (int i = startSequence; filePosition < contentLength; i++) {
	            	// Because the last part could be less than partSize MB, adjust the part size as needed.
	                partSize = Math.min(partSize, (contentLength - filePosition));
	                
	                if ( LOG_THREAD_INFO ) { 
		                System.out.print("Acquiring lock for Thread... " + "(" + SEMAPHORE.availablePermits());
		            	if ( SEMAPHORE.availablePermits() == 0 ) {
		            		System.out.println(") ===> All " + THREAD_POOL_SIZE + " Threads Started, no space available to launch more!");
		            	} else {
		            		System.out.println(")");
		            	}
	                }
	            	SEMAPHORE.acquire();
	            	if ( LOG_THREAD_INFO ) { 
	            		System.out.println("Got permit for Thread Start!");
	            	}
	            	
	                uploadRequest = new UploadRequest(bucket, key, file, s3, partSize, filePosition, i, partETags, initResponse,
	                		this.s3UploadTracker, this.progressListener);
	                Thread t = new Thread(uploadRequest);
	                t.setName("Upload-" + i);
	                t.setDaemon(true);
	                t.start();
	                
	                filePosition += partSize;
	            }
            } else {
            	partSize = Math.min(partSize, (contentLength - filePosition));
            	uploadRequest = new UploadRequest(bucket, key, file, s3, partSize, 
            			filePosition, startSequence, partETags, initResponse, 
                		this.s3UploadTracker, this.progressListener);
                Thread t = new Thread(uploadRequest);
                t.setName("***** Upload-0");
                t.setDaemon(true); 
                t.start();
            }
            
            if ( LOG_THREAD_INFO ) {
            	System.out.println("\n\n******************* Waiting the remaining Threads to Finalize its job");
            }
            while ( SEMAPHORE.availablePermits() < THREAD_POOL_SIZE );
            System.out.println("\nAll done!");
            
            if (finalizeMutilPartUpload) {
	            // Complete the multipart upload.
	            CompleteMultipartUploadRequest compRequest = new CompleteMultipartUploadRequest(bucket, key, initResponse.getUploadId(), partETags);
	            CompleteMultipartUploadResult resultUpload = s3.completeMultipartUpload(compRequest);
	            System.out.println(resultUpload.getKey() + " uploaded!\n");
				System.out.println(timeTracker.endTracking("Upload File S3"));
				this.s3UploadTracker.setFinalizedSuccessfully(true);
				this.s3UploadTracker.finishedSnapShot(Thread.currentThread().getName());
            }
			
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
		
		AmazonS3 s3 = S3ClientConnection.S3Client("tasadora-test");
		
        String bucket = "tasadoratest";
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
		private ProgressListener progressListener;
		
		public UploadRequest(String bucket, String key, File file, AmazonS3 s3, long partSize, long filePosition, 
				int sequence, List<PartETag> partETags,
				InitiateMultipartUploadResult initResponse, S3UploadTracker s3UploadTracker, ProgressListener progressListener) {
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
			this.progressListener = progressListener;
		}


		@Override
		public void run() {
			if ( LOG_THREAD_INFO ) {
				System.out.println(Thread.currentThread().getName() + "...: From FilePosition:" + filePosition);
			}
			
			//s3UploadTracker.startedSnapShot(Thread.currentThread().getName());
			
			UploadPartRequest uploadRequest = new UploadPartRequest()
			        .withBucketName(bucket)
			        .withKey(key)
			        .withUploadId(initResponse.getUploadId())
			        .withPartNumber(sequence)
			        .withFileOffset(filePosition)
			        .withFile(file)
			        .withPartSize(partSize);
			
			
			uploadRequest.setGeneralProgressListener(this.progressListener);
			
			// Upload the part and add the response's ETag to our list.
			UploadPartResult uploadResult = s3.uploadPart(uploadRequest);
			PartETag partETag = uploadResult.getPartETag();
			partETags.add(partETag);
			
			s3UploadTracker.addPartETag(partETag);
			s3UploadTracker.setFilePosition(filePosition);
			s3UploadTracker.finishedSnapShot(Thread.currentThread().getName());
			
			if ( LOG_THREAD_SUMMARY ) {
				StringBuffer sb = new StringBuffer();
				sb.append("\n\n");
				sb.append("*-----------------------------------------------------------------------*").append("\n");
				sb.append("> Thread ").append(Thread.currentThread().getName()).append(" Finalized:\n");
				sb.append(">>>  ").append("FilePosition...:").append(filePosition).append("\n");
				sb.append(">>>  ").append("PartSize.......:").append(partSize).append("\n");
				sb.append(">>>  ").append("ETag...........:").append(uploadResult.getPartETag().getETag()).append("\n");
				sb.append(">>>  ").append("PartNumber.....:").append(uploadResult.getPartETag().getPartNumber()).append("\n");
				sb.append(">>>  ").append("Release Lock...:").append(Thread.currentThread().getName()).append("\n");
				sb.append("*-----------------------------------------------------------------------*").append("\n\n");
				System.out.println(sb.toString());
			}
			SEMAPHORE.release();
			
			//System.out.println("Available now " +  SEMAPHORE.availablePermits() + " spaces to launch new Threds");
		}
		
	}

}
