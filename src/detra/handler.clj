(ns detra.handler
  (:require [compojure.core :refer [defroutes GET]]
            [detra.routes.home :refer [home-routes]]
            [detra.middleware :as middleware]
            [noir.util.middleware :refer [app-handler]]
            [compojure.route :as route]
            [taoensso.timbre :as timbre]
            [taoensso.timbre.appenders.rotor :as rotor]
            [selmer.parser :as parser]
            [environ.core :refer [env]]
            [detra.layout :as layout]
            [detra.routes.auth :refer [auth-routes]]
            [detra.db.schema :as schema]
            [detra.routes.cljsexample :refer [cljs-routes]]))



(defn main-page []
  (layout/render "main.html"))

(defroutes
  app-routes
  (GET "/main" [] (main-page))
  (route/resources "/")
  (route/not-found "Not Found"))

(defn init
  "init will be called once when\r
   app is deployed as a servlet on\r
   an app server such as Tomcat\r
   put any initialization code here"
  []
  (timbre/set-config!
    [:appenders :rotor]
    {:min-level :info,
     :enabled? true,
     :async? false,
     :max-message-per-msecs nil,
     :fn rotor/appender-fn})
  (timbre/set-config!
    [:shared-appender-config :rotor]
    {:path "detra.log", :max-size (* 512 1024), :backlog 10})
  (if (env :dev) (parser/cache-off!))
  (if-not (schema/initialized?) (schema/create-tables))
  (timbre/info "detra started successfully"))

(defn destroy
  "destroy will be called when your application\r
   shuts down, put any clean up code here"
  []
  (timbre/info "detra is shutting down..."))

(def app
 (app-handler
   [cljs-routes auth-routes home-routes app-routes]
   :middleware
   [middleware/template-error-page middleware/log-request]
   :access-rules
   []
   :formats
   [:json-kw :edn]))

