package org.testng.xml;

import org.testng.TestNGException;
import org.xml.sax.SAXException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * TestNG的suiteXML解析器。
 * 
 * @date 2014-5-20 下午5:18:13
 *
 */
public class SuiteXmlParser extends XMLParser<XmlSuite> {

	@Override
	public XmlSuite parse(String currentFile, InputStream inputStream, boolean loadClasses) {
		TestNGContentHandler contentHandler = new TestNGContentHandler(currentFile, loadClasses);

		try {
			m_saxParser.parse(inputStream, contentHandler);

			return contentHandler.getSuite();
		} catch (FileNotFoundException e) {
			throw new TestNGException(e);
		} catch (SAXException e) {
			throw new TestNGException(e);
		} catch (IOException e) {
			throw new TestNGException(e);
		}
	}

}
