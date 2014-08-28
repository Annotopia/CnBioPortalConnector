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
package org.annotopia.grails.connectors.plugin.bioportal.services.converters

import java.text.SimpleDateFormat;

import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject

/**
 * @author Paolo Ciccarese <paolo.ciccarese@gmail.com>
 */
class BioPortalTermSearchConversionService {
	
	SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");
	
	public void convert(def jsonResponse, JSONObject jsonResults, String pageSize, def ONTS2) {		
		JSONArray elements = new JSONArray();
		jsonResponse.collection.each {  // iterate over JSON 'status' object in the response:
			JSONObject element = new JSONObject();
			element.put("@id", it['@id']);
			element.put("rdfs:label", it.prefLabel);
			element.put("dc:description", it.definition[0]);
			
			JSONObject source = new JSONObject();
			source.put("@id", it.links.ontology);
			element.put("rdf:isDefinedBy", source);
			elements.add(element);
		}
		jsonResults.put("items", elements);
		jsonResults.put("currentPage", jsonResponse.page);
		jsonResults.put("totalPages", jsonResponse.pageCount);
		
		if(jsonResponse.prevPage==null) jsonResults.put("prevPage", "none");
		else jsonResults.put("prevPage", jsonResponse.prevPage);
		
		if(jsonResponse.nextPage==null) jsonResults.put("nextPage", "none");
		else jsonResults.put("nextPage", jsonResponse.nextPage);
	}
}
