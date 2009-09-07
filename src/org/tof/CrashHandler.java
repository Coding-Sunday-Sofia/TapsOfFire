/*
 * Taps of Fire
 * Copyright (C) 2009 Dmitry Skiba
 * 
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as 
 * published by the Free Software Foundation, either version 3 of 
 * the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
*/
package org.tof;

import java.lang.Thread.UncaughtExceptionHandler;
import android.content.Context;
import android.os.Process;

public class CrashHandler implements UncaughtExceptionHandler {

	public static void attachToCurrentThread(Context context) {
		Thread.setDefaultUncaughtExceptionHandler(
			new CrashHandler(context)
		);
	}
	
	public static void setDetails(String details) {
		UncaughtExceptionHandler handler=
			Thread.getDefaultUncaughtExceptionHandler();
		if (handler instanceof CrashHandler) {
			((CrashHandler)handler).m_details=details;
		}
	}
	
	///////////////////////////////////////////// implementation
	
	private CrashHandler(Context context) {
		m_context=context;
	}
	
	public void uncaughtException(Thread thread,Throwable exception) {
		ErrorReportActivity.report(
			m_context,
			ErrorReportActivity.CAUSE_CRASH,
			null,null,
			m_details,
			exception);
		
		Process.killProcess(Process.myPid());
		System.exit(10);
	}
	
	/////////////////////////////////// data

	private Context m_context;
	private String m_details;
}
