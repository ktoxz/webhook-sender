package dev.ktoxz.db;

import java.util.Arrays;
import java.util.List;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.json.JsonWriterSettings;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOptions;

public class MongoUpdate {

    private String dbName;
    private String colName;
    private MongoDatabase db;
    private MongoCollection<Document> col;

    public MongoUpdate(String dbName, String colName) {
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

    public boolean Update(Document filter, Document update) {
        try {
        	List<Document> filterList;
            if (filter instanceof Document) {
                filterList = Arrays.asList((Document) filter);
            } else if (filter instanceof List) {
                filterList = (List<Document>) filter;
            } else {
                throw new IllegalArgumentException("Filter must be a Document or a List<Document>");
            }

            // Handle update (Bson or List<Bson>)
            List<Bson> updateList;
            if (update instanceof Bson) {
                updateList = Arrays.asList((Bson) update);
            } else if (update instanceof List) {
                updateList = (List<Bson>) update;
            } else {
                throw new IllegalArgumentException("Update must be a Bson or a List<Bson>");
            }
            
            Mongo.getInstance().Update(col, filterList, updateList, new UpdateOptions().upsert(false));

        } catch (Exception e) {
        	return false;
        }
        
        return true;
        

        // Perform the update operation
    }


    public void Upsert(Object filter, Object update) {
        // Handle filter (Document or List<Document>)
        List<Document> filterList;
        if (filter instanceof Document) {
            filterList = Arrays.asList((Document) filter);
        } else if (filter instanceof List) {
            filterList = (List<Document>) filter;
        } else {
            throw new IllegalArgumentException("Filter must be a Document or a List<Document>");
        }

        // Handle update (Bson or List<Bson>)
        List<Bson> updateList;
        if (update instanceof Bson) {
            updateList = Arrays.asList((Bson) update);
        } else if (update instanceof List) {
            updateList = (List<Bson>) update;
        } else {
            throw new IllegalArgumentException("Update must be a Bson or a List<Bson>");
        }

        // Perform the update operation
        Mongo.getInstance().Update(col, filterList, updateList, new UpdateOptions().upsert(true));
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