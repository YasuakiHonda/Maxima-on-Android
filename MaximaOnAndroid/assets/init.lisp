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

(defun update-terminal (val)
  (let ((terms '($screen $png $pngcairo $jpg $gif $eps $eps_color $svg
                 $dumb $dumb_file $pdf $pdfcairo $wxt $animated_gif
                 $aquaterm $tiff $vrml $obj $pnm $canvas))) ;; $canvas added
     (cond
       ((member val terms)
          (when (and (eq val '$png) $draw_use_pngcairo)
            (setq val '$pngcairo))
          (setf (gethash '$terminal *gr-options*) val
                *draw-terminal-number* ""))
       ((and ($listp val)
             (= ($length val) 2)
             (member (cadr val) '($screen $wxt $aquaterm))
             (integerp (caddr val))
             (>= (caddr val) 0))
          (setf (gethash '$terminal *gr-options*) (cadr val)
                *draw-terminal-number* (caddr val)))
       (t
          (merror "draw: illegal terminal specification: ~M" val)))))

(defun draw_gnuplot (&rest args)
  (ini-global-options)
  (user-defaults)
  (setf *allocations* nil)
  (let ((counter 0)
        (scenes-list '((mlist simp)))  ; these two variables will be used
        gfn ; gnuplot_file_name
        dfn ; data_file_name
        scene-short-description        ; to build the text output
        scenes
        cmdstorage  ; file maxout.gnuplot
        datastorage ; file data.gnuplot
        datapath    ; path to data.gnuplot
        (ncols 1)
        nrows width height ; multiplot parameters
        isanimatedgif is1stobj biglist grouplist largs)

    (setf largs (listify-arguments))
    (dolist (x largs)
      (cond ((equal ($op x) "=")
              (case ($lhs x)
                ($terminal          (update-gr-option '$terminal ($rhs x)))
                ($columns           (update-gr-option '$columns ($rhs x)))
                ($dimensions        (update-gr-option '$dimensions ($rhs x)))
                ($file_name         (update-gr-option '$file_name ($rhs x)))
                ($gnuplot_file_name (update-gr-option '$gnuplot_file_name ($rhs x)))
                ($data_file_name    (update-gr-option '$data_file_name ($rhs x)))
                ($delay             (update-gr-option '$delay ($rhs x)))

                ; deprecated global options
                ($file_bgcolor      (update-gr-option '$file_bgcolor ($rhs x)))
                ($pic_width         (update-gr-option '$pic_width ($rhs x)))
                ($pic_height        (update-gr-option '$pic_height ($rhs x)))
                ($eps_width         (update-gr-option '$eps_width ($rhs x)))
                ($eps_height        (update-gr-option '$eps_height ($rhs x)))
                ($pdf_width         (update-gr-option '$pdf_width ($rhs x)))
                ($pdf_height        (update-gr-option '$pdf_height ($rhs x)))

                (otherwise (merror "draw: unknown global option ~M " ($lhs x)))))
            ((equal (caar x) '$gr3d)
              (setf scenes (append scenes (list (funcall #'make-scene-3d (rest x))))))
            ((equal (caar x) '$gr2d)
              (setf scenes (append scenes (list (funcall #'make-scene-2d (rest x))))))
            (t
              (merror "draw: item ~M is not recognized" x)))   )
    (setf isanimatedgif
          (equal (get-option '$terminal) '$animated_gif))
    (setf
       gfn (plot-temp-file (get-option '$gnuplot_file_name))
       dfn (plot-temp-file (get-option '$data_file_name)))

    ; we now create two files: maxout.gnuplot and data.gnuplot
    (setf cmdstorage
          (open gfn
                :direction :output :if-exists :supersede))
    (setf datastorage
          (open dfn
                :direction :output :if-exists :supersede))
    (setf datapath (format nil "'~a'" dfn))
    ; when one multiplot window is active, change of terminal is not allowed
    (if (not *multiplot-is-active*)
      (case (get-option '$terminal)
	($canvas (format cmdstorage "set terminal canvas enhanced ~a size ~a, ~a~%set out '~a.html'"
                           (write-font-type)
                           (round (first (get-option '$dimensions)))
                           (round (second (get-option '$dimensions)))
                           (get-option '$file_name) ) )
        ($dumb (format cmdstorage "set terminal dumb size ~a, ~a"
                           (round (/ (first (get-option '$dimensions)) 10))
                           (round (/ (second (get-option '$dimensions)) 10))))
        ($dumb_file (format cmdstorage "set terminal dumb size ~a, ~a~%set out '~a.dumb'"
                           (round (/ (first (get-option '$dimensions)) 10))
                           (round (/ (second (get-option '$dimensions)) 10))
                           (get-option '$file_name)))
        ($png (format cmdstorage "set terminal png enhanced truecolor ~a size ~a, ~a~%set out '~a.png'"
                           (write-font-type)
                           (round (first (get-option '$dimensions)))
                           (round (second (get-option '$dimensions)))
                           (get-option '$file_name) ) )
        ($pngcairo (format cmdstorage "set terminal pngcairo enhanced truecolor ~a size ~a, ~a~%set out '~a.png'"
                           (write-font-type)
                           (round (first (get-option '$dimensions)))
                           (round (second (get-option '$dimensions)))
                           (get-option '$file_name) ) )
        ($eps (format cmdstorage "set terminal postscript eps enhanced ~a size ~acm, ~acm~%set out '~a.eps'"
                           (write-font-type)
                           (/ (first (get-option '$dimensions)) 100.0)
                           (/ (second (get-option '$dimensions)) 100.0)
                           (get-option '$file_name)))
        ($eps_color (format cmdstorage "set terminal postscript eps enhanced ~a color size ~acm, ~acm~%set out '~a.eps'"
                           (write-font-type)
                           (/ (first (get-option '$dimensions)) 100.0)
                           (/ (second (get-option '$dimensions)) 100.0)
                           (get-option '$file_name)))
        ($pdf (format cmdstorage "set terminal pdf enhanced ~a color size ~acm, ~acm~%set out '~a.pdf'"
                           (write-font-type)
                           (/ (first (get-option '$dimensions)) 100.0)
                           (/ (second (get-option '$dimensions)) 100.0)
                           (get-option '$file_name)))
        ($pdfcairo (format cmdstorage "set terminal pdfcairo enhanced ~a color size ~acm, ~acm~%set out '~a.pdf'"
                           (write-font-type)
                           (/ (first (get-option '$dimensions)) 100.0)
                           (/ (second (get-option '$dimensions)) 100.0)
                           (get-option '$file_name)))
        ($jpg (format cmdstorage "set terminal jpeg enhanced ~a size ~a, ~a~%set out '~a.jpg'"
                           (write-font-type)
                           (round (first (get-option '$dimensions)))
                           (round (second (get-option '$dimensions)))
                           (get-option '$file_name)))
        ($gif (format cmdstorage "set terminal gif enhanced ~a size ~a, ~a~%set out '~a.gif'"
                           (write-font-type)
                           (round (first (get-option '$dimensions)))
                           (round (second (get-option '$dimensions)))
                           (get-option '$file_name)))
        ($svg (format cmdstorage "set terminal svg enhanced ~a size ~a, ~a~%set out '~a.svg'"
                           (write-font-type)
                           (round (first (get-option '$dimensions)))
                           (round (second (get-option '$dimensions)))
                           (get-option '$file_name)))
        ($animated_gif (format cmdstorage "set terminal gif enhanced animate ~a size ~a, ~a delay ~a~%set out '~a.gif'"
                           (write-font-type)
                           (round (first (get-option '$dimensions)))
                           (round (second (get-option '$dimensions)))
                           (get-option '$delay)
                           (get-option '$file_name)))
        ($aquaterm (format cmdstorage "set terminal aqua enhanced ~a ~a size ~a ~a~%"
                           *draw-terminal-number*
                           (write-font-type)
                           (round (first (get-option '$dimensions)))
                           (round (second (get-option '$dimensions)))))
        ($wxt (format cmdstorage "set terminal wxt enhanced ~a ~a size ~a, ~a~%"
                           *draw-terminal-number*
                           (write-font-type)
                           (round (first (get-option '$dimensions)))
                           (round (second (get-option '$dimensions)))))
        (otherwise ; default screen output
          (cond
            (*windows-OS*  ; running on windows operating system
              (format cmdstorage "set terminal windows enhanced ~a size ~a, ~a~%"
                          (write-font-type)
                          (round (first (get-option '$dimensions)))
                          (round (second (get-option '$dimensions)))))
            (t  ; other platforms
              (format cmdstorage "set terminal x11 enhanced ~a ~a size ~a, ~a~%"
                           *draw-terminal-number*
                           (write-font-type)
                           (round (first (get-option '$dimensions)))
                           (round (second (get-option '$dimensions))))))) ))

    ; compute some parameters for multiplot
    (when (not isanimatedgif)
      (setf ncols (get-option '$columns))
      (setf nrows (ceiling (/ (length scenes) ncols)))
      (if (> (length scenes) 1)
        (format cmdstorage "~%set size 1.0, 1.0~%set origin 0.0, 0.0~%set multiplot~%")) )

    ; write descriptions of 2d and 3d scenes
    (let ((i -1)
          (alloc (reverse *allocations*))
          (nilcounter 0)
          thisalloc origin1 origin2 size1 size2)

      ; recalculate nrows for automatic scene allocations
      (setf nrows (ceiling (/ (count nil alloc) ncols)))

      (when (> nrows 0)
        (setf width (/ 1.0 ncols)
              height (/ 1.0 nrows)))
      (dolist (scn scenes)
        ; write size and origin if necessary
        (cond (isanimatedgif
                (format cmdstorage "~%set size 1.0, 1.0~%") )
              (t ; it's not an animated gif
                (setf thisalloc (car alloc))
                (setf alloc (cdr alloc))
                (cond
                  (thisalloc ; user defined scene allocation
                     (setf origin1 (first thisalloc)
                           origin2 (second thisalloc)
                           size1   (third thisalloc)
                           size2   (fourth thisalloc)))
                  (t ; automatic scene allocation
                     (setf origin1 (* width (mod nilcounter ncols))
                           origin2 (* height (- nrows 1.0 (floor (/ nilcounter ncols))))
                           size1   width
                           size2   height)
                     (incf nilcounter)))
                (format cmdstorage "~%set size ~a, ~a~%" size1 size2)
                (format cmdstorage "set origin ~a, ~a~%" origin1 origin2)
                (format cmdstorage "set obj 1 rectangle behind from screen ~a,~a to screen ~a,~a~%" 
                                   origin1 origin2 (+ origin1 size1 ) (+ origin2 size2))  ))
        (setf is1stobj t
              biglist '()
              grouplist '())
        (format cmdstorage "~a" (second scn))
        (cond ((= (first scn) 2)    ; it's a 2d scene
                 (setf scene-short-description '(($gr2d simp)))
                 (format cmdstorage "plot "))
              ((= (first scn) 3)    ; it's a 3d scene
                 (setf scene-short-description '(($gr3d simp)))
                 (format cmdstorage "splot ")))
        (dolist (obj (third scn))
           (setf scene-short-description
                 (cons (gr-object-name obj) scene-short-description))
           (if is1stobj
             (setf is1stobj nil)
             (format cmdstorage ", \\~%")  )
           (let ((pcom (gr-object-command obj)))
             (cond
               ((listp pcom)
                  (while (consp pcom)
                    (format cmdstorage "~a~a~a~a"
                                       datapath
                                       (format nil " index ~a" (incf i))
                                       (pop pcom)
                                       (if (null pcom)
                                           ""
                                           "," )) ) )
               (t (format cmdstorage "~a~a~a"
                                     datapath
                                     (format nil " index ~a" (incf i))
                                     pcom) )))
           (setf grouplist (append grouplist (gr-object-groups obj)))
           (setf biglist (append biglist (gr-object-points obj))) )

        ; let's write data in data.gnuplot
        (do ( (blis biglist (cdr blis))
              (glis grouplist (cdr glis) ))
            ((null blis) 'done)
          (let* ((vect (car blis))
                 (k (length vect))
                 (ncol (caar glis))
                 (l 0)
                 (m (cadar glis))
                 (non-numeric-region nil)
                 coordinates)
             (cond
                ((= m 0)     ; no blank lines
                   (do ((cont 0 (+ cont ncol)))
                       ((= cont k) 'done)
                     (setf coordinates (subseq vect cont (+ cont ncol)))
                     ; control of non numeric y values,
                     ; code related to draw_realpart
                     (cond
                       (non-numeric-region
                         (when (numberp (aref coordinates 1))
                           (setf non-numeric-region nil)
                           (write-subarray coordinates datastorage) ))
                       (t
                         (cond
                           ((numberp (aref coordinates 1))
                             (write-subarray coordinates datastorage))
                           (t
                             (setf non-numeric-region t)
                             (format datastorage "~%")))))) )

                (t           ; blank lines every m lines
                   (do ((cont 0 (+ cont ncol)))
                       ((= cont k) 'done)
                     (when (eql l m)
                           (format datastorage "~%")
                           (setf l 0) )
                     (write-subarray (subseq vect cont (+ cont ncol)) datastorage)
                     (incf l)))))
          (format datastorage "~%~%") )
        (incf counter)
        (setf scenes-list (cons (reverse scene-short-description) scenes-list)) ))  ; end let-dolist scenes
    (close datastorage)

    (cond (isanimatedgif  ; this is an animated gif
             (format cmdstorage "~%quit~%~%")
             (close cmdstorage)
             ($system (format nil "~a \"~a\"" 
                                  $gnuplot_command
                                  gfn) ))
          (t ; non animated gif
             ; command file maxout.gnuplot is now ready
             (format cmdstorage "~%")
             (cond ((> (length scenes) 1)
                      (format cmdstorage "unset multiplot~%"))
                   ; if we want to save the coordinates in a file,
                   ; print them when hitting the x key after clicking the mouse button
                   ((not (string= (get-option '$xy_file) ""))
                      (format cmdstorage
                              "set print \"~a\" append~%bind x \"print MOUSE_X,MOUSE_Y\"~%"
                              (get-option '$xy_file))) )

             ; in svg and pdfcairo terminals, unset output to force
             ; Gnuplot to write </svg> at the end of the file (what about pdf?)
             (when (or (equal (get-option '$terminal) '$svg)
                       (equal (get-option '$terminal) '$pdfcairo))
                (format cmdstorage "unset output~%"))
             (close cmdstorage)
             ; get the plot
             (cond
                ; connect to gnuplot via pipes
                ((and (not *windows-OS*)
                      (member (get-option '$terminal) '($screen $aquaterm $wxt))
                      (equal $draw_renderer '$gnuplot_pipes))
                   (check-gnuplot-process)
                   (when (not *multiplot-is-active*) ; not in a one window multiplot
                     (send-gnuplot-command "unset output"))
                   (send-gnuplot-command "reset")
                   (send-gnuplot-command
                        (format nil "load '~a'" gfn)))
                ; call gnuplot via system command
                (t
                  ($system (if (member (get-option '$terminal) '($screen $aquaterm $wxt))
                                   (format nil "~a ~a"
                                               $gnuplot_command
                                               (format nil $gnuplot_view_args gfn))
                                   (format nil "~a \"~a\"" 
                                               $gnuplot_command
                                               gfn)))))))

    ; the output is a simplified description of the scene(s)
    (reverse scenes-list)) )

;;; /data/local/init.mac
(setq $file_search_maxima                                                  
        ($append '((mlist) "/data/local/###.{mac,mc}")                  
                 $file_search_maxima))                               
(if (probe-file "/data/local/init.mac") ($load "init.mac"))
