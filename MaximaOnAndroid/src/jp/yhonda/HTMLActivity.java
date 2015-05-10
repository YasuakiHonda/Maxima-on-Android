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

import java.io.File;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class HTMLActivity extends Activity {
	public String urlonCreate = null;
	WebView webview = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.htmlactivity);
		webview = (WebView) findViewById(R.id.webViewInHTMLActivity);
		webview.getSettings().setJavaScriptEnabled(true);
		webview.setWebViewClient(new WebViewClient() {
		});
		webview.getSettings().setBuiltInZoomControls(true);
		webview.getSettings().setUseWideViewPort(true);
		webview.getSettings().setLoadWithOverviewMode(true);
		webview.addJavascriptInterface(this, "MOA");
		webview.setWebChromeClient(new WebChromeClient() {
			public boolean onConsoleMessage(ConsoleMessage cm) {
				Log.d("MyApplication",
						cm.message() + " -- From line " + cm.lineNumber()
								+ " of " + cm.sourceId());
				return true;
			}
		});

		if (Build.VERSION.SDK_INT >= 11) {
			Intent intent = this.getIntent();
			boolean hwaccel = intent.getBooleanExtra("hwaccel", true);
			if (hwaccel == false) {
				webview.setLayerType(WebView.LAYER_TYPE_SOFTWARE, null);
			}
		}

		loadURLonCreate();

	}

	@JavascriptInterface
	public void setFocus() {
		class focussor implements Runnable {
			@Override
			public void run() {
				webview.requestFocus(View.FOCUS_DOWN);
				webview.loadUrl("javascript:textarea1Focus();");
			}
		}
		Log.v("MoA HTML", "setFocus is called");
		focussor ftask = new focussor();
		webview.post(ftask);
	}

	public void loadURLonCreate() {
		File f = new File("/data/data/jp.yhonda/files/maxout.html");
		if (f.exists() == true) {
			Log.v("MoA", "loadURLonCreate" + String.valueOf(f.length()));
		}
		Intent origIntent = this.getIntent();
		String urlonCreate = origIntent.getStringExtra("url");
		webview.setContentDescription(urlonCreate);
		webview.loadUrl(urlonCreate);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (event.getAction() == KeyEvent.ACTION_DOWN
				&& keyCode == KeyEvent.KEYCODE_BACK
				&& webview.canGoBack() == true) {
			webview.goBack();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}
}
