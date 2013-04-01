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

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class ManualActivity extends HTMLActivity {
	Context cont=this;
	
	@Override
	public void onResume () {
		super.onResume();
		Intent intent = this.getIntent();
		boolean manLangChanged=intent.getBooleanExtra("manLangChanged", false);
		if (manLangChanged) {
			String newURL=intent.getStringExtra("url");
			webview.loadUrl(newURL);
		}
	}
	private class CustomWebView extends WebViewClient {
		//Finish of the page loading
		@Override
		public void onPageFinished(WebView view, String url) {
			Log.v("man","onPageFinished");
			SharedPreferences pref=PreferenceManager.getDefaultSharedPreferences(cont);
			String targetURL=pref.getString("url", "");
			if (!targetURL.equals(url)) return;
			int scx=pref.getInt("scrollX", -1);
			int scy=pref.getInt("scrollY", -1);
			if (scx!=-1 && scy!=-1) {
				webview.scrollTo(scx, scy);
				//webview.loadUrl("javascript:window.scrollTo("+String.valueOf(scx)+","+String.valueOf(scy)+")");
			}
			Editor ed=pref.edit();
			ed.remove("scrollX");
			ed.remove("scrollY");
			ed.remove("url");
			ed.remove("scale");
			ed.commit();
		}
	}
	
	@Override
	public void loadURLonCreate() {
		webview.setWebViewClient(new CustomWebView());
		SharedPreferences pref=PreferenceManager.getDefaultSharedPreferences(this);
		String url=pref.getString("url", "");
		int sc=(int) (100*pref.getFloat("scale", 0.0f));
		if (url!="" && sc!=0) {
			webview.setInitialScale(sc);
			webview.loadUrl(url);
		} else {
			super.loadURLonCreate();
		}
		
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
	  switch (item.getItemId()) {
	  case R.id.gomaxima:
		  float scale=webview.getScale();
		  String url=webview.getUrl();
		  webview.setInitialScale(100);
		  int scy=webview.getScrollY();
		  int scx=webview.getScrollX();
		  SharedPreferences pref=PreferenceManager.getDefaultSharedPreferences(this);
		  Editor ed=pref.edit();
		  ed.putString("url", url);
		  ed.putInt("scrollY", scy);
		  ed.putInt("scrollX", scx);
		  ed.putFloat("scale", scale);
		  ed.commit();
		  
		  Intent intent = new Intent(this,MaximaOnAndroidActivity.class);
		  intent.setAction(Intent.ACTION_VIEW);
		  this.startActivity(intent);


		  return true;
	  default:
		  return super.onOptionsItemSelected(item);
	  }
    }
}
