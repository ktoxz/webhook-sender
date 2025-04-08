package dev.ktoxz.db;

import java.util.List;
import org.bson.Document;
import org.bson.json.JsonWriterSettings;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

public class MongoFind {
    
    private String dbName;
    private String colName;
    private MongoDatabase db;
    private MongoCollection<Document> col;
    
    public MongoFind(String dbName, String colName) {
        this.dbName = dbName;
        this.colName = colName;
        try {
            Mongo.getInstance().Connect();
            db = Mongo.getInstance().mongoClient.getDatabase(dbName);
            col = db.getCollection(colName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public Document One(List<Document> filters, List<Document> projections) {
        return Mongo.getInstance().Find(col, filters, projections).first();
    }
    
    public FindIterable<Document> Many(List<Document> filters, List<Document> projections) {
        return Mongo.getInstance().Find(col, filters, projections);
    }
    
    public Document One(Document query, Document projection) {
        return col.find(query).projection(projection).first();
    }
    
    public void Debug() {
        try {
            System.out.println("Contents of collection: " + colName);
            for (Document doc : col.find()) {
                System.out.println(doc.toJson(JsonWriterSettings.builder().indent(true).build()));
            }
        } catch (Exception e) {
            System.err.println("Error during debug operation: " + e.getMessage());
            e.printStackTrace();
        }
    }

	public MongoDatabase getDb() {
		return db;
	}

	public void setDb(MongoDatabase db) {
		this.db = db;
	}

	public MongoCollection<Document> getCol() {
		return col;
	}

	public void setCol(MongoCollection<Document> col) {
		this.col = col;
	}
    
    
}