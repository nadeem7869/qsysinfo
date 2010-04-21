/********************************************************************************
 * (C) Copyright 2000-2009, by Shawn Qualia.
 *
 * This library is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU Lesser General Public License as published by 
 * the Free Software Foundation; either version 2.1 of the License, or 
 * (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY 
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public 
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License 
 * along with this library; if not, write to the Free Software Foundation, 
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 *
 * [Java is a trademark or registered trademark of Sun Microsystems, Inc. 
 * in the United States and other countries.]
 ********************************************************************************/

package org.uguess.android.sysinfo;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Date;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TabActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TabHost;

/**
 * QSystemInfo
 */
public final class QSystemInfo extends TabActivity
{

	/** Called when the activity is first created. */
	@Override
	public void onCreate( Bundle savedInstanceState )
	{
		super.onCreate( savedInstanceState );

		requestWindowFeature( Window.FEATURE_NO_TITLE );

		if ( !( Thread.getDefaultUncaughtExceptionHandler( ) instanceof ErrorHandler ) )
		{
			Thread.setDefaultUncaughtExceptionHandler( new ErrorHandler( getApplicationContext( ),
					Thread.getDefaultUncaughtExceptionHandler( ) ) );
		}

		TabHost th = getTabHost( );

		Intent it = new Intent( Intent.ACTION_VIEW );
		it.setClass( this, SysInfoManager.class );
		th.addTab( th.newTabSpec( SysInfoManager.class.getName( ) )
				.setContent( it )
				.setIndicator( getString( R.string.tab_info ),
						getResources( ).getDrawable( R.drawable.info ) ) );

		it = new Intent( Intent.ACTION_VIEW );
		it.setClass( this, ApplicationManager.class );
		th.addTab( th.newTabSpec( ApplicationManager.class.getName( ) )
				.setContent( it )
				.setIndicator( getString( R.string.tab_apps ),
						getResources( ).getDrawable( R.drawable.applications ) ) );

		it = new Intent( Intent.ACTION_VIEW );
		it.setClass( this, ProcessManager.class );
		th.addTab( th.newTabSpec( ProcessManager.class.getName( ) )
				.setContent( it )
				.setIndicator( getString( R.string.tab_procs ),
						getResources( ).getDrawable( R.drawable.processes ) ) );

		it = new Intent( Intent.ACTION_VIEW );
		it.setClass( this, NetStateManager.class );
		th.addTab( th.newTabSpec( NetStateManager.class.getName( ) )
				.setContent( it )
				.setIndicator( getString( R.string.tab_netstat ),
						getResources( ).getDrawable( R.drawable.connection ) ) );

		Util.updateIcons( this,
				getSharedPreferences( SysInfoManager.class.getSimpleName( ),
						Context.MODE_PRIVATE ) );
	}

	/**
	 * ErrorHandler
	 */
	private static final class ErrorHandler implements UncaughtExceptionHandler
	{

		private UncaughtExceptionHandler parentHandler;
		private Context ctx;

		ErrorHandler( Context ctx, UncaughtExceptionHandler parentHandler )
		{
			this.parentHandler = parentHandler;
			this.ctx = ctx;
		}

		public void uncaughtException( Thread thread, Throwable ex )
		{
			Intent it = new Intent( Intent.ACTION_VIEW );
			it.setClass( ctx, ErrorReportActivity.class );
			it.setFlags( it.getFlags( )
					| Intent.FLAG_ACTIVITY_NEW_TASK
					| Intent.FLAG_ACTIVITY_CLEAR_TOP );
			it.putExtra( "thread", thread.toString( ) ); //$NON-NLS-1$
			it.putExtra( "exception", Log.getStackTraceString( ex ) ); //$NON-NLS-1$

			PendingIntent pi = PendingIntent.getActivity( ctx, 0, it, 0 );

			Notification nc = new Notification( R.drawable.icon, "Oops", //$NON-NLS-1$
					System.currentTimeMillis( ) );

			nc.flags |= Notification.FLAG_AUTO_CANCEL;
			nc.setLatestEventInfo( ctx,
					ctx.getString( R.string.oops ),
					ctx.getString( R.string.oops_msg ),
					pi );

			( (NotificationManager) ctx.getSystemService( NOTIFICATION_SERVICE ) ).notify( (int) System.currentTimeMillis( ),
					nc );

			if ( parentHandler != null )
			{
				parentHandler.uncaughtException( thread, ex );
			}
		}
	}

