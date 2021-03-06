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
package org.annotopia.grails.connectors.plugin.bioportal.services

import groovyx.net.http.ContentType
import groovyx.net.http.EncoderRegistry
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.Method

import org.annotopia.grails.connectors.BaseConnectorService
import org.annotopia.grails.connectors.ConnectorHttpResponseException
import org.annotopia.grails.connectors.IConnectorsParameters
import org.annotopia.grails.connectors.ITermSearchService
import org.annotopia.grails.connectors.ITextMiningService
import org.annotopia.grails.connectors.IVocabulariesListService
import org.annotopia.grails.connectors.MiscUtils
import org.annotopia.grails.connectors.plugin.bioportal.BioPortalAnnotatorRequestParameters
import org.annotopia.grails.connectors.plugin.bioportal.services.converters.co.BioPortalTermSearchCoConversionService
import org.annotopia.grails.connectors.plugin.bioportal.services.converters.co.BioPortalTextMiningCoConversionService
import org.annotopia.grails.connectors.plugin.bioportal.services.converters.domeo.BioPortalTermSearchDomeoConversionService
import org.annotopia.grails.connectors.plugin.bioportal.services.converters.domeo.BioPortalTextMiningDomeoConversionService
import org.annotopia.grails.connectors.plugin.bioportal.services.converters.ore.BioPortalTermSearchOreConversionService
import org.codehaus.groovy.grails.web.json.JSONObject

/**
 * @author Paolo Ciccarese <paolo.ciccarese@gmail.com>
 */
class BioPortalService extends BaseConnectorService implements IVocabulariesListService, ITermSearchService, ITextMiningService {

	final static int TENSECONDS = 10*1000;
	final static int THIRTYSECONDS = 30*1000;
	
	final static APIKEY = "?apikey=";
	final static QUERY = "&q=";
	final static ONTOLOGIES = "&ontologies=";
	final static PAGE = "&page=";
	final static PAGESIZE = "&pagesize=";
	
	def connectorsConfigAccessService;
	def termSearchResultsConverterV0Service;
	
	// New formats
	def bioPortalVocabulariesListConversionService
	def bioPortalTermSearchConversionService
	def bioPortalTextMiningConversionService
	// Collections ontology
	def bioPortalTermSearchCoConversionService
	def bioPortalTextMiningCoConversionService
	// Old Domeo format (deprecated)
	def bioPortalTermSearchDomeoConversionService
	def bioPortalTextMiningDomeoConversionService
	// ORE
	def bioPortalTermSearchOreConversionService
	
	final static PARAMS_ONTOLOGIES = 'ontologies'
	final static ONTS1 = ["PR":"http://data.bioontology.org/ontologies/PR", "NIFSTD":"http://data.bioontology.org/ontologies/NIFSTD"]
	final static ONTS2 = ["http://data.bioontology.org/ontologies/PR":"PR - Protein Ontology", "http://data.bioontology.org/ontologies/NIFSTD":"Neuroscience Information Framework (NIF) Standard Ontology"]
	
