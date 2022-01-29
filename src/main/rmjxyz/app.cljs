(ns rmjxyz.app
  (:require   [cljs.nodejs :as nodejs]
              ["express" :as express]
              ["express-handlebars" :as exphbs]
              [cljs.core.async :refer-macros [go]]
              [cljs.pprint :refer [pprint]]
              [cljs.core.async.interop :refer-macros [<p!]]))

(println "Initializing server...")
(defonce app (atom nil))
(defonce index-windows (atom nil))
(defonce fs (js/require "fs"))
(defonce path (js/require "path"))
(defonce permStrings ["---", "--x", "-w-", "-wx", "r--", "r-x", "rw-", "rwx"])
(defonce mons [ "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul",
                "Aug", "Sep", "Oct", "Nov", "Dec" ])

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
  ([dir ext names])
  ;; Cat.
  ([path args]))

(defn create-windows
  "Create the window data for the site."
  []
  (set! index-windows []))

(defn init-server 
  "Set the server's routes."
  []
  (println "Starting server...")
  (let [server (express)]
    (.engine server "handlebars" (exphbs (js-obj "defaultlayout" "main")))
    (.get server "/" (fn [req res next] (.render (.status res 200) "index"
                                              (js-obj))))
    (.listen server 3000 (fn [] (println "Starting server on port 3000")))))

(defn start!
  "Start the server."
  []
  (reset! app (init-server)))

(defn main!
  "Main function"
  []
 ; (start!)
  )

(defn reload!
  "Stop the server."
  []
  (.close @app)
  (reset! app nil)
  (start!))

