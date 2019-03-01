package ujr.aws.lambda.event.handler;


import java.io.IOException;
import java.util.List;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.GetObjectTaggingRequest;
import com.amazonaws.services.s3.model.GetObjectTaggingResult;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.Tag;
import com.fasterxml.jackson.databind.ObjectMapper;

import ujr.aws.lambda.event.handler.model.event.Record;
import ujr.aws.lambda.event.handler.model.event.Records;

public class LambdaEventReceiver implements RequestHandler<S3Event, String> {

    private AmazonS3 s3 = AmazonS3ClientBuilder.standard().build();

    public LambdaEventReceiver() {}

    // Test purpose only.
    public LambdaEventReceiver(AmazonS3 s3) {
        this.s3 = s3;
    }

    @Override
    public String handleRequest(S3Event event, Context context) {
        context.getLogger().log("Received event: " + event);
        
        String eventInJson = event.toJson();
        context.getLogger().log("Received event in JSON: " + eventInJson);
        
        
		try {
			ObjectMapper mapper = new ObjectMapper();
			Records records = mapper.readValue(eventInJson, Records.class);
			List<Record> listRecords = records.getRecords();
			for(Record record : listRecords ) {
				
				String bucketName = record.getS3().getBucket().getName();
				String tasadora   = bucketName.substring(bucketName.indexOf("-") + 1);
				String keyFile    = record.getS3().getObject().getKey();
				String direction  = keyFile.substring(0, keyFile.indexOf("/"));
				
				context.getLogger().log("---> TASADORA...: " + tasadora);
				context.getLogger().log("---> TYPE COMM..: " + direction);
				context.getLogger().log("---> FICHERO....: " + keyFile);
				context.getLogger().log("---> SIZE.......: " + record.getS3().getObject().getSize());
				
			}
		} catch (IOException e1) {
			context.getLogger().log("Exception: " + e1.getMessage());
		}
        

        // Get the object from the event and show its content type
        String bucket = event.getRecords().get(0).getS3().getBucket().getName();
        String key = event.getRecords().get(0).getS3().getObject().getKey();
        try {
        	GetObjectTaggingResult taggingResult = s3.getObjectTagging(new GetObjectTaggingRequest(bucket, key));
        	context.getLogger().log("Total Tags found: " + taggingResult.getTagSet().size());
        	for(Tag tag : taggingResult.getTagSet()) {
        		context.getLogger().log("tag: " + tag.getKey() + "=" + tag.getValue());
        	}
        	
            S3Object response = s3.getObject(new GetObjectRequest(bucket, key));
            String contentType = response.getObjectMetadata().getContentType();
            context.getLogger().log("CONTENT TYPE: " + contentType);
            
            
            return "Here's the Result of the Tasadora notification!";
            
        } catch (Exception e) {
            e.printStackTrace();
            context.getLogger().log(String.format(
                "Error getting object %s from bucket %s. Make sure they exist and"
                + " your bucket is in the same region as this function.", key, bucket));
            throw e;
        }
    }
}