/*
    Copyright 2015 Yasuaki Honda (yasuaki.honda@gmail.com)
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
    along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
 */

package jp.yhonda;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.os.Build;
import android.util.Log;

import jp.yhonda.CommandExec;

public final class CpuArchitecture {
	static final String X86="x86"; 
	static final String ARM="arm"; 
	static final String NOT_SUPPORTED="not supported"; 
	static final String NOT_INITIALIZED="not initialized"; 

	private static String cpuarch=NOT_INITIALIZED;
	
	private CpuArchitecture () {
	}
	
	public static final String getCpuArchitecture() {
		return cpuarch;
	}
	
	public static final void initCpuArchitecture() {
		if (! NOT_INITIALIZED.equals(cpuarch)) {
			return;
		}
		if ((new File("/data/data/jp.yhonda/files/additions/cpuarch.sh")).exists()) {
			CommandExec cmd = new CommandExec();
			List<String> list = new ArrayList<String>();
			list.add("/data/data/jp.yhonda/files/additions/cpuarch.sh");
			try {
				cmd.execCommand(list);
			} catch (Exception e) {
				Log.d("MoA", "CpuArchitecture exception1");
			}
			String res=cmd.getProcessResult().trim();
			if (res.equals(X86)) {
				cpuarch=X86;
			} else if (res.equals(ARM)) {
				cpuarch=ARM;
			} else if (res.equals(NOT_SUPPORTED)) {
				cpuarch=NOT_SUPPORTED;
			}
		}
	}
	
	public static final String getMaximaFile() {
		if (cpuarch.startsWith("not")) {
			return cpuarch;
		}
		if (cpuarch.equals(X86)) {
			if (Build.VERSION.SDK_INT >= 21) { // Lollipop requires pie
				return("maxima.x86.pie");
			} else {
				return("maxima.x86");
			}
		} else if (cpuarch.equals(ARM)) {
			if (Build.VERSION.SDK_INT >= 21) { // Lollipop requires pie
				return("maxima.pie");
			} else {
				return("maxima");
			}			
		}
		return cpuarch;
	}
}
