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
package org.annotopia.grails.connectors.plugin.bioportal.controllers

import org.annotopia.grails.connectors.BaseConnectorController
import org.annotopia.grails.connectors.IConnectorsParameters
import org.codehaus.groovy.grails.web.json.JSONObject

/**
 * @author Paolo Ciccarese <paolo.ciccarese@gmail.com>
 */
class BioPortalController extends BaseConnectorController {

	def connectorsConfigAccessService;
	def configAccessService;
	def apiKeyAuthenticationService;
	def bioPortalService;
	
	// curl -i -X GET http://localhost:8090/cn/bioportal/search --header "Content-Type: application/json" --data '{"apiKey":"164bb0e0-248f-11e4-8c21-0800200c9a66","q":"APP","offset":"1","format":"domeo"}'
	def search = {
		long startTime = System.currentTimeMillis();
		
		def apiKey = retrieveApiKey(startTime);
		if(!apiKey) {
			return;
		}

		// Pagination
		def max = (request.JSON.max!=null)?request.JSON.max:"10";
		if(params.max!=null) max = params.max;
		def offset = (request.JSON.offset!=null)?request.JSON.offset:"0";
		if(params.offset!=null) offset = params.offset;
		
		// retrieve the return format
		def format = retrieveValue(request.JSON.format, params.format, "annotopia");
		
		// retrieve the query
		def query = retrieveValue(request.JSON.q, params.q, "q", startTime);
		if(!query) {
			return;
		}
		
		if(query!=null && !query.empty) {
			HashMap parameters = new HashMap();
			parameters.put(IConnectorsParameters.APY_KEY, configAccessService.getAsString("annotopia.plugins.connector.bioportal.apikey"))
			parameters.put("pagenumber", Integer.parseInt(offset)+1);
			parameters.put("pagesize", max);
			parameters.put(IConnectorsParameters.RETURN_FORMAT, format);
			JSONObject results = bioPortalService.search(query, parameters);
			
			response.outputStream << results.toString()
			response.outputStream.flush()
		} else {
			def message = 'Query text is null';
			render(status: 200, text: returnMessage(apiKey, "nocontent", message, startTime), contentType: "text/json", encoding: "UTF-8");
			return;
		}	
	}
	
	// curl -i -X POST http://localhost:8080/cn/bioportal/textmine --header "Content-Type: application/json" --data '{"apiKey":"testKey","text":"APP is bad for you","offset":"1","format":"annotopia"}'
	def textmine = {
		long startTime = System.currentTimeMillis();
		
		def apiKey = request.JSON.apiKey;
		if(apiKey==null) apiKey = params.apiKey;
		if(!apiKeyAuthenticationService.isApiKeyValid(request.getRemoteAddr(), apiKey)) {
			invalidApiKey(request.getRemoteAddr()); return;
		}
		
		// Return format
		def format = (request.JSON.format!=null)?request.JSON.format:"annotopia";
		if(params.format!=null) format = params.format;
		
		// Search query
		def text = (request.JSON.text!=null)?request.JSON.text:"";
		if(params.text!=null) text = URLDecoder.decode(params.text,"UTF-8");
		
		if(text!=null && !text.empty) {
			HashMap parameters = new HashMap();
			parameters.put(IConnectorsParameters.APY_KEY, configAccessService.getAsString("annotopia.plugins.connector.bioportal.apikey"))
			parameters.put(IConnectorsParameters.RETURN_FORMAT, format);
			JSONObject results = bioPortalService.textmine(null, text, parameters);
				
			response.outputStream << results.toString()
			response.outputStream.flush()
		} else {
			def message = 'Content text is null';
			render(status: 200, text: returnMessage(apiKey, "nocontent", message, startTime), contentType: "text/json", encoding: "UTF-8");
			return;
		}
	}
	
	// curl -i -X GET http://localhost:8080/cn/bioportal/vocabularies --header "Content-Type: application/json" --data '{"apiKey":"testKey","offset":"1","format":"domeo"}'
	def vocabularies = {
		long startTime = System.currentTimeMillis();
		
		def apiKey = request.JSON.apiKey;
		if(apiKey==null) apiKey = params.apiKey;
		if(!apiKeyAuthenticationService.isApiKeyValid(request.getRemoteAddr(), apiKey)) {
			invalidApiKey(request.getRemoteAddr()); return;
		}
		
		// Return format
		def format = (request.JSON.format!=null)?request.JSON.format:"annotopia";
		if(params.format!=null) format = params.format;
		
		HashMap parameters = new HashMap();
		parameters.put(IConnectorsParameters.APY_KEY, configAccessService.getAsString("annotopia.plugins.connector.bioportal.apikey"))
		parameters.put(IConnectorsParameters.RETURN_FORMAT, format);
		JSONObject results = bioPortalService.listVocabularies(parameters);
		
		response.outputStream << results.toString()
		response.outputStream.flush()		
	}
}
