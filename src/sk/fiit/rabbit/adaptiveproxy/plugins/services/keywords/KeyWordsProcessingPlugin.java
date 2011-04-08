package sk.fiit.rabbit.adaptiveproxy.plugins.services.keywords;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.sql.Connection;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONObject;

import sk.fiit.peweproxy.messages.HttpMessageFactory;
import sk.fiit.peweproxy.messages.HttpResponse;
import sk.fiit.peweproxy.messages.ModifiableHttpRequest;
import sk.fiit.peweproxy.messages.ModifiableHttpResponse;
import sk.fiit.peweproxy.services.content.ModifiableStringService;
import sk.fiit.peweproxy.services.content.StringContentService;
import sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions.DatabaseConnectionProviderService;
import sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions.DatabaseSessionProviderService;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.common.SqlUtils;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.injector.ClientBubbleMenuProcessingPlugin;

import com.fourspaces.couchdb.Database;
import com.fourspaces.couchdb.Document;


public class KeyWordsProcessingPlugin  extends ClientBubbleMenuProcessingPlugin {
	
	
	@Override
	public HttpResponse getResponse(ModifiableHttpRequest request, HttpMessageFactory messageFactory) {
		StringContentService stringContentService = request.getServicesHandle().getService(StringContentService.class);
		String pageId = "569d223f-fd9b-493c-a973-57b460c4ec45";
		
		Map<String, String> postData = getPostDataFromRequest(stringContentService.getContent());
		String content = "";
		Connection connection = null;
		Database database = null;
		try {
			connection = request.getServicesHandle().getService(DatabaseConnectionProviderService.class).getDatabaseConnection();
			database = request.getServicesHandle().getService(DatabaseSessionProviderService.class).getDatabase();
			//String pageId = postData.get("checksum");
			
			if (request.getRequestHeader().getRequestURI().contains("action=getKeyWords")) {
				content = this.getKeyWordsFromCouchDb(database, pageId, request.getRequestHeader().getField("Referer"));
			}
			if (request.getRequestHeader().getRequestURI().contains("action=editKeyWord")) {
				content = this.editKeyWordInCouchDb(database, pageId, postData.get("id"), postData.get("term"), postData.get("relevance"), postData.get("type"));
			}
			if (request.getRequestHeader().getRequestURI().contains("action=removeKeyWord")) {
				content = this.removeKeyWordFromCouchDb(database, pageId, postData.get("id"));
			}
			if (request.getRequestHeader().getRequestURI().contains("action=addKeyWord")) {
				content = this.addKeyWordIntoCouchDb(database, pageId, postData.get("term"), postData.get("relevance"), postData.get("type"));
			}
		} finally {
			SqlUtils.close(connection);
		}
		
		
		ModifiableHttpResponse httpResponse = messageFactory.constructHttpResponse(null, "text/html");
		ModifiableStringService stringService = httpResponse.getServicesHandle().getService(ModifiableStringService.class);
		stringService.setContent(content);
		
		return httpResponse;
	}
	
	private String addKeyWordIntoCouchDb(Database database, String pageId, String term, String relevance, String type) {
		try {
			Document doc = database.getDocument(pageId);
			net.sf.json.JSONObject jsonPageTerm;
			
			net.sf.json.JSONArray pages_terms = doc.getJSONArray("pages_terms");
			JSONObject newTerm = new JSONObject();
			newTerm.put("label", term);
			newTerm.put("relevance", relevance);
			newTerm.put("type", type);
			newTerm.put("created_at", new Timestamp(System.currentTimeMillis()).toString().substring(0, 10));
			newTerm.put("updated_at", new Timestamp(System.currentTimeMillis()).toString().substring(0, 10));
			newTerm.put("source", "human");
			
			pages_terms.add(newTerm);
			doc.put("pages_terms", pages_terms);
			database.saveDocument(doc);
		} catch (Exception e) {
			logger.error("Unable to remove key word", e);
			return "FAIL";
		}
		return "OK";
	}

	private String removeKeyWordFromCouchDb(Database database, String pageId, String termId) {
		try {
			Document doc = database.getDocument(pageId);
			net.sf.json.JSONObject jsonPageTerm;
			
			net.sf.json.JSONArray pages_terms = doc.getJSONArray("pages_terms");
			
			for (Object ptObj : pages_terms) {
				jsonPageTerm = (net.sf.json.JSONObject)ptObj;
				
				if (jsonPageTerm.getString("label").toString().equals(termId)) {
					pages_terms.remove(ptObj);
				}
			}
			
			doc.put("pages_terms", pages_terms);
			database.saveDocument(doc);
		} catch (Exception e) {
			logger.error("Unable to remove key word", e);
			return "FAIL";
		}
		return "OK";
	}

	private String editKeyWordInCouchDb(Database database, String pageId, String termId, String term, String relevance, String type) {
		try {
			Document doc = database.getDocument(pageId);
			net.sf.json.JSONObject jsonPageTerm;
			
			net.sf.json.JSONArray pages_terms = doc.getJSONArray("pages_terms");
			
			for (Object ptObj : pages_terms) {
				jsonPageTerm = (net.sf.json.JSONObject)ptObj;
				
				if (jsonPageTerm.getString("label").toString().equals(termId)) {
					if (!"".equals(term) || !(term == null)) {
						jsonPageTerm.element("label", term);
					}
					if (!"".equals(type) || !(type == null)) {
						jsonPageTerm.element("term_type", type);
					}
					if (!"".equals(relevance) || !(relevance == null)) {
						jsonPageTerm.element("relevance", relevance);
					}
					jsonPageTerm.element("updated_at", new Timestamp(System.currentTimeMillis()).toString().substring(0, 10));
					ptObj = jsonPageTerm;
				}
			}
			
			doc.put("pages_terms", pages_terms);
			database.saveDocument(doc);
		} catch (Exception e) {
			logger.error("Unable to edit key word", e);
			return "FAIL";
		}
		return "OK";
	}

	private String getKeyWordsFromCouchDb(Database db, String pageId, String url) {
		Document doc = db.getDocument(pageId);
		String jsonString = "{\"keywords\":" + doc.get("pages_terms").toString() + "}";

		return jsonString;
	}

	private Map<String, String> getPostDataFromRequest (String requestContent) {
		try {
			requestContent = URLDecoder.decode(requestContent, "utf-8");
		} catch (UnsupportedEncodingException e) {
			logger.warn(e);
		}
		Map<String, String> postData = new HashMap<String, String>();
		String attributeName;
		String attributeValue;

		for (String postPair : requestContent.split("&")) {
			if (postPair.split("=").length == 2) {
				attributeName = postPair.split("=")[0];
				attributeValue = postPair.split("=")[1];
				postData.put(attributeName, attributeValue);
			}
		}

		return postData;
	}
	
}
