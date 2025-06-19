package dev.ktoxz.db;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.*;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.InsertManyOptions;
import com.mongodb.client.model.UpdateOptions;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.json.JsonWriterSettings;

import static com.mongodb.client.model.Updates.*;

import java.util.List;

public class Mongo {
    private static Mongo instance;
    private boolean isConnected = false;
    protected MongoClient mongoClient;

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

        try {
            ConnectionString connectionString = new ConnectionString(
                "mongodb://thanhkhoi448:mongo123@ac-mozrfmu-shard-00-00.ehgjc1m.mongodb.net:27017," +
                "ac-mozrfmu-shard-00-01.ehgjc1m.mongodb.net:27017," +
                "ac-mozrfmu-shard-00-02.ehgjc1m.mongodb.net:27017/" +
                "?replicaSet=atlas-9om431-shard-0&ssl=true&authSource=admin&retryWrites=true&w=majority&appName=DiscordMinecraftOwO"
            );

            MongoClientSettings settings = MongoClientSettings.builder()
                    .applyConnectionString(connectionString)
                    .applyToSslSettings(ssl -> ssl.enabled(true).invalidHostNameAllowed(true)) // ⚠ chỉ nên bật khi test
                    .build();

            mongoClient = MongoClients.create(settings);

            Document ping = new Document("ping", 1);
            Document response = mongoClient.getDatabase("admin").runCommand(ping);
            isConnected = response.get("ok", Number.class).intValue() == 1;

            if (isConnected) {
                System.out.println("✅ MongoDB connected.");
                System.out.println(response.toJson(JsonWriterSettings.builder().indent(true).build()));
            }

            return isConnected;
        } catch (Exception e) {
            System.err.println("❌ Error while connecting to MongoDB: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public void closeConnection() {
        if (mongoClient != null) {
            mongoClient.close();
            isConnected = false;
            System.out.println("🔌 MongoDB connection closed.");
        }
    }

    // Find
    public FindIterable<Document> Find(MongoCollection<Document> collection, List<Document> filters, List<Document> projections) {
        try {
            Document filter = new Document();
            if (filters != null) filters.forEach(filter::putAll);

            Document projection = new Document();
            if (projections != null) projections.forEach(projection::putAll);

            return collection.find(filter).projection(projection);
        } catch (Exception e) {
            System.err.println("❌ Error during find: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // Insert
    public void Insert(MongoCollection<Document> col, List<Document> lDoc) throws Exception {
    	if (lDoc == null || lDoc.isEmpty()) return;
        if (lDoc.size() == 1) {
            col.insertOne(lDoc.get(0));
        } else {
            col.insertMany(lDoc, new InsertManyOptions().ordered(false));
        }
    }

    // Update
    public void Update(MongoCollection<Document> col, List<Document> filters, List<Bson> updates, UpdateOptions upsert) {
        try {
            Document filter = new Document();
            if (filters != null) filters.forEach(filter::putAll);
            Bson update = combine(updates);
            col.updateMany(filter, update, upsert);
        } catch (Exception e) {
            System.err.println("❌ Error during update: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Delete
    public void Delete(MongoCollection<Document> col, List<Bson> filters) {
        try {
            col.deleteMany(Filters.and(filters));
        } catch (Exception e) {
            System.err.println("❌ Error during delete: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Aggregate
    public AggregateIterable<Document> Aggregate(MongoCollection<Document> col, List<Bson> pipeline) {
        try {
            return col.aggregate(pipeline);
        } catch (Exception e) {
            System.err.println("❌ Error during aggregate: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // Thêm phương thức này để lấy collection "arena"
    public MongoCollection<Document> getArenas() {
        if (!isConnected || mongoClient == null) {
            Connect(); // Đảm bảo kết nối được thiết lập
            if (!isConnected) {
                System.err.println("❌ Không thể kết nối MongoDB để lấy collection 'arena'.");
                return null;
            }
        }
        return mongoClient.getDatabase("minecraft").getCollection("arena"); // "minecraft" là tên database
    }
}