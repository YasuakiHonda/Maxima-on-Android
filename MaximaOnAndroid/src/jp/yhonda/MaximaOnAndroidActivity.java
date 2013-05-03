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
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
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
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View.MeasureSpec;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.webkit.JavascriptInterface;

public class MaximaOnAndroidActivity extends Activity
{
	
	String maximaURL="file:///android_asset/maxima.html";

	//String maximaURL="http://192.168.0.20/~yasube/maxima.html";

	String manjp="file:///android_asset/maxima-doc/ja/maxima.html";
	String manen="file:///android_asset/maxima-doc/en/maxima.html";
	String mande="file:///android_asset/maxima-doc/en/de/maxima.html";
	String manURL=manen;
	boolean manLangChanged=true;
	Semaphore sem = new Semaphore(1);
    WebView webview;
    CommandExec maximaProccess;
    File internalDir;
    File externalDir;
    MaximaVersion mvers=new MaximaVersion(5,29,1);
    
      @Override
    public void onCreate(Bundle savedInstanceState)
    {
        Log.d("MoA", "onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        SharedPreferences pref=PreferenceManager.getDefaultSharedPreferences(this);
        manURL=pref.getString("manURL", manen);
    	internalDir = this.getFilesDir();
    	externalDir = this.getExternalFilesDir(null);

        webview = (WebView) findViewById(R.id.webView1);
        webview.getSettings().setJavaScriptEnabled(true); 
        webview.setWebViewClient(new WebViewClient() {}); 
        //webview.getSettings().setBuiltInZoomControls(true);
        webview.setWebChromeClient(new WebChromeClient() {
        	  public boolean onConsoleMessage(ConsoleMessage cm) {
        	    Log.d("MoA w", cm.message() + " -- From line "
        	                         + cm.lineNumber() + " of "
        	                         + cm.sourceId() );
        	    return true;
        	  }
        	});
        
        webview.addJavascriptInterface(this, "MOA");
        Log.v("MoA","webview.loadUrl(maximaURL)");
        webview.loadUrl(maximaURL);
        
        MaximaVersion prevVers=new MaximaVersion();
        prevVers.loadVersFromSharedPrefs(this);
        long verNo = prevVers.versionInteger();
        long thisVerNo = mvers.versionInteger();
        
    	if ((thisVerNo > verNo) || 
    		!((new File(internalDir+"/maxima")).exists()) || 
    		!((new File(internalDir+"/additions")).exists()) ||
    		!((new File(internalDir+"/init.lisp")).exists()) ||
    		(!( new File( internalDir+"/maxima-"+mvers.versionString() ) ).exists() 
        	    && ! ( new File( externalDir+"/maxima-"+mvers.versionString() ) ).exists()))
    	{
              	Intent intent = new Intent(this,MOAInstallerActivity.class);
              	intent.setAction(Intent.ACTION_VIEW);
              	intent.putExtra("version", mvers.versionString());
              	this.startActivityForResult(intent,0);
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
    	if (resultCode==RESULT_OK) {
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
    		.setMessage("The installation NOT completed. Please uninstall this apk and try to re-install again.")
    		.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
            		finish();
            	}
            	})
    		.show();
    	}
    }
    
    private void startMaxima() {
    	Log.d("MoA","startMaxima()");
    	try {
			sem.acquire();
		} catch (InterruptedException e1) {
			Log.d("MoA", "exception1");
			e1.printStackTrace();
		}
    	if ( ! ( new File( internalDir+"/maxima-"+mvers.versionString() ) ).exists() &&
             	 ! ( new File( externalDir+"/maxima-"+mvers.versionString() ) ).exists()) {
             	this.finish();
      	} 
        List<String> list = new ArrayList<String>();
        list.add(internalDir+"/maxima");
        list.add("--init-lisp="+internalDir+"/init.lisp");
        maximaProccess = new CommandExec();
        try {
            maximaProccess.execCommand(list);
        } catch (Exception e) {
            Log.d("MoA","exception2");
            exitMOA();
        }
        maximaProccess.clearStringBuilder();
        sem.release();        
        Log.v("MoA","sem released.");
    }
    
    @JavascriptInterface
   	public void sendToMaxima(final String cmdstr) {
    	// Run sendToMaximaInternal() on UI thread
    	Handler handler=new Handler(Looper.getMainLooper());
    	handler.post(new Runnable() {
    		@Override
    		public void run() {
    			sendToMaximaInternal(cmdstr);
    		}
    	});
    }
    private void sendToMaximaInternal(String cmdstr) {
   		try {
   			Log.v("MoA","sendToMaximaInternal");
			sem.acquire();
		} catch (InterruptedException e1) {
			Log.d("MoA","exception3");
			e1.printStackTrace();
			exitMOA();
		}
   		sem.release();
   		Log.v("MoA","sem released");
		if (cmdstr.equals("reload;")) {
			webview.loadUrl(maximaURL);
	        return;
		}
		if (cmdstr.equals("quit();")) exitMOA();
		removeTmpFiles();
		cmdstr=maxima_syntax_check(cmdstr);
		try {
			maximaProccess.maximaCmd(cmdstr+"\n");
		} catch (IOException e1) {
			e1.printStackTrace();
			exitMOA();
		} catch (Exception e1) {
			e1.printStackTrace();
			exitMOA();
		}

		String resString=maximaProccess.getProcessResult();
		maximaProccess.clearStringBuilder();
		displayMaximaCmdResults(resString);

		if (isGraphFile()) {
	        List<String> list = new ArrayList<String>();
	        list.add(internalDir+"/additions/gnuplot/bin/gnuplot");
	        list.add(internalDir+"/maxout.gnuplot");
	        CommandExec gnuplotcom = new CommandExec();
	        try {
	        	gnuplotcom.execCommand(list);
	        } catch (Exception e) {
	            Log.d("MoA","exception6");
	        }
	        if ((new File("/data/data/jp.yhonda/files/maxout.html")).exists()) {
	        	showHTML("file:///data/data/jp.yhonda/files/maxout.html");
	        }
		}
		if (isQepcadFile()) {
	        List<String> list = new ArrayList<String>();
	        list.add("/data/data/jp.yhonda/files/additions/qepcad/qepcad.sh");
	        CommandExec qepcadcom = new CommandExec();
	        try {
	        	qepcadcom.execCommand(list);
	        } catch (Exception e) {
	        	Log.d("MoA","exception7");
	        }
			
		}

   	}
   	
   	private String maxima_syntax_check(String cmd) {
   		/*
   		 * Search the last char which is not white spaces.
   		 * If the last char is semi-colon or dollar, that is OK.
   		 * Otherwise, semi-colon is added at the end.
   		 */
   		int i=cmd.length()-1;
   		assert(i>=0);
   		char c=';';
   		while (i>=0) {
   			c=cmd.charAt(i);
   			if (c==' ' || c=='\t') {
   				i--;
   			} else {
   				break;
   			}
   		}

   		if (c==';' || c=='$') {
   				return(cmd.substring(0, i+1));
   		} else {
   			return(cmd.substring(0, i+1)+';');
   		}
   	}
   	
   	private String escapeChars(String cmd) {
   		return substitute(cmd, "'", "\\'");
   	}
   	
   	private void displayMaximaCmdResults(String resString) {
		String [] resArray=resString.split("\\$\\$");
		for (int i = 0 ; i < resArray.length ; i++) {
			if (i%2 == 0) {
				/* normal text, as we are outside of $$...$$ */
				if (resArray[i].equals("")) continue;
				String htmlStr=substitute(resArray[i],"\n","<br>");
				webview.loadUrl("javascript:UpdateText('"+ htmlStr +"')");
			} else {
				/* tex commands, as we are inside of $$...$$ */
				String texStr=substCRinMBOX(resArray[i]);
				texStr=substitute(texStr,"\n"," \\\\\\\\ ");
				String urlstr="javascript:UpdateMath('"+ texStr +"')";
				webview.loadUrl(urlstr);
			}
		}
   	}
   	
   	private String substCRinMBOX(String str) {
   		String resValue="";
   		String tmpValue=str;
   		int p;
   		while ((p=tmpValue.indexOf("mbox{")) != -1) {
   			resValue=resValue+tmpValue.substring(0,p)+"mbox{";
   			int p2=tmpValue.indexOf("}",p+5);
   			assert(p2>0);
   			String tmp2Value=tmpValue.substring(p+5, p2);
   			resValue=resValue+substitute(tmp2Value,"\n","}\\\\\\\\ \\\\mbox{");
   			tmpValue=tmpValue.substring(p2,tmpValue.length());
   		}
   		resValue=resValue+tmpValue;
   		return (resValue);
   	}
   	
   	static private String substitute(String input, String pattern, String replacement) {
   	    int index = input.indexOf(pattern);

   	    if(index == -1) {
   	        return input;
   	    }

   	    StringBuffer buffer = new StringBuffer();

   	    buffer.append(input.substring(0, index) + replacement);

   	    if(index + pattern.length() < input.length()) {
   	        String rest = input.substring(index + pattern.length(), input.length());
   	        buffer.append(substitute(rest, pattern, replacement));
   	    }
   	    return buffer.toString();
   	}
   	
   	private void showHTML(String url) {
      	Intent intent = new Intent(this,HTMLActivity.class);
      	intent.setAction(Intent.ACTION_VIEW);
      	intent.putExtra("url", url);
      	this.startActivity(intent);
   	}
   	private void showManual() {
   		webview.loadUrl("javascript:manMaxSwitch();");
   		/*
      	Intent intent = new Intent(this,ManualActivity.class);
      	intent.setAction(Intent.ACTION_VIEW);
      	intent.putExtra("url", manURL);
      	intent.putExtra("manLangChanged", manLangChanged);
      	manLangChanged=false;
      	this.startActivity(intent);
      	*/
   	}   	
   	private void showGraph() {
        if ((new File("/data/data/jp.yhonda/files/maxout.html")).exists()) {
        	showHTML("file:///data/data/jp.yhonda/files/maxout.html");
        } else {
			Toast.makeText(this, "No graph to show.", Toast.LENGTH_LONG).show();        	
        }
   	}

   	private void removeTmpFiles() {
   		File a=new File("/data/data/jp.yhonda/files/maxout.gnuplot");
   		if (a.exists()) {
   			a.delete();
   		}
   		a=new File("/data/data/jp.yhonda/files/maxout.html");
   		if (a.exists()) {
   			a.delete();
   		}
   		a=new File("/data/data/jp.yhonda/files/qepcad_input.txt");
   		if (a.exists()) {
   			a.delete();
   		}
   	}
   	
   	private Boolean isGraphFile() {
   		File a=new File("/data/data/jp.yhonda/files/maxout.gnuplot");
   		return(a.exists());   		
   	}
   	private Boolean isQepcadFile() {
   		File a=new File("/data/data/jp.yhonda/files/qepcad_input.txt");
   		return(a.exists());   		
   	}
   	private void exitMOA() {
   		try {
			maximaProccess.maximaCmd("quit();\n");
			finish();
		} catch (IOException e) {
			Log.d("MoA","exception7");
			e.printStackTrace();
		} catch (Exception e) {
			Log.d("MoA","exception8");
			e.printStackTrace();
		}
   		finish();
   	}
   	@Override
   	public boolean dispatchKeyEvent(KeyEvent event) {
   	    if (event.getAction()==KeyEvent.ACTION_DOWN) {
   	        switch (event.getKeyCode()) {
   	        case KeyEvent.KEYCODE_BACK:
   				Toast.makeText(this, "Use Quit in the menu.", Toast.LENGTH_LONG).show();        	
   	            return true;
   	        }
   	    }
   	    return super.dispatchKeyEvent(event);
   	}
}


