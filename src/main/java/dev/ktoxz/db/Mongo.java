package dev.ktoxz.db;

import com.mongodb.client.AggregateIterable;
import com.mongodb.client.FindIterable;


import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.InsertManyOptions;
import com.mongodb.client.model.UpdateOptions;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.json.JsonWriterSettings;

import static com.mongodb.client.model.Updates.*;

import java.util.List;


public class Mongo {
    //I. Attributes
    private static Mongo instance;
    private boolean isConnected = false;
    protected MongoClient mongoClient;

    
    //II. Basic methods: getInstance, Connect, closeConnection
    public static Mongo getInstance() {
        if (instance == null) {
            synchronized (Mongo.class) {
                if (instance == null) {
                    instance = new Mongo();
                }
            }
        }
        return instance;
    }
    
    
    public MongoClient getMongoClient() {
    	return mongoClient;
    }

    public boolean Connect() {
        if (isConnected) return true;

        String connectionString = "mongodb://thanhkhoi448:mongo123@ac-mozrfmu-shard-00-00.ehgjc1m.mongodb.net:27017,ac-mozrfmu-shard-00-01.ehgjc1m.mongodb.net:27017,ac-mozrfmu-shard-00-02.ehgjc1m.mongodb.net:27017/?replicaSet=atlas-9om431-shard-0&ssl=true&authSource=admin&retryWrites=true&w=majority&appName=DiscordMinecraftOwO";
        try {
            mongoClient = MongoClients.create(connectionString);
            Document pingCommand = new Document("ping", 1);
            Document response = mongoClient.getDatabase("admin").runCommand(pingCommand);
            isConnected = response.get("ok", Number.class).intValue() == 1;

            if (isConnected) {
                System.out.println("Connected to MongoDB.");
                System.out.println(response.toJson(JsonWriterSettings.builder().indent(true).build()));
            }
            return isConnected;
        } catch (Exception e) {
            System.err.println("Error while connecting to MongoDB: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public void closeConnection() {
        if (mongoClient != null) {
            mongoClient.close();
            isConnected = false;
            System.out.println("MongoDB connection closed.");
        }
    }

    //III. Main methods: Find, Insert, Update, Delete
    FindIterable<Document> Find(MongoCollection<Document> collection, List<Document> filters, List<Document> projections) {
        try {
            Document filter = new Document();
            if (filters != null) {
                for (Document doc : filters) {
                    filter.putAll(doc);
                }
            }

            Document projection = new Document();
            if (projections != null) {
                for (Document doc : projections) {
                    projection.putAll(doc);
                }
            }

            return collection.find(filter).projection(projection);
        } catch (Exception e) {
            System.err.println("Error during find operation: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    void Insert(MongoCollection<Document> col, List<Document> lDoc) {
        if (lDoc == null || lDoc.isEmpty()) {
            return;
        }
        if (lDoc.size() == 1) {
            col.insertOne(lDoc.get(0));
        } else {
            col.insertMany(lDoc, new InsertManyOptions().ordered(false));
        }
    }

    void Update(MongoCollection<Document> col, List<Document> filters, List<Bson> updates, UpdateOptions upsert) {
        /*
        //Who to update?
        List<Document> filters = Arrays.asList(new Document("student_id", 10001));

        //What to update?
        List<Bson> updates = Arrays.asList(
            new Document("$set", new Document("comment", "Updated comment")),
            new Document("$inc", new Document("score", 5))
        );

        //Do you want to upsert or not?
        UpdateOptions upsert = new UpdateOptions().upsert(true);

        //Call the method
        mongoInstance.Update(collection, filters, updates, upsert);
         */
        try {
            Document filter = new Document();
            if (filters != null) {
                for (Document doc : filters) {
                    filter.putAll(doc);
                }
            }

            Bson update = combine(updates); //The combine method is specifically for combining multiple update operations, not for combining filters

            col.updateMany(filter, update, upsert);
        } catch (Exception e) {
            System.err.println("Error during update operation: " + e.getMessage());
            e.printStackTrace();
        }
    }

    void Delete(MongoCollection<Document> col, List<Bson> filters) {
        /*
        List<Bson> filters = Arrays.asList(
                eq("student_id", 10000),
                eq("class_id", 10)
        );
        mongoInstance.Delete(collection, filters);
        */
        try {
            col.deleteMany(Filters.and(filters));
        } catch (Exception e) {
            System.err.println("Error during delete operation: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public AggregateIterable<Document> Aggregate(MongoCollection<Document> col, List<Bson> pipeline) {
        /*
        // Ví dụ sử dụng:
        List<Bson> pipeline = Arrays.asList(
            Aggregates.match(Filters.eq("class_id", 10)),
            Aggregates.group("$student_id", Accumulators.sum("total_score", "$score"))
        );
        AggregateIterable<Document> result = mongoInstance.Aggregate(collection, pipeline);
        */

        try {
            return col.aggregate(pipeline);
        } catch (Exception e) {
            System.err.println("Error during aggregation: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    
}
