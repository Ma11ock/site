(ns rmjxyz.app
  (:require   [cljs.nodejs :as nodejs]
              ["express" :as express]
              ["express-handlebars" :refer [engine]]
              [cljs.core.async :refer-macros [go]]
              [cljs.pprint :refer [pprint]]
              [cljs.core.async.interop :refer-macros [<p!]]))

(println "Initializing server...")
(defonce app (atom nil))
(defonce index-windows (atom nil))
(defonce fs (js/require "fs"))
(defonce path (js/require "path"))
(defonce process (js/require "process"))
(defonce permStrings ["---", "--x", "-w-", "-wx", "r--", "r-x", "rw-", "rwx"])
(defonce mons [ "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul",
                "Aug", "Sep", "Oct", "Nov", "Dec" ])
(defonce post-items (atom nil))
(defonce index-items (atom nil))

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
    (str (mon-by-index (.getMonth file-date)) " " (.getDate file-date)
         ;; If the file was updated this year then set the last column to the
         ;; hour and minute. Else, the last column should be the year.
         (if (= (compare (.getFullYear file-date) (.getFullYear (js/Date.))) 0)
           (str (.getHours file-date) ":" (.getMinutes file-date))
           (str (.getFullYear file-date))))))

(defn create-lstat
  "Create an LSStat object for use in rendering."
  [path]
  (let [stats (.statSync fs path)
        unixFilePerms (if stats(.toString (bit-and (.-mode stats) (js/parseInt "777")))
                          nil)]
    (if stats
      (js-obj
       "perms" (str (if (.isDirectory stats) "d" "-")
                    (permissions-to-string (js/parseInt (first unixFilePerms)))
                    (permissions-to-string (js/parseInt (second unixFilePerms)))
                    (permissions-to-string (js/parseInt (nth unixFilePerms 2))))
       "numLinks" (.-nlink stats)
       "fileSize" (.-size stats)
       "mtime" (ls-time (.-mtimeMs stats))
       "basename" path))))

(defn ls-list
  "Create a list of ls-stats from a list of file paths."
  ([paths]
   (for [file paths]
     (create-lstat file)))
  ([basedir ext paths]
   (for [file paths]
     (create-lstat (.join path basedir (str file ext))))))

(defn ls-dir
  "Create a list of ls-stats from a directory."
  [dir-path]
  (when (and (.existsSync fs dir-path) (.isDirectory (.lstatSync fs dir-path)))
    (ls-list (.readdirSync fs dir-path))))

(defn create-command
  "Create a command object for rendering in the website."
  ;; LS list.
  ([dir ext paths] (js-obj "args" dir
                           "lsList" (ls-list dir ext paths)))
  ;; Cat.
  ([path] (js-obj "args" path
                  "markup" (.readFileSync fs path "utf8"))))

(defn create-windows
  "Create the window data for the site."
  [commands]
  (js-obj "commands" commands))

(defn serve-404
  "Serve the 404 page from path to res."
  [file res] (.render (.status res 404) "404"))

(defn serve-200
  "Serve a page with result 200."
  ([template res] (.render (.status res 200) template))
  ([template res obj] (.render (.status res 200) template obj)))


(defn serve-file-to
  "Serve file to res asynchronously."
  [file res]
  (go
    (try
      (<p! (.readFile fs file "utf8" (fn [err buf]
                                       (if err
                                         (js/console.log "Error when looking for item " file)
                                         (serve-200 "post" res (js-obj "text" buf))))))
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
    (.engine server "handlebars" (engine (js-obj "defaultlayout" "main")))
    (.set server "view engine" "handlebars")
    (.set server "views" "./views")

    ;; Server paths.
    (.get server "/posts/:post" (fn [req res next]
                                  (let [post (.toLowerCase (.-item (.-params req)))]
                                    (if (some #(= post %) post-items)
                                      (serve-file-to post res)
                                      (serve-404 post res)))))
    (.get server "/posts" (fn [req res next]
                                  ))
    (.get server "/:item" (fn [req res next]
                            (let [item (.toLowerCase (.-item (.-params req)))]
                              (if (some #(= item %) (ls-list "." ".html" ["main software" "sneed"]))
                                (serve-file-to item res)
                                (serve-404 item res)))))
    (.get server "/" (fn [req res next]
                       (serve-200 "index" res index-items)))
    (.get server "*" (fn [req res next] (.toLowerCase (.-item (.-params req)) res)))
    (.listen server 3000 (fn [] (println "Starting server on port 3000")))))

(defn start!
  "Start the server."
  []
  (reset! app (init-server))
  (reset! post-items (ls-dir "posts"))
  (reset! index-items (create-windows [(create-command "public/figlet.html")
                                       (create-command "." ".html" ["main" "software" "sneed"])
                                       (create-command "public/front.html")])))

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

