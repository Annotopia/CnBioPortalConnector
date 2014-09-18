class UrlMappings {

	static mappings = {
		
		"/cn/bioportal/search"{
			controller = "bioPortal"
			action = [GET:"search"]
		}
		
		"/cn/bioportal/textmine"{
			controller = "bioPortal"
			action = [POST:"textmine"]
		}
		
		"/$controller/$action?/$id?"{
			constraints {
				// apply constraints here
			}
		}

		"/"(view:"/index")
		"500"(view:'/error')
	}
}
