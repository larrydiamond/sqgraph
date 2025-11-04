/**
 *    Copyright (C) 2023-present Larry Diamond, All Rights Reserved.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    license linked below for more details.
 *
 *    For license terms, see <https://github.com/larrydiamond/sqgraph/blob/main/LICENSE.md>.
 **/
package com.ldiamond.sqgraph;

import lombok.Data;

@Data
public class ApiProjectsSearchResults {
    ApiProjectsSearchResultsComponents [] components;
}

@Data
class ApiProjectsSearchResultsComponents {
    String key;
    String name;

    public Application getApplication () {
		final Application application = new Application();
        application.setKey (key);
        application.setTitle (name);
        return application;
    }
}