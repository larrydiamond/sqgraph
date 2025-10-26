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

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

import com.lowagie.text.Document;
import com.lowagie.text.Rectangle;

class PDFOutputTests {

    @Test
    void testAddNoGraphsReturnsSameDocument() {
        Document document = new Document(new Rectangle(1800, (1400 * 10)));
        Config config = new Config();
        config.setMetrics(new SQMetrics[0]);
        Document resultDocument = PDFOutput.addGraphs(document, config);
        assertEquals (document, resultDocument);
    }
}
