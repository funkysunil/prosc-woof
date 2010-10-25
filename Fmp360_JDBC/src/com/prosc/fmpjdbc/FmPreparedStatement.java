package com.prosc.fmpjdbc;

import java.sql.*;
import java.math.BigDecimal;
import java.io.InputStream;
import java.io.Reader;
import java.io.IOException;
import java.util.Calendar;
import java.util.Vector;
import java.util.Iterator;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Filter;
import java.util.logging.Logger;
import java.net.URL;

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
 * Created by IntelliJ IDEA. User: jesse Date: Apr 16, 2005 Time: 6:39:54 PM
 */
public class FmPreparedStatement extends FmStatement implements PreparedStatement {
	String sql;
	Vector params;

	public FmPreparedStatement( FmConnection connection ) {
		super( connection );
	}

	public FmPreparedStatement( FmConnection connection, String sql ) throws SqlParseException {
		super( connection );
		this.sql = sql;
		int size = 0;
		SqlCommand command = new SqlCommand( sql );
		setProcessor( new StatementProcessor( this, command ) );
		for( Iterator it = command.getSearchTerms().iterator(); it.hasNext(); ) {
			SearchTerm eachTerm = (SearchTerm)it.next();
			if( eachTerm.isPlaceholder() ) size++;
		}
		for( Iterator it = command.getAssignmentTerms().iterator(); it.hasNext(); ) {
			AssignmentTerm eachTerm = (AssignmentTerm)it.next();
			if( eachTerm.isPlaceholder() ) size++;
		}
		params = new Vector(Arrays.asList(new Object[size]));
	}

	public boolean execute() throws SQLException {
		processor().setParams( params );
		try {
			processor().execute();
		} catch (RuntimeException e) {
			e.printStackTrace();
			throw e;
		}
		return processor().hasResultSet();
	}

/*	private String generateSQL(){
	//	"INSERT INTO Portrait (contactID, mimeType, \"Alternate Mime Type\", \"Date Created\", \"Time inserted\", \"Picture taken\" " +
	//			"values(?,?,'JDBC testing',?,?,?)"\

	}*/

	public ResultSet executeQuery() throws SQLException {
		execute();
		return processor().getResultSet();
	}

	public int executeUpdate() throws SQLException {
		execute();
		return processor().getUpdateRowCount();
	}

	public void setBoolean( int i, boolean b ) throws SQLException {
		setParam( i, new Boolean(b) );
	}

	public void setByte( int i, byte b ) throws SQLException {
		setParam( i, new Byte(b) );
	}

	public void setShort( int i, short i1 ) throws SQLException {
		setParam( i, new Short(i1) );
	}

	public void setInt( int i, int i1 ) throws SQLException {
		setParam( i, new Integer(i1) );
	}

	public void setLong( int i, long l ) throws SQLException {
		setParam( i, new Long(l) );
	}

	public void setFloat( int i, float v ) throws SQLException {
		setParam( i, new Float(v) );
	}

	public void setDouble( int i, double v ) throws SQLException {
		setParam( i, new Double(v) );
	}

	public void setBigDecimal( int i, BigDecimal decimal ) throws SQLException {
		setParam( i, decimal );
	}

	public void setString( int i, String s ) throws SQLException {
		setParam( i, s );
	}

	public void setBytes( int i, byte[] bytes ) throws SQLException {
		setParam( i, bytes );
	}

	public void setDate( int i, Date date ) throws SQLException {
		setParam( i, date );
	}

	public void setDate( int i, Date date, Calendar calendar ) throws SQLException {
		setParam( i, new DateWithZone(date,calendar.getTimeZone()) );
	}

	public void setTime( int i, Time time ) throws SQLException {
		setParam( i, time );
	}

	public void setTime( int i, Time time, Calendar calendar ) throws SQLException {
		setParam( i, new TimeWithZone( time, calendar.getTimeZone() ) );
	}

	public void setTimestamp( int i, Timestamp timestamp ) throws SQLException {
		setParam( i, timestamp );
	}

