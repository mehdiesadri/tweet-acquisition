//package util;
//
//import java.io.File;
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.Map.Entry;
//
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;
//import org.apache.lucene.analysis.standard.StandardAnalyzer;
//import org.apache.lucene.document.Document;
//import org.apache.lucene.document.Field;
//import org.apache.lucene.document.FieldType;
//import org.apache.lucene.index.DirectoryReader;
//import org.apache.lucene.index.IndexWriter;
//import org.apache.lucene.index.IndexWriterConfig;
//import org.apache.lucene.index.IndexableField;
//import org.apache.lucene.queryparser.classic.QueryParser;
//import org.apache.lucene.search.BooleanClause.Occur;
//import org.apache.lucene.search.BooleanQuery;
//import org.apache.lucene.search.IndexSearcher;
//import org.apache.lucene.search.ScoreDoc;
//import org.apache.lucene.search.TopDocs;
//import org.apache.lucene.store.Directory;
//import org.apache.lucene.store.FSDirectory;
//import org.apache.lucene.store.RAMDirectory;
//import org.apache.lucene.util.Version;
//
//public class IndexManager {
//	static final Logger logger = LogManager.getLogger(IndexManager.class
//			.getName());
//
//	private StandardAnalyzer analyzer;
//	private Directory index;
//	private IndexWriter iwriter;
//	private String indexParentDir;
//	private DirectoryReader reader;
//
//	public IndexManager(String dir) {
//		this.indexParentDir = dir;
//		analyzer = new StandardAnalyzer();
//		index = new RAMDirectory();
//	}
//
//	public void initiateIndex(String indexName) {
//		try {
//			index = FSDirectory.open(new File(indexParentDir, indexName));
//			IndexWriterConfig luceneConfig = new IndexWriterConfig(
//					Version.LATEST, analyzer);
//			iwriter = new IndexWriter(index, luceneConfig);
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}
//
//	public void loadIndex(String indexName) {
//		try {
//			index = FSDirectory.open(new File(indexParentDir, indexName));
//			reader = DirectoryReader.open(index);
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}
//
//	public void addDoc(Map<String, String> docFields) {
//		Document doc = new Document();
//		FieldType type = new FieldType();
//		type.setIndexed(true);
//		type.setStored(true);
//		type.setStoreTermVectors(true);
//
//		for (Entry<String, String> field : docFields.entrySet())
//			doc.add(new Field(field.getKey(), field.getValue(), type));
//
//		addDoc(doc);
//	}
//
//	void addDoc(Document doc) {
//		try {
//			iwriter.addDocument(doc);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//	}
//
//	public void close() {
//		try {
//			iwriter.commit();
//			reader = DirectoryReader.open(index);
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}
//
//	public int count(Map<String, String> fields) {
//		try {
//			IndexSearcher searcher = new IndexSearcher(reader);
//			BooleanQuery q = new BooleanQuery();
//
//			for (Entry<String, String> field : fields.entrySet()) {
//				QueryParser fParser = new QueryParser(field.getKey(), analyzer);
//
//				for (String item : field.getValue().split(" ")) {
//					item = item.trim();
//					if (item.length() > 0)
//						q.add(fParser.parse(item), Occur.MUST);
//				}
//			}
//
//			TopDocs searchRes = searcher.search(q, Integer.MAX_VALUE);
//			int count = searchRes.scoreDocs.length;
//			return count;
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//
//		return -1;
//	}
//
//	public List<Map<String, String>> search(String fieldName, String query) {
//		List<Map<String, String>> results = new ArrayList<Map<String, String>>();
//
//		try {
//			QueryParser parser = new QueryParser(fieldName, analyzer);
//			IndexSearcher searcher = new IndexSearcher(reader);
//
//			BooleanQuery q = new BooleanQuery();
//			for (String item : query.split(" ")) {
//				item = item.trim();
//				if (item.length() > 0)
//					q.add(parser.parse(item), Occur.MUST);
//			}
//
//			TopDocs searchRes = searcher.search(q, Integer.MAX_VALUE);
//			ScoreDoc[] hits = searchRes.scoreDocs;
//			for (ScoreDoc hit : hits) {
//				Document doc = searcher.doc(hit.doc);
//				Map<String, String> docFields = new HashMap<String, String>();
//				for (IndexableField f : doc.getFields())
//					docFields.put(f.name(), f.stringValue());
//
//				results.add(docFields);
//			}
//
//			return results;
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//
//		return null;
//	}
//
//	public void shutdown() {
//		try {
//			if (iwriter != null)
//				iwriter.close();
//
//			if (reader != null)
//				reader.close();
//
//			index.close();
//			analyzer.close();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}
//
//	public static void main(String[] args) throws Exception {
//		IndexManager indexManager = new IndexManager("../data/index/test/");
//		indexManager.initiateIndex("test1");
//
//		Map<String, String> docFields = new HashMap<String, String>();
//		docFields.put("1", "hi");
//		docFields.put("2", "bye");
//
//		indexManager.addDoc(docFields);
//		indexManager.close();
//
//		Map<String, String> searchFields = new HashMap<String, String>();
//		searchFields.put("1", "hi");
//
//		logger.info(indexManager.count(searchFields));
//		logger.info(indexManager.search("1", "hi"));
//
//		indexManager.shutdown();
//	}
//}