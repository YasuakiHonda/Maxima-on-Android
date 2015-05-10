/*
    Copyright 2012, 2013 Yasuaki Honda (yasuaki.honda@gmail.com)
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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.webkit.JavascriptInterface;

public class MaximaOnAndroidActivity extends Activity implements
		TextView.OnEditorActionListener, OnTouchListener {
	boolean inited=false; /* expSize initialize is done or not */
	String[] mcmdArray = null; /* manual example input will be stored. */
	int mcmdArrayIndex = 0;
	Activity thisActivity = this;
	String maximaURL = null;

	String manjp = "file:///android_asset/maxima-doc/ja/maxima.html";
	String manen = "file:///android_asset/maxima-doc/en/maxima.html";
	String mande = "file:///android_asset/maxima-doc/en/de/maxima.html";
	String manes = "file:///android_asset/maxima-doc/en/es/maxima.html";
	String manpt = "file:///android_asset/maxima-doc/en/pt/maxima.html";
	String manptbr = "file:///android_asset/maxima-doc/en/pt_BR/maxima.html";
	String manURL;
	boolean manLangChanged = true;
	boolean allExampleFinished = false;
	Semaphore sem = new Semaphore(1);
	EditText editText;
	Button enterB;
	WebView webview;
	ScrollView scview;
	CommandExec maximaProccess;
	File internalDir;
	File externalDir;
	MaximaVersion mvers = new MaximaVersion(5, 34, 1);

	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.d("MoA", "onCreate()");
		super.onCreate(savedInstanceState);

		if (Build.VERSION.SDK_INT > 16) { // > JELLY_BEAN
			maximaURL = "file:///android_asset/maxima_svg.html";
			// maximaURL="http://192.168.0.20/~yasube/maxima_svg.html";
		} else {
			maximaURL = "file:///android_asset/maxima_html.html";
		}

		SharedPreferences pref = PreferenceManager
				.getDefaultSharedPreferences(this);
		if (new File("/data/local/tmp/maxima-doc").exists()) {
			manjp = "file:///data/local/tmp/maxima-doc/ja/maxima.html";
			manen = "file:///data/local/tmp/maxima-doc/en/maxima.html";
			mande = "file:///data/local/tmp/maxima-doc/en/de/maxima.html";
			manes = "file:///data/local/tmp/maxima-doc/en/es/maxima.html";
			manpt = "file:///data/local/tmp/maxima-doc/en/pt/maxima.html";
			manptbr = "file:///data/local/tmp/maxima-doc/en/pt_BR/maxima.html";
			manURL = pref.getString("manURL", manen);
			if (manURL.startsWith("file:///android_asset")) {
				Editor edit = pref.edit();
				edit.putString("manURL", manen);
				edit.commit();
			}
		} else {
			manURL = pref.getString("manURL", manen);
		}

		setContentView(R.layout.main);
		internalDir = this.getFilesDir();
		externalDir = this.getExternalFilesDir(null);

		webview = (WebView) findViewById(R.id.webView1);
		webview.getSettings().setJavaScriptEnabled(true);
		webview.setWebViewClient(new WebViewClient() {
			public void onPageFinished(WebView view, String url) {
				Log.v("MoA", "onPageFinished");
				SharedPreferences settings = PreferenceManager
						.getDefaultSharedPreferences(thisActivity);
				float sc = settings.getFloat("maxima main scale", 1.5f);
				Log.v("MoA", "sc=" + Float.toString(sc));
				view.setInitialScale((int) (100 * sc));
			}

		});
		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(thisActivity);
		float sc = settings.getFloat("maxima main scale", 1.5f);
		Log.v("MoA", "onCreate sc=" + Float.toString(sc));
		webview.setInitialScale((int) (100 * sc));

		webview.getSettings().setBuiltInZoomControls(true);
		if (Build.VERSION.SDK_INT > 11) {
			webview.getSettings().setDisplayZoomControls(false);
		}

		webview.setWebChromeClient(new WebChromeClient() {
			public boolean onConsoleMessage(ConsoleMessage cm) {
				Log.d("MyApplication",
						cm.message() + " -- From line " + cm.lineNumber()
								+ " of " + cm.sourceId());
				return true;
			}
		});
		scview = (ScrollView) findViewById(R.id.scrollView1);

		webview.addJavascriptInterface(this, "MOA");

		editText = (EditText) findViewById(R.id.editText1);
		editText.setOnEditorActionListener(this);
		int newsize = settings.getInt("MCIAfontSize", -1);
		if (newsize != -1) {
			setMCIAfontSize(newsize);
		}

		webview.setOnTouchListener(this);

		enterB = (Button) findViewById(R.id.enterB);
		enterB.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				editText.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,
						KeyEvent.KEYCODE_ENTER));
				editText.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,
						KeyEvent.KEYCODE_ENTER));
			}
		});

		MaximaVersion prevVers = new MaximaVersion();
		prevVers.loadVersFromSharedPrefs(this);
		long verNo = prevVers.versionInteger();
		long thisVerNo = mvers.versionInteger();

		if ((thisVerNo > verNo)
				|| !maximaBinaryExists()
				|| !((new File(internalDir + "/additions")).exists())
				|| !((new File(internalDir + "/init.lisp")).exists())
				|| (!(new File(internalDir + "/maxima-" + mvers.versionString()))
						.exists() && !(new File(externalDir + "/maxima-"
						+ mvers.versionString())).exists())) {
			Intent intent = new Intent(this, MOAInstallerActivity.class);
			intent.setAction(Intent.ACTION_VIEW);
			intent.putExtra("version", mvers.versionString());
			this.startActivityForResult(intent, 0);
		} else {
			// startMaxima();
			new Thread(new Runnable() {
				@Override
				public void run() {
					startMaxima();
				}
			}).start();
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.v("MoA", "onActivityResult()");
		// super.onActivityResult(requestCode, resultCode, data);
		String sender;
		if (data != null) {
			sender = data.getStringExtra("sender");
		} else {
			sender = "anon";
		}
		Log.v("MoA", "sender = " + sender);
		if (sender.equals("manualActivity")) {
			if (resultCode == RESULT_OK) {
				String mcmd = data.getStringExtra("maxima command");
				if (mcmd != null) {
					copyExample(mcmd);
				}
			}
		} else if (sender.equals("FBActivity")) {
			if (resultCode == RESULT_OK) {
				String mcmd = data.getStringExtra("maxima command");
				if (mcmd != null) {
					editText.setText(mcmd);
					editText.setSelection(mcmd.length());
					editText.requestFocus();
				}
			}
		} else if (sender.equals("MOAInstallerActivity")) {
			if (resultCode == RESULT_OK) {
				/* everything is installed properly. */
				mvers.saveVersToSharedPrefs(this);
				// startMaxima();

				new Thread(new Runnable() {
					@Override
					public void run() {
						startMaxima();
					}
				}).start();

			} else {
				new AlertDialog.Builder(this)
						.setTitle("MaximaOnAndroid Installer")
						.setMessage(
								"The installation NOT completed. Please uninstall this apk and try to re-install again.")
						.setPositiveButton("OK",
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog,
											int which) {
										finish();
									}
								}).show();
			}
		}
	}

	private void startMaxima() {
		Log.d("MoA", "startMaxima()");
		try {
			sem.acquire();
		} catch (InterruptedException e1) {
			Log.d("MoA", "exception1");
			e1.printStackTrace();
		}
		
		CpuArchitecture.initCpuArchitecture();

		webview.loadUrl(maximaURL);
		if (!(new File(internalDir + "/maxima-" + mvers.versionString()))
				.exists()
				&& !(new File(externalDir + "/maxima-" + mvers.versionString()))
						.exists()) {
			this.finish();
		}
		List<String> list = new ArrayList<String>();
		list.add(internalDir + "/" + CpuArchitecture.getMaximaFile());
		list.add("--init-lisp=" + internalDir + "/init.lisp");
	
		maximaProccess = new CommandExec();
		try {
			maximaProccess.execCommand(list);
		} catch (Exception e) {
			Log.d("MoA", "exception2");
			exitMOA();
		}
		maximaProccess.clearStringBuilder();
		sem.release();
		Log.v("MoA", "sem released.");
	}

	private void copyExample(String mcmd) {
		Log.d("MoA", "copyExample()");
		String[] mcmd2 = mcmd.split("\n\\(%");
		mcmd2[0] = mcmd2[0].replaceFirst("^\\(%", "");
		for (int i = 0; i < mcmd2.length; i++) {
			mcmd2[i] = mcmd2[i].replaceAll("\n", " ");
		}

		int j = 0;
		for (int i = 0; i < mcmd2.length; i++) {
			if (mcmd2[i].startsWith("i")) {
				j++;
			}
		}
		mcmdArray = new String[j];
		j = 0;
		for (int i = 0; i < mcmd2.length; i++) {
			if (mcmd2[i].startsWith("i")) {
				int a = mcmd2[i].indexOf(" ");
				mcmdArray[j] = mcmd2[i].substring(a + 1);
				a = mcmdArray[j].lastIndexOf(";");
				if (a > 0) {
					mcmdArray[j] = mcmdArray[j].substring(0, a + 1); /*
																	 * to
																	 * include ;
																	 */
				} else {
					a = mcmdArray[j].lastIndexOf("$");
					if (a > 0) {
						mcmdArray[j] = mcmdArray[j].substring(0, a + 1); /*
																		 * to
																		 * include
																		 * $
																		 */
					}
				}
				j++;
			}
		}
		mcmdArrayIndex = 0;
		copyExampleInputToInputArea();
	}

	private void copyExampleInputToInputArea() {
		if (mcmdArray == null)
			return;
		Log.d("MoA", "copyExampleInputToInputArea()");
		editText.setText(mcmdArray[mcmdArrayIndex]);
		editText.setSelection(mcmdArray[mcmdArrayIndex].length());
		editText.requestFocus();
		mcmdArrayIndex++;
		if (mcmdArrayIndex == mcmdArray.length) {
			allExampleFinished = true;
			mcmdArrayIndex = 0;
		}
	}

	@JavascriptInterface
	public void reuseByTouch(String maximacmd) {
		class rbttask implements Runnable {
			String text = "";

			@Override
			public void run() {
				editText.setText(text);
			}

			public void settext(String tt) {
				text = tt;
			}
		}
		rbttask viewtask = new rbttask();
		viewtask.settext(substitute(maximacmd, "<br>", ""));
		editText.post(viewtask);
	}

	@JavascriptInterface
	public void scrollToEnd() {
		Handler handler = new Handler();
		Runnable task = new Runnable() {

			@Override
			public void run() {
				Runnable viewtask = new Runnable() {
					@Override
					public void run() {
						scview.fullScroll(ScrollView.FOCUS_DOWN);
						Log.v("MoA", "scroll!");
					}
				};
				scview.post(viewtask);
			}
		};
		handler.postDelayed(task, 1000);
	}

	@JavascriptInterface
	public void fileLoaded() {
		Handler handler = new Handler();
		Runnable task = new Runnable() {

			@Override
			public void run() {
				Runnable viewtask = new Runnable() {
					@Override
					public void run() {
						if (Build.VERSION.SDK_INT > 16) { // > JELLY_BEAN
							webview.loadUrl("javascript:initSVGRenderer()");
						} else {
							webview.loadUrl("javascript:initHTMLRenderer()");
						}

						Log.v("MoA", "fileLoaded");
					}
				};
				scview.post(viewtask);
			}
		};
		handler.postDelayed(task, 1000);
	}

	public boolean onEditorAction(TextView testview, int id, KeyEvent keyEvent) {
		try {
			Log.v("MoA", "onEditorAction");
			sem.acquire();
		} catch (InterruptedException e1) {
			Log.d("MoA", "exception3");
			e1.printStackTrace();
			exitMOA();
		}
		sem.release();
		if (!inited) {
			SharedPreferences settings = PreferenceManager
					.getDefaultSharedPreferences(thisActivity);
			String newsize = settings.getString("expSize", "");
			if (newsize != "") {
				webview.loadUrl("javascript:window.ChangeExpSize("+newsize+")");
			}
			inited=true;
		}

		Log.v("MoA", "sem released");
		String cmdstr = "";
		if ((keyEvent == null) || (keyEvent.getAction() == KeyEvent.ACTION_UP)) {
			try {
				cmdstr = editText.getText().toString();
				if (cmdstr.equals("")) {
					return true;
				}
				if (cmdstr.equals("reload;")) {
					webview.loadUrl(maximaURL);
					return true;
				}
				if (cmdstr.equals("sc;")) {
					this.scrollToEnd();
					return true;
				}
				if (cmdstr.equals("quit();"))
					exitMOA();
				if (cmdstr.equals("aboutme;")) {
					showHTML("file:///android_asset/docs/aboutMOA.html", true);
					return true;
				}
				if (cmdstr.equals("man;")) {
					showHTML("file://" + internalDir
							+ "/additions/en/maxima.html", true);
					return true;
				}
				if (cmdstr.equals("manj;")) {
					showHTML("file://" + internalDir
							+ "/additions/ja/maxima.html", true);
					return true;
				}
				if (cmdstr.startsWith("textSize:")) {
					int len = "textSize:".length();
					String sizeString = cmdstr.substring(len);
					int textSize = Integer.parseInt(sizeString);
					setMCIAfontSize(textSize);
					return true;
				}
				if (cmdstr.startsWith("expSize:")) {
					int len = "expSize:".length();
					String sizeString = cmdstr.substring(len);
					webview.loadUrl("javascript:window.ChangeExpSize("+sizeString+")");
					SharedPreferences settings = PreferenceManager
							.getDefaultSharedPreferences(this);
					Editor editor = settings.edit();
					editor.putString("expSize", sizeString);
					editor.commit();
					return true;
				}
				removeTmpFiles();
				cmdstr = maxima_syntax_check(cmdstr);
				maximaProccess.maximaCmd(cmdstr + "\n");
			} catch (IOException e) {
				Log.d("MoA", "exception4");
				e.printStackTrace();
				exitMOA();
			} catch (Exception e) {
				Log.d("MoA", "exception5");
				e.printStackTrace();
				exitMOA();
			}

			webview.loadUrl("javascript:window.UpdateInput('"
					+ escapeChars(cmdstr) + "<br>" + "')");
			String resString = maximaProccess.getProcessResult();
			maximaProccess.clearStringBuilder();
			while (isStartQepcadString(resString)) {
				List<String> list = new ArrayList<String>();
				list.add("/data/data/jp.yhonda/files/additions/qepcad/qepcad.sh");
				CommandExec qepcadcom = new CommandExec();
				try {
					qepcadcom.execCommand(list);
				} catch (Exception e) {
					Log.d("MoA", "exception7");
				}
				try {
					maximaProccess.maximaCmd("qepcad finished\n");
				} catch (Exception e) {
					Log.d("MoA", "exception8");
				}
				resString = maximaProccess.getProcessResult();
				maximaProccess.clearStringBuilder();
			}

			if (isGraphFile()) {
				List<String> list = new ArrayList<String>();
				if (CpuArchitecture.getCpuArchitecture().equals(CpuArchitecture.X86)) {
					list.add(internalDir + "/additions/gnuplot/bin/gnuplot.x86");
				} else {
					list.add(internalDir + "/additions/gnuplot/bin/gnuplot");
				}
				list.add(internalDir + "/maxout.gnuplot");
				CommandExec gnuplotcom = new CommandExec();
				try {
					gnuplotcom.execCommand(list);
				} catch (Exception e) {
					Log.d("MoA", "exception6");
				}
				if ((new File("/data/data/jp.yhonda/files/maxout.html"))
						.exists()) {
					showGraph();
				}
			}

			displayMaximaCmdResults(resString);

		}

		return true;
	}

	private boolean isStartQepcadString(String res) {
		int i = res.indexOf("start qepcad");
		if (i < 0) {
			return false;
		} else if (i == 0) {
			return true;
		} else {
			// i>0
			displayMaximaCmdResults(res.substring(0, i));
			return true;
		}

	}

	private String maxima_syntax_check(String cmd) {
		/*
		 * Search the last char which is not white spaces. If the last char is
		 * semi-colon or dollar, that is OK. Otherwise, semi-colon is added at
		 * the end.
		 */
		int i = cmd.length() - 1;
		assert (i >= 0);
		char c = ';';
		while (i >= 0) {
			c = cmd.charAt(i);
			if (c == ' ' || c == '\t') {
				i--;
			} else {
				break;
			}
		}

		if (c == ';' || c == '$') {
			return (cmd.substring(0, i + 1));
		} else {
			return (cmd.substring(0, i + 1) + ';');
		}
	}

	private String escapeChars(String cmd) {
		return substitute(cmd, "'", "\\'");
	}

	private void displayMaximaCmdResults(String resString) {
		Log.v("MoA cmd", resString);
		String[] resArray = resString.split("\\$\\$\\$\\$\\$\\$");
		for (int i = 0; i < resArray.length; i++) {
			if (i % 2 == 0) {
				/* normal text, as we are outside of $$$$$$...$$$$$$ */
				if (resArray[i].equals(""))
					continue;
				String htmlStr = substitute(resArray[i], "\n", "<br>");
				webview.loadUrl("javascript:window.UpdateText('" + htmlStr
						+ "')");
			} else {
				/* tex commands, as we are inside of $$$$$$...$$$$$$ */
				String texStr = substCRinMBOX(resArray[i]);
				texStr = substitute(texStr, "\n", " \\\\\\\\ ");
				String urlstr = "javascript:window.UpdateMath('" + texStr
						+ "')";
				webview.loadUrl(urlstr);
			}
		}
		if (allExampleFinished == true) {
			Toast.makeText(this, "All examples are executed.",
					Toast.LENGTH_LONG).show();
			allExampleFinished = false;
		}
	}

	private String substCRinMBOX(String str) {
		String resValue = "";
		String tmpValue = str;
		int p;
		while ((p = tmpValue.indexOf("mbox{")) != -1) {
			resValue = resValue + tmpValue.substring(0, p) + "mbox{";
			int p2 = tmpValue.indexOf("}", p + 5);
			assert (p2 > 0);
			String tmp2Value = tmpValue.substring(p + 5, p2);
			resValue = resValue
					+ substitute(tmp2Value, "\n", "}\\\\\\\\ \\\\mbox{");
			tmpValue = tmpValue.substring(p2, tmpValue.length());
		}
		resValue = resValue + tmpValue;
		return (resValue);
	}

	static private String substitute(String input, String pattern,
			String replacement) {
		int index = input.indexOf(pattern);

		if (index == -1) {
			return input;
		}

		StringBuffer buffer = new StringBuffer();

		buffer.append(input.substring(0, index) + replacement);

		if (index + pattern.length() < input.length()) {
			String rest = input.substring(index + pattern.length(),
					input.length());
			buffer.append(substitute(rest, pattern, replacement));
		}
		return buffer.toString();
	}

	private void showHTML(String url, boolean hwaccel) {
		Intent intent = new Intent(this, HTMLActivity.class);
		intent.setAction(Intent.ACTION_VIEW);
		intent.putExtra("url", url);
		intent.putExtra("hwaccel", hwaccel);
		this.startActivity(intent);
	}

	private void showManual() {
		Intent intent = new Intent(this, ManualActivity.class);
		intent.setAction(Intent.ACTION_VIEW);
		intent.putExtra("url", manURL);
		intent.putExtra("manLangChanged", manLangChanged);
		manLangChanged = false;
		this.startActivityForResult(intent, 0);
	}

	private void showGraph() {
		if ((new File("/data/data/jp.yhonda/files/maxout.html")).exists()) {
			showHTML("file:///data/data/jp.yhonda/files/maxout.html", false);
		} else {
			Toast.makeText(this, "No graph to show.", Toast.LENGTH_LONG).show();
		}
	}

	private void removeTmpFiles() {
		File a = new File("/data/data/jp.yhonda/files/maxout.gnuplot");
		if (a.exists()) {
			a.delete();
		}
		a = new File("/data/data/jp.yhonda/files/maxout.html");
		if (a.exists()) {
			a.delete();
		}
		a = new File("/data/data/jp.yhonda/files/qepcad_input.txt");
		if (a.exists()) {
			a.delete();
		}
	}

	private Boolean isGraphFile() {
		File a = new File("/data/data/jp.yhonda/files/maxout.gnuplot");
		return (a.exists());
	}

	private Boolean isQepcadFile() {
		File a = new File("/data/data/jp.yhonda/files/qepcad_input.txt");
		return (a.exists());
	}

	private void exitMOA() {
		try {
			maximaProccess.maximaCmd("quit();\n");
			finish();
		} catch (IOException e) {
			Log.d("MoA", "exception7");
			e.printStackTrace();
		} catch (Exception e) {
			Log.d("MoA", "exception8");
			e.printStackTrace();
		}
		finish();
	}
	
	private boolean maximaBinaryExists() {
		CpuArchitecture.initCpuArchitecture();
		String res=CpuArchitecture.getMaximaFile();
		if (res.startsWith("not")) {
			return false;
		}
		return((new File(internalDir.getAbsolutePath()+"/"+res)).exists());
		
	}

	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		if (event.getAction() == KeyEvent.ACTION_DOWN) {
			switch (event.getKeyCode()) {
			case KeyEvent.KEYCODE_BACK:
				Toast.makeText(this, "Use Quit in the menu.", Toast.LENGTH_LONG)
						.show();
				return true;
			}
		}
		return super.dispatchKeyEvent(event);
	}

	@Override
	public boolean onTouch(View arg0, MotionEvent arg1) {
		if ((arg0 == webview) && (arg1.getAction() == MotionEvent.ACTION_UP)) {
			Log.v("MoA", "onTouch on webview");
			@SuppressWarnings("deprecation")
			float sc = webview.getScale();
			Log.v("MoA", "sc=" + Float.toString(sc));
			SharedPreferences settings = PreferenceManager
					.getDefaultSharedPreferences(this);
			Editor editor = settings.edit();
			editor.putFloat("maxima main scale", sc);
			editor.commit();
			// setMCIAfontSize((int)(sc*12));
		}
		return false;
	}

	private void setMCIAfontSize(int newsize) {
		editText.setTextSize((float) newsize);
		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(this);
		Editor editor = settings.edit();
		editor.putInt("MCIAfontSize", newsize);
		editor.commit();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		boolean retval = false;
		switch (item.getItemId()) {
		case R.id.about:
			showHTML("file:///android_asset/About_MoA/index.html", true);
			retval = true;
			break;
		case R.id.graph:
			showGraph();
			retval = true;
			break;
		case R.id.quit:
			exitMOA();
			retval = true;
			break;
		case R.id.nextexample:
			copyExampleInputToInputArea();
			break;
		case R.id.man:
			showManual();
			retval = true;
			break;
		case R.id.jp:
			manURL = manjp;
			manLangChanged = true;
			retval = true;
			break;
		case R.id.en:
			manURL = manen;
			manLangChanged = true;
			retval = true;
			break;
		case R.id.de:
			manURL = mande;
			manLangChanged = true;
			retval = true;
			break;
		case R.id.es:
			manURL = manes;
			manLangChanged = true;
			retval = true;
			break;
		case R.id.pt:
			manURL = manpt;
			manLangChanged = true;
			retval = true;
			break;
		case R.id.ptbr:
			manURL = manptbr;
			manLangChanged = true;
			retval = true;
			break;
		case R.id.save:
			sessionMenu("ssave();");
			retval = true;
			break;
		case R.id.restore:
			sessionMenu("srestore();");
			retval = true;
			break;
		case R.id.playback:
			sessionMenu("playback();");
			retval = true;
			break;
		default:
			return super.onOptionsItemSelected(item);
		}
		if (manLangChanged) {
			SharedPreferences pref = PreferenceManager
					.getDefaultSharedPreferences(this);
			Editor edit = pref.edit();
			edit.putString("manURL", manURL);
			edit.commit();
		}
		return retval;
	}

	private void sessionMenu(String cmd) {
		editText.setText(cmd);
		editText.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,
				KeyEvent.KEYCODE_ENTER));
		editText.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,
				KeyEvent.KEYCODE_ENTER));
	}
}
