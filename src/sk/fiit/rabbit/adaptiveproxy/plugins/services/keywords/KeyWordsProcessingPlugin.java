package sk.fiit.rabbit.adaptiveproxy.plugins.services.keywords;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jcouchdb.db.Database;
import org.json.simple.JSONObject;

import sk.fiit.peweproxy.messages.HttpMessageFactory;
import sk.fiit.peweproxy.messages.HttpResponse;
import sk.fiit.peweproxy.messages.ModifiableHttpRequest;
import sk.fiit.peweproxy.messages.ModifiableHttpResponse;
import sk.fiit.peweproxy.services.content.ModifiableStringService;
import sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions.CouchDBProviderService;
import sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions.RequestDataParserService;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.bubble.BubbleMenuProcessingPlugin;

public class KeyWordsProcessingPlugin extends BubbleMenuProcessingPlugin {
	
	@Override
	public HttpResponse getResponse(ModifiableHttpRequest request, HttpMessageFactory messageFactory) {
		String content = "";

		if(request.getServicesHandle().isServiceAvailable(CouchDBProviderService.class)) {
			Database database = request.getServicesHandle().getService(CouchDBProviderService.class).getDatabase();
			String url = request.getRequestHeader().getField("Referer");
			
			if (request.getRequestHeader().getRequestURI().contains("action=getKeyWords")) {
				content = this.getKeyWordsFromCouchDb(database, url);
			}
			
			if(request.getServicesHandle().isServiceAvailable(RequestDataParserService.class)) {
				Map<String, String> postData = request.getServicesHandle().getService(RequestDataParserService.class).getDataFromPOST();
				if (request.getRequestHeader().getRequestURI().contains("action=editKeyWord")) {
					content = this.editKeyWordInCouchDb(database, url, postData.get("id"), postData.get("term"), postData.get("relevance"), postData.get("type"));
				}
				if (request.getRequestHeader().getRequestURI().contains("action=removeKeyWord")) {
					content = this.removeKeyWordFromCouchDb(database, url, postData.get("id"));
				}
				if (request.getRequestHeader().getRequestURI().contains("action=addKeyWord")) {
					content = this.addKeyWordIntoCouchDb(database, url, postData.get("term"), postData.get("relevance"), postData.get("type"));
				}
			}
		}
		
		ModifiableHttpResponse httpResponse = messageFactory.constructHttpResponse(null, "text/html");
		ModifiableStringService stringService = httpResponse.getServicesHandle().getService(ModifiableStringService.class);
		stringService.setContent(content);
		
		return httpResponse;
	}
	
	@SuppressWarnings("all")
	private String addKeyWordIntoCouchDb(Database couch, String url, String name, String relevance, String type) {
		try {
			HashMap page = couch.getDocument(HashMap.class, url);
			List<Map> terms = (List<Map>) page.get("terms");
			Map term = new HashMap();
			term.put("name", name);
			term.put("relevance", relevance);
			term.put("type", type);
			term.put("source", "human");
			terms.add(term);
			couch.updateDocument(page);
		} catch (Exception e) {
			logger.error("Unable to edit keyword", e);
			return "FAIL";
		}
		return "OK";
	}
	
	@SuppressWarnings("all")
	private String removeKeyWordFromCouchDb(Database couch, String url, String termId) {
		try {
			HashMap page = couch.getDocument(HashMap.class, url);
			List<Map> terms = (List<Map>) page.get("terms");
			
			Map termToRemove = null;
			for(Map term : terms) {
				if(term.get("name").equals(termId)) {
					termToRemove = term;
					break;
				}
			}
			terms.remove(termToRemove);
			
			couch.updateDocument(page);
		} catch (Exception e) {
			logger.error("Unable to remove keyword", e);
			return "FAIL";
		}
		return "OK";
	}

	@SuppressWarnings("all")
	private String editKeyWordInCouchDb(Database couch, String url, String termId, String name, String relevance, String type) {
		try {
			HashMap page = couch.getDocument(HashMap.class, url);
			List<Map> terms = (List<Map>) page.get("terms");
			for(Map term : terms) {
				if(term.get("name").equals(termId)) {
					term.put("name", name);
					term.put("type", type);
					term.put("relevance", relevance);
				}
			}
			couch.updateDocument(page);
		} catch (Exception e) {
			logger.error("Unable to edit keyword", e);
			return "FAIL";
		}
		return "OK";
	}

	@SuppressWarnings("all")
	private String getKeyWordsFromCouchDb(Database couch, String url) {
		HashMap page = couch.getDocument(HashMap.class, url);
		
		JSONObject response = new JSONObject();
		response.put("keywords", page.get("terms"));
		
		return response.toJSONString();
	}
}
