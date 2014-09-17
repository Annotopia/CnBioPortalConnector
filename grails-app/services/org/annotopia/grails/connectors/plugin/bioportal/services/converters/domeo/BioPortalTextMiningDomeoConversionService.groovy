/*
 * Copyright 2014 Massachusetts General Hospital
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.annotopia.grails.connectors.plugin.bioportal.services.converters.domeo

import groovyx.net.http.ContentType
import groovyx.net.http.EncoderRegistry
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.Method

import java.text.SimpleDateFormat
import java.util.regex.Matcher
import java.util.regex.Pattern

import org.annotopia.grails.connectors.ConnectorHttpResponseException
import org.annotopia.grails.connectors.MiscUtils
import org.annotopia.grails.connectors.plugin.bioportal.BioPortalAnnotatorRequestParameters
import org.annotopia.grails.connectors.plugin.bioportal.services.BioPortalService
import org.annotopia.grails.connectors.vocabularies.IOAccessRestrictions
import org.annotopia.grails.connectors.vocabularies.IODomeo
import org.annotopia.grails.connectors.vocabularies.IODublinCoreTerms
import org.annotopia.grails.connectors.vocabularies.IOJsonLd
import org.annotopia.grails.connectors.vocabularies.IOPav
import org.annotopia.grails.connectors.vocabularies.IORdfs
import org.apache.http.conn.params.ConnRoutePNames
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject


/**
 * @author Paolo Ciccarese <paolo.ciccarese@gmail.com>
 */
@Deprecated
class BioPortalTextMiningDomeoConversionService {

	public static final String RETURN_FORMAT = "domeo";
	
	def connectorsConfigAccessService;
	
	private static String URN_SNIPPET_PREFIX = "urn:domeo:contentsnippet:uuid:";
	private static String URN_ANNOTATION_SET_PREFIX = "urn:domeo:annotationset:uuid:";
	private static String URN_ANNOTATION_PREFIX = "urn:domeo:annotation:uuid:";
	private static String URN_SPECIFIC_RESOURCE_PREFIX = "urn:domeo:specificresource:uuid:";
	private static String URN_SELECTOR_PREFIX = "urn:domeo:selector:uuid:";
	
	SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");
	
