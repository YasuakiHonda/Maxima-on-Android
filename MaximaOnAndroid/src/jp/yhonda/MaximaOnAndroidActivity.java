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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;


public class MaximaOnAndroidActivity extends Activity implements TextView.OnEditorActionListener
{
	String maximaURL="file:///android_asset/index.html";
	String oldmaximaURL="file:///android_asset/index.html";
	String newmaximaURL="file:///android_asset/maxima.html";
	Semaphore sem = new Semaphore(1);
	EditText _editText;
    WebView webview;
    ScrollView scview;
    CommandExec maximaProccess;
    File internalDir;
    File externalDir;
    MaximaVersion mvers=new MaximaVersion(5,29,1);

      @Override
    public boolean onCreateOptionsMenu(Menu menu) {
  	  super.onCreateOptionsMenu(menu);
  	  MenuInflater inflater = getMenuInflater();
  	  inflater.inflate(R.menu.menu, menu);
  	  return true;
    }
      @Override
	  public boolean onOptionsItemSelected(MenuItem item) {
		  switch (item.getItemId()) {
		  case R.id.about:
			   showHTML("file:///android_asset/docs/aboutMOA.html");
			   return true;
		  case R.id.graph:
			  showGraph();
			  return true;
		  case R.id.quit:
			  exitMOA();
			  return true;
		  case R.id.man:
			  showManual("file:///android_asset/maxima-doc/en/de/maxima.html");
			  return true;
		  case R.id.manj:
			  showManual("file:///android_asset/maxima-doc/ja/maxima.html");
			  return true;
		  default:
			   return super.onOptionsItemSelected(item);
		  }
	 }
    
