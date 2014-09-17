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
package org.annotopia.grails.connectors.plugin.bioportal.tests;

import static org.junit.Assert.*
import grails.test.mixin.TestFor

import org.annotopia.grails.connectors.IConnectorsParameters
import org.annotopia.grails.connectors.ITextMiningService
import org.annotopia.grails.connectors.plugin.bioportal.services.BioPortalService
import org.codehaus.groovy.grails.web.json.JSONObject
import org.junit.Test

/**
 * @author Paolo Ciccarese <paolo.ciccarese@gmail.com>
 */
@TestFor(BioPortalService)
class BioPortalAnnotatorTest extends GroovyTestCase {

	def grailsApplication = new org.codehaus.groovy.grails.commons.DefaultGrailsApplication()
	
	def bioPortalService
	
	private LOG_SEPARATOR() {
		logSeparator('=' as char);
	}
	
	private logSeparator(char c) {
		if (c==null) c = '=';
		log.info(stringOf(c as char, (68) as int))
	}
	
	private final StringBuilder builder = new StringBuilder();
	private String stringOf( char c , int times ) {
		for( int i = 0 ; i < times ; i++  ) {
			builder.append( c );
		}
		String result = builder.toString();
		builder.delete( 0 , builder.length() -1 );
		return result;
	}
	
	@Test
	public void testNothingDefined() {
		log.info "TEST:testNoApiKeyDefined"
		JSONObject result = bioPortalService.textmine(null, null, null);
		assertNotNull result.error
		assertEquals result.error, 'No apikey found'
	}
	
	@Test
	public void testNoApiKeyDefined() {
		log.info "TEST:testNoApiKeyDefined"
		JSONObject result = bioPortalService.textmine(null, null, new HashMap()); 
		assertNotNull result.error
		assertEquals result.error, 'No apikey found'
	}
	
	@Test
	public void testWithApiKeyAndNoResourceDefined() {
		log.info "TEST:testWithApiKeyAndNoResourceDefined"
		HashMap parameters = new HashMap();
		parameters.put(IConnectorsParameters.APY_KEY, grailsApplication.config.annotopia.plugins.connector.bioportal.apikey)
		JSONObject result = bioPortalService.textmine(null, null, parameters);
		assertNotNull result.error
		assertEquals result.error, 'No content found'
	}

	@Test
	public void testWithApiKeyAndNoContentDefined() {
		log.info "TEST:testWithApiKeyAndNoContentDefined"
		HashMap parameters = new HashMap();
		parameters.put(IConnectorsParameters.APY_KEY, grailsApplication.config.annotopia.plugins.connector.bioportal.apikey)
		JSONObject result = bioPortalService.textmine("http://paolociccarese.info", null, parameters);
		assertNotNull result.error
		assertEquals result.error, 'No content found'
	}
	
	@Test
	public void testWithApiKeyAndContentDefined() {
		log.info "TEST:testWithApiKeyAndContentDefined"
		HashMap parameters = new HashMap();
		parameters.put(IConnectorsParameters.APY_KEY, grailsApplication.config.annotopia.plugins.connector.bioportal.apikey)
		JSONObject result = bioPortalService.textmine("http://paolociccarese.info", "APP protein accumulation is not good for humans.", parameters);
		log.info result
		assertNotNull result
	}
	
	@Test
	public void testSearchWithApiKeyAndContentDefined() {
		log.info "TEST:testSearchWithApiKeyAndContentDefined"
		HashMap parameters = new HashMap();
		parameters.put(IConnectorsParameters.APY_KEY, grailsApplication.config.annotopia.plugins.connector.bioportal.apikey)
		JSONObject result = bioPortalService.search("APP", parameters);
		log.info result
		assertNotNull result
	}
	
	@Test
	public void testVocabulariesListWithApiKey() {
		log.info "TEST:testVocabulariesListWithApiKey"
		HashMap parameters = new HashMap();
		parameters.put(IConnectorsParameters.APY_KEY, grailsApplication.config.annotopia.plugins.connector.bioportal.apikey)
		JSONObject result = bioPortalService.listVocabularies(parameters);
		//log.info result
		assertNotNull result
	}
}
