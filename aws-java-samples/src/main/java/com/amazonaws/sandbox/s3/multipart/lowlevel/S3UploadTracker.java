package com.amazonaws.sandbox.s3.multipart.lowlevel;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import com.amazonaws.services.s3.model.PartETag;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;

public class S3UploadTracker {
	
	private String bucketName;
	private String key;
	private String uploadId;
	private String file;
	private int    partSize;
	private long   filePosition;
	private List<PartSaved> listPartSaved = new ArrayList<PartSaved>();
	private boolean finalizedSuccessfully;
	private String pathFiles = "/Users/ualter/Temp/";
	private String folder;
	private int sequence;
	private boolean setUp;
	
	private static DecimalFormat DF = new DecimalFormat("#00");
	
	public static void main(String[] args) {
		S3UploadTracker s3UploadTracker = new S3UploadTracker("bucketName","keyName");
		s3UploadTracker.setFilePosition(12390);
		s3UploadTracker.setPartSize(10);
		s3UploadTracker.setUploadId("fdsjfklsjdlkfjsdl");
		s3UploadTracker.addPartETag(new PartETag(1, "1231231432423"));
		s3UploadTracker.addPartETag(new PartETag(2, "1231231432423"));
		s3UploadTracker.addPartETag(new PartETag(3, "1231231432423"));
		String fileName = s3UploadTracker.printSnapShot();
		
		S3UploadTracker s3UploadTrackerSaved = S3UploadTracker.loadMe(fileName);
		System.out.println(s3UploadTrackerSaved.getBucketName());
		System.out.println(s3UploadTrackerSaved.getListPartETagSaved());
	}
	
	public S3UploadTracker() {
	}
	
	public S3UploadTracker(String bucketName, String key) {
		super();
		this.bucketName = bucketName;
		this.key = key;
	}

	public String printSnapShot() {
		if ( !this.setUp ) {
			setUpS3UploadTracker();
		}
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyddmm_hhMMss");
		
		String resultFileName = this.folder 
				+ DF.format(++sequence)
				+ "-"
				+ this.key.replaceAll("/", "_") 
				+ "-" 
				+ sdf.format(Calendar.getInstance().getTime());
		
		if ( this.isFinalizedSuccessfully() ) {
			resultFileName += "-finalized.txt";
		} else {
			resultFileName += ".txt";
		}
		ObjectMapper mapper = new ObjectMapper();
		try {
			mapper.writeValue(new File(resultFileName), this);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return resultFileName;
	}

	private void setUpS3UploadTracker() {
		this.folder = pathFiles + this.key.replaceAll("/", "_") + "/";
		File fDir = new File(this.folder);
		
		fDir.mkdir();
		this.setUp = true;
	}
	
	public static S3UploadTracker loadMe(String fromWhere) {
		ObjectMapper     mapper         = new ObjectMapper();
		try {
			return mapper.readValue(new File(fromWhere), S3UploadTracker.class);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public void addPartETag(PartETag partETag) {
		PartSaved partETagSaved = new PartSaved();
		partETagSaved.setPartNumberValue(partETag.getPartNumber());
		partETagSaved.setTagValue(partETag.getETag());
		this.listPartSaved.add(partETagSaved);
	}
	
	public List<PartSaved> getListPartSaved() {
		return listPartSaved;
	}

	public void setListPartSaved(List<PartSaved> listPartSaved) {
		this.listPartSaved = listPartSaved;
	}

	@JsonIgnore
	public List<PartETag> getListPartETagSaved() {
		List<PartETag> listPartETag = new ArrayList<PartETag>();
		for(PartSaved ps : this.listPartSaved ) {
			listPartETag.add(new PartETag(ps.getPartNumberValue(), ps.getTagValue()));
		}
		return listPartETag;
	}

	
	public boolean isFinalizedSuccessfully() {
		return finalizedSuccessfully;
	}

	public void setFinalizedSuccessfully(boolean finalizedSuccessfully) {
		this.finalizedSuccessfully = finalizedSuccessfully;
	}

	public String getBucketName() {
		return bucketName;
	}

	public void setBucketName(String bucketName) {
		this.bucketName = bucketName;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getUploadId() {
		return uploadId;
	}

	public void setUploadId(String uploadId) {
		this.uploadId = uploadId;
	}

	public String getFile() {
		return file;
	}

	public void setFile(String file) {
		this.file = file;
	}

	public int getPartSize() {
		return partSize;
	}

	public void setPartSize(int partSize) {
		this.partSize = partSize;
	}

	public long getFilePosition() {
		return filePosition;
	}

	public void setFilePosition(long filePosition) {
		this.filePosition = filePosition;
	}


	
	public static class PartSaved {
		private int partNumberValue;
		private String tagValue;
		
		public int getPartNumberValue() {
			return partNumberValue;
		}
		public void setPartNumberValue(int partNumber) {
			this.partNumberValue = partNumber;
		}
		public String getTagValue() {
			return tagValue;
		}
		public void setTagValue(String tag) {
			this.tagValue = tag;
		}
		
	}
	
	

}