	// http://rest.bioontology.org/bioportal/search/?query=Gene&isexactmatch=1&apikey=fef6b9da-4b3b-46d2-9d83-9a1a718f6a22
	// http://data.bioontology.org/search?q=melanoma (page of 50 items)
	// http://data.bioontology.org/search?q=melanoma&page=2&pagesize=5 (page of 5 items)
	
	
	@Override
	public JSONObject listVocabularies(HashMap parametrization) {
		
		log.info 'listVocabularies:Parametrization: ' + parametrization
		long startTime = System.currentTimeMillis();
		
		String apikey = verifyApikey(parametrization);

		String pageNumber = (parametrization.get("pagenumber")?parametrization.get("pagenumber"):1);
		String pageSize = (parametrization.get("pagesize")?parametrization.get("pagesize"):50);
		
		String uri = 'http://data.bioontology.org/ontologies' +
			"?page=" + pageSize +
			PAGE + pageNumber;
		log.info("List vocabularies with URI: " + uri);

		JSONObject jsonResponse = new JSONObject();
		try {
			def http = new HTTPBuilder(uri)
			http.getClient().getParams().setParameter("http.connection.timeout", new Integer(TENSECONDS))
			http.getClient().getParams().setParameter("http.socket.timeout", new Integer(THIRTYSECONDS))
			http.encoderRegistry = new EncoderRegistry(charset: MiscUtils.DEFAULT_ENCODING)
			evaluateProxy(http, uri);
						
			// perform a POST request, expecting TEXT response
			http.request(Method.GET, ContentType.JSON) {
				requestContentType = ContentType.URLENC
				headers.'Authorization' = 'apikey token=' + apikey
				
				response.success = { resp, json ->			
					if(true) {
						// Default format
						JSONObject jsonReturn = new JSONObject();
						jsonReturn.put("duration", System.currentTimeMillis() - startTime + "ms");
						jsonReturn.put("total", json.size());
						
						bioPortalVocabulariesListConversionService.convert(json, jsonReturn);
						
						jsonResponse.put("status", "results");
						jsonResponse.put("result", jsonReturn);					
					}						
					return jsonResponse;
				}
				
				response.'404' = { resp ->
					log.error('Not found: ' + resp.getStatusLine())
					throw new ConnectorHttpResponseException(resp, 404, 'Service not found. The problem has been reported')
				}
				response.'401' = { resp ->
					log.error('UNAUTHORIZED access to URI: ' + uri)
					throw new ConnectorHttpResponseException(resp, 401, 'Unauthorized access to the BioPortal service.')
				}
				response.'503' = { resp ->
					log.error('Not available: ' + resp.getStatusLine())
					throw new ConnectorHttpResponseException(resp, 503, 'Service temporarily not available. Try again later.')
				}
				
				response.failure = { resp, xml ->
					log.error('failure: ' + resp.getStatusLine())
					throw new ConnectorHttpResponseException(resp, resp.getStatusLine().toString())
				}
			}
		} catch (groovyx.net.http.HttpResponseException ex) {
			log.error("HttpResponseException: " + ex.getMessage())
			throw new RuntimeException(ex);
		} catch (java.net.ConnectException ex) {
			log.error("ConnectException: " + ex.getMessage())
			throw new RuntimeException(ex);
		}
	}
	
