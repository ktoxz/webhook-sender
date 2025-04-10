package dev.ktoxz.db;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

public class MongoInsert{

    private String dbName;
    private String colName;
    private MongoDatabase db;
    private MongoCollection<Document> col;

    public MongoInsert(String dbName, String colName) {
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
    
    public void One(Document doc) throws Exception{
        Mongo.getInstance().Insert(col, List.of(doc));
    }
     
    public void Many(List<Document> docs) throws Exception {
    	Mongo.getInstance().Insert(col, docs);
    }
}

