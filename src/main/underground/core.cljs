(ns underground.core
  (:require ["express" :as express]
            ["express-handlebars" :refer [engine]]))

(set! *warn-on-infer* true)

(defonce server (atom nil))

(defn start-server []
  (println "Starting server")
  (let [app (express)]
    (.engine app "handlebars" (engine))
    (.set app "view engine", "handlebars")
    (.get app "/" (fn [req res] (.render res "home")))
    (.use app (.static express "public"))
    (.listen app 3000 (fn [] (println "Listening on port 3000...")))))

(defn start! []
  ;; called by main and after reloading code
  (reset! server (start-server)))

(defn stop! []
  ;; called before reloading code
  (.close @server)
  (reset! server nil))

(defn main []
  ;; executed once, on startup, can do one time setup here
  (start!))
