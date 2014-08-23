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
import org.codehaus.groovy.grails.web.json.JSONObject

/**
 * @author Paolo Ciccarese <paolo.ciccarese@gmail.com>
 */
class BioPortalTermSearchService implements ITermSearchService {

	JSONObject search(String content, HashMap parameters) {
		return new JSONObject();
	}
}
