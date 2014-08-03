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

;;; Dropbox support
(let ((top (pop $file_search_maxima))) 
    (push "/sdcard/Download/$$$.txt" $file_search_maxima) 
    (push top $file_search_maxima))
(let ((top (pop $file_type_maxima))) 
    (push "txt" $file_type_maxima) 
    (push top $file_type_maxima))

;;; qepcad support
(defun $system (&rest args)
  (let ((bashline "bash -c 'export qe="))
    (if (>= (string> (first args) bashline) 
            (length bashline))
      ;; perform qepcad
      (progn
        (format t "start qepcad~A" *prompt-suffix*)
        (read-line)))))

(let ((top (pop $file_search_lisp))) 
    (push "/data/data/jp.yhonda/files/additions/qepcad/qepmax/$$$.{lsp,lisp,fasl}" $file_search_lisp) 
    (push top $file_search_lisp))
(let ((top (pop $file_search_maxima))) 
    (push "/data/data/jp.yhonda/files/additions/qepcad/qepmax/$$$.{mac,mc}" $file_search_maxima) 
    (push top $file_search_maxima))


(progn                                                                      
  (if (not (boundp '$qepcad_installed_dir))                                 
      (add2lnc '$qepcad_installed_dir $values))                             
  (defparameter $qepcad_installed_dir                                              
                "/data/data/jp.yhonda/files/additions/qepcad")
  (if (not (boundp '$qepcad_input_file))                                 
      (add2lnc '$qepcad_input_file $values))                             
  (defparameter $qepcad_input_file                                              
                "/data/data/jp.yhonda/files/qepcad_input.txt")           
  (if (not (boundp '$qepcad_output_file))                                       
      (add2lnc '$qepcad_output_file $values))                                   
  (defparameter $qepcad_output_file                                             
                "/data/data/jp.yhonda/files/qepcad_output.txt")
  (if (not (boundp '$qepcad_file_pattern))                                       
      (add2lnc '$qepcad_file_pattern $values))                                   
  (defparameter $qepcad_file_pattern "/data/data/jp.yhonda/files/qepcad*.txt")
  (if (not (boundp '$qepcad_option))                                       
      (add2lnc '$qepcad_option $values))                                   
  (defparameter $qepcad_option " +N20000000 +L100000 "))

;;; always save support
(defvar *save_file* "/data/data/jp.yhonda/files/saveddata")
(defun $ssave () (meval `(($save) ,*save_file* $labels ((mequal) $linenum $linenum))) t)
(defun $srestore () (load *save_file*) t)

(setq *maxima-tempdir* "/data/data/jp.yhonda/files")
(setq $in_netmath nil)

($set_plot_option '((mlist) $plot_format $gnuplot))
($set_plot_option '((mlist) $gnuplot_term $canvas))
($set_plot_option '((mlist) $gnuplot_out_file "/data/data/jp.yhonda/files/maxout.html"))
(setq $draw_graph_terminal '$canvas)
  
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


;;; some functions in matrun.lisp does not work. They are redefined here.
;;; It is just like fib above.

(defmspec $apply1 (l) (setq l (cdr l))
	  (let ((expr (meval (car l))))
	    (mapc #'(lambda (z) (setq expr (apply1 expr z 0))) (cdr l))
	    expr))

(defmfun apply1 (expr *rule depth) 
  (cond
    ((> depth $maxapplydepth) expr)
    (t
     (prog nil 
	(*rulechk *rule)
	(setq expr (rule-apply *rule expr))
	b    (cond
	       ((or (atom expr) (mnump expr)) (return expr))
	       ((eq (caar expr) 'mrat)
		(setq expr (ratdisrep expr)) (go b))
	       (t
		(return
		  (simplifya
		   (cons
		    (delsimp (car expr))
		    (mapcar #'(lambda (z) (apply1 z *rule (1+ depth)))
			    (cdr expr)))
		   t))))))))

(defmspec $applyb1 (l)  (setq l (cdr l))
	  (let ((expr (meval (car l))))
	    (mapc #'(lambda (z) (setq expr (car (apply1hack expr z)))) (cdr l))
	    expr))

(defmfun apply1hack (expr *rule) 
  (prog (pairs max) 
     (*rulechk *rule)
     (setq max 0)
     b    (cond
	    ((atom expr) (return (cons (multiple-value-bind (ans rule-hit) (mcall *rule expr) (if rule-hit ans expr)) 0)))
	    ((specrepp expr) (setq expr (specdisrep expr)) (go b)))
     (setq pairs (mapcar #'(lambda (z) (apply1hack z *rule))
			 (cdr expr)))
     (setq max 0)
     (mapc #'(lambda (l) (setq max (max max (cdr l)))) pairs)
     (setq expr (simplifya (cons (delsimp (car expr))
				 (mapcar #'car pairs))
			   t))
     (cond ((= max $maxapplyheight) (return (cons expr max))))
     (setq expr (rule-apply *rule expr))
     (return (cons expr (1+ max)))))

(defun rule-apply (*rule expr)
  (prog (ans rule-hit)
   loop (multiple-value-setq (ans rule-hit) (mcall *rule expr))
   (cond ((and rule-hit (not (alike1 ans expr)))
	  (setq expr ans) (go loop)))
   (return expr)))

(defmspec $apply2 (l) (setq l (cdr l))
	  (let ((rulelist (cdr l))) (apply2 rulelist (meval (car l)) 0)))

(defmfun apply2 (rulelist expr depth) 
  (cond
    ((> depth $maxapplydepth) expr)
    (t
     (prog (ans ruleptr rule-hit) 
      a    (setq ruleptr rulelist)
      b    (cond
	     ((null ruleptr)
	      (cond
		((atom expr) (return expr))
		((eq (caar expr) 'mrat)
		 (setq expr (ratdisrep expr)) (go b))
		(t
		 (return
		   (simplifya
		    (cons
		     (delsimp (car expr))
		     (mapcar #'(lambda (z) (apply2 rulelist z (1+ depth)))
			     (cdr expr)))
		    t))))))
      (cond ((progn (multiple-value-setq (ans rule-hit) (mcall (car ruleptr) expr)) rule-hit)
	     (setq expr ans)
	     (go a))
	    (t (setq ruleptr (cdr ruleptr)) (go b)))))))

(defmspec $applyb2 (l) (setq l (cdr l))
	  (let ((rulelist (cdr l))) (car (apply2hack rulelist (meval (car l))))))

(defmfun apply2hack (rulelist e) 
  (prog (pairs max) 
     (setq max 0)
     (cond ((atom e) (return (cons (apply2 rulelist e -1) 0)))
	   ((specrepp e) (return (apply2hack rulelist (specdisrep e)))))
     (setq pairs (mapcar #'(lambda (x) (apply2hack rulelist x)) (cdr e)))
     (setq max 0)
     (mapc #'(lambda (l) (setq max (max max (cdr l)))) pairs)
     (setq e (simplifya (cons (delsimp (car e)) (mapcar #'car pairs)) t))
     (cond ((= max $maxapplyheight) (return (cons e max)))
	   (t (return (cons (apply2 rulelist e -1) (1+ max)))))))
