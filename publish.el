;;; package --- summary Website generator.

;;; Copyright (C) Ryan Jeffrey 2021

;;; Author: Ryan Jeffrey <ryan@ryanmj.xyz>
;;; Created: 2021-02-12
;;; Keywords: website org
;;; Version: 0.1
;;; Package-Requires: ((emacs "27.1"))
;;; URL: https://gitlab.com/Mallock/site

;;; License:

;; This file is part of Ryan's Homepage.
;;
;; Ryan's Homepage is free software: you can redistribute it and/or modify
;; it under the terms of the GNU General Public License as published by
;; the Free Software Foundation, either version 3 of the License, or
;; (at your option) any later version.
;;
;; Ryan's Homepage is distributed in the hope that it will be useful,
;; but WITHOUT ANY WARRANTY; without even the implied warranty of
;; MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
;; GNU General Public License for more details.
;;
;; You should have received a copy of the GNU General Public License
;; along with Ryan's Homepage.  If not, see <http://www.gnu.org/licenses/>.


;;; Commentary:

;; This script uses org-mode to export my site.

;;; Code:


(require 'ox-publish)
(require 'ox-html)

(setq site-dir (concat (getenv "HOME") "/src/site"))
(setq export-site "/ssh:root@ryanmj.xyz:/var/www/underground/")

(defun create-preamble (plist)
  "Insert preamble, PLIST is list of options."
  (let* ((file-name (file-name-nondirectory (plist-get plist :output-file))))
    (cond
     ;;((string= file-name "index.html")
     (t
      (with-temp-buffer
        (insert-file-contents "views/preamble-i.html") (buffer-string))))))
     ;;(t (insert-file-contents "views/preamble-e.html")))))

(defun create-postamble (plist)
  "Insert postamble, PLIST is list of options."
  (let* ((file-name (file-name-nondirectory (plist-get plist :output-file))))
    (cond
     ;;((string= file-name "index.html")
     (t
      (with-temp-buffer
        (insert-file-contents "views/postamble-i.html") (buffer-string))))))
     ;;(t (insert-file-contents "views/postamble-e.html")))))

;; Replace __PROMPT__ with the actual prompt
(add-hook 'org-export-before-parsing-hook #'(lambda (backend)
                                              (goto-char (point-min))
                                              (while (search-forward "__PROMPT__" (point-max) t)
                                                (kill-backward-chars (length "__PROMPT__"))
                                                (insert "@@html:<span class=\"prompt1\">ryan</span><span class=\"prompt2\">@</span><span class=\"prompt3\">themainframe</span><span class=\"prompt4\"></span>@@"))))


(defun index-sitemap-entry (entry _style project)
  "Create ls-like output on file ENTRY with style _STYLE and from project PROJECT."
  (if (string= entry "index.org")
      ""
    (concat
     "@@html:<p>"
     (cond ; Get the prefix if the ls -l output.
      ((file-symlink-p entry) "lrwxrwxrwx 1")
      ((file-directory-p entry) "drwxr-xr-x 2")
      (t "-rw-r--r-- 1"))
     " ryan ryan "
     (format "%4s "
             (shell-command-to-string
              (concat "find " entry " -name '*.org' -exec cat {} + | wc -c | numfmt --to=si | tr -d '\n'")))
     (shell-command-to-string (concat "ls -dl '--time-style=+%b %m %Y' "
                                      entry
                                      " | awk '{printf \"%s %2d %s \", $6, $7, $8} '" ))
     "@@"
     (format "[[file:%s]]" entry)
     "@@html:</p>@@")))



