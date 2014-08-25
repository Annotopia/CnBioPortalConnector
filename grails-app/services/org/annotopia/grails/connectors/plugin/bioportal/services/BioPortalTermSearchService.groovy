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

import org.annotopia.grails.connectors.ITermSearchService
import org.annotopia.grails.connectors.ITextMiningService
import org.annotopia.grails.connectors.MiscUtils
import org.codehaus.groovy.grails.web.json.JSONObject

/**
 * @author Paolo Ciccarese <paolo.ciccarese@gmail.com>
 */
class BioPortalTermSearchService implements ITermSearchService {

	def jsonBioPortalVocabulariesService;
	
	final static PARAMS_ONTOLOGIES = 'ontologies'
	final static ONTS1 = ["PR":"http://data.bioontology.org/ontologies/PR", "NIFSTD":"http://data.bioontology.org/ontologies/NIFSTD"]
	
	JSONObject search(String content, HashMap parametrization) {
		
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
		}
		
		def textQuery = null
		if(content==null || content.trim().length()==0) {
			def message = 'No content found';
			JSONObject returnMessage = new JSONObject();
			returnMessage.put("error", message);
			log.error(message);
			return returnMessage;
		} else {
			textQuery = URLEncoder.encode(content.trim(), MiscUtils.DEFAULT_ENCODING);
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
		
		String pageNumber = (parametrization.get("pagenumber")?parametrization.get("pagenumber"):1);
		String pageSize = (parametrization.get("pagesize")?parametrization.get("pagesize"):50);

		JSONObject jsonResult = jsonBioPortalVocabulariesService.search(apiKey, textQuery, "", pageNumber, pageSize);
		return jsonResult;
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
