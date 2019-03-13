package com.amazonaws.sandbox.s3.multipart.lowlevel;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.event.ProgressListener;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.CompleteMultipartUploadResult;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.PartETag;
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
		String uploadId          = "IAxz_FRe7MAh1mmTDJ__Re4z2VFmyxcQ2FjKFr3kYa3kPZCP6goMF7jpA40iAfznvTotQKgBoa.wojPWb0CbeDQpQIeeWQNCxYT4py2JI3UpJvxl2fZBxzcPMGn2cBCE";
		String file              = "/Users/ualter/Temp/529-2712-1-PB.pdf";
		int    partSize          = 10;
		long   startFilePosition = 31457280 + (partSize * 1024 * 1024); // Add the part already uploaded with the Last Thread (filePosition + partSize)
		
		List<PartETag> partETags = new ArrayList<PartETag>();
		partETags.add(new PartETag(1, "3beb7e9af8674013a48d78ba14b4b075"));
		partETags.add(new PartETag(2, "5bfdc19fa1fa7b7b347f2c13a500d14c"));
		partETags.add(new PartETag(3, "ef18c3c8c4b3162258a35c3bf424bc15"));
		partETags.add(new PartETag(4, "f31091770d8053754a02715dca601fa3"));
		partETags.add(new PartETag(5, "f31aa43f51fba0f8991eb3971a9c55b0"));
		
		boolean incremental             = true;
		boolean finalizeMutilPartUpload = true;
		
		S3ResumeLowLevelSendMultiPartFile s3ResumeUpload = new S3ResumeLowLevelSendMultiPartFile();
		s3ResumeUpload.multipartResumeUploadsFile(
				bucket,
				key,
				uploadId,
				file,
				partETags,
				6,
				startFilePosition,
				partSize,
				incremental, finalizeMutilPartUpload);
	}

	static int THREAD_POOL_SIZE = 5;
	static Semaphore SEMAPHORE  = new Semaphore(THREAD_POOL_SIZE);
	
	S3UploadTracker s3UploadTracker = new S3UploadTracker();
	S3LowLevelProgressListener progressListener = new S3LowLevelProgressListener();
	
	private void multipartResumeUploadsFile(String bucket, String key, 
			String uploadId, String fileName, List<PartETag> parETagsAlreadyUploaded,
			int startSequence, long startFilePosition, int partSizeMB, boolean incremental, boolean finalizeMutilPartUpload) {
		
		s3UploadTracker.setBucketName(bucket);
    	s3UploadTracker.setKey(key);
    	s3UploadTracker.setFile(fileName);
		
        try {
			File file           = new File(fileName);
        	long contentLength  = file.length();
        	long partSize       = partSizeMB * 1024 * 1024;
        	
        	TimeTracker timeTracker = TimeTracker.getInstance();
        	
        	AmazonS3 s3 = S3ClientConnection.S3Client("tasadora-test");
    		
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
            if ( incremental ) {
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
	            	
	                uploadRequest = new UploadRequest(bucket, key, file, s3, partSize, filePosition, i, partETags, initResponse, 
	                		this.s3UploadTracker, this.progressListener);
	                Thread t = new Thread(uploadRequest);
	                t.setName("***** Upload-" + i);
	                t.setDaemon(true); 
	                t.start();
	                
	                filePosition += partSize;
	            }
	            
	            System.out.println("\nWaiting the remaining Threads to Finalize its job");
	            while ( SEMAPHORE.availablePermits() < THREAD_POOL_SIZE );
	            System.out.println("All done!");
	            
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
            
            if (finalizeMutilPartUpload) {
	            // Complete the multipart upload.
	            CompleteMultipartUploadRequest compRequest = new CompleteMultipartUploadRequest(bucket, key, initResponse.getUploadId(), partETags);
	            CompleteMultipartUploadResult resultUpload = s3.completeMultipartUpload(compRequest);
	            System.out.println(resultUpload.getKey() + " uploaded!\n");
				System.out.println(timeTracker.endTracking("Upload File S3"));
            }
			
			this.s3UploadTracker.setFinalizedSuccessfully(true);
			this.s3UploadTracker.finishedSnapShot();
			
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
			System.out.println(Thread.currentThread().getName() + "...: FilePosition:" + filePosition + ", PartSize:" + partSize);
			
			UploadPartRequest uploadRequest = new UploadPartRequest()
			        .withBucketName(bucket)
			        .withKey(key)
			        .withUploadId(initResponse.getUploadId())
			        .withPartNumber(sequence)
			        .withFileOffset(filePosition)
			        .withFile(file)
			        .withPartSize(partSize);
			
			uploadRequest.setGeneralProgressListener(this.progressListener);
			
			s3UploadTracker.setFilePosition(filePosition);
			s3UploadTracker.startedSnapShot();
			
			// Upload the part and add the response's ETag to our list.
			UploadPartResult uploadResult = s3.uploadPart(uploadRequest);
			PartETag partETag = uploadResult.getPartETag();
			partETags.add(partETag);
			
			s3UploadTracker.addPartETag(partETag);
			s3UploadTracker.setFilePosition(filePosition);
			s3UploadTracker.finishedSnapShot();
			
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