(defun create-index-blogmap (title list)
  "Create the sitemap for the posts/ directory.
Return sitemap using TITLE and LIST returned by `create-blogmap-entry'."
    (concat "#+TITLE: " title "\n\n"
          (mapconcat (lambda (li)
                       (format "%s" (car li)))
                     (seq-filter #'car (cdr list))
                     "\n")))

;; Replace <!--LS HERE--> with ls output.
(add-hook 'org-export-before-parsing-hook #'(lambda (backend)
                                              "Create fake ls listing."
                                              (goto-char (point-min))
                                              (while (search-forward "<!--LS HERE-->" (point-max) t)
                                                (kill-whole-line)
                                                (insert (concat
                                                         "@@html:<p>total "
                                                         (shell-command-to-string
                                                          "find . -name '*.org' -exec cat {} + | wc -c | numfmt --to=si | tr -d '\n'")
                                                         " Words</p>@@\n")))))

(defun create-blogmap-entry (entry _style project)
  "Create an entry for the blogmap.
One string for each ENTRY in PROJECT."
  (if (string= entry "index.org")
      ""
    (format "@@html:<p>-rw-r--r-- 1 ryan ryan @@ %4s [[file:%s][%s]] @@html:</p>@@"
            (shell-command-to-string (format "wc -c < %s | numfmt --to=si | tr -d '\n'" (org-publish--expand-file-name entry project)))
                                        ;(format-time-string "%h %d, %Y"
                                        ;                    (org-publish-find-date entry project))
            entry
            (org-publish-find-title entry project))))

(defun create-blogmap (title list)
  "Create the sitemap for the posts/ directory.
Return sitemap using TITLE and LIST returned by `create-blogmap-entry'."
    (concat "#+TITLE: " title "\n\n"
          "\n#+begin_archive\n"
          (mapconcat (lambda (li)
                       (format "%s" (car li)))
                     (seq-filter #'car (cdr list))
                     "")
          "\n#+end_archive\n"))

(defun force-main-publish ()
  "Force evaluation of main project."
  (org-publish "main" t)
  (org-publish "posts" t)
  (org-publish-all))

;; Sets up exporting defaults for org mode.
;; "posts" are blog posts.
;; "main" is for index files like index.html, blog.html, etc.
(setq org-publish-project-alist
      '(("main"
         :base-directory ""
         :base-extension "org"
         :publishing-directory "public"
         :publishing-function org-html-publish-to-html
         :html-preamble create-preamble
         :html-postamble create-postamble
         :auto-sitemap t
         :sitemap-filename "sitemap.org"
         :sitemap-title nil
         :sitemap-style list
         :sitemap-sort-files anti-chronologically
         :sitemap-format-entry index-sitemap-entry
         :sitemap-function create-index-blogmap
         :sitemap-sort-folders first
         ;;:html-link-up "/"
         ;;:html-link-home "/"
         :recursive nil)
        ("posts"
         :base-directory "posts"
         :base-extension "org"
         :publishing-directory "public/posts"
         :recursive t
         :publishing-function org-html-publish-to-html

         :html-preamble create-preamble
         :html-postamble create-postamble

         ;; Sitemap.
         
         :auto-sitemap t
         :sitemap-filename "sitemap.org"
         :sitemap-title "Blog Map"
         :sitemap-style list
         :sitemap-sort-files anti-chronologically
         :sitemap-format-entry create-blogmap-entry
         :sitemap-function create-blogmap
         )
        ("misc"
         :base-directory "misc"
         :base-extension "org"
         :publishing-directory "public/misc"
         :recursive t
         :publishing-function org-html-publish-to-html
         :html-preamble create-preamble
         :html-postamble create-postamble
         :auto-sitemap nil)
        ("css"
          :base-directory "css/"
          :base-extension "css"
          :publishing-directory "public/css"
          :publishing-function org-publish-attachment
          :recursive nil)
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
        ("scripts"
         :base-directory "scripts/"
         :publishing-directory "public/scripts"
         :base-extension "js"
         :recursive t
         :publishing-function org-publish-attachment)
         ("all" :components ("posts" "css" "main" "res" "files" "scripts"))))

(provide 'publish)
;;; publish.el ends here
