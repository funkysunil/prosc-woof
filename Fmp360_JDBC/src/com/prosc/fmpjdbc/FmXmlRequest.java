package com.prosc.fmpjdbc;

import sun.misc.BASE64Encoder;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.HttpURLConnection;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.sql.SQLException;

import org.xml.sax.*;

/*
    Fmp360_JDBC is a FileMaker JDBC driver that uses the XML publishing features of FileMaker Server Advanced.
    Copyright (C) 2006  Prometheus Systems Consulting, LLC d/b/a 360Works

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
*/

/**
 * Created by IntelliJ IDEA.
 * User: jesse
 * Date: Apr 17, 2005
 * Time: 2:22:20 PM
 */
public class FmXmlRequest extends FmRequest {
	private static final int SERVER_STREAM_BUFFERSIZE = 16384;
	private URL theUrl;
	private InputStream serverStream;
	private SAXParser xParser;
	private String authString;
	private String postPrefix = "";
	private String postArgs;
	private Logger log = Logger.getLogger( FmXmlRequest.class.getName() );
	/** A set that initially contains all fields, and is trimmed down as metadata is parsed.  If there are any missingFields left after parsing metadata, an exception is thrown */
	private Set missingFields;

	public FmXmlRequest(String protocol, String host, String url, int portNumber, String username, String password, float fmVersion)  {
		try {
			this.theUrl = new URL(protocol, host, portNumber, url);
		} catch (MalformedURLException murle) {
			log.severe("Trying to create the url " + protocol + host + ":" + portNumber + "/" + url + " threw this exception" + murle);
			throw new RuntimeException(murle);
		}
		if (username != null || password != null) {
			String tempString = username + ":" + password;
			authString = new BASE64Encoder().encode(tempString.getBytes());
		}
		if (fmVersion >= 5 && fmVersion < 7) {
			this.setPostPrefix("-format=-fmp_xml&");
		}
		try {
			xParser = javax.xml.parsers.SAXParserFactory.newInstance().newSAXParser();
			setFeature( "http://xml.org/sax/features/validation", false );
			setFeature( "http://xml.org/sax/features/namespaces", false );
			setFeature( "http://apache.org/xml/features/nonvalidating/load-external-dtd", false );
			log.finer( "Created an XML parser; class is: " + xParser.getClass() );
		} catch( ParserConfigurationException e ) {
			throw new RuntimeException( e );
		} catch ( SAXException e) {
			throw new RuntimeException(e);
		}
	}

	private void setFeature( String feature, boolean enabled ) {
		try {
			xParser.getXMLReader().setFeature( feature, enabled );
		} catch( SAXException e ) { //Ignore
			log.finer( "Could not enable feature " + feature + " because of a SAXException: " + e );
		}
	}

	public void setPostPrefix(String s) {
		postPrefix = s;
	}


	public void doRequest(String input) throws IOException, FileMakerException {
		postArgs = input;
		if (serverStream != null) throw new IllegalStateException("You must call closeRequest() before sending another request.");
		HttpURLConnection theConnection = (HttpURLConnection) theUrl.openConnection();
		theConnection.setUseCaches(false);
		if (authString != null) theConnection.addRequestProperty("Authorization", "Basic " + authString);
		if (postArgs != null) {
			postArgs = postPrefix + postArgs;
			log.log(Level.FINE, theUrl + "?" + postArgs);
			theConnection.setDoOutput(true);
			PrintWriter out = new PrintWriter( theConnection.getOutputStream() );
			//out.println("-db=Contacts&-lay=Contacts&-findall=");
			out.print(postPrefix);
			out.println(postArgs);
			out.close();
		}

		try {
			int httpStatusCode = theConnection.getResponseCode();
			if( httpStatusCode == 401 ) throw new HttpAuthenticationException( theConnection.getResponseMessage() );
			if( httpStatusCode == 501 ) throw new IOException("Server returned a 501 (Not Implemented) error. If you are using FileMaker 6, be sure to add ?&fmversion=6 to the end of your JDBC URL.");
			serverStream = theConnection.getInputStream(); // new BufferedInputStream(theConnection.getInputStream(), SERVER_STREAM_BUFFERSIZE);
		} catch( IOException e ) {
			if( e.getCause() instanceof FileNotFoundException ) {
				String message = "Remote URL " + e.getCause().getMessage() + " could not be located.";
				String missingFileName = e.getCause().getMessage();
				if( missingFileName.endsWith("FMPXMLRESULT.xml") ) message += " If you are using FileMaker 6, be sure to add ?&fmversion=6 to the end of your JDBC URL.";
				throw new IOException(message);
			}
			else throw e;
		}
		readResult();
	}

