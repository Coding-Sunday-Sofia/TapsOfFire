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

import java.io.PrintWriter;
import java.io.StringWriter;
import org.tof.R;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.TextView;

public class ErrorReportActivity extends Activity implements View.OnClickListener {
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.error_report);
		
		{
			StringBuilder fullReport=new StringBuilder(128);
			fullReport.append(getString(R.string.app_name_version)).append('\n');
			String details=getIntent().getStringExtra(EXTRA_DETAILS);
			if (details!=null) {
				fullReport.append(details).append("\n");
			}
			String report=getIntent().getStringExtra(EXTRA_REPORT);
			if (report!=null) {
				fullReport.append("\n").append(report);
			}
			m_report=fullReport.toString();
		}

		TextView textView=(TextView)findViewById(R.id.causeText);
		CharSequence causeText=getIntent().getCharSequenceExtra(EXTRA_CAUSE_TEXT);
		if (causeText!=null) {
			textView.setText(causeText);
		} else {
			textView.setText(getCauseText());
		}
		
		textView=(TextView)findViewById(R.id.pleaseText);
		CharSequence pleaseText=getIntent().getCharSequenceExtra(EXTRA_PLEASE_TEXT);
		if (pleaseText!=null) {
			textView.setText(pleaseText);
		}
			
		textView=(TextView)findViewById(R.id.report);
		textView.setHorizontallyScrolling(true);
		textView.setMovementMethod(ScrollingMovementMethod.getInstance());
		textView.setClickable(false);
		textView.setLongClickable(false);
		textView.setText(m_report);

		findViewById(R.id.send).setOnClickListener(this);
		findViewById(R.id.close).setOnClickListener(this);
		if (getCause()!=CAUSE_ERROR) {
			findViewById(R.id.close).setVisibility(View.GONE);
		}
	}
	
	public static void report(
			Context context,
			int cause,
			CharSequence causeText,
			CharSequence pleaseText,
			String details,
			Object reportOrException)
	{
		Intent intent=new Intent(context,ErrorReportActivity.class);
		intent.putExtra(EXTRA_CAUSE,cause);
		if (causeText!=null) {
			intent.putExtra(EXTRA_CAUSE_TEXT,causeText);
		}
		if (pleaseText!=null) {
			intent.putExtra(EXTRA_PLEASE_TEXT,pleaseText);
		}
		if (details!=null) {
			intent.putExtra(EXTRA_DETAILS,details);
		}
		if (reportOrException instanceof Throwable) {
			intent.putExtra(
				EXTRA_REPORT,
				getStackTrace((Throwable)reportOrException));			
		}
		if (reportOrException instanceof String) {
			intent.putExtra(
				EXTRA_REPORT,
				(String)reportOrException);			
		}
		context.startActivity(intent);
	}

	public static final String
		EXTRA_				="org.tof.ErrorReport.",
		EXTRA_CAUSE			=EXTRA_+"CAUSE",
		EXTRA_CAUSE_TEXT	=EXTRA_+"CAUSE_TEXT",
		EXTRA_PLEASE_TEXT	=EXTRA_+"PLEASE_TEXT",
		EXTRA_DETAILS		=EXTRA_+"DETAILS",
		EXTRA_REPORT		=EXTRA_+"REPORT";
	
	public static final int
		CAUSE_CRASH			=0,
		CAUSE_ERROR			=1;
	
	///////////////////////////////////////////// implementation
	
	public void onClick(View view) {
		if (view.getId()==R.id.send) {
			Intent intent=new Intent(Intent.ACTION_SEND);
			intent.setType("text/plain");
			intent.putExtra(Intent.EXTRA_EMAIL,new String[]{EMAIL});
			intent.putExtra(Intent.EXTRA_SUBJECT,getCauseMailSubject());
			intent.putExtra(Intent.EXTRA_TEXT,m_report);
			intent=Intent.createChooser(intent,getString(R.string.send_report_via));
			startActivity(intent);
			finish();
			return;
		}
		if (view.getId()==R.id.close) {
			finish();
			return;
		}
	}
	
	private int getCause() {
		int cause=getIntent().getIntExtra(EXTRA_CAUSE,-1);
		if (cause<CAUSE_CRASH || cause>CAUSE_ERROR) {
			cause=CAUSE_ERROR;
		}
		return cause;
	}
	
	private CharSequence getCauseText() {
		switch (getCause()) {
			case CAUSE_CRASH:	return getResources().getText(R.string.crashes_sad_panda);
			default:			return getResources().getText(R.string.errors_sad_panda);
		}
	}

	private String getCauseMailSubject() {
		switch (getCause()) {
			case CAUSE_CRASH:	return getString(R.string.crash_report);
			default:			return getString(R.string.error_report);
		}
	}
	
	private static String getStackTrace(Throwable throwable) {
		StringWriter stackTrace=new StringWriter();
		{
			String message=throwable.getMessage();
			if (message!=null && message.length()!=0) {
				stackTrace.append(message);
				stackTrace.append("\n\n");
			}
		}
		{
			PrintWriter writer=new PrintWriter(stackTrace);
			throwable.printStackTrace(writer);
			writer.flush();
		}
		return stackTrace.toString();		
	}
	
	/////////////////////////////////// data
	
	private String m_report;
	
	private static final String EMAIL="Dmitry.Skiba@gmail.com";
}
