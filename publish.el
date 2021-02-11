(setq site-dir (concat (getenv "HOME") "/src/site"))
(setq export-site "/ssh:root@ryanmj.xyz:/var/www/underground/")

(require 'ox-publish)

(defun create-preamble (plist)
  "Insert preamble, PLIST is list of options."
  (with-temp-buffer
    (insert-file-contents "views/preamble.html") (buffer-string)))

(defun create-postamble (plist)
  "Insert postamble, PLIST is list of options."
  (with-temp-buffer
    (insert-file-contents "views/postamble.html") (buffer-string)))

;; Replace __PROMPT__ with the actual prompt
(add-hook 'org-export-before-parsing-hook #'(lambda (backend)
                                              (goto-char (point-min))
                                              (replace-string "__PROMPT__" "@@html:<span style='color: var(--bisque4)'>ryan</span><span style='color:blue'>@</span><span style='color:yellow'>themainframe</span><span style='font-weight:bold'></span>@@")))


(defun do-ls-on-list (files)
  "Create ls-like output on a list FILES (string paths).
Assumes that all files in FILES exist."
  (goto-char (point-min))
  (replace-string
   "<!--LS HERE-->"
   (concat
    "<p>"
    (cond ; Get the prefix if the ls -l output.
     ((file-symlink-p (car files)) "lrwxrwxrwx 1")
     ((file-directory-p (car files)) "drwxr-xr-x 2")
      (t "-rw-r--r-- 1"))
    " ryan ryan "
    (replace-regexp-in-string
     "\n$" " "
     (shell-command-to-string (concat "find " (car files) " -name '*.org' -exec cat {} + | wc -c | numfmt --to=si")))
    (shell-command-to-string (concat "ls -dl '--time-style=+%b %m %Y' "
                                     (car files)
                                     " | awk '{printf \"%s %2d %s \", $6, $7, $8} '" ))
    "<a href=\""
    (car files)
    "\">"
    (car files)
    "</a></p>")))


;; Replace <!--LS HERE--> with ls output.
(add-hook 'org-export-before-parsing-hook #'(lambda (backend)
                                              (do-ls-on-list (list "files" "posts"))))


;; Sets up exporting defaults for org mode.
;; "posts" are blog posts.
;; "main" is for index files like index.html, blog.html, etc.
(setq org-publish-project-alist
      '(("main"
         :base-directory ""
         :base-extension "org"
         :publishing-directory "public"
         :recursive nil
         :publishing-function org-html-publish-to-html
         :html-preamble create-preamble
         :html-postamble create-postamble
         :auto-sitemap -1)
        ("posts"
         :base-directory "posts"
         :base-extension "org"
         :publishing-directory "public/posts"
         :recursive t
         :publishing-function org-html-publish-to-html
         :auto-sitemap t)
        ("css"
          :base-directory "css/"
          :base-extension "css"
          :publishing-directory "public/css"
          :publishing-function org-publish-attachment
          :recursive t)
        ("res"
         :base-directory "res/"
         :publishing-directory "public/res"
         :base-extension "png\\|jpg\\|gif\\|pdf\\|mp3\\|ogg\\|swf\\|otf\\|ttf"
         :recursive t
         :publishing-function org-publish-attachment)
        ("files"
         :base-directory "files/"
         :publishing-directory "public/files"
         :base-extension "html\\|txt\\|org"
         :recursive t
         :publishing-function org-publish-attachment
         :htmlized-source nil
         :html-preamble nil
         :html-postamble nil)
        ("all" :components ("posts" "css" "main" "res" "files"))))

(provide 'publish)
;;; publish.el ends here