	public void closeRequest() {
		//useSelectFields = false;
		//fieldDefinitions = null;
		//usedFieldArray = null;
		//allFieldNames = new ArrayList();
		//fmTable = null;
		//    foundCount = 0;
		if (serverStream != null)
			try {
				serverStream.close();
				//serverStream = null;
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
	}

	protected void finalize() throws Throwable {
		if (serverStream != null) serverStream.close();
		super.finalize();
	}

	private void readResult() throws FileMakerException {
		Thread myThread = new Thread("Parsing Thread") {
			public void run() {
				InputStream streamToParse;
				streamToParse = serverStream;
				InputSource input = new InputSource(streamToParse);
//				input.setSystemId("http://" + theUrl.getHost() + ":" + theUrl.getPort() + "/fmi/xml/" );
				//input.setSystemId("http://" + theUrl.getHost() + ":" + theUrl.getPort() );
				input.setSystemId("http://");
				FmXmlHandler xmlHandler = new FmXmlHandler();
				try {
					xParser.parse( input, xmlHandler ); // FIX!!! need some real exception handling here
				} catch (IOException ioe) {
					if (ioe.getMessage().equals("stream is closed")) {
						log.finest("The parsing thread was in the middle of parsing data from FM but someone closed the stream");
					} else {
						System.out.println("There was an error, so i'm setting all of the variables and continuing");
						onErrorSetAllVariables(ioe);
						throw new RuntimeException(ioe);
					}
				} catch (SAXException e) {
					log.fine("There was SAXException: " + e.getMessage() + ", so the parsing thread is setting all of the threading variables to true and notifying all threads.\n Here's the stack trace: ");
					e.printStackTrace();
					onErrorSetAllVariables(e);
					throw new RuntimeException(e);
				} catch (RuntimeException e) {
					log.fine("There was an error in the parsing thread: " + e.getMessage() + ", so the parsing thread is setting all of the threading "
							+ "variables to true and notifying all threads.");
					onErrorSetAllVariables(e);
					throw new RuntimeException(e);

				} finally {
					closeRequest();

				}
			}


		};
		myThread.start();
		if(hasError()) {
			FileMakerException fileMakerException = FileMakerException.exceptionForErrorCode( new Integer(errorCode) );
			log.log(Level.WARNING, fileMakerException.toString());
			throw fileMakerException;
		}

	}

	private synchronized void onErrorSetAllVariables(Throwable t) {
		recordIterator.setStoredError(t);
		productVersionIsSet = true;
		databaseNameIsSet = true;
		foundCountIsSet = true;
		recordIteratorIsSet = true;
		recordIterator.setFinished();
		//recordIterator = null;
		fieldDefinitionsListIsSet = true;
		fieldDefinitions = null;
		notifyAll();
	}

	public synchronized String getProductVersion() {
		while (!productVersionIsSet) {
			try {
				wait();
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
		return productVersion;
	}

	private synchronized void setProductVersion(String pv) {
		// some thread stuff
		productVersion = pv;
		productVersionIsSet = true;
		notifyAll();
	}

	public synchronized String getDatabaseName() {
		while (!databaseNameIsSet) {
			try {
				wait();
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
		return databaseName;
	}

	private synchronized void setDatabaseName(String dbName) {
		// some thread stuff
		databaseName = dbName;
		databaseNameIsSet = true;
		notifyAll();
	}

	public synchronized int getFoundCount() {
		while (!foundCountIsSet) {
			try {
				wait();
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
		return foundCount;
	}

	private synchronized void setFoundCount(int i) {
		// some thread stuff
		foundCount = i;
		foundCountIsSet = true;
		notifyAll();
	}


	public synchronized Iterator getRecordIterator() {
		while (!recordIteratorIsSet) {
			try {
				wait();
			} catch (InterruptedException e) {

			}
		}

		return recordIterator;
	}

	private synchronized void setRecordIterator(ResultQueue i) {
		// some thread stuff
		recordIterator = i;
		recordIteratorIsSet = true;
		notifyAll();
	}

	public synchronized FmFieldList getFieldDefinitions() throws SQLException {
		while (!fieldDefinitionsListIsSet) {
			try {
				wait();
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
		if( getErrorCode() == 0 && missingFields != null && missingFields.size() > 0 ) {
			List missingFieldNames = new LinkedList();
			for( Iterator it = missingFields.iterator(); it.hasNext(); ) {
				missingFieldNames.add( ((FmField)it.next()).getColumnName() );
			}
			closeRequest();
			throw new SQLException("The requested fields are not on the layout: " + missingFieldNames );
		}
		return fieldDefinitions;
	}

	private synchronized void setErrorCode(int code) {
		// 0 is ok
		// 401 is no results
		errorCode = code;
		errorCodeIsSet = true;
		notifyAll();

	}

	public synchronized int getErrorCode() {
		while(!errorCodeIsSet) {
			try {
				wait();
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
		return errorCode;
	}

	public boolean hasError() {
		// 0 is ok
		// 401 is no results
		int error = getErrorCode();
		if (error == 0 || error == 401) {
			return false;
		} else {
			return true;
		}
	}


	/*
	In this class we have a reference to FmFieldList called fieldDefinitions (see below). In all the uses of this class,
	the state of this reference falls into 3 categories. Either it is set by the setSelectFields() method below,
	in which case it has one or more FmFields or it might just contain 1 FmField which contains an asterisk '*" for 'select *'.
	The last case is when fieldDefinitions is initially null. This will be the case when it is not set by setSelectFields().
	i.e  for updates and inserts etc.
	*/

	private volatile FmFieldList fieldDefinitions;
	private FmTable fmTable;
	private boolean useSelectFields = false;

	private volatile String productVersion;
	private volatile String databaseName;
	private volatile int foundCount = -1;
	private FmRecord currentRow;
	private volatile ResultQueue recordIterator;
	private transient StringBuffer currentData = new StringBuffer(255);
	private transient int insertionIndex;
	private volatile int errorCode;

	private static transient int code = 0;
	private static Integer IGNORE_NODE = new Integer(code++);
	private static Integer DATA_NODE = new Integer(code++);
	private static Integer ERROR_NODE = new Integer(code++);

	/**
	 * Maps data indices in the XML data with data indices in the currentRow.  If a field in the XML data is not used, the value of that index will be -1.
	 */
	private int[] usedFieldArray = new int[32]; // The array used by the characters() method in xmlHandler.
	//private List allFieldNames = new ArrayList(); // a list of Strings.  All the Field names inside the METADATA tag.


	private boolean fieldDefinitionsListIsSet = false;
	private boolean productVersionIsSet = false;
	private boolean databaseNameIsSet = false;
	private boolean foundCountIsSet = false;
	private boolean recordIteratorIsSet = false;
	private boolean errorCodeIsSet = false;

	// ---XML parsing SAX implementation ---
	private class FmXmlHandler extends org.xml.sax.helpers.DefaultHandler {
		private StringBuffer requestContent = new StringBuffer();
		private static final boolean debugMode = false; //If true, then the content of the XML will be stored in requestContent
		private Integer currentNode = null;
		private int columnIndex;
		private InputSource emptyInput = new InputSource( new ByteArrayInputStream(new byte[0]) );
		private int sizeEstimate;
		/** Incremented as metadata fields are parsed */
		private int currentMetaDataFieldIndex;

		public FmXmlHandler() {
			super();
		}

		public void fatalError(SAXParseException e) throws SAXException {
			log.log(Level.SEVERE, e.toString(), e );
			super.fatalError(e);
		}

		public void warning( SAXParseException e ) throws SAXException {
			log.warning( e.toString() );
			super.warning( e );	//To change body of overridden methods use File | Settings | File Templates.
		}

		public void error( SAXParseException e ) throws SAXException {
			log.warning( e.toString() );
			super.error( e );	//To change body of overridden methods use File | Settings | File Templates.
		}

		/** This is necessary to work around a bug in the Crimson XML parser, which is used in the 1.4 JDK. Crimson
		 * cannot handle relative HTTP URL's, which is what FileMaker uses for it's DTDs: "/fmi/xml/FMPXMLRESULT.dtd"
		 * By returning an empty value here, we short-circuit the DTD lookup process.
		 */
		public InputSource resolveEntity( String publicId, String systemId ) {
			return emptyInput;
		}

		public void startDocument() {
			if( debugMode ) requestContent.append( "Starting document\n" );
			log.log(Level.FINEST, "Start parsing response");
			setRecordIterator(new ResultQueue(256*1024, 64*1024));  // FIX! is this a good size? -britt
			currentNode = null;
		}

		public void startElement(String uri, String xlocalName, String qName, Attributes attributes) {
			if( debugMode ) {
				requestContent.append( "<" + qName);
				for( int n=0; n<attributes.getLength(); n++ ) {
					requestContent.append( " " + attributes.getQName(n) + "=" + attributes.getValue(n) );
				}
				requestContent.append( ">");
			}
			// Frequently repeated nodes
			log.finest("Starting element qName = " + qName + " for " + this);
			if ("DATA".equals(qName)) { //FIX! What if we have multiple DATA nodes per COL, ie. repeating fields? Our insertionIndex won't change? --jsb
				columnIndex++;
				insertionIndex = usedFieldArray[columnIndex];
				currentNode = insertionIndex == -1 ? IGNORE_NODE : DATA_NODE;
				//currentData.delete( 0, currentData.length() );
				currentData = new StringBuffer( 255 );
			//} else if ("COL".equals(qName)) {
			} else if ("ROW".equals(qName)) {
				//dt.markTime("  Starting row");
				//This refers directly to the fieldDefinitions instance variable, because we don't care if we're missing fields and we don't want a checked exception. --jsb
				currentRow = new FmRecord(fieldDefinitions, Integer.valueOf(attributes.getValue("RECORDID")), Integer.valueOf(attributes.getValue("MODID")));
				columnIndex = -1;
			}
			// One-shot nodes
			else if ("FIELD".equals(qName)) {
				String fieldName = attributes.getValue("NAME");

				String fieldType = attributes.getValue("TYPE");
				FmFieldType theType = (FmFieldType)FmFieldType.typesByName.get(fieldType);
				boolean allowsNulls = "YES".equals(attributes.getValue("EMPTYOK"));
				int maxRepeat = Integer.parseInt(attributes.getValue("MAXREPEAT"));

				if (!useSelectFields) { // this is a select * query.  create a new non-repeating field and add it to the fieldDefinitions
					FmField fmField = new FmField(fmTable, fieldName, fieldName, theType, allowsNulls);
					fieldDefinitions.add(fmField);
				}
				if (maxRepeat > 1) { // this is a repeating field.  handle each repetition as a virtual field.
					for (int eachRepIndex =1; eachRepIndex <= maxRepeat; eachRepIndex++) {
						handleParsedMetaDataField(fieldName, eachRepIndex, theType, allowsNulls);
					}
				} else {
					handleParsedMetaDataField(fieldName, 0, theType, allowsNulls);
				}

			} else if ("RESULTSET".equals(qName)) {
				setFoundCount(Integer.valueOf(attributes.getValue("FOUND")).intValue()); //foundCount = Integer.valueOf(attributes.getValue("FOUND")).intValue();
				if (log.isLoggable(Level.FINE)) {
					log.log(Level.FINE, "Resultset size: " + foundCount);
				}
			} else if ("PRODUCT".equals(qName)) {
				setDatabaseName(attributes.getValue("NAME")); // databaseName = attributes.getValue("NAME");
				setProductVersion(attributes.getValue("VERSION")); // productVersion = attributes.getValue("VERSION");
			} else if ("DATABASE".equals(qName)) {
				fmTable =  new FmTable( attributes.getValue("NAME") );

				if (fieldDefinitions == null) {
					fieldDefinitions = new FmFieldList();
				}


			} else if ("ERRORCODE".equals(qName)) {
				currentNode = ERROR_NODE;
			}
		}

		/**
		 * Handle a field which was parsed in the metadata.  This will set the usedFieldArrayIndex, remove the field from missingFields, and set some metadata on the FmField object in the fieldDefintions list.
		 * @param fieldName
		 * @param repetitionIndex for non-repeating fields, this should be zero.
		 * @param theType
		 * @param allowsNulls
		 */
		private void handleParsedMetaDataField(String fieldName, int repetitionIndex, FmFieldType theType, boolean allowsNulls) {
			//assert repetitionIndex >= 0;
			// ensure that the currentMetaDataFieldIndex can hold another value
			if (currentMetaDataFieldIndex == usedFieldArray.length) {
				int[] tmp = new int[usedFieldArray.length * 2];
				System.arraycopy(usedFieldArray, 0, tmp, 0, usedFieldArray.length);
				usedFieldArray = tmp;
			}
			//
			String adjustedName = repetitionIndex == 0 ? fieldName : fieldName + "[" + repetitionIndex + "]";
			int fieldDefinitionIndex = fieldDefinitions.indexOfFieldWithColumnName(adjustedName);
			if (fieldDefinitionIndex == -1 && repetitionIndex == 1) {
				// this is the first repetition of a repeating field.  Look for a fieldDefinition name without the brackets
				fieldDefinitionIndex = fieldDefinitions.indexOfFieldWithColumnName(fieldName);
			}
			if (fieldDefinitionIndex != -1) { // set the type and nullable if this is a field in the field definitions
				FmField fmField = fieldDefinitions.get(fieldDefinitionIndex);
				fmField.setType(theType);
				fmField.setNullable(allowsNulls);
				usedFieldArray[currentMetaDataFieldIndex++] = fieldDefinitionIndex;
				if (missingFields != null) {
					missingFields.remove(fmField);
				}
			} else {
				usedFieldArray[currentMetaDataFieldIndex++] = -1;
			}
		}

		public void endDocument() throws SAXException {
			recordIterator.setFinished();
		}

		public void endElement(String uri, String localName, String qName) {
			if( debugMode ) requestContent.append( "</" + qName + ">" );
			if( "DATA".equals(qName) ) {
				if ( currentNode == DATA_NODE ) {
					currentRow.setRawValue(currentData.toString(), insertionIndex);
				}
			}
			if ("ROW".equals(qName)) {
				recordIterator.add(currentRow, (long) sizeEstimate);
				sizeEstimate = 0; // set it to 0 and start estimating again
				//records.add(currentRow);
				//if( FmConnection.getDebugLevel() >= 3 ) System.out.println("Finished record: " + ++currentRowIndex);
			}
			if ("METADATA".equals(qName)) { // Create the usedorder array.  This is done once.
				//usedFieldArray = new int[allFieldNames.size()];
				//missingFields = new LinkedHashSet( fieldDefinitions.getFields() );
				//
				//int i = 0;
				//Iterator it = allFieldNames.iterator();
				//
				//while ( it.hasNext() ) {
				//	String aFieldName = (String)it.next();
				//
				//	int columnIndex;
				//
				//	if ( (columnIndex = fieldDefinitions.indexOfFieldWithColumnName(aFieldName)) > -1) {
				//		// Get the index of the fieldName w.r.t fieldDefinitions, and put that value into the usedFieldArray
				//		usedFieldArray[i] = columnIndex;
				//		missingFields.remove( fieldDefinitions.get( columnIndex ) );
				//	} else {
				//		usedFieldArray[i] = -1; // This field columnName will not be used.
				//	}
				//	i++;
				//}
				// when i come to the metadata tag, i know all of the fields that are going to be in the table, so
				// I can let people get the fieldDefinitions

				synchronized (FmXmlRequest.this) { // this is different from the other attributes in the xml, since this one is being built on the fly and the variable is not just being "set" once we're finished reading it
					fieldDefinitionsListIsSet = true;
					FmXmlRequest.this.notifyAll();
				}
			}
		}

		public void characters(char ch[], int start, int length) {
			if( debugMode ) requestContent.append( ch, start, length );
			sizeEstimate += length;
			if (currentNode == DATA_NODE) {
				currentData.append( ch, start, length );
			} else if (currentNode == ERROR_NODE) {
				String error = new String(ch, start, length);
				// all of the error code handling has been moved to readResult method -britt
				setErrorCode(Integer.valueOf(error).intValue());
			}
		} // end of characters


	}


	/*
	*  Use this to set the fields that are actually used in the select statement.
	*  The data we get from filmaker contains all the fields so we need to parse it appropriately.
	*/
	public void setSelectFields(FmFieldList selectFields) {
		fieldDefinitions = selectFields;

		// Code will use select fields if they exist and the first one is not an asterisk
		if ("*".equals(fieldDefinitions.get(0).getColumnName() )) {
			fieldDefinitions.getFields().remove(0);
		} else {
			useSelectFields = true;
		}

		missingFields = new LinkedHashSet( fieldDefinitions.getFields() ); // this will be trimmed down as metadata is parsed
	}

	public static class HttpAuthenticationException extends IOException {
		public HttpAuthenticationException(String s) {
			super(s);
		}
	}


	public static void main(String[] args) throws IOException, FileMakerException {
		FmXmlRequest request;
		//request = new FmXmlRequest("http", "hercules.360works.com", "/fmi/xml/FMPXMLRESULT.xml", 80, null, null);
		//request.doRequest("-db=Contacts&-lay=Contacts&-findall");
		//FmXmlRequest fmXmlRequest = new FmXmlRequest("http", "fmp.360works.com", "/FMPro?-db=Names&-format=-fmp_xml&-findall", 4000);
		//FmXmlRequest fmXmlRequest = new FmXmlRequest("http", "fmp.360works.com", "/FMPro?-db=Insertions&-lay=AMSLogic&-format=-fmp_xml&-findall", 4000, "exchange", "waffle");
		//FmXmlRequest fmXmlRequest = new FmXmlRequest("http", "localhost", "/fmi/xml/FMPXMLRESULT.xml?-db=Contacts&-lay=Contacts&-findall", 3000, null, null);

		request = new FmXmlRequest("http", "orion.360works.com", "/fmi/xml/FMPXMLRESULT.xml", 80, null, null, 5);
		for (int n = 1; n <= 10; n++) {
			try {
				request.doRequest("-db=Contacts&-lay=Calc Test&-max=100&-findany");
			} finally {
				request.closeRequest();
			}
		}
	}

}
