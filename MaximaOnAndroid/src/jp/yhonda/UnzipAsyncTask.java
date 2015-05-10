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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.util.Log;

public final class UnzipAsyncTask extends AsyncTask<Integer, Integer, Integer> {
	private final static int CHUNK_SIZE = 32 * 1024;
	byte[] _fileIOBuffer = new byte[CHUNK_SIZE];
	InputStream inst;
	String directory;
	private MOAInstallerActivity activity;
	private ProgressDialog dialog;
	private String msg1, msg2;

	public UnzipAsyncTask(MOAInstallerActivity anActivity) {
		this.activity = anActivity;
	}

	public void setParams(InputStream in, String dir, String msg1, String msg2) {
		inst = in;
		directory = dir;
		this.msg1 = msg1;
		this.msg2 = msg2;
	}

	@Override
	protected void onPreExecute() {
		// progres dialog
		dialog = new ProgressDialog(activity);
		dialog.setTitle("Install in progress.");
		dialog.setMessage(msg1);
		dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		dialog.setCancelable(false);
		dialog.setMax(100);
		dialog.setProgress(0);

		dialog.show();
	}

	@Override
	protected void onProgressUpdate(Integer... arg) {
		dialog.setProgress(arg[0]);
	}

	@Override
	protected void onPostExecute(Integer stage) {
		// close the progres dialog
		if (stage == -1) {
			activity.install(10); // indication of error
			return;
		}
		dialog.setMessage(msg2);
		dialog.show();
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
		}
		dialog.dismiss();
		activity.install(stage + 1);
	}

	@Override
	protected Integer doInBackground(Integer... arg) {
		Integer stage = arg[0];
		if (inst == null || directory == null) {
			return (-1);
		}
		ZipInputStream zin = new ZipInputStream(inst);
		ZipEntry ze = null;
		int c = 0;
		BufferedOutputStream fos = null;
		File file = null;
		byte[] buf = new byte[1024 * 1024];
		try {
			while ((ze = zin.getNextEntry()) != null) {
				String name = ze.getName();
				file = new File(directory, name);
				if (ze.isDirectory()) {
					// case of directory
					if (!file.mkdirs()) {
						return (-1);
					}
				} else {
					// case of file
					if (file.exists()) {
						file.delete();
					}
					fos = new BufferedOutputStream(new FileOutputStream(file),
							64 * 1024);
					int numread = 0;
					while ((numread = zin.read(buf)) != -1) {
						fos.write(buf, 0, numread);
						publishProgress(c++);
					}
					fos.close();
				}
			}
		} catch (IOException e) {
			Log.d("MoA", "exception12");
			e.printStackTrace();
			try {
				fos.close();
			} catch (IOException e1) {
				Log.d("MoA", "exception13");
				e1.printStackTrace();
				return (-1);
			}
			file.delete();
			return (-1);
		}
		return stage;
	}

}
