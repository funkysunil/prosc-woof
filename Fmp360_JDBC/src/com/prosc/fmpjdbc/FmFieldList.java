package com.prosc.fmpjdbc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

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
 * An ordered list of FmFields which may be referenced by index or alias (slower).
 * @author sbarnum
 */
public class FmFieldList {
	private ArrayList<FmField> fields;
	boolean wasNull;

	FmFieldList() {
		this.fields = new ArrayList<FmField>();
	}

	FmFieldList(FmField... fields) {
		this.fields = new ArrayList<FmField>(Arrays.asList(fields));
	}

	public void add(FmField field) {
		fields.add(field);
	}

	public FmField get(int index) {
		return fields.get(index);
	}

	/** Returns the FmField objects contained in this FmFieldList. */
	public List<FmField> getFields() {
		return fields;
	}

	public int size() {
		return fields.size();
	}

	//OPTIMIZE This could be faster, and it is called very often
	public int indexOfFieldWithAlias(String alias) {
		int i=0;
		for( FmField fmField : fields ) {
			if( fmField.getAlias().equalsIgnoreCase( alias ) ) return i;
			else i++;
		}
		return -1;
	}

	/**
	 * Returns the index of the field whose column name is case-insensitive equal to to the provided <code>columnName</code>.
	 * @param columnName The columnName to search for. Do not include repeating field brackets.
	 * @param startingIndex the index to start searching from. You can call this method multiple times with different startingIndexes to get multiple column indexes with the same name.
	 * @return the index of the matching field, or -1 for no match.
	 */
	int indexOfFieldWithColumnName(String columnName, int startingIndex) {
		int fieldCount = fields.size();
		for( int n=startingIndex; n< fieldCount; n++ ) {
			FmField fmField = fields.get( n );
			if( fmField.getColumnNameNoRep().equalsIgnoreCase( columnName ) ) return n;
		}
		return -1;
	}

	public Iterator<FmField> iterator() {
		return fields.iterator();
	}

	public String toString() {
		return fields.toString();
	}
}