      @Override
    public void onCreate(Bundle savedInstanceState)
    {
        Log.d("My Test", "Clicked!1");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
    	internalDir = this.getFilesDir();
    	externalDir = this.getExternalFilesDir(null);

        webview = (WebView) findViewById(R.id.webView1);
        webview.getSettings().setJavaScriptEnabled(true); 
        webview.setWebViewClient(new WebViewClient() {}); 
        webview.getSettings().setBuiltInZoomControls(true);
        scview = (ScrollView) findViewById(R.id.scrollView1);
        
        if (Build.VERSION.SDK_INT > 16) { // > JELLY_BEAN
        	maximaURL=newmaximaURL;
        } else {
        	maximaURL=oldmaximaURL;
        }
        webview.loadUrl(maximaURL);
        webview.addJavascriptInterface(this, "MOA");
        _editText=(EditText)findViewById(R.id.editText1);
        _editText.setOnEditorActionListener(this);

        MaximaVersion prevVers=new MaximaVersion();
        prevVers.loadVersFromSharedPrefs(this);
        long verNo = prevVers.versionInteger();
        long thisVerNo = mvers.versionInteger();
        
    	if ((thisVerNo > verNo) || 
    		!((new File(internalDir+"/maxima")).exists()) || 
    		!((new File(internalDir+"/additions")).exists()) ||
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
    	try {
			sem.acquire();
		} catch (InterruptedException e1) {
			// TODO 自動生成された catch ブロック
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
        Log.d("My Test", "Clicked!2");
        try {
            maximaProccess.execCommand(list);
        } catch (Exception e) {
            // 例外処理
        }
        maximaProccess.clearStringBuilder();
        sem.release();
        Log.d("My Test", "Clicked!3");
                
    }
    public void scrollToEnd() {
    	Log.v("My Test", "scrollToEnd called");
    	Handler handler = new Handler();
    	Runnable task = new Runnable() {
    		
			@Override
			public void run() {
				Runnable viewtask = new Runnable() {
					@Override
					public void run() {
						scview.fullScroll(ScrollView.FOCUS_DOWN);
						Log.v("My Test","scroll!");
					}
				};
				scview.post(viewtask);
			}
    	};
    	handler.postDelayed(task, 1000);
    }
    
   	public boolean onEditorAction(TextView testview, int id, KeyEvent keyEvent) {
   		try {
			sem.acquire();
		} catch (InterruptedException e1) {
			// TODO 自動生成された catch ブロック
			e1.printStackTrace();
		}
   		sem.release();
   		String cmdstr="";
   		if ((keyEvent == null) || (keyEvent.getAction()==KeyEvent.ACTION_UP)) {
   			try {
   				cmdstr=_editText.getText().toString();
   				if (cmdstr.equals("reload;")) {webview.loadUrl(maximaURL);return true;}
   				if (cmdstr.equals("sc;")) {this.scrollToEnd();return true;}
   				if (cmdstr.equals("quit();")) exitMOA();
   				if (cmdstr.equals("aboutme;")) {
   					showHTML("file:///android_asset/docs/aboutMOA.html");
   					return true;
   				}
   				if (cmdstr.equals("man;")) {
   					showHTML("file://"+internalDir+"/additions/en/maxima.html");
   					return true;
   				}
   				if (cmdstr.equals("manj;")) {
   					showHTML("file://"+internalDir+"/additions/ja/maxima.html");
   					return true;
   				}
   				removeTmpFiles();
   				cmdstr=maxima_syntax_check(cmdstr);
   				maximaProccess.maximaCmd(cmdstr+"\n");
			} catch (IOException e) {
				// TODO 自動生成された catch ブロック
				e.printStackTrace();
			} catch (Exception e) {
				// TODO 自動生成された catch ブロック
				e.printStackTrace();
			}

   			webview.loadUrl("javascript:window.UpdateText('"+ escapeChars(cmdstr) +"<br>" +"')");
   			String[] resArray=maximaProccess.getProcessResult().split("\n");
   	        maximaProccess.clearStringBuilder();

  			for (int i = 0 ; i < resArray.length ; i++) {
   				if (resArray[i].indexOf("$$") != -1) {
   					String reststr=resArray[i];
   					while (reststr.indexOf("$$") != -1) {
   						int start=reststr.indexOf("$$");
   						reststr=reststr.substring(start+2);
   						int end=reststr.indexOf("$$");
   						webview.loadUrl("javascript:window.UpdateMath('"+ reststr.substring(0,end) +"')");
   						reststr=reststr.substring(end+2);
   					}
   					if (reststr.length()>0) {
   	  		   			String brstr="";
   	   		   			if (i<resArray.length-1) brstr="<br>";
   	   		   			webview.loadUrl("javascript:window.UpdateText('"+ reststr + brstr +"')");
   					}
   				} else {
   		   			String brstr="";
   		   			if (i<resArray.length-1) brstr="<br>";
   		   			webview.loadUrl("javascript:window.UpdateText('"+ resArray[i] + brstr +"')");
   				}
   			}
			if (isGraphFile()) {
		        List<String> list = new ArrayList<String>();
		        list.add(internalDir+"/additions/gnuplot/bin/gnuplot");
		        list.add(internalDir+"/maxout.gnuplot");
		        CommandExec gnuplotcom = new CommandExec();
		        try {
		        	gnuplotcom.execCommand(list);
		        } catch (Exception e) {
		            // 例外処理
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
		            // 例外処理
		        }
				
			}

   		}

   		return true;
   	}
   	
   	private String maxima_syntax_check(String cmd) {
   		/*
   		 * Search the last char which is not white spaces.
   		 * If the last char is semi-colon or dollar, that is OK.
   		 * Otherwise, semi-colon is added at the end.
   		 */
   		int i=cmd.length()-1;
   		while (i>=0) {
   			char c=cmd.charAt(i);
   			if (c==' ' || c=='\t') {
   				i--;
   			} else if (c==';' || c=='$') {
   				return(cmd);
   			} else {
   				return(cmd.substring(0, i+1)+';');
   			}
   		}
   		return(";");
   	}
   	
   	private String escapeChars(String cmd) {
   		return substitute(cmd, "'", "\\'");
   	}
   	
   	static private String substitute(String input, String pattern, String replacement) {
   	    // 置換対象文字列が存在する場所を取得
   	    int index = input.indexOf(pattern);

   	    // 置換対象文字列が存在しなければ終了
   	    if(index == -1) {
   	        return input;
   	    }

   	    // 処理を行うための StringBuffer
   	    StringBuffer buffer = new StringBuffer();

   	    buffer.append(input.substring(0, index) + replacement);

   	    if(index + pattern.length() < input.length()) {
   	        // 残りの文字列を再帰的に置換
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
   	private void showManual(String url) {
      	Intent intent = new Intent(this,ManualActivity.class);
      	intent.setAction(Intent.ACTION_VIEW);
      	intent.putExtra("url", url);
      	intent.putExtra("dir", internalDir);
      	this.startActivity(intent);
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
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		} catch (Exception e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
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


