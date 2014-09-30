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

import java.text.SimpleDateFormat
import java.util.regex.Matcher
import java.util.regex.Pattern

import org.annotopia.grails.connectors.converters.BaseTextMiningConversionService
import org.annotopia.grails.connectors.vocabularies.IOAccessRestrictions
import org.annotopia.grails.connectors.vocabularies.IODomeo
import org.annotopia.grails.connectors.vocabularies.IODublinCoreTerms
import org.annotopia.grails.connectors.vocabularies.IOFoaf
import org.annotopia.grails.connectors.vocabularies.IOJsonLd
import org.annotopia.grails.connectors.vocabularies.IOOpenAnnotation
import org.annotopia.grails.connectors.vocabularies.IOPav
import org.annotopia.grails.connectors.vocabularies.IORdfs
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject

/**
 * @author Paolo Ciccarese <paolo.ciccarese@gmail.com>
 */
class BioPortalTextMiningConversionService extends BaseTextMiningConversionService {

	/** The return format that this conversion service generates. */
	public static final String RETURN_FORMAT = "annotopia";
	
	JSONObject convert(def apiKey, def url, def text, def results) {
		String snippetUrn = URN_SNIPPET_PREFIX + org.annotopia.grails.connectors.utils.UUID.uuid();
		
		JSONObject annotationSet = new JSONObject();
		annotationSet.put(IOJsonLd.jsonLdId, URN_ANNOTATION_SET_PREFIX + org.annotopia.grails.connectors.utils.UUID.uuid());
		annotationSet.put(IOJsonLd.jsonLdType, "ao:AnnotationSet");
		annotationSet.put(IORdfs.label, "NCBO Annotator Results");
		annotationSet.put(IODublinCoreTerms.description, "NCBO Annotator Results");
		annotationSet.put("ao:onResource", snippetUrn);
		
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
		
		// Put Agents
		annotationSet.put(IODomeo.agents, agents);
		
		//  Permissions
		// --------------------------------------------------------------------
		annotationSet.put(IOAccessRestrictions.permissions, getPublicPermissions());
		
		//  Resources
		// --------------------------------------------------------------------
		JSONArray resources = new JSONArray();
		
		JSONObject contentSnippet = new JSONObject();
		contentSnippet.put(IOJsonLd.jsonLdId, snippetUrn);
		contentSnippet.put(IOJsonLd.jsonLdType, IOOpenAnnotation.ContentAsText);
		contentSnippet.put(IOOpenAnnotation.chars, text);
		contentSnippet.put(IOPav.derivedFrom, url);
		resources.add(resources.size(), contentSnippet);
		
		// Put Resources
		annotationSet.put(IODomeo.resources, resources);
		
		//  Annotations
		// --------------------------------------------------------------------
		JSONArray annotations = new JSONArray();
		results.each{
			println it.annotatedClass['@id']
			println it.annotatedClass.links.ontology
			it.annotations.each { annotation ->
				//println annotation
				//println findOrCreateAndSaveSelectorUsingStringSearch(text, annotation.text, annotation.from, annotation.to);
				JSONObject ann = new JSONObject();
				ann.put(IOJsonLd.jsonLdId, URN_ANNOTATION_PREFIX+org.annotopia.grails.connectors.utils.UUID.uuid());
				ann.put(IOJsonLd.jsonLdType, IOOpenAnnotation.Annotation);
				
				JSONObject body = new JSONObject();
				body.put(IOJsonLd.jsonLdId, it.annotatedClass['@id']);
				body.put(IOJsonLd.jsonLdType, IOOpenAnnotation.SemanticTag);
				ann.put(IOOpenAnnotation.hasBody, body);
				
				JSONObject specificTarget = new JSONObject();
				specificTarget.put(IOJsonLd.jsonLdId, URN_SPECIFIC_RESOURCE_PREFIX + org.annotopia.grails.connectors.utils.UUID.uuid());
				specificTarget.put(IOJsonLd.jsonLdType, IOOpenAnnotation.SpecificResource);
				specificTarget.put(IOOpenAnnotation.hasSource, snippetUrn);
				specificTarget.put(IOOpenAnnotation.hasSelector, findOrCreateAndSaveSelectorUsingStringSearch(text, annotation.text, annotation.from, annotation.to));
				
				ann.put(IOOpenAnnotation.hasTarget, specificTarget);
				annotations.add(annotations.size(), ann);
			}
			annotationSet.put("items", annotations);
			println '-----------'
		}
		
		return annotationSet;
	}
	
	private JSONObject findOrCreateAndSaveSelectorUsingStringSearch(String text, String match, Integer start, Integer end){
		//println text + " " + match+ " " + start + " ";
		Map<String,Object> matchInfo = searchForMatch(text, match, start-1);  // -1 because they start from 1
		//println matchInfo;

		JSONObject selector = new JSONObject();
		selector.put(IOJsonLd.jsonLdId, URN_SELECTOR_PREFIX + org.annotopia.grails.connectors.utils.UUID.uuid());
		selector.put(IOJsonLd.jsonLdType, IOOpenAnnotation.TextQuoteSelector);
		selector.put(IOPav.createdOn, dateFormat.format(new Date()));
		selector.put(IOOpenAnnotation.prefix, matchInfo.prefix);
		selector.put(IOOpenAnnotation.exact, matchInfo.exact);
		selector.put(IOOpenAnnotation.suffix, matchInfo.suffix);
		return selector;
	}

	private JSONObject getConnectorAgent() {
		JSONObject bioportalConnector = new JSONObject();
		def connectorUrn = "urn:domeo:software:service:ConnectorBioPortalService:0.1-001";
		bioportalConnector.put(IOJsonLd.jsonLdId, connectorUrn);
		bioportalConnector.put(IOJsonLd.jsonLdType, "foafx:Software");
		bioportalConnector.put(IORdfs.label, "BioPortalConnector");
		bioportalConnector.put(IOFoaf.name, "BioPortalConnector");
		bioportalConnector.put(IOPav.version, "0.1 b001");
		bioportalConnector;
	}
	
	// http://data.bioontology.org/annotator
	private JSONObject getAnnotatorAgent() {
		JSONObject ncboAnnotator = new JSONObject();
		ncboAnnotator.put(IOJsonLd.jsonLdId, "http://www.bioontology.org/wiki/index.php/Annotator_Web_service");
		ncboAnnotator.put(IOJsonLd.jsonLdType, "foafx:Software");
		ncboAnnotator.put(IORdfs.label, "NCBO Annotator Web Service");
		ncboAnnotator.put(IOFoaf.name, "NCBO Annotator Web Service");
		ncboAnnotator.put(IOPav.version, "1.0");
		ncboAnnotator
	}
}
