package com.amazonaws.sandbox.dynamodb;

public class DynamoDBSandbox {
  
  private static String TABLE_NAME = "Books";

	public static void main(String[] args) {
		createTable();
		createRecords(10);
	}

	private static void createRecords(int qtde) {
		if (qtde > 0 ) {
			AmazonDynamoDB dynamoDbClient = createDynamoDBClient("tasadora-test");
			DynamoDB dynamoDB = new DynamoDB(dynamoDbClient);
			Table table = dynamoDB.getTable(TABLE_NAME);
	
			Random rand = new Random();
			
			for( int i = 1; i <= qtde; i++ ) {
				Item item = new Item()
						.withPrimaryKey("Id", i)
						.withPrimaryKey("TimeStamp",Calendar.getInstance().getTimeInMillis())
						.withString("Title", "Book " + i + " Title")
						.withString("ISBN", i + "-1111111111")
						.withStringSet("Authors", new HashSet<String>(Arrays.asList("Author" + rand.nextInt(2), "Author2" + rand.nextInt(2))))
						.withNumber("Price", rand.nextFloat() * rand.nextInt(99)).withNumber("PageCount", rand.nextInt(500))
						.withString("ProductCategory", "Book");
				table.putItem(item);
			}
		}
	}
	
	public static boolean isTableExist() {
        try {
        	AmazonDynamoDB dynamoDbClient = createDynamoDBClient("tasadora-test");
    		DynamoDB dynamoDB = new DynamoDB(dynamoDbClient);
            TableDescription tableDescription = dynamoDB.getTable(TABLE_NAME).describe();
            System.out.println("Table description: " + tableDescription.getTableStatus());
            return true;
        } catch (com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException rnfe) {
            System.out.println("Table does not exist");
        }
        return false;
    }

	private static void createTable() {
		if ( !isTableExist() ) {
			AmazonDynamoDB dynamoDbClient = createDynamoDBClient("tasadora-test");
			DynamoDB dynamoDB = new DynamoDB(dynamoDbClient);
	
			List<AttributeDefinition> attributeDefinitions = new ArrayList<AttributeDefinition>();
			attributeDefinitions.add(new AttributeDefinition().withAttributeName("Id").withAttributeType("N"));
			attributeDefinitions.add(new AttributeDefinition().withAttributeName("TimeStamp").withAttributeType("N"));
	
			List<KeySchemaElement> keySchema = new ArrayList<KeySchemaElement>();
			keySchema.add(new KeySchemaElement().withAttributeName("Id").withKeyType(KeyType.HASH));
			keySchema.add(new KeySchemaElement().withAttributeName("TimeStamp").withKeyType(KeyType.RANGE));
	
			CreateTableRequest request = new CreateTableRequest().withTableName(TABLE_NAME).withKeySchema(keySchema)
					.withAttributeDefinitions(attributeDefinitions).withProvisionedThroughput(
							new ProvisionedThroughput().withReadCapacityUnits(5L).withWriteCapacityUnits(5L));
	
			Table table = dynamoDB.createTable(request);
	
			try {
				table.waitForActive();
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
	}

	private static AmazonDynamoDB createDynamoDBClient(String user) {
		File credentialsFilePath = new File(
				S3ClientConnection.class.getClassLoader().getResource(user + "-credentials.properties").getFile());
		PropertiesFileCredentialsProvider propertiesFileCredentialsProvider = new PropertiesFileCredentialsProvider(
				credentialsFilePath.getPath());

		ClientConfiguration clientConfiguration = new ClientConfiguration();
		clientConfiguration.setProtocol(Protocol.HTTPS);
		clientConfiguration.setProxyHost("cache.bancsabadell.com");
		clientConfiguration.setProxyPort(8080);

		AmazonDynamoDB dynamoDbClient = AmazonDynamoDBClientBuilder.standard().withRegion(Regions.EU_WEST_1)
				.withCredentials(propertiesFileCredentialsProvider).withClientConfiguration(clientConfiguration)
				.build();
		return dynamoDbClient;
	}

}
