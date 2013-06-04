package com.prosc.fmpjdbc;

import java.sql.*;

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
 * Created by IntelliJ IDEA. User: jesse Date: Apr 16, 2005 Time: 6:19:55 PM
 */
public class FmStatement implements Statement {
	private FmConnection connection;
	private StatementProcessor processor;

	public FmStatement( FmConnection connection ) {
		this.connection = connection;
	}

	protected StatementProcessor getProcessor() { return processor; }

	protected void setProcessor( StatementProcessor processor ) {
		this.processor = processor;
	}

	@Override
	public String toString() {
		return processor == null ? super.toString() : processor.toString();
	}

	//---These methods must be implemented---
	public ResultSet executeQuery( String s ) throws SQLException {
		SqlCommand command = new SqlCommand(s, connection.getCatalogSeparator() );
		close(); //Close any previous StatementProcessor
		processor = new StatementProcessor(this, command);
		processor.execute();
		return processor.getResultSet();
	}

	public int executeUpdate( String s ) throws SQLException {
		return executeUpdate( s, Statement.NO_GENERATED_KEYS );
	}

	public boolean execute( String s ) throws SQLException {
		return execute( s, Statement.NO_GENERATED_KEYS );
	}

	public ResultSet getResultSet() throws SQLException {
		if( processor == null || !processor.hasResultSet() ) return null;
		return processor.getResultSet();
	}

	public int getUpdateCount() throws SQLException {
		if( processor == null || processor.hasResultSet() ) return -1;
		return processor.getUpdateRowCount();
	}

	public void close() throws SQLException {
		if( processor != null ) {
			processor.close();
			processor = null; //Assist garbage collection
		}
	}

	public ResultSet getGeneratedKeys() throws SQLException {
		return processor.getGeneratedKeys();
	}

	public boolean getMoreResults() throws SQLException {
		return false;
	}

	public Connection getConnection() {
		return connection;
	}

	public boolean execute( String s, int i ) throws SQLException {
		SqlCommand command = new SqlCommand(s, connection.getCatalogSeparator() );
		close(); //Close any previous StatementProcessor
		processor = new StatementProcessor(this, command);
		if( i == Statement.RETURN_GENERATED_KEYS ) {
			processor.setReturnGeneratedKeys( true );
		}
		processor.execute();
		return processor.getUpdateRowCount() == 0; //If it updated zero rows then it's a SELECT
	}


	public int executeUpdate( String s, int i ) throws SQLException {
		SqlCommand command = new SqlCommand(s, connection.getCatalogSeparator() );
		close(); //Close any previous StatementProcessor
		processor = new StatementProcessor(this, command);
		if( i == Statement.RETURN_GENERATED_KEYS ) {
			processor.setReturnGeneratedKeys( true );
		}
		processor.execute();
		return processor.getUpdateRowCount();
	}
	
	public void setFetchSize( int i ) throws SQLException {
		//Ignore this hint
	}

	public int getFetchSize() throws SQLException {
		return 1;
	}
	//---These can be left abstract for now---

	public int[] executeBatch() throws SQLException {
		throw new AbstractMethodError("This feature has not been implemented yet."); //FIX!!! Broken placeholder
	}
	
	public int getQueryTimeout() throws SQLException {
		throw new AbstractMethodError( "getQueryTimeout is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void setQueryTimeout( int i ) throws SQLException {
		throw new AbstractMethodError( "setQueryTimeout is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public SQLWarning getWarnings() throws SQLException {
		throw new AbstractMethodError( "getWarnings is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void clearWarnings() throws SQLException {
		throw new AbstractMethodError( "clearWarnings is not implemented yet." ); //FIX!!! Broken placeholder
	}
	
	public int executeUpdate( String s, int[] ints ) throws SQLException {
		throw new AbstractMethodError( "executeUpdate is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public int executeUpdate( String s, String[] strings ) throws SQLException {
		throw new AbstractMethodError( "executeUpdate is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean execute( String s, int[] ints ) throws SQLException {
		throw new AbstractMethodError( "execute is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean execute( String s, String[] strings ) throws SQLException {
		throw new AbstractMethodError( "execute is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public int getMaxFieldSize() throws SQLException {
		throw new AbstractMethodError( "getMaxFieldSize is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void setMaxFieldSize( int i ) throws SQLException {
		throw new AbstractMethodError( "setMaxFieldSize is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public int getMaxRows() throws SQLException {
		throw new AbstractMethodError( "getMaxRows is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void setMaxRows( int i ) throws SQLException {
		throw new AbstractMethodError( "setMaxRows is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void setEscapeProcessing( boolean b ) throws SQLException {
		throw new AbstractMethodError( "setEscapeProcessing is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void cancel() throws SQLException {
		throw new AbstractMethodError( "cancel is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void setCursorName( String s ) throws SQLException {
		throw new AbstractMethodError( "setCursorName is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void setFetchDirection( int i ) throws SQLException {
		throw new AbstractMethodError( "setFetchDirection is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public int getFetchDirection() throws SQLException {
		throw new AbstractMethodError( "getFetchDirection is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public int getResultSetConcurrency() throws SQLException {
		throw new AbstractMethodError( "getResultSetConcurrency is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public int getResultSetType() throws SQLException {
		throw new AbstractMethodError( "getResultSetType is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void addBatch( String s ) throws SQLException {
		throw new AbstractMethodError( "addBatch is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void clearBatch() throws SQLException {
		throw new AbstractMethodError( "clearBatch is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean getMoreResults( int i ) throws SQLException {
		throw new AbstractMethodError( "getMoreResults is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public int getResultSetHoldability() throws SQLException {
		throw new AbstractMethodError( "getResultSetHoldability is not implemented yet." ); //FIX!!! Broken placeholder
	}
	
	// ==== New methods added in Java 1.5 ====

	public boolean isClosed() throws SQLException {
		throw new AbstractMethodError( "getResultSetHoldability is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void setPoolable( boolean b ) throws SQLException {
		throw new AbstractMethodError( "getResultSetHoldability is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean isPoolable() throws SQLException {
		throw new AbstractMethodError( "getResultSetHoldability is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public <T> T unwrap( Class<T> aClass ) throws SQLException {
		throw new AbstractMethodError( "getResultSetHoldability is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean isWrapperFor( Class<?> aClass ) throws SQLException {
		throw new AbstractMethodError( "getResultSetHoldability is not implemented yet." ); //FIX!!! Broken placeholder
	}
}
