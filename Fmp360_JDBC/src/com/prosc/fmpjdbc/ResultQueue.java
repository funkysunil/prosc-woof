package com.prosc.fmpjdbc;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;

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
 * User: brittany
 * Date: Jun 6, 2006
 * Time: 12:17:09 PM
 * To change this template use File | Settings | File Templates.
 */
public class ResultQueue implements Iterator<FmRecord> {
	private static final Logger log = Logger.getLogger( ResultQueue.class.getName() );

	private final long resumeSize; // this is when we'll start adding items again
	private final LinkedList<FmRecord> objects; // LinkedLists are un-synchronized
	private final LinkedList<Long> sizes;

	private volatile Throwable storedError;
	private long maxSize; // this is the max size of the queue
	private long currentSize; // this is the currentSize of the queue
	private boolean finished;
	private int rowsReturned = 0;
	private int rowsReceived = 0;
	private int errorRow = -1;
	private String errorFieldName;


	public ResultQueue(long msize, long rsize) {
		maxSize = msize; // so they can't change the size in the middle of processing
		resumeSize = rsize;
		currentSize = 0;
		finished = false;
		objects = new LinkedList<FmRecord>();
		sizes = new LinkedList<Long>();
	}

	public synchronized void setStoredError( Throwable storedError, String field ) {
		log.finest( "*** Stored an error ***" );
		this.storedError = storedError;
		this.errorFieldName = field;
		errorRow = rowsReceived;
		notify();
	}


	/**
	 * This method will add something to the "queue", it takes in an estimate of the objects size
	 *  and whether or not this is the last item to EVER be added to the queue.
	 * @param toAdd - The item (Object) to add to the queue
	 * @param size - An estimate of the size of toAdd
	 */
	public synchronized void add(FmRecord toAdd, long size) throws InterruptedException {
		// keep track of total size and only blocks until it can add new elements when
		// what about when the first item you try to add to the list is LARGER than the max size?
		// INCREASE THE MAX SIZE AND ADD IT ANYWAY
		if (!(currentSize >= maxSize && objects.size() == 0)) {
			while (currentSize >= maxSize) {
				wait();
			} // now it's ok to add something to the queue
		} else {
			// increase the current size because i'm trying to add the first object to the queue, and it's
			// bigger than the max size
			maxSize = size;
		}

		// when it's ok for me to add (notify will be called), add toAdd and size to
		// respective queues, and update the current size of the queue
		objects.addLast(toAdd);
		sizes.addLast( size );
		currentSize += size;
		rowsReceived++;
		notifyAll(); // just in case someone's waiting to get something out of the queue
	}

	/**
	 * This iterator has a next if the last item has not been added yet
	 * @return true if there are more items to iterate thru
	 */
	public synchronized boolean hasNext() {
		// might not be finished, but nothing ready now so wait...
		boolean resetInterrupt = false;
		while ( !finished && objects.size() <= 0 ) {
			if( storedError != null ) return true; //This will be thrown in the next() method
			try {
				wait();
				if( storedError != null ) return true; //We neeed to check before and after the call to wait()
			} catch (InterruptedException ie) {
				log.log( Level.WARNING, "Interrupted while waiting for next item in ResultQueue" );
				resetInterrupt = true;
			}
		}
		if( storedError != null ) return true; //We need to check this again, because it's possible that finished could be set to true at the same time that an error was stored. In that case, we want to throw the error, and the only way to do that is to return true so that the client will call next(). --jsb
		if( resetInterrupt ) {
			Thread.currentThread().interrupt();
		}

		return objects.size() > 0;
	}

	public synchronized void setFinished() {
		finished = true;
		notifyAll();
	}

	/**
	 * This method will return the next item in the iterator, and it will notify
	 * the add method that it's OK to add more Objects to the iterator
	 * @return the next item in the iterator
	 */
	public synchronized FmRecord next() {
		while (objects.size() == 0 && storedError == null ) { // objects and sizes should always have the same # of elements
			// just in case i'm taking them out faster than i can put them in
			if (finished) {
				// somebody forgot to check for hasNext before calling next!!!!
				throw new NoSuchElementException("There are no elements left in the ResultQueue");
			} else {
				boolean resetInterrupt = false;
				try {
					wait();
				} catch (InterruptedException e) {
					resetInterrupt = true;
				}
				if( resetInterrupt ) {
					Thread.currentThread().interrupt();
				}
			}
		} // now there's something in the queue

		if( storedError != null && errorRow == rowsReturned ) {
			if( storedError instanceof RuntimeException ) throw (RuntimeException)storedError;
			else if( storedError instanceof Error ) throw (Error)storedError;
			else throw new RuntimeException("Error while trying to access field '" + errorFieldName + "' in zero-indexed row " + errorRow + ": " + storedError.toString(), storedError);
		}

		FmRecord toReturn = objects.removeFirst();
		Object toReturnSize = sizes.removeFirst();

		currentSize -= (Long)toReturnSize;
		if (currentSize < resumeSize) {
			notifyAll();
		}

		rowsReturned++;
		return toReturn;
	}

	public void remove() {
		throw new AbstractMethodError("Not implemented");
	}
}
