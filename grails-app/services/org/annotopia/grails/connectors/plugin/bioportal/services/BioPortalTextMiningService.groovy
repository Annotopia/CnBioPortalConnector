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

import org.annotopia.grails.connectors.ConnectorHttpResponseException
import org.annotopia.grails.connectors.ITextMiningService
import org.annotopia.grails.connectors.MiscUtils
import org.annotopia.grails.connectors.plugin.bioportal.BioPortalAnnotatorRequestParameters
import org.apache.http.conn.params.ConnRoutePNames
import org.codehaus.groovy.grails.web.json.JSONObject

/**
 * Documentation: http://data.bioontology.org/documentation#nav_annotator
 * 
 * @author Paolo Ciccarese <paolo.ciccarese@gmail.com>
 */
class BioPortalTextMiningService implements ITextMiningService {

	final static PARAMS_ONTOLOGIES = 'ontologies'
	
	final static ONTS1 = ["PR":"http://data.bioontology.org/ontologies/PR", "NIFSTD":"http://data.bioontology.org/ontologies/NIFSTD"]
	
	final static SERVICE_URL = 'http://data.bioontology.org/annotator';
	final static ONTOLOGIES = '&ontologies=';
	
	def connectorsConfigAccessService;
	def jsonBioPortalAnnotatorResultsConverterV0Service;
	
	@Override
	public JSONObject textmine(String resourceUri, String content, HashMap parametrization) {
		
		log.info 'Parametrization: ' + parametrization
		BioPortalAnnotatorRequestParameters parameters = defaultParameters();
		
		// ApiKey is necessary to connect to the BioPortal API
		def apiKey = null
		if(parametrization==null || !parametrization.containsKey(ITextMiningService.APY_KEY)) {
			def message = 'No api Key found';
			JSONObject returnMessage = new JSONObject();
			returnMessage.put("error", message);
			log.error(message);
			return returnMessage;
		} else {
			apiKey = parametrization.get(ITextMiningService.APY_KEY).toString();
			parameters.apikey = apiKey
		}
		
		if(content==null || content.trim().length()==0) {
			def message = 'No content found';
			JSONObject returnMessage = new JSONObject();
			returnMessage.put("error", message);
			log.error(message);
			return returnMessage;
		} else {
			parameters.text = URLEncoder.encode(content.trim(), MiscUtils.DEFAULT_ENCODING);
		}
		
		// If no ontology is specified, the default list 
		// of ontologies is used
		String ontologies = null;
		if(!parametrization.containsKey(PARAMS_ONTOLOGIES)) {
			log.info("Default list of ontologies selected");
			ontologies = parseOntologiesIds(ONTS1.keySet());
		} else {
			ontologies = parametrization.getAt(PARAMS_ONTOLOGIES)
		}
		
		if(parametrization.getAt("minimum_match_length")!=null)
			parameters.minimum_match_length = new Integer(parametrization.getAt("minimum_match_length"));
		if(parametrization.getAt("max_level")!=null)
			parameters.max_level = new Integer(parametrization.getAt("max_level"));
		
		String uri = 'http://data.bioontology.org/annotator' + parameters.toParameterString() +
			((!ontologies.isEmpty())?(ONTOLOGIES + ontologies):'');
		log.info("Annotate with URI: " + uri);
		if(connectorsConfigAccessService.isProxyDefined()) {
			log.info("proxy: " + connectorsConfigAccessService.getProxyIp() + "-" + connectorsConfigAccessService.getProxyPort());
		} else {
			log.info("NO PROXY selected while accessing " + uri);
		}
		
		try {
			def http = new HTTPBuilder(uri);
			
			int TENSECONDS = 10*1000;
			int THIRTYSECONDS = 30*1000;
			
			http.getClient().getParams().setParameter("http.connection.timeout", new Integer(TENSECONDS))
			http.getClient().getParams().setParameter("http.socket.timeout", new Integer(THIRTYSECONDS))
			
			http.encoderRegistry = new EncoderRegistry(charset: MiscUtils.DEFAULT_ENCODING)
			if(connectorsConfigAccessService.isProxyDefined()) {
				http.client.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, domeoConfigAccessService.getProxyHttpHost());
			}
			
			JSONObject jsonResponse = new JSONObject();
			http.request(Method.GET, ContentType.JSON) {
				requestContentType = ContentType.URLENC
				
				headers.'Authorization' = 'apikey token=fef6b9da-4b3b-46d2-9d83-9a1a718f6a22'
				
				response.success = { resp, json ->
					log.info json.size();
					
					json.eachWithIndex { annotation, i ->
						log.info annotation
						
						def annotations = annotation.annotations;
						
						def conceptId = annotation.annotatedClass["@id"]
						def ontologyId = annotation.annotatedClass.links.ontology
						
						annotations.each{ ann ->
							log.info conceptId + " - " + ontologyId + " - " + ann;
						}
					}
					
					jsonResponse = jsonBioPortalAnnotatorResultsConverterV0Service.convert(apiKey, uri, content.trim(), json);
				}
				
				response.'404' = { resp ->
					log.error('Not found: ' + resp.getStatusLine())
					throw new ConnectorHttpResponseException(resp, 404, 'Service not found. The problem has been reported')
				}

				response.'503' = { resp ->
					log.error('Not available: ' + resp.getStatusLine())
					throw new ConnectorHttpResponseException(resp, 503, 'Service temporarily not available. Try again later.')
				}

				response.failure = { resp, xml ->
					log.error('Failure: ' + resp.getStatusLine())
					throw new ConnectorHttpResponseException(resp, resp.getStatusLine())
				}
			}
			
		} catch (groovyx.net.http.HttpResponseException ex) {
			log.error("HttpResponseException: " + ex.getMessage())
			throw new RuntimeException(ex);
		} catch (java.net.ConnectException ex) {
			log.error("ConnectException: " + ex.getMessage())
			throw new RuntimeException(ex);
		}
		return new JSONObject();
	}
	
	BioPortalAnnotatorRequestParameters defaultParameters(){
		BioPortalAnnotatorRequestParameters parameters = new BioPortalAnnotatorRequestParameters();
		parameters.mappingTypes = ['Manual'] as Set
		parameters
	}
	
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
