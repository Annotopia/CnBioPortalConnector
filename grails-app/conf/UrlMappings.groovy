class UrlMappings {

	static mappings = {
		
		"/cn/bioportal/search"{
			controller = "bioPortal"
			action = [GET:"search"]
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
