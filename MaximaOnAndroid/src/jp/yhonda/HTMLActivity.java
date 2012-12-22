/*
    Copyright 2012, Yasuaki Honda (yasuaki.honda@gmail.com)
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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.webkit.WebView;
import android.webkit.WebViewClient;


public class HTMLActivity extends Activity {
	public String urlonCreate=null;
    WebView webview=null;
	
	@Override
  public void onCreate(Bundle savedInstanceState)
  {
	  super.onCreate(savedInstanceState);
      setContentView(R.layout.htmlactivity);
      webview = (WebView) findViewById(R.id.webViewInHTMLActivity);
      webview.getSettings().setJavaScriptEnabled(true); 
      webview.setWebViewClient(new WebViewClient() {}); 
      webview.getSettings().setBuiltInZoomControls(true);
      webview.getSettings().setUseWideViewPort(true);
      webview.getSettings().setLoadWithOverviewMode(true);
      loadURLonCreate();

  }
	
	public void loadURLonCreate() {
		Intent origIntent=this.getIntent();
	    String urlonCreate=origIntent.getStringExtra("url");
	    webview.loadUrl(urlonCreate);
	}

	@Override
  public boolean onKeyDown( int keyCode, KeyEvent event ) {
	  if ( event.getAction() == KeyEvent.ACTION_DOWN
	          && keyCode == KeyEvent.KEYCODE_BACK
	          && webview.canGoBack() == true ) {
	      webview.goBack();
	      return true;
	  }
	  return super.onKeyDown( keyCode, event );
  }
}
