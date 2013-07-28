/*
    Copyright 2012, 2013, Yasuaki Honda (yasuaki.honda@gmail.com)
    This file is part of MaximaOnAndroid.

    MaximaOnAndroid is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 2 of the License, or
    (at your option) any later version.

    MaximaOnAndroid is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with MaximaOnAndroid.  If not, see <http://www.gnu.org/licenses/>.
*/

package jp.yhonda;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import android.webkit.JsResult;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcel;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class ManualActivity extends Activity {
	WebView webview=null;
	String curURL="";
	Activity thisActivity;
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.v("MoAMan", "onCreate");
		thisActivity = this;
		setContentView(R.layout.htmlactivity);
		webview = (WebView) findViewById(R.id.webViewInHTMLActivity);
		webview.getSettings().setJavaScriptEnabled(true); 
		webview.getSettings().setBuiltInZoomControls(true);
		webview.getSettings().setUseWideViewPort(true);
		webview.getSettings().setLoadWithOverviewMode(true);
		webview.setWebViewClient(new WebViewClient() {
			public void onPageFinished (WebView view, String url) {
				Log.v("MoAMan","onPageFinished");
				addJSforCopyExample();
			}
		});
		webview.setWebChromeClient(new WebChromeClient() {
			public boolean onConsoleMessage(ConsoleMessage cm) {
				/*
				 * When cm.message() starts with "CECB:", it is not a log message, 
				 * but a string containing manual examples. This is first processed
				 * by the copyExampleCallback() function, then displayed as log.
				 */
				String msg=cm.message();
				if (msg.startsWith("CECB:")) {
					copyExampleCallback(msg.substring("CECB:".length()));
				}
				Log.d("MyApplication", cm.message() + " -- From line "
						+ cm.lineNumber() + " of "
						+ cm.sourceId() );
				return true;
			}
		});

		Intent origIntent=this.getIntent();
	    String urlinIntent=origIntent.getStringExtra("url");
	    boolean manLangChanged=origIntent.getBooleanExtra("manLangChanged", true);

	    Bundle bundle = null;
	    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
	    String serialized = settings.getString("parcel", null);

	    if ((manLangChanged == false) && (serialized != null)) {
	        Parcel parcel = Parcel.obtain();
	        try {
	            byte[] data = Base64.decode(serialized, 0);
	            parcel.unmarshall(data, 0, data.length);
	            parcel.setDataPosition(0);
	            bundle = parcel.readBundle();
	        } finally {
	            parcel.recycle();
	        }
	        webview.restoreState(bundle);
	    } else {
		    webview.loadUrl(urlinIntent);
	    }
        
		Editor edit=settings.edit();
		edit.remove("parcel");
		edit.commit();	        
	}

	@Override
	public boolean onKeyDown( int keyCode, KeyEvent event ) {
		if ( event.getAction() == KeyEvent.ACTION_DOWN
			&& keyCode == KeyEvent.KEYCODE_BACK ) {
			
			if ( webview.canGoBack() == true ) {
				webview.goBack();
				return true;
			} else {
		    	Intent intent = new Intent(this,MaximaOnAndroidActivity.class);
		    	setResult(RESULT_OK,intent);
		    	intent.putExtra("sender", "manualActivity");
		    	finish();
			}
		}
		return super.onKeyDown( keyCode, event );
	}

    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.manmenu, menu);
		return true;
	}
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		boolean retval=false;
		switch (item.getItemId()) {
		case R.id.gomaxima:
	      	Intent intent = new Intent(this,MaximaOnAndroidActivity.class);
	      	setResult(RESULT_OK, intent);
	      	intent.putExtra("sender", "ManualActivity");
	      	finish();
			retval= true;
			break;
		default:
			retval=false;
		}
		return retval;
	}
	
	private void addJSforCopyExample() {
		String mcmd="var scm=document.createElement('script');" +
				"scm.type='text/javascript';" +
				"scm.src='file:///android_asset/copyExample.js';" +
				"document.getElementsByTagName('head')[0].appendChild(scm);";
		webview.loadUrl("javascript:"+mcmd);
	}

	@Override
	protected void onStart() {
		super.onStart();
		Log.v("MoAMan", "onStart");
	}
	
	@Override
	protected void onRestart() {
		super.onRestart();
		Log.v("MoAMan", "onRestart");
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		Log.v("MoAMan", "onResume");
	}
	
	@Override
	protected void onPause() {
		Log.v("MoAMan", "onPause");
	    Bundle outState = new Bundle ();
		webview.saveState(outState);
	    Parcel parcel = Parcel.obtain();
	    String serialized = null;
	    try {
	    	outState.writeToParcel(parcel, 0);

	        ByteArrayOutputStream bos = new ByteArrayOutputStream();
	        bos.write(parcel.marshall());

	        serialized = Base64.encodeToString(bos.toByteArray(), 0);
	    } catch (IOException e) {
	        Log.e(getClass().getSimpleName(), e.toString(), e);
	    } finally {
	        parcel.recycle();
	    }
	    if (serialized != null) {
	        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
	        Editor editor = settings.edit();
	        editor.putString("parcel", serialized);
	        editor.commit();
	    }
	    super.onPause();
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		Log.v("MoAMan", "onStop");
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		Log.v("MoAMan", "onDestroy");
	}
	
	public void copyExampleCallback(String maximacmd) {
    	Log.v("MoAMan","copyExampleCallback()");
    	if (! maximacmd.startsWith("(%i")) {
    		String msg="This is not an execution script example.";
			Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
			return;
    	}
    	final Intent intent = new Intent(this,MaximaOnAndroidActivity.class);
    	setResult(RESULT_OK,intent);
    	intent.putExtra("maxima command", maximacmd);
    	intent.putExtra("sender", "manualActivity");
    	
    	/*
    	 * 0.5 second after this method call is returned, the Manual activity
    	 * will receive the finish() method, which will send the intent and 
    	 * terminate the manual activity.
    	 */
    	class ActivityFinisher implements Runnable {
    		Intent theIntent=intent;
    		@Override
    		public void run() {
    			thisActivity.finish();
    		}
    	}
    	Handler handle=new Handler();
    	ActivityFinisher af = new ActivityFinisher();
    	handle.postDelayed(af,500);
    	Log.v("MoAMan","end of copyExampleCallback()");

    }
}