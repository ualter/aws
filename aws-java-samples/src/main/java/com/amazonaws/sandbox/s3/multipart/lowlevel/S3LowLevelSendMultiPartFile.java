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
	
	private static final boolean LOG_THREAD_INFO = false;
	
	public static void main(String[] args) throws Exception {
		
		String bucket   = "tasadoratest";
		String key      = "request/tasacion-file-multipartedUploaded.pdf";
		String fileName = "/Users/ualter/Temp/529-2712-1-PB.pdf";
		long   partSize = 10;
		
		S3LowLevelSendMultiPartFile s = new S3LowLevelSendMultiPartFile();
		s.multipartUploadsFile(bucket, key, fileName, partSize);
	}

    static int THREAD_POOL_SIZE = 5;
    static Semaphore SEMAPHORE  = new Semaphore(THREAD_POOL_SIZE);
    
    S3UploadTracker s3UploadTracker = new S3UploadTracker();
    S3LowLevelProgressListener progressListener = new S3LowLevelProgressListener();
	
	private void multipartUploadsFile(String bucket, String key, String fileName, long blockSize) {
        try {
            File file           = new File(fileName);
        	long contentLength  = file.length();
        	long partSize       = blockSize * 1024 * 1024;
        	
        	s3UploadTracker.setBucketName(bucket);
        	s3UploadTracker.setKey(key);
        	s3UploadTracker.setFile(fileName);
        	
        	AmazonS3 s3 = S3ClientConnection.S3Client("tasadora-test");
        	
        	TimeTracker timeTracker = TimeTracker.getInstance();        	
    		
    		List<PartETag> partETags = new ArrayList<PartETag>();
    		UploadRequest uploadRequest;
    		// Initiate the multipart upload.
            InitiateMultipartUploadRequest initRequest  = new InitiateMultipartUploadRequest(bucket, key);
            InitiateMultipartUploadResult  initResponse = s3.initiateMultipartUpload(initRequest);
            
            System.out.println("***************************************************");
            System.out.println("*** Starting the Upload: " + initResponse.getUploadId());
            timeTracker.startTracking("Upload File S3");
            
            s3UploadTracker.setUploadId(initResponse.getUploadId());
            
            boolean startListen = false;
            // Upload the file parts.
            long filePosition = 0;
            for (int i = 1; filePosition < contentLength; i++) {
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
            
            System.out.println("\n\n******************* Waiting the remaining Threads to Finalize its job");
            while ( SEMAPHORE.availablePermits() < THREAD_POOL_SIZE );
            System.out.println("******************* All done!");
            
            // Complete the multipart upload.
            CompleteMultipartUploadRequest compRequest = new CompleteMultipartUploadRequest(bucket, key, initResponse.getUploadId(), partETags);
            CompleteMultipartUploadResult resultUpload = s3.completeMultipartUpload(compRequest);
            System.out.println(resultUpload.getKey() + " uploaded!\n");
			System.out.println(timeTracker.endTracking("Upload File S3"));
			
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
			
			System.out.println(Thread.currentThread().getName() + "...: From FilePosition:" + filePosition);
			
			UploadPartRequest uploadRequest = new UploadPartRequest()
			        .withBucketName(bucket)
			        .withKey(key)
			        .withUploadId(initResponse.getUploadId())
			        .withPartNumber(sequence)
			        .withFileOffset(filePosition)
			        .withFile(file)
			        .withPartSize(partSize);
			
			
			uploadRequest.setGeneralProgressListener(this.progressListener);
			
			s3UploadTracker.setSequenceName(Thread.currentThread().getName());
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
			sb.append("***** Thread ").append(Thread.currentThread().getName()).append(" Finalized:\n");
			sb.append("            ").append("FilePosition...:").append(filePosition).append("\n");
			sb.append("            ").append("PartSize.......:").append(partSize).append("\n");
			sb.append("            ").append("ETag...........:").append(uploadResult.getPartETag().getETag()).append("\n");
			sb.append("            ").append("PartNumber.....:").append(uploadResult.getPartETag().getPartNumber()).append("\n");
			sb.append("            ").append("Release Lock...:").append(Thread.currentThread().getName()).append("\n");
			System.out.println(sb.toString());
			SEMAPHORE.release();
			
			//System.out.println("Available now " +  SEMAPHORE.availablePermits() + " spaces to launch new Threds");
		}
		
	}

}