	public void setNull( int i, int i1 ) throws SQLException {
		throw new AbstractMethodError( "setNull is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void setAsciiStream( int i, InputStream stream, int i1 ) throws SQLException {
		throw new AbstractMethodError( "setAsciiStream is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void setUnicodeStream( int i, InputStream stream, int i1 ) throws SQLException {
		throw new AbstractMethodError( "setUnicodeStream is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void setBinaryStream( int i, InputStream stream, int i1 ) throws SQLException {
		throw new AbstractMethodError( "setBinaryStream is not implemented yet." ); //FIX!!! Broken placeholder
	}

	private void setParam(int index, Object value) {
		try {
			params.set( index - 1, value );
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new ArrayIndexOutOfBoundsException("There is no placeholder at position " + index + "; please check your query." );
		}
	}

	public void clearParameters() throws SQLException {
		int size = params.size();
		params.clear();
		for( int n=0; n<size; n++ ) {
			params.add( null );
		}
	}

	public void setObject( int i, Object o, int i1, int i2 ) throws SQLException {
		throw new AbstractMethodError( "setObject is not implemented yet." ); //FIX!!! Broken placeholder
	}

	//This is used by EOF, we have to support this --jsb
	//FIX! Being a little lazy on the switch statement; should these be more differentiated?
	public void setObject( int i, Object o, int sqlType ) throws SQLException {
		switch( sqlType ) {
			case Types.CHAR:
			case Types.VARCHAR:
			case Types.LONGVARCHAR:
				setString( i, String.valueOf(o) );
				break;

			case Types.INTEGER:
			case Types.TINYINT:
			case Types.BIGINT:
				setInt( i, ((Number)o).intValue() );
				break;

			case Types.FLOAT:
			case Types.DOUBLE:
				setDouble( i, ((Number)o).doubleValue() );
				break;

			//BigDecimal? BigInt?

			case Types.DATE:
				long milliseconds = ((java.util.Date)o).getTime();
				setDate( i, new java.sql.Date(milliseconds) );
				break;

				 //FIX!!! Do conversions for all data types
			case Types.TIME:
				milliseconds = ((java.util.Date)o).getTime();
				setTime( i, new java.sql.Time(milliseconds) );
				break;

			case Types.TIMESTAMP:
				milliseconds = ((java.util.Date)o).getTime();
				setTimestamp( i, new java.sql.Timestamp(milliseconds) );
				break;

			default:
				// ("FIX!!! I'm supposed to do something here.... --jsb");
				break;
		}
	}

	public void setObject( int i, Object o ) throws SQLException {
		params.set( i - 1, o );
		//throw new AbstractMethodError( "setObject is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void setCharacterStream( int i, Reader reader, int length ) throws SQLException {
		char[] buffer = new char[length];
		try {
			reader.read( buffer );
		} catch( IOException e ) {
			throw new RuntimeException( e );
		}
		setString( i, new String(buffer) );
		//throw new AbstractMethodError( "setCharacterStream is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void addBatch() throws SQLException {
		throw new AbstractMethodError( "addBatch is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void setRef( int i, Ref ref ) throws SQLException {
		throw new AbstractMethodError( "setRef is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void setBlob( int i, Blob blob ) throws SQLException {
		throw new AbstractMethodError( "setBlob is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void setClob( int i, Clob clob ) throws SQLException {
		throw new AbstractMethodError( "setClob is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void setArray( int i, Array array ) throws SQLException {
		throw new AbstractMethodError( "setArray is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public ResultSetMetaData getMetaData() throws SQLException {
		throw new AbstractMethodError( "getMetaData is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void setTimestamp( int i, Timestamp timestamp, Calendar calendar ) throws SQLException {
		throw new AbstractMethodError( "setTimestamp is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void setNull( int i, int i1, String s ) throws SQLException {
		throw new AbstractMethodError( "setNull is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void setURL( int i, URL url ) throws SQLException {
		throw new AbstractMethodError( "setURL is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public ParameterMetaData getParameterMetaData() throws SQLException {
		throw new AbstractMethodError( "getParameterMetaData is not implemented yet." ); //FIX!!! Broken placeholder
	}
	
	// === These methods were added in JDK 1.5 ===

	public void setNString( int i, String s ) throws SQLException {
		throw new AbstractMethodError("This feature has not been implemented yet."); //FIX!!! Broken placeholder
	}

	public void setNCharacterStream( int i, Reader reader, long l ) throws SQLException {
		throw new AbstractMethodError("This feature has not been implemented yet."); //FIX!!! Broken placeholder
	}

	public void setClob( int i, Reader reader, long l ) throws SQLException {
		throw new AbstractMethodError("This feature has not been implemented yet."); //FIX!!! Broken placeholder
	}

	public void setBlob( int i, InputStream stream, long l ) throws SQLException {
		throw new AbstractMethodError("This feature has not been implemented yet."); //FIX!!! Broken placeholder
	}

	public void setNClob( int i, Reader reader, long l ) throws SQLException {
		throw new AbstractMethodError("This feature has not been implemented yet."); //FIX!!! Broken placeholder
	}

	public void setAsciiStream( int i, InputStream stream, long l ) throws SQLException {
		throw new AbstractMethodError("This feature has not been implemented yet."); //FIX!!! Broken placeholder
	}

	public void setBinaryStream( int i, InputStream stream, long l ) throws SQLException {
		throw new AbstractMethodError("This feature has not been implemented yet."); //FIX!!! Broken placeholder
	}

	public void setCharacterStream( int i, Reader reader, long l ) throws SQLException {
		throw new AbstractMethodError("This feature has not been implemented yet."); //FIX!!! Broken placeholder
	}

	public void setAsciiStream( int i, InputStream stream ) throws SQLException {
		throw new AbstractMethodError("This feature has not been implemented yet."); //FIX!!! Broken placeholder
	}

	public void setBinaryStream( int i, InputStream stream ) throws SQLException {
		throw new AbstractMethodError("This feature has not been implemented yet."); //FIX!!! Broken placeholder
	}

	public void setCharacterStream( int i, Reader reader ) throws SQLException {
		throw new AbstractMethodError("This feature has not been implemented yet."); //FIX!!! Broken placeholder
	}

	public void setNCharacterStream( int i, Reader reader ) throws SQLException {
		throw new AbstractMethodError("This feature has not been implemented yet."); //FIX!!! Broken placeholder
	}

	public void setClob( int i, Reader reader ) throws SQLException {
		throw new AbstractMethodError("This feature has not been implemented yet."); //FIX!!! Broken placeholder
	}

	public void setBlob( int i, InputStream stream ) throws SQLException {
		throw new AbstractMethodError("This feature has not been implemented yet."); //FIX!!! Broken placeholder
	}

	public void setNClob( int i, Reader reader ) throws SQLException {
		throw new AbstractMethodError("This feature has not been implemented yet."); //FIX!!! Broken placeholder
	}
	
	// === Comment these methods out to compile in Java 1.5 ===

	public void setRowId( int i, RowId id ) throws SQLException {
		throw new AbstractMethodError("This feature has not been implemented yet."); //FIX!!! Broken placeholder
	}

	public void setNClob( int i, NClob clob ) throws SQLException {
		throw new AbstractMethodError("This feature has not been implemented yet."); //FIX!!! Broken placeholder
	}

	public void setSQLXML( int i, SQLXML sqlxml ) throws SQLException {
		throw new AbstractMethodError("This feature has not been implemented yet."); //FIX!!! Broken placeholder
	}
}