	/**
	 * ErrorReportActivity
	 */
	public static final class ErrorReportActivity extends Activity
	{

		@Override
		protected void onCreate( Bundle savedInstanceState )
		{
			super.onCreate( savedInstanceState );

			getWindow( ).setFlags( WindowManager.LayoutParams.FLAG_FULLSCREEN,
					WindowManager.LayoutParams.FLAG_FULLSCREEN );

			if ( !( Thread.getDefaultUncaughtExceptionHandler( ) instanceof ErrorHandler ) )
			{
				Thread.setDefaultUncaughtExceptionHandler( new ErrorHandler( getApplicationContext( ),
						Thread.getDefaultUncaughtExceptionHandler( ) ) );
			}

			OnClickListener listener = new OnClickListener( ) {

				public void onClick( DialogInterface dialog, int which )
				{
					if ( which == DialogInterface.BUTTON_POSITIVE )
					{
						sendBugReport( );
					}

					ErrorReportActivity.this.finish( );
				}
			};

			new AlertDialog.Builder( this ).setTitle( R.string.bug_title )
					.setMessage( R.string.bug_detail )
					.setPositiveButton( R.string.agree, listener )
					.setNegativeButton( android.R.string.no, listener )
					.setCancelable( false )
					.create( )
					.show( );
		}

		private void sendBugReport( )
		{
			StringBuffer msg = new StringBuffer( );
			Intent it = new Intent( Intent.ACTION_SENDTO );
			String content = null;

			try
			{
				String title = "Bug Report - " + new Date( ).toLocaleString( ); //$NON-NLS-1$

				it.setData( Uri.parse( "mailto:qauck.aa@gmail.com" ) ); //$NON-NLS-1$

				it.putExtra( Intent.EXTRA_SUBJECT, title );

				SysInfoManager.createTextHeader( this, msg, title );

				msg.append( "\n-----THREAD-----\n" ) //$NON-NLS-1$
						.append( getIntent( ).getStringExtra( "thread" ) ); //$NON-NLS-1$

				msg.append( "\n\n-----EXCEPTION-----\n" ) //$NON-NLS-1$
						.append( getIntent( ).getStringExtra( "exception" ) );; //$NON-NLS-1$

				// try get the intermediate report first
				content = msg.toString( );

				msg.append( "\n\n-----LOGCAT-----\n" ); //$NON-NLS-1$

				Process proc = Runtime.getRuntime( )
						.exec( "logcat -d -v time *:V" ); //$NON-NLS-1$

				SysInfoManager.readRawText( msg, proc.getInputStream( ) );
			}
			catch ( Throwable e )
			{
				try
				{
					msg.append( "\n\n-----ERROR-COLLECT-REPORT-----\n" ); //$NON-NLS-1$
					msg.append( Log.getStackTraceString( e ) );
				}
				catch ( Throwable t )
				{
					// must be OOM, doing nothing
				}
			}
			finally
			{
				try
				{
					// get the final report
					content = msg.toString( );
				}
				catch ( Throwable t )
				{
					// mostly still be OOM, doing nothing
				}
				finally
				{
					if ( content != null )
					{
						try
						{
							it.putExtra( Intent.EXTRA_TEXT, content );

							it = Intent.createChooser( it, null );
							startActivity( it );

							return;
						}
						catch ( Throwable t )
						{
							// failed at last stage, log and give up
							Log.e( getClass( ).getName( ),
									t.getLocalizedMessage( ),
									t );
						}
					}

					Util.shortToast( this, R.string.bug_failed );
				}
			}
		}
	}

	/**
	 * BootReceiver
	 */
	public static final class BootReceiver extends BroadcastReceiver
	{

		@Override
		public void onReceive( Context context, Intent intent )
		{
			SharedPreferences sp = context.getSharedPreferences( SysInfoManager.class.getSimpleName( ),
					Context.MODE_PRIVATE );

			if ( sp != null
					&& sp.getBoolean( SysInfoManager.PREF_KEY_AUTO_START_ICON,
							false ) )
			{
				Util.updateIcons( context, sp );
			}
		}
	}
}