;;; (setq *maxima-dir* "path/maxima-5.X.Y") will be added before here.
(if (not (probe-file *maxima-dir*)) (quit))
#|
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
|#

(defun maxima-getenv (a) (if (string-equal a "MAXIMA_PREFIX") *maxima-dir*))
(setq *maxima-default-layout-autotools* "false")
(setq *autoconf-prefix* *maxima-dir*)
(setq *maxima-source-root* *maxima-dir*)
(setq *maxima-prefix* *maxima-dir*)
(set-pathnames)                     
(setq *prompt-suffix* (format nil "~A" (code-char 4)))

(defun tex-char (x) 
  (cond ((equal x #\ ) "\\space ")
        ((equal x #\_) "\\_ ")
        (t x)))

(defprop mlessp ("\\lt ") texsym)
(defprop mgreaterp ("\\gt ") texsym)

(defun tex-string (x)
  (cond ((equal x "") "")
	((eql (elt x 0) #\\) x)
	(t (concatenate 'string "\\text{" x "}"))))

;;; Don't know why, but fib(n) returns 0 regardless n value.
;;; The followings fix this.
(defmfun $fib (n)
  (cond ((fixnump n) (ffib n))
    (t (setq $prevfib `(($fib) ,(add2* n -1)))
       `(($fib) ,n))))

(defun ffib (%n)
  (declare (fixnum %n))
  (cond ((= %n -1)
     (setq $prevfib -1)
     1)
    ((zerop %n)
     (setq $prevfib 1)
     0)
    (t
     (let* ((f2 (ffib (ash (logandc2 %n 1) -1))) ; f2 = fib(n/2) or fib((n-1)/2)
        (x (+ f2 $prevfib))
        (y (* $prevfib $prevfib))
        (z (* f2 f2)))
       (setq f2 (- (* x x) y)
         $prevfib (+ y z))
       (when (oddp %n)
         (psetq $prevfib f2
            f2 (+ f2 $prevfib)))
       f2))))

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
                "/data/data/jp.yhonda/files/qepcad_output.txt")
  (if (not (boundp '$qepcad_on_moa))                                       
      (add2lnc '$qepcad_on_moa $values))                                   
  (defparameter $qepcad_on_moa t))

;;; always save support
(defvar *save_file* "/data/data/jp.yhonda/files/saveddata")
(defun $ssave () (meval `(($save) ,*save_file* $labels ((mequal) $linenum $linenum))) t)
(defun $srestore () (load *save_file*) t)

(defun $system (&rest args) (declare (ignore args)))
(setq *maxima-tempdir* "/data/data/jp.yhonda/files")
(setq $in_netmath nil)

($set_plot_option '((mlist) $plot_format $gnuplot))
($set_plot_option '((mlist) $gnuplot_term $canvas))
($set_plot_option '((mlist) $gnuplot_out_file "/data/data/jp.yhonda/files/maxout.html"))
  
(setq $display2d '$imaxima)

($load '$draw)

($set_draw_defaults                                                             
   '((mequal simp) $terminal $canvas)                                           
   '((mequal simp) $file_name "/data/data/jp.yhonda/files/maxout"))             

;;; /data/local/tmp/maxima-init.mac
(setq $file_search_maxima                                                  
        ($append '((mlist) "/data/local/tmp/###.{mac,mc}")                  
                 $file_search_maxima))                               
(if (probe-file "/data/local/tmp/maxima-init.mac") ($load "/data/local/tmp/maxima-init.mac"))

;;; lisp-utils/defsystem.lisp must be loaded.
($load "lisp-utils/defsystem")
