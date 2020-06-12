package com.amazonaws.sandbox.s3.multipart.lowlevel;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.exception.ExceptionUtils;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.transfer.MultipleFileUpload;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;

public class ArtifactoryTransferManager {
	
	public static void main(String[] args) {
		
		//String origin = "/var/ecommerce/artifactory_home/data/filestore/";
		String ORIGIN      = "/data/dev/uaza/";
		String BUCKET_NAME = "emagin-delivery/general/artifactory/";
		String KEY_PREFIX  = "";
		
		if (args.length < 1) {
			System.out.println("Usage (in the order):");
			System.out.println("  ArtifactoryTransferManager --folder [FOLDER_NAME]");
			System.out.println("  ArtifactoryTransferManager --foldersfromfile");
			System.exit(0);
		} else
		if ( args[0].equalsIgnoreCase("--folder") ){
			String originFolder  = args[1];
			String bucketDestiny = args[1];
			
			copyFolderRecursively(ORIGIN + originFolder, BUCKET_NAME + bucketDestiny, KEY_PREFIX);
			
		} else
		if ( args[0].equalsIgnoreCase("--foldersfromfile") ){
			String fileName = "/data/dev/uaza/aws/aws-java-samples/src/main/resources/folders_to_copy.txt";
			Stream<String> stream;
			String currentFolder = "";
			try {
				stream = Files.lines(Paths.get(fileName));
				List<String> foldersToCopy = stream.collect(Collectors.toList());
				
				for(String folder : foldersToCopy) {
					currentFolder = folder;
					String source   = ORIGIN      + folder;
					String destiny  = BUCKET_NAME + folder;
					
					try {
						//if ( folder.equalsIgnoreCase("08") ) {
						//	throw new RuntimeException("Deu merda!");
						//}
						System.out.println(source + " --> to --> " +  destiny);
						copyFolderRecursively(source, destiny, KEY_PREFIX);
						saveTheSuccess(currentFolder);
					} catch (Throwable e) {
						saveTheError(currentFolder, e);
					}
				}
				
			} catch (Throwable e) {
				System.out.println(e.getMessage());
				saveTheError(currentFolder, e);
				e.printStackTrace();
			}
			
			System.exit(0);
			
		} 
		
		System.out.println("Sorry! You did not say anything, what exactly can I do for you? \n");
		System.out.println("Usage (in the order):");
		System.out.println("  ArtifactoryTransferManager --folder 00");
		System.out.println("");
		
	}
	
	private static void saveTheSuccess(String currentFolder) {
		try {
			String fSuccess = "/data/dev/uaza/aws/aws-java-samples/" + currentFolder + "_OK.txt";
			FileWriter fw = new FileWriter(fSuccess);
			LocalDateTime today = LocalDateTime.now();
			String formattedDate = today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
			fw.write("Finish copy at " + formattedDate + "\n");
			fw.flush();
			fw.close();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	private static void saveTheError(String currentFolder, Throwable e) {
		try {
			String fError = "/data/dev/uaza/aws/aws-java-samples/" + currentFolder + "_Error.txt";
			System.out.print("\033[0;33m");
			System.out.println("Houston! For the folder " +  currentFolder + ", was thrown the error: \033[1;34m" +  e.getMessage() + "\033[0;33m, check file:" + fError);
			System.out.print("\033[0m");
			FileWriter fw = new FileWriter(fError);
			fw.write(ExceptionUtils.getStackTrace(e));
			fw.flush();
			fw.close();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	private static void copyFolderRecursively(String origin, String bucketName, String keyPrefix) {
		boolean recursive = true;
		AmazonS3 s3 = S3ClientConnection.S3Client("ecomm");
		TransferManager xfer_mgr = TransferManagerBuilder
				.standard()
				.withS3Client(s3)
				.build();
		try {
		    MultipleFileUpload xfer = xfer_mgr.uploadDirectory(bucketName,
		    		keyPrefix, new File(origin), recursive);
		    // loop with Transfer.isDone()
		    XferMgrProgress.showTransferProgress(xfer,"Uploading folder " + origin);
		    // or block with Transfer.waitForCompletion()
		    XferMgrProgress.waitForCompletion(xfer);
		} catch (AmazonServiceException e) {
		    System.err.println(e.getErrorMessage());
		    System.exit(1);
		}
		xfer_mgr.shutdownNow();
	}

}