	@Override
	public JSONObject search(String content, HashMap parametrization) {
		log.info 'search:Content: ' + content + ' Parametrization: ' + parametrization
		long startTime = System.currentTimeMillis();
		
		String apikey = verifyApikey(parametrization);
		String ontologies = selectOntologies(parametrization);
		String queryText = encodeContent(content);
		
		String pageNumber = (parametrization.get("pagenumber")?parametrization.get("pagenumber"):1);
		String pageSize = (parametrization.get("pagesize")?parametrization.get("pagesize"):10);

		String uri = 'http://data.bioontology.org/search' + 
			//APIKEY + apikey + 
			"?q=" + URLEncoder.encode(queryText, MiscUtils.DEFAULT_ENCODING)  +
			ONTOLOGIES + ((!ontologies.isEmpty())?(ONTOLOGIES + ontologies):'') +
			PAGESIZE + pageSize +
			PAGE + pageNumber;
			
		log.info("Search term with URI: " + uri);

		JSONObject jsonResponse = new JSONObject();
		try {
			def http = new HTTPBuilder(uri)
			http.getClient().getParams().setParameter("http.connection.timeout", new Integer(TENSECONDS))
			http.getClient().getParams().setParameter("http.socket.timeout", new Integer(THIRTYSECONDS))				
			http.encoderRegistry = new EncoderRegistry(charset: MiscUtils.DEFAULT_ENCODING)
			evaluateProxy(http, uri);
		
			http.request(Method.GET, ContentType.JSON) {
				requestContentType = ContentType.URLENC
				headers.'Authorization' = 'apikey token=' + apikey
				
				response.success = { resp, json ->	
					//log.info json			
					boolean isFormatDefined = parametrization.containsKey(IConnectorsParameters.RETURN_FORMAT);		
					if(isFormatDefined && parametrization.get(IConnectorsParameters.RETURN_FORMAT).equals(BioPortalTermSearchDomeoConversionService.RETURN_FORMAT)) {
						bioPortalTermSearchDomeoConversionService.convert(json, jsonResponse, pageSize, ONTS2)
					} else if(isFormatDefined && parametrization.get(IConnectorsParameters.RETURN_FORMAT).equals(BioPortalTermSearchCoConversionService.RETURN_FORMAT)) {
						bioPortalTermSearchCoConversionService.convert(json, jsonResponse, pageSize, ONTS2)
					} else if(isFormatDefined && parametrization.get(IConnectorsParameters.RETURN_FORMAT).equals(BioPortalTermSearchOreConversionService.RETURN_FORMAT)) {
						bioPortalTermSearchOreConversionService.convert(json, jsonResponse, pageSize, ONTS2)
					} else {
						// Default format
						JSONObject jsonReturn = new JSONObject();
						jsonReturn.put("duration", System.currentTimeMillis() - startTime + "ms");
						jsonReturn.put("total", "unknown");
						jsonReturn.put("offset", Integer.parseInt(pageNumber)-1);
						jsonReturn.put("max", pageSize);
						
						bioPortalTermSearchConversionService.convert(json, jsonReturn, pageSize, ONTS2);
						
						jsonResponse.put("status", "results");
						jsonResponse.put("result", jsonReturn);
					}						
				}
				
				response.'404' = { resp ->
					log.error('NOT FOUND: ' + resp.getStatusLine())
					throw new ConnectorHttpResponseException(resp, 404, 'Service not found. The problem has been reported')
				}				 
				response.'503' = { resp ->
					log.error('NOT AVAILABLE: ' + resp.getStatusLine())
					throw new ConnectorHttpResponseException(resp, 503, 'Service temporarily not available. Try again later.')
				}
				response.'401' = { resp ->
					log.error('UNAUTHORIZED access to URI: ' + uri)
					throw new ConnectorHttpResponseException(resp, 401, 'Unauthorized access ot the BioPortal service.')
				}
				response.'400' = { resp ->
					log.error('BAD REQUEST: ' + uri)
					throw new ConnectorHttpResponseException(resp, 401, 'Unauthorized access ot the service.')
				}					
				response.failure = { resp, json ->
					log.error('FAILURE: ' + resp.getStatusLine().toString())
				}
			}
		} catch (groovyx.net.http.HttpResponseException ex) {
			log.error("HttpResponseException: " + ex.getMessage())
			throw new RuntimeException(ex);
		}  catch (java.net.SocketTimeoutException ex) {
			log.error("SocketTimeoutException: " + ex.getMessage())
			throw new RuntimeException(ex);
		} catch (java.net.ConnectException ex) {
			log.error("ConnectException: " + ex.getMessage())
			throw new RuntimeException(ex);
		} catch (java.net.UnknownHostException ex) {
			log.error("UnknownHostException: " + ex.getMessage())
			throw new RuntimeException(ex);
		}	
		return jsonResponse;
	}
	
