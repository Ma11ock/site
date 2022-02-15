(ns rmjxyz.app
  (:require   [cljs.nodejs :as nodejs]
              ["express" :as express]
              ["express-handlebars" :refer [engine]]
              [goog.string :as gstring]
              [goog.string.format]
              [cljs.core.async :refer-macros [go]]
              [cljs.pprint :refer [pprint]]
              [cljs.core.async.interop :refer-macros [<p!]]))

(println "Initializing server...")
(defonce app (atom nil))
(defonce index-windows (atom nil))
(defonce fs (js/require "fs"))
(defonce path (js/require "path"))
(defonce process (js/require "process"))
(defonce permStrings ["---" "--x" "-w-" "-wx" "r--" "r-x" "rw-" "rwx"])
(defonce mons [ "Jan" "Feb" "Mar" "Apr" "May" "Jun" "Jul"
                "Aug" "Sep" "Oct" "Nov" "Dec" ])
(defonce post-items (atom nil))
(defonce post-windows (atom nil))
(defonce index-items (atom nil))
(defonce all-bkg-scripts (atom nil))
(defonce item-update-time (* 1000 60 5))
(defonce comments-list '("industrial_society" "sneed" "france"
                         "anime" "choppa" "gigachad" "peter"
                         "shrek" "tanku" "toem" "troll" "virusexe"
                         "windows"))

(defn mon-by-index
  "Get a month (abbreviation) by its index. Returns nil if i is out of range."
  [i]
  (nth mons i nil))

(defn permissions-to-string
  "Convert Unix file permission mode to string. Return nil on invalid mode."
  [mode]
  (nth permStrings mode nil))

(defn get-file-name
  "Get just the name of a file (no directory, no extension)."
  [file-path]
  (.-name (. path parse file-path)))

(defn ls-time
  "Convert time stamp in milliseconds to LS time format."
  [timeMS]
  (let [file-date (js/Date. timeMS)]
    (str (mon-by-index (.getMonth file-date)) " " (gstring/format "%02d" (.getDate file-date)) " "
         ;; If the file was updated this year then set the last column to the
         ;; hour and minute. Else, the last column should be the year.
         (if (= (.getFullYear file-date) (.getFullYear (js/Date.)))
           (str (gstring/format "%02d" (.getHours file-date)) ":" (gstring/format "%02d" (.getMinutes file-date)))
           (str (.getFullYear file-date))))))

(defn create-lstat
  "Create an LSStat object for use in rendering."
  [file-path]
  (let [stats (.statSync fs file-path)
        unixFilePerms (when stats (.toString (bit-and (.-mode stats) (js/parseInt "777" 8)) 8))]
    (if stats
      {:perms (str (if (.isDirectory stats) "d" "-")
                   (permissions-to-string (js/parseInt (first unixFilePerms)))
                   (permissions-to-string (js/parseInt (second unixFilePerms)))
                   (permissions-to-string (js/parseInt (nth unixFilePerms 2))))
       :numLinks (.-nlink stats)
       :fileSize (gstring/format "%4d" (.-size stats))
       :mtime (ls-time (.-mtimeMs stats))
       :basename (get-file-name (.basename path file-path))
       :realpath file-path
       }
      ;; TODO actually deal with error.
      (js/console.error "Could not stat" file-path))))

(defn ls-list
  "Create a list of ls-stats from a list of file paths. Looks into public."
  ([paths]
   (for [file paths]
     (create-lstat file)))
  ([basedir ext paths]
   (for [file paths]
     (create-lstat (.join path basedir (str file ext))))))

(defn ls-dir
  "Create a list of ls-stats from a directory."
  [dir-path ext]
  (when (and (.existsSync fs dir-path) (.isDirectory (.lstatSync fs dir-path)))
    (vec (for [file (.readdirSync fs dir-path)
               :when (= (.extname path file) ext)]
           (create-lstat (.join path dir-path file))))))

(defn create-command
  "Create a command object for rendering in the website."
  ;; LS list.
  ([dir ext paths display-path] {:args (if display-path display-path dir)
                                 :lsList (ls-list dir ext paths)})
  ;; Cat.
  ([the-path trim-path] {:args (if trim-path (get-file-name the-path) the-path)
                         :markup the-path}))

(defn create-ls
  "Create a ls-listing from a pre-existing set of files."
  [dir ls-list site-path]
  {:args dir :lsList 
   (for [new-stat ls-list]
     (assoc new-stat :basename (.join path site-path (get new-stat :basename))))})

(defn create-windows
  "Create the window data for the site."
  [commands-list]
  {:windows (for [cmds commands-list]
                {:commands cmds})})

