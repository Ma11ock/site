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

(defn mon-by-index
  "Get a month (abbreviation) by its index. Returns nil if i is out of range."
  [i]
  (nth mons i nil))

(defn permissions-to-string
  "Convert Unix file permission mode to string. Return nil on invalid mode."
  [mode]
  (nth permStrings mode nil))

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
      { :perms (str (if (.isDirectory stats) "d" "-")
                     (permissions-to-string (js/parseInt (first unixFilePerms)))
                     (permissions-to-string (js/parseInt (second unixFilePerms)))
                     (permissions-to-string (js/parseInt (nth unixFilePerms 2))))
       :numLinks (.-nlink stats)
       :fileSize (gstring/format "%4d" (.-size stats))
       :mtime (ls-time (.-mtimeMs stats))
       :basename (.-name (.parse path (.basename path file-path))) }
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
  ([dir ext paths] {"args" dir
                    "lsList" (ls-list dir ext paths)})
  ;; Cat.
  ([the-path] {"args" the-path
           "markup" (.-name (.parse path the-path))}))

(defn create-ls
  "Create a ls-listing from a pre-existing set of files."
  [dir ls-list]
  {"args" dir "lsList" ls-list})

(defn create-windows
  "Create the window data for the site."
  [commands-list]
  {"windows" (for [cmds commands-list]
                {"commands" cmds})})

(defn serve-404
  "Serve the 404 page from path to res."
  [file ^js res] (.. res (status 404)))

(defn serve-200
  "Serve a page with result 200."
  ([template ^js res] (.. res (status 200) (render template)))
   ([template ^js res obj]
    (.. res (status 200) (render template obj))))


(defn serve-file-to
  "Serve file to res asynchronously."
  [file res]
  (go
    (try
      (<p! (.readFile fs file "utf8" (fn [err buf]
                                       (if err
                                         (js/console.log "Error when looking for item " file)
                                         (serve-200 "post" res #js{ :text buf })))))
      ;; TOTO internal error.
      (catch js/Error err (js/console.error err)))))

(defn init-server 
  "Set the server's routes."
  []
  (println "Starting server...")
  (let [server (express)]
    ;; Server settings.
    (.use server (.static express (.join path (.cwd process) "public")))
    (.use server (.static express (.join path (.cwd process) "external")))
    (.use server (.json express))
    (.engine server "handlebars" (engine (clj->js { :defaultLayout "main" })))
    (.set server "view engine" "handlebars")
    (.set server "views" "./views")

    ;; Server paths.
    (.get server "/posts/:post" (fn [^js req res next]
                                  (let [post (.toLowerCase (.-post (.-params req)))]
                                    (if (some #(= post %) (get @post-items :content))
                                      (serve-200 "index" res (clj->js (merge )))
                                      (serve-404 post res)))))
    (.get server "/posts" (fn [^js req res next]
                            (serve-200 "index" res (clj->js (merge @post-windows
                                                                   {:bkgScript (.join path "/site-bkgs/bin/" (rand-nth (deref all-bkg-scripts)))})))))
    (.get server "/:item" (fn [^js req res next]
                            (let [item (.toLowerCase (.-item (.-params req)))]
                              (if (some #(= item %) (ls-list "." ".html" ["software"]))
                                (serve-file-to item res)
                                (serve-404 item res)))))
    (.get server "/" (fn [^js req res next]
                       (serve-200 "index" res (clj->js (merge @index-items
                                                              {:bkgScript (.join path "/site-bkgs/bin/" (rand-nth (deref all-bkg-scripts)))})))))
    (.get server "*" (fn [^js req res next] (serve-404 "Sneed" res)))
    (.listen server 3000 (fn [] (println "Starting server on port 3000")))))

(defn start!
  "Start the server."
  []
  (reset! app (init-server))
  (reset! post-items {:when (js/Date.) :content (ls-dir "./content/partials/posts" ".handlebars")})
  (reset! post-windows (create-windows [[(create-ls "posts" (get @post-items :content))]]))
  ;; TODO put these in a json object. 
  (reset! index-items (create-windows [[(create-command "./content/partials/figlet.handlebars")
                                        (create-command "./content/partials" "" ["software.handlebars" "posts"])
                                        (create-command "./content/partials/front.handlebars")]]))
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