	JSONObject convert(def apiKey, def url, def text, def results) {
		String snippetUrn = URN_SNIPPET_PREFIX + org.annotopia.grails.connectors.utils.UUID.uuid();
		
		JSONObject annotationSet = new JSONObject();
		annotationSet.put(IOJsonLd.jsonLdId, URN_ANNOTATION_SET_PREFIX + org.annotopia.grails.connectors.utils.UUID.uuid());
		annotationSet.put(IOJsonLd.jsonLdType, "ao:AnnotationSet");
		annotationSet.put(IORdfs.label, "NCBO Annotator Results");
		annotationSet.put(IODublinCoreTerms.description, "NCBO Annotator Results");
		//annotationSet.put("ao:onResource", snippetUrn);
		
		//  Agents
		// --------------------------------------------------------------------
		JSONArray agents = new JSONArray();
		
		// Connector
		def bioportalConnector = getConnectorAgent();
		annotationSet.put(IOPav.importedOn, dateFormat.format(new Date()));
		annotationSet.put(IOPav.importedBy, bioportalConnector[IOJsonLd.jsonLdId]);
		agents.add(agents.size(), bioportalConnector);
		
		// Annotator
		def bioportalAnnotator = getAnnotatorAgent();
		annotationSet.put(IOPav.importedFrom, bioportalAnnotator[IOJsonLd.jsonLdId]);
		agents.add(agents.size(), bioportalAnnotator);
		
		def domeo = getDomeo()
		agents.add(agents.size(), domeo);
		
		// Put Agents
		annotationSet.put(IODomeo.agents, agents);
		
		//  Permissions
		// --------------------------------------------------------------------
		annotationSet.put(IOAccessRestrictions.permissions, getPublicPermissions());
		
//		//  Resources
//		// --------------------------------------------------------------------
//		JSONArray resources = new JSONArray();
//		
//		JSONObject contentSnippet = new JSONObject();
//		contentSnippet.put(IOJsonLd.jsonLdId, snippetUrn);
//		contentSnippet.put(IOJsonLd.jsonLdType, IOOpenAnnotation.ContentAsText);
//		contentSnippet.put(IOOpenAnnotation.chars, text);
//		contentSnippet.put(IOPav.derivedFrom, url);
//		resources.add(resources.size(), contentSnippet);
//		
//		// Put Resources
//		annotationSet.put(IODomeo.resources, resources);
		
		//  Annotations
		// --------------------------------------------------------------------
		Map<String, String> terms = new HashMap<String, String>();
		Map<String, String> cache = new HashMap<String, String>();
		JSONArray annotations = new JSONArray();
		results.each{
			println it.annotatedClass['@id']
			println it.annotatedClass.links.ontology
			it.annotations.each { annotation ->
				//println annotation
				//println findOrCreateAndSaveSelectorUsingStringSearch(text, annotation.text, annotation.from, annotation.to);
				JSONObject ann = new JSONObject();
				ann.put(IOJsonLd.jsonLdId, URN_ANNOTATION_PREFIX + org.annotopia.grails.connectors.utils.UUID.uuid());
				ann.put(IOJsonLd.jsonLdType, "ao:Qualifier");
				ann.put(IORdfs.label, "Qualifier");
				ann.put("pav:createdWith", "urn:domeo:software:id:Domeo-2.0alpha-040");
				
				ann.put("pav:importedBy", "urn:domeo:software:id:BioPortalConnector-0.1-001")
				ann.put("pav:createdBy", "http://www.bioontology.org/wiki/index.php/Annotator_Web_service")
				ann.put("pav:importedFrom","http://www.bioontology.org/wiki/index.php/Annotator_Web_service")
				ann.put("pav:lastSavedOn", dateFormat.format(new Date()))
				ann.put("pav:versionNumber", "")
				
				JSONObject body = new JSONObject();
				body.put(IOJsonLd.jsonLdId, it.annotatedClass['@id']);
				body.put("domeo:category", "NCBO BioPortal concept");
				cache.put(it.annotatedClass['@id'], body);
				
				JSONArray bodies = new JSONArray();
				bodies.add(body);
				
				JSONObject source = new JSONObject();
				source.put(IOJsonLd.jsonLdId, it.annotatedClass.links.ontology);
				source.put(IORdfs.label, BioPortalService.ONTS2.get(it.annotatedClass.links.ontology));
				body.put("dct:source", source);
				
				terms.put(it.annotatedClass['@id'], it.annotatedClass.links.ontology);
				
				ann.put("ao:hasTopic", bodies);
				
				ann.put("pav:previousVersion", "");
				ann.put("pav:createdOn", dateFormat.format(new Date()));
				
				JSONObject specificTarget = new JSONObject();
				specificTarget.put(IOJsonLd.jsonLdId, URN_SPECIFIC_RESOURCE_PREFIX + org.annotopia.grails.connectors.utils.UUID.uuid());
				specificTarget.put(IOJsonLd.jsonLdType, "ao:SpecificResource");
				specificTarget.put("ao:hasSource", snippetUrn);
				specificTarget.put("ao:hasSelector", findOrCreateAndSaveSelectorUsingStringSearch(text, annotation.text, annotation.from, annotation.to));
				
				JSONArray contexts = new JSONArray();
				contexts.add(specificTarget);
				
				ann.put("oa:context", contexts);
				annotations.add(annotations.size(), ann);
			}
			annotationSet.put("ao:item", annotations);
			println '-----------'
		}
		
		JSONObject retrieveTermsMessage = retrieveTerms(terms);

		BioPortalAnnotatorRequestParameters params = new BioPortalAnnotatorRequestParameters();
		params.apikey = apiKey;
		//params.text = URLEncoder.encode(retrieveTermsMessage, MiscUtils.DEFAULT_ENCODING);
		
		String uri = 'http://data.bioontology.org/batch' 
		if(connectorsConfigAccessService.isProxyDefined()) {
			log.info("proxy: " + connectorsConfigAccessService.getProxyIp() + "-" + connectorsConfigAccessService.getProxyPort());
		} else {
			log.info("NO PROXY selected while accessing " + uri);
		}
		
		JSONObject jsonResponse = new JSONObject();
		try {
			def http = new HTTPBuilder(uri)
			
			int TENSECONDS = 10*1000;
			int THIRTYSECONDS = 30*1000;
			
			http.getClient().getParams().setParameter("http.connection.timeout", new Integer(TENSECONDS))
			http.getClient().getParams().setParameter("http.socket.timeout", new Integer(THIRTYSECONDS))
			
			http.encoderRegistry = new EncoderRegistry(charset: MiscUtils.DEFAULT_ENCODING)
			if(connectorsConfigAccessService.isProxyDefined()) {
				http.client.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, connectorsConfigAccessService.getProxyHttpHost());
			}

			http.setHeaders(Accept: 'application/json')
			// perform a POST request, expecting TEXT response
			http.request(Method.POST, ContentType.JSON) {
				requestContentType = ContentType.JSON
				headers.Authorization = 'apikey token=' + apiKey
				headers.'Content-Type' = 'application/json'	

				body = retrieveTermsMessage.toString()
				
				response.success = { resp, json ->
					json.keySet().each {  
						json.get(it).each { item ->
							def cachedItem = cache.get(item['@id']);
							cachedItem.put(IORdfs.label, item['prefLabel']);
							cachedItem.put('http://www.w3.org/2004/02/skos/core#prefLabel', item['prefLabel']);
							item['synonym'].each { sitem -> 
								cachedItem.put('http://www.w3.org/2004/02/skos/core#altLabel', sitem);
							}					
						}
					}
				}
				
				 response.'404' = { resp ->
					 log.error('Not found: ' + resp.getStatusLine() + ' ' + resp.entity.content.text)
					 throw new ConnectorHttpResponseException(resp, 404, 'Service not found. The problem has been reported')
				 }
			  
				 response.'503' = { resp ->
					 log.error('Not available: ' + resp.getStatusLine())
					 throw new ConnectorHttpResponseException(resp, 503, 'Service temporarily not available. Try again later.')
				 }
				 
				 response.'401' = { resp ->
					 log.error('UNAUTHORIZED access to URI: ' + uri)
					 throw new ConnectorHttpResponseException(resp, 401, 'Unauthorized access to the service.')
				 }
				 
				 response.'400' = { resp ->
					 log.error('BAD REQUEST: ' + uri)
					 throw new ConnectorHttpResponseException(resp, 401, 'Unauthorized access to the service.')
				 }
			 
				 response.failure = { resp, json ->
					 log.error('++Failure: ' + resp.getStatusLine() + ' + ' + resp.getStatusLine().getReasonPhrase())
				 }
			 }
		 } catch (groovyx.net.http.HttpResponseException ex) {
			 log.error("HttpResponseException: [" + ex.getStatusCode() + "] " + ex.getMessage())
			 throw new RuntimeException(ex);
		 } catch (java.net.SocketTimeoutException ex) {
			 log.error("SocketTimeoutException: " + ex.getMessage())
			 throw new RuntimeException(ex);
		 } catch (java.net.ConnectException ex) {
			 log.error("ConnectException: " + ex.getMessage())
			 throw new RuntimeException(ex);
		 } catch (Exception ex) {
			 log.error("Exception: " + ex.getMessage())
			 throw new RuntimeException(ex);
		 }
		