(defn serve-404
  "Serve the 404 page from path to res."
  [file ^js res] (.. res (status 404) (render "404")))

(defn serve-200
  "Serve a page with result 200."
  ([template ^js res] (.. res (status 200) (render template)))
  ([template ^js res obj]
   (.. res (status 200) (render template obj))))



(defn index-information
  "Make a JS object for use in index.handlebars."
  [window-list]
  (clj->js (merge window-list
                  {:bkgScript (.join path "/site-bkgs/bin/" (rand-nth @all-bkg-scripts))
                   :comment (.join path "comments/" (rand-nth comments-list))})))


(defn json-create-windows
  "Create a windows vector from JSON file."
  [json-path]
  (let [^js obj (.parse js/JSON (.readFileSync fs json-path "utf8"))]
    (create-windows
     (vec
      (for [^js win (.-wins obj)]
        (vec
         (for [^js cmd (.-cmds win)]
           (cond
             (= (.-type cmd) "cat") (create-command (.-where cmd)
                                                    (when (.-trim cmd) true))
             (= (.-type cmd) "ls") (create-command (.dirname path json-path)
                                                   (if (.-ext cmd) (.-ext cmd) "")
                                                   (.-what cmd)
                                                   (when (.-where cmd) (.-where cmd)))))))))))
(defn get-mtime
  "Get time (ms) when the file at file-path was updated."
  [file-path]
  (.-mtimeMs (.statSync fs file-path)))

(defn update-files!
  "If a list of files has changed on disk and it's been five minutes then re-read them."
  [file-list collection-path update-func]
  (reset! file-list
          (let [time-ms (.getTime (get @file-list :when))]
            (if (and (>= (- time-ms (.getTime (js/Date.))) item-update-time)
                     (> (get-mtime collection-path) time-ms))
              ;; Update the entire list, new post added, post removed, name change, etc..
              (update-func collection-path)
              ;; Something was added to.
              (vec (for [file (get file-list :content)
                         :let [realpath (get file :realpath)]]
                     (if (> (get-mtime realpath time-ms))
                       (create-lstat realpath)
                       file)))))))

(defonce update-post-items (fn [dir-path] {:when (js/Date.) :content (ls-dir dir-path ".handlebars")}))

(defn init-server 
  "Set the server's routes."
  []
  (println "Starting server...")
  (let [server (express)]
    ;; Server settings.
    (.use server (.static express (.join path (.cwd process) "public")))
    (.use server (.static express (.join path (.cwd process) "external")))
    (.use server (.json express))
    (.engine server "handlebars" (engine (clj->js {:defaultLayout "main"})))
    (.set server "view engine" "handlebars")
    (.set server "views" (.join path (.cwd process) "views"))


    ;; Server paths.
    (.get server "/posts/:post" (fn [^js req res next]
                                  (let [post (.toLowerCase (.-post (.-params req)))]
                                    (if (some #(= post (get % :basename)) (get @post-items :content))
                                      (serve-200 "index" res (index-information
                                                              (create-windows [[(create-command (.join path "content/posts" post) true)]])))
                                      (serve-404 post res)))))

    (.get server "/posts" (fn [^js req res next]
                            (update-files! post-items "./views/partials/content/posts" update-post-items)
                            (serve-200 "index" res (index-information @post-windows))))
    (.get server "/:item" (fn [^js req res next]
                            (let [item (.toLowerCase (.-item (.-params req)))]
                              ;; TODO fix file stat situation.
                              (if (some #(= item (get % :basename)) (ls-list "./views/partials/content/" ".handlebars" ["software" "harmful"]))
                                (serve-200 "index" res (index-information (create-windows [[(create-command (.join path "content/" item) true)]])))
                                (serve-404 item res)))))
    (.get server "/" (fn [^js req res next]
                       (serve-200 "index" res (index-information @index-items))))
    (.get server "*" (fn [^js req res next] (serve-404 "Sneed" res)))
    (.listen server 3000 (fn [] (println "Starting server on port 3000")))))

(defn start!
  "Start the server."
  []
  (reset! app (init-server))
  (reset! post-items (update-post-items "./views/partials/content/posts"))
  (reset! post-windows (create-windows [[(create-ls "posts" (get @post-items :content) "posts")]]))
  ;; TODO put these in a json object. 
  (reset! index-items (json-create-windows "./views/partials/content/index.json"))

  (reset! all-bkg-scripts (let [files (.readdirSync fs "./external/site-bkgs/bin/")]
                            (for [file files
                                  :when (and (= (.extname path file) ".js") (not= file "backs.js"))]
                              file))))

(defn main!
  "Main function"
  []
  (start!))

(defn reload!
  "Stop the server."
  []
  (.close @app)
  (reset! app nil)
  (start!))

