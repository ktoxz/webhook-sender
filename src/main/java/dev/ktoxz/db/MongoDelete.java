package dev.ktoxz.db;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.json.JsonWriterSettings;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOptions;

public class MongoDelete {

    private String dbName;
    private String colName;
    private MongoDatabase db;
    private MongoCollection<Document> col;

    public MongoDelete(String dbName, String colName) {
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

    //Delete----------------
    public void Delete(List<Bson> filters) {
        Mongo.getInstance().Delete(col, filters);
    }

    public boolean Delete(Bson filter) {
    	try {
        	Mongo.getInstance().Delete(col, Arrays.asList(filter));
    	} catch (Exception e) {
    		return false;
    	}
    	return true;

    }


    //Debug----------------------
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
}