		return annotationSet;
	}
	
	private static Integer MAX_LENGTH_PREFIX_AND_SUFFIX=50
	private JSONObject findOrCreateAndSaveSelectorUsingStringSearch(String text, String match, Integer start, Integer end){
		//println text + " " + match+ " " + start + " ";
		Map<String,Object> matchInfo = searchForMatch(text, match, start-1);  // -1 because they start from 1
		//println matchInfo;

		JSONObject selector = new JSONObject();
		selector.put(IOJsonLd.jsonLdId, URN_SELECTOR_PREFIX + org.annotopia.grails.connectors.utils.UUID.uuid());
		selector.put(IOJsonLd.jsonLdType, "ao:PrefixSuffixTextSelector");
		selector.put(IOPav.createdOn, dateFormat.format(new Date()));
		selector.put("ao:prefix", matchInfo.prefix);
		selector.put("ao:exact", matchInfo.exact);
		selector.put("ao:suffix", matchInfo.suffix);
		return selector;
	}
	
	private def searchForMatch(String textToAnnotate, String putativeExactMatch, int start) {
		String matchRegex = putativeExactMatch.replaceAll(/\s+/,"\\\\s+")
		matchRegex = matchRegex.replaceAll("[)]", "\\\\)")
		matchRegex = matchRegex.replaceAll("[(]", "\\\\(")
		Pattern pattern = Pattern.compile("\\b${matchRegex}\\b", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE)
		Matcher matcher = pattern.matcher(textToAnnotate)
		int startPos = -1
		int endPos = -1
		if (matcher.find(start)) {
			println 'in'
			startPos = matcher.start()
			endPos = matcher.end()
			String exactMatch = textToAnnotate[startPos..endPos - 1]
			
			String prefix = null;
			if(startPos == 0) {
				prefix = '';
			} else {
				 prefix = textToAnnotate.getAt([
					 Math.max(startPos - (MAX_LENGTH_PREFIX_AND_SUFFIX + 1), 0)..Math.max(0, startPos - 1)
				])
			}
			
			String suffix = null;
			if(Math.min(endPos, textToAnnotate.length() - 1)==Math.min(startPos + MAX_LENGTH_PREFIX_AND_SUFFIX, textToAnnotate.length()-1)) {
				suffix = "";
			} else {
				suffix = textToAnnotate.getAt([
					Math.min(endPos, textToAnnotate.length() - 1)..Math.min(startPos + MAX_LENGTH_PREFIX_AND_SUFFIX, textToAnnotate.length()-1)
				])
			}
			
			return ['offset':startPos,'prefix': prefix, 'exact': exactMatch, 'suffix': suffix]
		}else{
			println 'out'
			return null
		}

	}
	
	private JSONObject retrieveTerms(Map terms) {

		JSONArray collection = new JSONArray();
		terms.keySet().each {
			JSONObject term = new JSONObject();
			term.put("class", it);
			term.put("ontology", terms.get(it));
			collection.add(term);
		}

		JSONObject messageRequest = new JSONObject();
		messageRequest.put( "collection", collection)
		messageRequest.put( "include", "prefLabel,synonym,semanticTypes")

		JSONObject wrapper = new JSONObject();
		wrapper.put("http://www.w3.org/2002/07/owl#Class", messageRequest);
	}
	
	private JSONObject getPublicPermissions() {
		JSONObject permissions = new JSONObject();
		permissions.put("permissions:isLocked", "false");
		permissions.put("permissions:accessType", "urn:domeo:access:public");
		permissions;
	}
	
	private JSONObject getConnectorAgent() {
		JSONObject bioportalConnector = new JSONObject();
		def connectorUrn = "urn:domeo:software:service:ConnectorBioPortalService:0.1-001";
		bioportalConnector.put(IOJsonLd.jsonLdId, connectorUrn);
		bioportalConnector.put(IOJsonLd.jsonLdType, "foafx:Software");
		bioportalConnector.put(IORdfs.label, "BioPortalConnector");
		bioportalConnector.put("foafx:name", "BioPortalConnector");
		bioportalConnector.put("foafx:build", "001");
		bioportalConnector.put("foafx:version", "0.1");
		bioportalConnector;
	}
	
	// http://data.bioontology.org/annotator
	private JSONObject getAnnotatorAgent() {
		JSONObject ncboAnnotator = new JSONObject();
		ncboAnnotator.put(IOJsonLd.jsonLdId, "http://www.bioontology.org/wiki/index.php/Annotator_Web_service");
		ncboAnnotator.put(IOJsonLd.jsonLdType, "foafx:Software");
		ncboAnnotator.put(IORdfs.label, "NCBO Annotator Web Service");
		ncboAnnotator.put("foafx:name", "NCBO Annotator Web Service");
		ncboAnnotator.put("foafx:build", "001");
		ncboAnnotator.put("foafx:version", "1.0");
		ncboAnnotator
	}
	
	private JSONObject getDomeo() {
		JSONObject domeo = new JSONObject();
		domeo.put(IOJsonLd.jsonLdId, "urn:domeo:software:id:Domeo-2.0alpha-040");
		domeo.put(IOJsonLd.jsonLdType, "foafx:Software");
		domeo.put(IORdfs.label, "Domeo Annotation Toolkit");
		domeo.put("foafx:name", "Domeo");
		domeo.put("foafx:build", "040");
		domeo.put("foafx:version", "1.0");
		domeo
	}
}
