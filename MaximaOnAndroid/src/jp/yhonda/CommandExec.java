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

import java.io.InputStream;
import java.util.List;
import java.io.IOException;
import java.io.OutputStream;
 
public class CommandExec {
	// �o�͕ێ��p
    StringBuilder sb = new StringBuilder();
    ProcessBuilder builder=null;
    Process process;
    InputStream is;
    OutputStream os;
    public void execCommand(List<String> commandList) throws IOException, Exception  {
        // �v���Z�X�r���_���\�z (�R�}���h�Z�b�g)
    	builder = new ProcessBuilder(commandList);
        // �v���Z�X�N��
        process = builder.start();
        // �v���Z�X�̃G���[�X�g���[���擾
        is = process.getInputStream();
        while (true) { 
            int c = is.read();
            if (c == -1) {
                is.close();
                break;
            }
        	if (c == 0x04) {
        		break;
        	}
            this.sb.append((char)c);
        }
    }
    public void maximaCmd(String mcmd) throws IOException, Exception  {
        // �v���Z�X�̕W�����̓X�g���[���擾
        os = process.getOutputStream();
        os.write(mcmd.getBytes("UTF-8"));
        while (true) {
            int c = is.read();
            if (c == 0x04) {
            	/* 0x04�͏o�͏I���̈� */
            	if (is.available()==0) {
            		break;
            	}
            } else if (c == -1) {
                is.close();
                break;
            } else if (c == 0x5c) { // 0x5c�̓o�b�N�X���b�V���Ȃ̂ŃG�X�P�[�v����B
            	this.sb.append((char)c);
            	this.sb.append((char)c);
            } else if (c == 0x27) { // 0x27�̓V���O���N�H�[�g�Ȃ̂ŃG�X�P�[�v����B
            	this.sb.append((char)0x5c);
            	this.sb.append((char)c);
            } else {
            	this.sb.append((char)c);
            } 
        }
    }
    // �v���Z�X���s���ʂ��擾����
    public String getProcessResult() {
        return (new String(this.sb));
    }
    // �o�͕ێ����e���폜
    public void clearStringBuilder() {
        this.sb.delete(0, this.sb.length());
    }
}