;;; package --- summary Blog post generator.

;;; Copyright (C) Ryan Jeffrey 2022

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

;; This script uses org-mode to export posts for my site.

;;; Code:

(require 'ox-publish)
(require 'ox-html)

;; Define the publishing project
(setq org-publish-project-alist
      (list
       (list "org-site:blog"
             :base-directory "./"
             :publishing-directory "./"
             :publishing-function 'org-html-publish-to-html
             :with-creator t            ;; Include Emacs and Org versions in footer
             :with-toc 2                ;; Include a table of contents with a depth of 2
             :section-numbers t         ;; Don't include section numbers
             :exclude ".*"              ;; Exclude all files
             :with-latex t                     ;; Enable latex for maths
             :body-only t               ;; Only publish the body of the HTML file
             :include (list (nth 0 argv))  ;; Except the ones we want to compile
             )))

;; Generate the site output
(org-publish-all t)