	@Override
	public JSONObject textmine(String resourceUri, String content, HashMap parametrization) {
		log.info 'textmine:Resource: ' + resourceUri + ' Content: ' + content + ' Parametrization: ' + parametrization
		BioPortalAnnotatorRequestParameters parameters = defaultParameters();
		
		try {
			String apikey = verifyApikey(parametrization);
			String ontologies = selectOntologies(parametrization);
			String contentText = encodeContent(content);
			
			if(parametrization.getAt("minimum_match_length")!=null)
				parameters.minimum_match_length = new Integer(parametrization.getAt("minimum_match_length"));
			if(parametrization.getAt("max_level")!=null)
				parameters.max_level = new Integer(parametrization.getAt("max_level"));
				
			parameters.apikey = apikey;
			parameters.text = contentText;
			parameters.ontologies = getOntologies(parametrization);
			
			String uri = 'http://data.bioontology.org/annotator' + parameters.toParameterString() +
				((!ontologies.isEmpty())?(ONTOLOGIES + ontologies):'');
			log.info("Annotate with URI: " + uri);

			def http = new HTTPBuilder(uri);
			http.getClient().getParams().setParameter("http.connection.timeout", new Integer(TENSECONDS))
			http.getClient().getParams().setParameter("http.socket.timeout", new Integer(THIRTYSECONDS))	
			http.encoderRegistry = new EncoderRegistry(charset: MiscUtils.DEFAULT_ENCODING)
			evaluateProxy(http, uri);
			
			JSONObject jsonResponse = new JSONObject();
			http.request(Method.GET, ContentType.JSON) {
				requestContentType = ContentType.URLENC
				headers.'Authorization' = 'apikey token=' + apikey
				
				contentText = URLDecoder.decode(contentText, "UTF-8");
				
				response.success = { resp, json ->
					println json;
					boolean isFormatDefined = parametrization.containsKey(IConnectorsParameters.RETURN_FORMAT);
					if(isFormatDefined && parametrization.get(IConnectorsParameters.RETURN_FORMAT).equals(BioPortalTextMiningDomeoConversionService.RETURN_FORMAT)) {
						jsonResponse = bioPortalTextMiningDomeoConversionService.convert(apikey, resourceUri, contentText, json)
					} else if(isFormatDefined && parametrization.get(IConnectorsParameters.RETURN_FORMAT).equals(BioPortalTextMiningCoConversionService.RETURN_FORMAT)) {
						jsonResponse = bioPortalTextMiningCoConversionService.convert(apikey, resourceUri, contentText, json)
					} else {
						jsonResponse = bioPortalTextMiningConversionService.convert(apikey, resourceUri, contentText, json)
					}
				}
						
				response.'404' = { resp ->
					log.error('Not found: ' + resp.getStatusLine())
					throw new ConnectorHttpResponseException(resp, 404, 'Service not found. The problem has been reported')
				}
				response.'401' = { resp ->
					log.error('UNAUTHORIZED access to URI: ' + uri)
					throw new ConnectorHttpResponseException(resp, 401, 'Unauthorized access to the BioPortal service.')
				}
				response.'503' = { resp ->
					log.error('Not available: ' + resp.getStatusLine())
					throw new ConnectorHttpResponseException(resp, 503, 'Service temporarily not available. Try again later.')
				}
	
				response.failure = { resp, xml ->
					log.error('Failure: ' + resp.getStatusLine())
					throw new ConnectorHttpResponseException(resp, resp.getStatusLine().toString())
				}
			}
		} catch (groovyx.net.http.HttpResponseException ex) {
			log.error("HttpResponseException: " + ex.getMessage())
			throw new RuntimeException(ex);
		} catch (java.net.ConnectException ex) {
			log.error("ConnectException: " + ex.getMessage())
			throw new RuntimeException(ex);
		} 
	}
	
	private BioPortalAnnotatorRequestParameters defaultParameters(){
		BioPortalAnnotatorRequestParameters parameters = new BioPortalAnnotatorRequestParameters();
		parameters.mappingTypes = ['Manual'] as Set
		parameters
	}
	
	/**
	 * Verifies the existence of the apikey. Returns an exception
	 * if no apikey is defined. 
	 * @param parametrization Map of parameters
	 * @return The apikey if specified.
	 */
	private String verifyApikey(HashMap parametrization) {
		if(parametrization==null || !parametrization.containsKey(IConnectorsParameters.APY_KEY)) {
			throw new RuntimeException('No apikey found');
		} else {
			return parametrization.get(IConnectorsParameters.APY_KEY).toString();
		}
	}
	
	/**
	 * Select either the ontologies specified with the parametrization or
	 * the default list.
	 * @param parametrization Map of parameters
	 * @return The list of ontologies
	 */
	private String selectOntologies(HashMap parametrization) {
		String ontologies = null;
		if(!parametrization.containsKey(PARAMS_ONTOLOGIES)) {
			log.info("Default list of ontologies selected");
			ontologies = parseOntologiesIds(ONTS1.keySet());
		} else {
			return parametrization.getAt(PARAMS_ONTOLOGIES)
		}
	}
	
	private def getOntologies(HashMap parametrization) {
		if(!parametrization.containsKey(PARAMS_ONTOLOGIES)) {
			return parametrization.getAt(PARAMS_ONTOLOGIES)
		}
	}
	
	/**
	 * Parses the ontologies and generates a comma separated list.
	 * @param ontologies The list of ontologies.
	 * @return The comma separated list of ontologies.
	 */
	private String parseOntologiesIds(def ontologies) {
		StringBuffer ontos = new StringBuffer();
		int counter=0;
		ontologies.each {
			ontos.append(it);
			if((counter++)<ontologies.size()-1) ontos.append(",");
		}
		return ontos.toString();
	}

}
