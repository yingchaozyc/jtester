package org.testng.xml;

import org.testng.TestNGException;
import org.testng.internal.ClassHelper;
import org.xml.sax.SAXException;

import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

abstract public class XMLParser<T> implements IFileParser<T> {

	protected static SAXParser m_saxParser;

	static {
		SAXParserFactory spf = loadSAXParserFactory();

		if (supportsValidation(spf)) {
			spf.setValidating(true);
		}

		try {
			m_saxParser = spf.newSAXParser();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Tries to load a <code>SAXParserFactory</code> by trying in order the
	 * following:
	 * <tt>com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl</tt>
	 * (SUN JDK5) <tt>org.apache.crimson.jaxp.SAXParserFactoryImpl</tt> (SUN
	 * JDK1.4) and last <code>SAXParserFactory.newInstance()</code>.
	 * 
	 * @return a <code>SAXParserFactory</code> implementation
	 * @throws TestNGException
	 *             thrown if no <code>SAXParserFactory</code> can be loaded
	 */
	@SuppressWarnings("rawtypes")
	private static SAXParserFactory loadSAXParserFactory() {
		SAXParserFactory spf = null;

		StringBuffer errorLog = new StringBuffer();
		try {
			Class factoryClass = ClassHelper
					.forName("com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl");
			spf = (SAXParserFactory) factoryClass.newInstance();
		} catch (Exception ex) {
			errorLog.append("JDK5 SAXParserFactory cannot be loaded: "
					+ ex.getMessage());
		}

		if (null == spf) {
			// If running with JDK 1.4
			try {
				Class factoryClass = ClassHelper
						.forName("org.apache.crimson.jaxp.SAXParserFactoryImpl");
				spf = (SAXParserFactory) factoryClass.newInstance();
			} catch (Exception ex) {
				errorLog.append("\n").append(
						"JDK1.4 SAXParserFactory cannot be loaded: "
								+ ex.getMessage());
			}
		}

		Throwable cause = null;
		if (null == spf) {
			try {
				spf = SAXParserFactory.newInstance();
			} catch (FactoryConfigurationError fcerr) {
				cause = fcerr;
			}
		}

		if (null == spf) {
			throw new TestNGException("Cannot initialize a SAXParserFactory\n"
					+ errorLog.toString(), cause);
		}

		return spf;
	}

	/**
	 * Tests if the current <code>SAXParserFactory</code> supports DTD
	 * validation.
	 * 
	 * @param spf
	 * @return
	 */
	private static boolean supportsValidation(SAXParserFactory spf) {
		try {
			return spf.getFeature("http://xml.org/sax/features/validation");
		} catch (Exception ex) {
			;
		}

		return false;
	}
}
