;;; (setq *maxima-dir* "path/maxima-5.X.Y") will be added before here.
  
#|
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
|#

(defun maxima-getenv (a) (if (string-equal a "MAXIMA_PREFIX") *maxima-dir*))
(setq *maxima-default-layout-autotools* "false")
(setq *autoconf-prefix* *maxima-dir*)
(setq *maxima-source-root* *maxima-dir*)
(setq *maxima-prefix* *maxima-dir*)
(set-pathnames)                     
(setq *prompt-suffix* (code-char 4))

(defun tex-char (x) (if (equal x #\ ) "\\:" x))

(defun tex-string (x)
  (cond ((equal x "") "")
	((eql (elt x 0) #\\) x)
	(t (concatenate 'string "\\mbox{" x "}"))))

;;; qepcad support
(let ((top (pop $file_search_lisp))) 
    (push "/data/data/jp.yhonda/files/additions/qepcad/$$$.{lsp,lisp,fasl}" $file_search_lisp) 
    (push top $file_search_lisp))
(let ((top (pop $file_search_maxima))) 
    (push "/data/data/jp.yhonda/files/additions/qepcad/$$$.{mac,mc}" $file_search_maxima) 
    (push top $file_search_maxima))

(progn                                                                      
  (if (not (boundp '$qepcad_input_file))                                 
      (add2lnc '$qepcad_input_file $values))                             
  (defparameter $qepcad_input_file                                              
                "/data/data/jp.yhonda/files/qepcad_input.txt")           
  (if (not (boundp '$qepcad_output_file))                                       
      (add2lnc '$qepcad_output_file $values))                                   
  (defparameter $qepcad_output_file                                             
                "/data/data/jp.yhonda/files/qepcad_output.txt"))                

;;; always save support
(defvar *save_file* "/data/data/jp.yhonda/files/saveddata")
(defun $ssave () (meval `(($save) ,*save_file* $labels ((mequal) $linenum $linenum))) t)
(defun $srestore () (load *save_file*) t)

(defun $system (&rest args) (declare (ignore args)))
(setq *maxima-tempdir* "/data/data/jp.yhonda/files")
(setq $in_netmath nil)
(setq $plot_options ($append '((mlist)
  ((mlist) $plot_format $gnuplot)
  ((mlist) $gnuplot_term $canvas)
  ((mlist) $gnuplot_out_file "/data/data/jp.yhonda/files/maxout.html"))
  $plot_options))

($load '$draw)

($set_draw_defaults                                                             
   '((mequal simp) $terminal $canvas)                                           
   '((mequal simp) $file_name "/data/data/jp.yhonda/files/maxout"))             

;;; /data/local/init.mac
(setq $file_search_maxima                                                  
        ($append '((mlist) "/data/local/###.{mac,mc}")                  
                 $file_search_maxima))                               
(if (probe-file "/data/local/init.mac") ($load "init.mac"))
