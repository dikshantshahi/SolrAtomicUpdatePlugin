package com.idivine.solr.processor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.lucene.util.BytesRef;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.SolrInputField;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.SolrCore;
import org.apache.solr.handler.component.RealTimeGetComponent;
import org.apache.solr.request.LocalSolrQueryRequest;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.request.SolrRequestHandler;
import org.apache.solr.response.ResultContext;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.schema.SchemaField;
import org.apache.solr.search.DocList;
import org.apache.solr.update.AddUpdateCommand;
import org.apache.solr.update.processor.UpdateRequestProcessor;
import org.apache.solr.update.processor.UpdateRequestProcessorFactory;
import org.apache.solr.util.SolrPluginUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.idivine.solr.common.SolrConstants;

public class SqlLikeUpdateProcessorFactory extends UpdateRequestProcessorFactory {
	public static Logger log = LoggerFactory.getLogger(SqlLikeUpdateProcessorFactory.class);
	@Override
	public UpdateRequestProcessor getInstance(SolrQueryRequest req,
			SolrQueryResponse rsp, UpdateRequestProcessor next) {
		return new SqlLikeUpdateProcessor(req, rsp, this, next);
	}
	
	class SqlLikeUpdateProcessor extends UpdateRequestProcessor {
		private static final float DEFAULT_BOOST = 1.0F;
		private static final String VERSION_FIELD = "_version_";
		private List<String> skipFields = new ArrayList<String>();
		private final SolrQueryRequest req;
		public SqlLikeUpdateProcessor(SolrQueryRequest req,
		        SolrQueryResponse rsp, SqlLikeUpdateProcessorFactory factory,
		        UpdateRequestProcessor next) {
	      super(next);
	      this.req = req;
	      
	      skipFields.add(getUniqueFieldName());
	      skipFields.add(VERSION_FIELD);
	    }
		
		private String getUniqueFieldName() {
			IndexSchema schema = req.getSchema();
			SchemaField sf = schema.getUniqueKeyField();
			return sf.getName();
		}
		
		public void processAdd(AddUpdateCommand cmd) throws IOException {			
			SolrInputDocument doc = cmd.getSolrInputDocument();

			SolrInputField uniqueField = doc.getField(SolrConstants.IS_UNIQUE_PARAM);
			// If user provides the uniqueKey
			if (null != uniqueField && (Boolean) uniqueField.getFirstValue() == true ) {
				String uniqueId = (String) doc.getField(SolrConstants.QUERY_PARAM).getFirstValue();
				doc.setField(getUniqueFieldName(), uniqueId, DEFAULT_BOOST);

				BytesRef id = cmd.getIndexedId();

				doc.removeField(SolrConstants.QUERY_PARAM);
				doc.removeField(SolrConstants.IS_UNIQUE_PARAM);
				SolrInputDocument oldDoc = RealTimeGetComponent.getInputDocument(cmd.getReq().getCore(), id);
				
				for(String fieldName : doc.getFieldNames()) {
					oldDoc.removeField(fieldName);
					oldDoc.addField(fieldName, doc.getFieldValues(fieldName));
				}
				cmd.solrDoc = oldDoc;
				
			} else { // If user provides the query
				String query = (String) doc.getField(SolrConstants.QUERY_PARAM).getFirstValue();
				
				SolrCore core = req.getCore();
				SolrRequestHandler handler = core.getRequestHandler("/select");

				// TODO: Check other parameters
				SolrQueryRequest request = new LocalSolrQueryRequest(core, query, null, 0, 100, new HashMap());
				SolrQueryResponse response = new SolrQueryResponse();
				core.execute(handler, request, response);

				NamedList nl = response.getValues();
				ResultContext results = (ResultContext) nl.get("response");
				DocList docList = results.docs;
				
				if (docList.matches() > 1) {
					log.error("ERROR: Invalid query. Must return only one document instead of " + docList.matches());
				}
				
				SolrDocumentList solrDocs = SolrPluginUtils.docListToSolrDocumentList(docList, req.getSearcher(), null, null);
				
				SolrInputDocument inDoc = docListToSolrInputDocument(solrDocs);
				doc.removeField(SolrConstants.QUERY_PARAM);
				doc.removeField(SolrConstants.IS_UNIQUE_PARAM);
				
				for(String fieldName : doc.getFieldNames()) {
					if (skipFields.contains(fieldName))
						continue;
					inDoc.removeField(fieldName);
					for(Object fieldValue : doc.getFieldValues(fieldName)) {
						inDoc.addField(fieldName, fieldValue, 1.0f);
					}
				}
				cmd.solrDoc = inDoc;
				request.close();
			}
		
		    if (next != null) next.processAdd(cmd);
		}
		
		/**
		 * Convert SolrDocumentList to SolrInputDocument
		 * @param sdl
		 * @return
		 */
		private SolrInputDocument docListToSolrInputDocument(SolrDocumentList sdl) {
			SolrInputDocument inDoc = new SolrInputDocument();

			Iterator<SolrDocument> docIter = sdl.iterator();
			
			if(docIter.hasNext()) {
				SolrDocument solrDoc = docIter.next();
				
				Map<String, Collection<Object>> docFieldsMap = solrDoc.getFieldValuesMap();
				
				for(String fieldName : docFieldsMap.keySet()) {
					Collection<Object> values = docFieldsMap.get(fieldName);
					for (Object value : values) {
						inDoc.addField(fieldName, value, 1.0f);
					}
				}
			}
			return inDoc;
		}
		
	}

}
